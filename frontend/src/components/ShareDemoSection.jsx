import React, { useEffect, useMemo, useState } from 'react';
import {
  CheckCircle,
  Download,
  Loader,
  LockKeyhole,
  LogIn,
  LogOut,
  MessageSquare,
  Send,
  ShieldCheck,
  Unlock,
  UserPlus,
  XCircle,
} from 'lucide-react';
import ImageUpload from './ImageUpload';
import api from '../services/api';

const TOKEN_KEY = 'stegacrypt-auth-token';
const MEMBER_REFRESH_EVENT = 'stegacrypt-members-updated';
const MEMBER_REFRESH_MS = 5000;

function ShareDemoSection() {
  const [token, setToken] = useState(() => window.localStorage.getItem(TOKEN_KEY) || '');
  const [currentUser, setCurrentUser] = useState(null);
  const [availableMembers, setAvailableMembers] = useState([]);
  const [members, setMembers] = useState([]);
  const [seededMembers, setSeededMembers] = useState([]);
  const [messages, setMessages] = useState([]);
  const [recipientUsernames, setRecipientUsernames] = useState([]);
  const [message, setMessage] = useState('');
  const [imageFile, setImageFile] = useState(null);
  const [imagePreview, setImagePreview] = useState(null);
  const [useCompression, setUseCompression] = useState(true);
  const [loginForm, setLoginForm] = useState({ username: '', password: 'Stega@123' });
  const [registerForm, setRegisterForm] = useState({ fullName: '', username: '', password: '' });
  const [loadingChat, setLoadingChat] = useState(false);
  const [sending, setSending] = useState(false);
  const [authLoading, setAuthLoading] = useState(false);
  const [decryptingId, setDecryptingId] = useState('');
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);

  const selectedRecipients = useMemo(
    () => members.filter((member) => recipientUsernames.includes(member.username)),
    [members, recipientUsernames]
  );

  useEffect(() => {
    if (!token) {
      return;
    }
    hydrateChat(token);
  }, [token]);

  useEffect(() => {
    let active = true;

    const loadMembers = async () => {
      try {
        const result = await api.getSecureChatMembers();
        if (!active) {
          return;
        }
        setAvailableMembers(result.members || []);
      } catch (err) {
        console.error('Failed to load member directory:', err);
      }
    };

    const handleRefresh = () => {
      loadMembers();
    };

    loadMembers();
    window.addEventListener(MEMBER_REFRESH_EVENT, handleRefresh);
    const intervalId = window.setInterval(loadMembers, MEMBER_REFRESH_MS);

    return () => {
      active = false;
      window.removeEventListener(MEMBER_REFRESH_EVENT, handleRefresh);
      window.clearInterval(intervalId);
    };
  }, []);

  const broadcastMemberRefresh = () => {
    window.dispatchEvent(new Event(MEMBER_REFRESH_EVENT));
  };

  const hydrateChat = async (sessionToken) => {
    setLoadingChat(true);
    setError(null);

    try {
      const data = await api.getSecureChatBootstrap(sessionToken);
      setCurrentUser(data.currentUser);
      setMembers(data.members || []);
      setSeededMembers(data.seededMembers || []);
      setMessages(data.messages || []);
      setRecipientUsernames((current) => {
        const nextMembers = data.members || [];
        return current.filter((username) =>
          nextMembers.some((member) => member.username === username)
        );
      });
      window.localStorage.setItem(TOKEN_KEY, sessionToken);
      broadcastMemberRefresh();
    } catch (err) {
      console.error('Chat bootstrap failed:', err);
      setError(await api.getErrorMessage(err, 'Failed to load secure chat.'));
      handleLogout(false);
    } finally {
      setLoadingChat(false);
    }
  };

  const handleImageSelect = (file) => {
    setImageFile(file);
    setError(null);

    const reader = new FileReader();
    reader.onload = (event) => setImagePreview(event.target?.result || null);
    reader.readAsDataURL(file);
  };

  const handleLogin = async () => {
    setAuthLoading(true);
    setError(null);
    setSuccess(null);

    try {
      const result = await api.login(loginForm.username, loginForm.password);
      setToken(result.token);
      setSuccess(`Logged in as ${result.user.fullName}.`);
      broadcastMemberRefresh();
    } catch (err) {
      console.error('Login failed:', err);
      setError(await api.getErrorMessage(err, 'Login failed.'));
    } finally {
      setAuthLoading(false);
    }
  };

  const handleRegister = async () => {
    setAuthLoading(true);
    setError(null);
    setSuccess(null);

    try {
      const result = await api.register(registerForm);
      setToken(result.token);
      setRegisterForm({ fullName: '', username: '', password: '' });
      setSuccess(`Account created for ${result.user.fullName}.`);
      broadcastMemberRefresh();
    } catch (err) {
      console.error('Registration failed:', err);
      setError(await api.getErrorMessage(err, 'Registration failed.'));
    } finally {
      setAuthLoading(false);
    }
  };

  const handleLogout = (clearSuccess = true) => {
    window.localStorage.removeItem(TOKEN_KEY);
    setToken('');
    setCurrentUser(null);
    setMembers([]);
    setMessages([]);
    setRecipientUsernames([]);
    setImageFile(null);
    setImagePreview(null);
    broadcastMemberRefresh();
    if (clearSuccess) {
      setSuccess(null);
    }
  };

  const handleSend = async () => {
    if (!token) {
      setError('Please log in first.');
      return;
    }
    if (recipientUsernames.length === 0) {
      setError('Please choose at least one recipient.');
      return;
    }
    if (!imageFile) {
      setError('Please choose a carrier image.');
      return;
    }
    if (!message.trim()) {
      setError('Please type a message to share.');
      return;
    }

    setSending(true);
    setError(null);
    setSuccess(null);

    try {
      await api.sendSecureChatMessage(token, {
        recipientUsernames,
        message,
        useCompression,
        imageFile,
      });
      setMessage('');
      setImageFile(null);
      setImagePreview(null);
      setSuccess(`Secure stego message sent to ${selectedRecipients.length} recipient${selectedRecipients.length === 1 ? '' : 's'}.`);
      await hydrateChat(token);
    } catch (err) {
      console.error('Send failed:', err);
      setError(await api.getErrorMessage(err, 'Failed to send secure message.'));
    } finally {
      setSending(false);
    }
  };

  const handleDecrypt = async (shareId) => {
    setDecryptingId(`${shareId}`);
    setError(null);

    try {
      await api.decryptSecureChatMessage(token, shareId);
      await hydrateChat(token);
    } catch (err) {
      console.error('Decrypt failed:', err);
      setError(await api.getErrorMessage(err, 'Failed to decrypt secure share.'));
    } finally {
      setDecryptingId('');
    }
  };

  const downloadShare = (share) => {
    const link = document.createElement('a');
    link.href = `data:image/png;base64,${share.stegoImageBase64}`;
    link.download = `chat_share_${share.id}.png`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  if (!token || !currentUser) {
    return (
      <div className="section">
        <h2 className="section-title">Secure Chat Login and Registration</h2>

        <div className="auth-layout">
          <div className="auth-card">
            <div className="auth-card-title">
              <LogIn size={20} />
              <h3>Login</h3>
            </div>
            <input
              className="input"
              placeholder="Username"
              value={loginForm.username}
              onChange={(event) => setLoginForm((current) => ({ ...current, username: event.target.value }))}
            />
            <input
              className="input auth-input-gap"
              type="password"
              placeholder="Password"
              value={loginForm.password}
              onChange={(event) => setLoginForm((current) => ({ ...current, password: event.target.value }))}
            />
            <button className="btn btn-primary auth-button" onClick={handleLogin} disabled={authLoading}>
              {authLoading ? <Loader size={18} className="spin" /> : <LogIn size={18} />}
              <span>Login</span>
            </button>
          </div>

          <div className="auth-card">
            <div className="auth-card-title">
              <UserPlus size={20} />
              <h3>Register</h3>
            </div>
            <input
              className="input"
              placeholder="Full name"
              value={registerForm.fullName}
              onChange={(event) => setRegisterForm((current) => ({ ...current, fullName: event.target.value }))}
            />
            <input
              className="input auth-input-gap"
              placeholder="Username"
              value={registerForm.username}
              onChange={(event) => setRegisterForm((current) => ({ ...current, username: event.target.value }))}
            />
            <input
              className="input auth-input-gap"
              type="password"
              placeholder="Password"
              value={registerForm.password}
              onChange={(event) => setRegisterForm((current) => ({ ...current, password: event.target.value }))}
            />
            <button className="btn btn-secondary auth-button" onClick={handleRegister} disabled={authLoading}>
              {authLoading ? <Loader size={18} className="spin" /> : <UserPlus size={18} />}
              <span>Create Account</span>
            </button>
          </div>
        </div>

        <div className="info-box subtle-box">
          <h4>Available members</h4>
          <p className="field-hint">
            Seeded project accounts use password <strong>Stega@123</strong>. Newly registered accounts appear here automatically.
          </p>
          <div className="member-list">
            {availableMembers.map((member) => (
              <div key={member.username} className="member-chip">
                <strong>{member.fullName}</strong>
                <span>{member.username}{member.seeded ? ' - seeded' : ' - registered'}</span>
              </div>
            ))}
          </div>
        </div>

        {error && (
          <div className="alert alert-error">
            <XCircle size={20} />
            <span>{error}</span>
          </div>
        )}

        {success && (
          <div className="alert alert-success">
            <CheckCircle size={20} />
            <span>{success}</span>
          </div>
        )}
      </div>
    );
  }

  return (
    <div className="section">
      <h2 className="section-title">Secure Chat Sharing</h2>

      <div className="chat-header-card">
        <div>
          <p className="chat-header-label">Logged in as</p>
          <h3>{currentUser.fullName}</h3>
          <p className="field-hint">@{currentUser.username}</p>
        </div>
        <button className="btn btn-secondary" onClick={() => handleLogout()}>
          <LogOut size={18} />
          <span>Logout</span>
        </button>
      </div>

      <div className="info-box subtle-box">
        <h4>Chat flow</h4>
        <ol>
          <li>Login as one member and choose one or more registered members as recipients.</li>
          <li>Upload a carrier image and send the hidden message through the chat.</li>
          <li>Each selected recipient logs in and decrypts their own stego share from the chat timeline.</li>
        </ol>
      </div>

      {error && (
        <div className="alert alert-error">
          <XCircle size={20} />
          <span>{error}</span>
        </div>
      )}

      {success && (
        <div className="alert alert-success">
          <CheckCircle size={20} />
          <span>{success}</span>
        </div>
      )}

      <div className="chat-layout">
        <div className="chat-compose-card">
          <div className="auth-card-title">
            <ShieldCheck size={20} />
            <h3>Compose Secure Share</h3>
          </div>

          <div className="form-group">
            <label>Recipients</label>
            <div className="recipient-checkbox-list">
              {members.map((member) => (
                <label key={member.username} className="checkbox-label">
                  <input
                    type="checkbox"
                    checked={recipientUsernames.includes(member.username)}
                    onChange={(event) => {
                      setRecipientUsernames((current) =>
                        event.target.checked
                          ? [...current, member.username]
                          : current.filter((username) => username !== member.username)
                      );
                    }}
                  />
                  <span>{member.fullName} (@{member.username})</span>
                </label>
              ))}
            </div>
          </div>

          <div className="form-group">
            <label>Carrier Image</label>
            <ImageUpload onImageSelect={handleImageSelect} preview={imagePreview} />
          </div>

          <div className="form-group">
            <label>Secret Message</label>
            <textarea
              className="textarea"
              rows={6}
              value={message}
              onChange={(event) => setMessage(event.target.value)}
              placeholder="Send the hidden message that should be visible only to the selected recipients."
            />
          </div>

          <label className="checkbox-label">
            <input
              type="checkbox"
              checked={useCompression}
              onChange={(event) => setUseCompression(event.target.checked)}
            />
            <span>Enable compression before encryption</span>
          </label>

          <div className="button-group">
            <button className="btn btn-primary" onClick={handleSend} disabled={sending || loadingChat}>
              {sending ? <Loader size={18} className="spin" /> : <Send size={18} />}
              <span>{sending ? 'Sending...' : 'Send Secure Chat Share'}</span>
            </button>
          </div>
        </div>

        <div className="chat-timeline-card">
          <div className="auth-card-title">
            <MessageSquare size={20} />
            <h3>Chat Timeline</h3>
          </div>

          {loadingChat ? (
            <div className="alert">
              <Loader size={18} className="spin" />
              <span>Loading chat messages...</span>
            </div>
          ) : messages.length === 0 ? (
            <div className="info-box">
              <h4>No secure chats yet</h4>
              <p className="field-hint">Send the first stego message to start the conversation.</p>
            </div>
          ) : (
            <div className="chat-timeline">
              {messages.map((share) => (
                <article key={share.id} className={`chat-bubble ${share.mine ? 'chat-bubble-mine' : 'chat-bubble-theirs'}`}>
                  <div className="chat-bubble-top">
                    <strong>{share.mine ? `You to ${share.recipientName}` : `${share.senderName} to You`}</strong>
                    <span>{new Date(share.createdAt).toLocaleString()}</span>
                  </div>
                  <p className="share-preview-text">{share.messagePreview}</p>
                  <p className="field-hint">
                    Hidden inside PNG stego image {share.canDecrypt ? 'for your account' : 'sent from your account'}.
                  </p>

                  <div className="button-group compact-buttons">
                    <button className="btn btn-success" onClick={() => downloadShare(share)}>
                      <Download size={18} />
                      <span>Download PNG</span>
                    </button>

                    {share.canDecrypt && !share.decrypted && (
                      <button
                        className="btn btn-secondary"
                        onClick={() => handleDecrypt(share.id)}
                        disabled={decryptingId === `${share.id}`}
                      >
                        {decryptingId === `${share.id}` ? <Loader size={18} className="spin" /> : <Unlock size={18} />}
                        <span>{decryptingId === `${share.id}` ? 'Decrypting...' : 'Decrypt'}</span>
                      </button>
                    )}
                  </div>

                  {share.decrypted && (
                    <div className="share-decrypted-panel">
                      <div className="alert alert-success">
                        <LockKeyhole size={18} />
                        <span>The recipient account decrypted this message successfully.</span>
                      </div>

                      <div className="message-box">
                        <p>{share.decryptedMessage}</p>
                      </div>

                      {share.decryptInfo && (
                        <div className="extract-info">
                          <p>Message Length: {share.decryptInfo.messageLength} characters</p>
                          <p>Embedded Payload Size: {share.decryptInfo.encryptedSize} bytes</p>
                          <p>RSA Wrapped Session Key: {share.decryptInfo.wrappedKeyLength} bytes</p>
                          <p>Compression Used: {share.decryptInfo.usedCompression ? 'Yes' : 'No'}</p>
                        </div>
                      )}
                    </div>
                  )}
                </article>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

export default ShareDemoSection;

import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

class StegaCryptAPI {
  async generateKeyPair() {
    const response = await axios.post(`${API_BASE_URL}/generate-keys`);
    return response.data;
  }

  async embedMessage(imageFile, message, publicKey, useCompression = true) {
    const formData = new FormData();
    formData.append('image', imageFile);
    formData.append('message', message);
    formData.append('publicKey', publicKey);
    formData.append('useCompression', useCompression);

    const response = await axios.post(`${API_BASE_URL}/embed`, formData, {
      responseType: 'blob',
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });

    return response.data;
  }

  async extractMessage(imageFile, privateKey) {
    const formData = new FormData();
    formData.append('image', imageFile);
    formData.append('privateKey', privateKey);

    const response = await axios.post(`${API_BASE_URL}/extract`, formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });

    return response.data;
  }

  async checkCapacity(imageFile) {
    const formData = new FormData();
    formData.append('image', imageFile);

    const response = await axios.post(`${API_BASE_URL}/capacity`, formData);
    return response.data;
  }

  async healthCheck() {
    const response = await axios.get(`${API_BASE_URL}/health`);
    return response.data;
  }

  async getDemoUsers() {
    const response = await axios.get(`${API_BASE_URL}/demo-users`);
    return response.data;
  }

  async login(username, password) {
    const formData = new FormData();
    formData.append('username', username);
    formData.append('password', password);
    const response = await axios.post(`${API_BASE_URL}/auth/login`, formData);
    return response.data;
  }

  async register({ fullName, username, password }) {
    const formData = new FormData();
    formData.append('fullName', fullName);
    formData.append('username', username);
    formData.append('password', password);
    const response = await axios.post(`${API_BASE_URL}/auth/register`, formData);
    return response.data;
  }

  async getSecureChatBootstrap(token) {
    const response = await axios.get(`${API_BASE_URL}/auth/chat`, {
      headers: {
        'X-Auth-Token': token,
      },
    });
    return response.data;
  }

  async sendSecureChatMessage(token, { recipientUsernames, message, useCompression, imageFile }) {
    const formData = new FormData();

    recipientUsernames.forEach((username) => {
      formData.append('recipientUsernames', username);
    });

    formData.append('message', message);
    formData.append('useCompression', useCompression);
    formData.append('image', imageFile);

    const response = await axios.post(`${API_BASE_URL}/auth/chat/send`, formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
        'X-Auth-Token': token,
      },
    });
    return response.data;
  }

  async decryptSecureChatMessage(token, shareId) {
    const response = await axios.post(`${API_BASE_URL}/auth/chat/decrypt/${shareId}`, null, {
      headers: {
        'X-Auth-Token': token,
      },
    });
    return response.data;
  }

  async getSecureChatMembers() {
    const response = await axios.get(`${API_BASE_URL}/auth/members`);
    return response.data;
  }

  async extractSharedImage(imageFile, recipientUsername, senderUsername) {
    const formData = new FormData();
    formData.append('image', imageFile);
    formData.append('recipientUsername', recipientUsername);
    if (senderUsername) {
      formData.append('senderUsername', senderUsername);
    }

    const response = await axios.post(`${API_BASE_URL}/auth/extract-shared-image`, formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return response.data;
  }

  async getErrorMessage(error, fallbackMessage) {
    const responseData = error?.response?.data;

    if (responseData instanceof Blob) {
      try {
        const text = await responseData.text();
        const parsed = JSON.parse(text);
        return parsed.message || fallbackMessage;
      } catch {
        return fallbackMessage;
      }
    }

    return responseData?.message || fallbackMessage;
  }
}

export default new StegaCryptAPI();


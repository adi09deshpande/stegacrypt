package com.stegacrypt.service;

import com.stegacrypt.util.AESUtil;
import com.stegacrypt.util.RSAUtil;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class AuthChatService {

    private static final String DEFAULT_PASSWORD = "Stega@123";

    private final Map<String, UserAccount> usersByUsername = new ConcurrentHashMap<>();
    private final Map<String, String> tokenToUsername = new ConcurrentHashMap<>();
    private final Map<Long, ChatShare> sharesById = new ConcurrentHashMap<>();
    private final AtomicLong shareIdSequence = new AtomicLong(1);

    public AuthChatService() throws Exception {
        seedMember("Abhishek Sushant Chaskar");
        seedMember("Aditya Atul Deshpande");
        seedMember("Tanvi Dongare");
        seedMember("Atharva Abhijeet Mahalkar");
        seedMember("Prakhar Kumar Vishawakarma");
    }

    public Map<String, Object> login(String username, String password) throws Exception {
        UserAccount user = usersByUsername.get(normalizeUsername(username));
        if (user == null || !user.passwordHash().equals(hashPassword(password))) {
            throw new IllegalArgumentException("Invalid username or password.");
        }

        String token = UUID.randomUUID().toString();
        tokenToUsername.put(token, user.username());
        return buildAuthResponse(user, token, "Login successful.");
    }

    public Map<String, Object> register(String fullName, String username, String password) throws Exception {
        String normalizedUsername = normalizeUsername(username);
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("Full name is required.");
        }
        if (normalizedUsername.length() < 4) {
            throw new IllegalArgumentException("Username must be at least 4 characters.");
        }
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters.");
        }
        if (usersByUsername.containsKey(normalizedUsername)) {
            throw new IllegalArgumentException("That username is already registered.");
        }

        UserAccount account = createUser(fullName.trim(), normalizedUsername, password, false);
        usersByUsername.put(account.username(), account);

        String token = UUID.randomUUID().toString();
        tokenToUsername.put(token, account.username());
        return buildAuthResponse(account, token, "Registration successful.");
    }

    public Map<String, Object> getChatBootstrap(String token) {
        UserAccount currentUser = requireUser(token);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("currentUser", buildUserProfile(currentUser));
        response.put("members", getVisibleUsers(currentUser.username()));
        response.put("seededMembers", getSeededMembers());
        response.put("defaultPassword", DEFAULT_PASSWORD);
        response.put("messages", getVisibleShares(currentUser.username()));
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }

    public Map<String, Object> sendShare(
        String token,
        String recipientUsername,
        String message,
        boolean useCompression,
        byte[] stegoImageBytes
    ) {
        UserAccount sender = requireUser(token);
        UserAccount recipient = usersByUsername.get(normalizeUsername(recipientUsername));

        if (recipient == null) {
            throw new IllegalArgumentException("Recipient account was not found.");
        }
        if (recipient.username().equals(sender.username())) {
            throw new IllegalArgumentException("Choose another member to receive the secure share.");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Message is required.");
        }
        if (stegoImageBytes == null || stegoImageBytes.length == 0) {
            throw new IllegalArgumentException("Stego image data is missing.");
        }

        long shareId = shareIdSequence.getAndIncrement();
        ChatShare share = new ChatShare(
            shareId,
            sender.username(),
            sender.fullName(),
            recipient.username(),
            recipient.fullName(),
            message.trim(),
            useCompression,
            Base64.getEncoder().encodeToString(stegoImageBytes),
            Instant.now().toString(),
            false,
            null,
            null
        );

        sharesById.put(shareId, share);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("message", "Secure share sent successfully.");
        response.put("share", buildSharePayload(share, sender.username()));
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }

    public Map<String, Object> sendShares(String token, String message, List<PreparedShare> preparedShares) {
        UserAccount sender = requireUser(token);
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Message is required.");
        }
        if (preparedShares == null || preparedShares.isEmpty()) {
            throw new IllegalArgumentException("Choose at least one recipient.");
        }

        List<Map<String, Object>> shares = new ArrayList<>();
        for (PreparedShare preparedShare : preparedShares) {
            UserAccount recipient = usersByUsername.get(normalizeUsername(preparedShare.recipientUsername()));

            if (recipient == null) {
                throw new IllegalArgumentException("Recipient account was not found.");
            }
            if (recipient.username().equals(sender.username())) {
                throw new IllegalArgumentException("Choose another member to receive the secure share.");
            }
            if (preparedShare.stegoImageBytes() == null || preparedShare.stegoImageBytes().length == 0) {
                throw new IllegalArgumentException("Stego image data is missing.");
            }

            long shareId = shareIdSequence.getAndIncrement();
            ChatShare share = new ChatShare(
                shareId,
                sender.username(),
                sender.fullName(),
                recipient.username(),
                recipient.fullName(),
                message.trim(),
                preparedShare.useCompression(),
                Base64.getEncoder().encodeToString(preparedShare.stegoImageBytes()),
                Instant.now().toString(),
                false,
                null,
                null
            );

            sharesById.put(shareId, share);
            shares.add(buildSharePayload(share, sender.username()));
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("message", "Secure shares sent successfully.");
        response.put("shares", shares);
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }

    public Map<String, Object> decryptShare(
        String token,
        long shareId,
        String message,
        int encryptedSize,
        int wrappedKeyLength,
        boolean usedCompression
    ) {
        UserAccount currentUser = requireUser(token);
        ChatShare share = sharesById.get(shareId);

        if (share == null) {
            throw new IllegalArgumentException("Chat share not found.");
        }
        if (!share.recipientUsername().equals(currentUser.username())) {
            throw new IllegalArgumentException("Only the intended recipient can decrypt this share.");
        }

        ChatShare updated = share.withDecryption(
            true,
            message,
            Map.of(
                "encryptedSize", encryptedSize,
                "messageLength", message.length(),
                "wrappedKeyLength", wrappedKeyLength,
                "usedCompression", usedCompression,
                "encryptionMode", "RSA-OAEP + AES-256-GCM"
            )
        );
        sharesById.put(shareId, updated);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("share", buildSharePayload(updated, currentUser.username()));
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }

    public byte[] getShareImageBytes(String token, long shareId) {
        UserAccount currentUser = requireUser(token);
        ChatShare share = sharesById.get(shareId);

        if (share == null) {
            throw new IllegalArgumentException("Chat share not found.");
        }
        if (!share.senderUsername().equals(currentUser.username()) && !share.recipientUsername().equals(currentUser.username())) {
            throw new IllegalArgumentException("You do not have access to this share.");
        }

        return Base64.getDecoder().decode(share.stegoImageBase64());
    }

    public PublicKey getRecipientPublicKey(String username) {
        UserAccount user = usersByUsername.get(normalizeUsername(username));
        if (user == null) {
            throw new IllegalArgumentException("Recipient account was not found.");
        }
        return user.publicKey();
    }

    public List<String> normalizeRecipientUsernames(List<String> recipientUsernames) {
        if (recipientUsernames == null || recipientUsernames.isEmpty()) {
            throw new IllegalArgumentException("Choose at least one recipient.");
        }

        Set<String> normalizedRecipients = new LinkedHashSet<>();
        for (String recipientUsername : recipientUsernames) {
            String normalized = normalizeUsername(recipientUsername);
            if (!normalized.isBlank()) {
                normalizedRecipients.add(normalized);
            }
        }

        if (normalizedRecipients.isEmpty()) {
            throw new IllegalArgumentException("Choose at least one recipient.");
        }

        return List.copyOf(normalizedRecipients);
    }

    public UserAccount getUserByUsername(String username) {
        UserAccount user = usersByUsername.get(normalizeUsername(username));
        if (user == null) {
            throw new IllegalArgumentException("Account was not found.");
        }
        return user;
    }

    public List<Map<String, Object>> getAllUsers() {
        return usersByUsername.values().stream()
            .sorted(Comparator.comparing(UserAccount::fullName))
            .map(this::buildUserProfile)
            .toList();
    }

    public UserAccount requireUser(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Authentication token is required.");
        }

        String username = tokenToUsername.get(token);
        if (username == null) {
            throw new IllegalArgumentException("Your session is not valid. Please log in again.");
        }

        UserAccount user = usersByUsername.get(username);
        if (user == null) {
            throw new IllegalArgumentException("Authenticated account not found.");
        }
        return user;
    }

    private void seedMember(String fullName) throws Exception {
        String username = generateUsername(fullName);
        UserAccount account = createUser(fullName, username, DEFAULT_PASSWORD, true);
        usersByUsername.put(account.username(), account);
    }

    private UserAccount createUser(String fullName, String username, String password, boolean seeded) throws Exception {
        KeyPair keyPair = RSAUtil.generateKeyPair();
        return new UserAccount(
            fullName,
            username,
            hashPassword(password),
            seeded,
            keyPair.getPublic(),
            keyPair.getPrivate()
        );
    }

    private String hashPassword(String password) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hash);
    }

    private String normalizeUsername(String username) {
        return username == null ? "" : username.trim().toLowerCase();
    }

    private String generateUsername(String fullName) {
        String cleaned = fullName.toLowerCase().replaceAll("[^a-z0-9]+", ".");
        return cleaned.replaceAll("^\\.+|\\.+$", "");
    }

    private Map<String, Object> buildAuthResponse(UserAccount user, String token, String message) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("message", message);
        response.put("token", token);
        response.put("user", buildUserProfile(user));
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }

    private Map<String, Object> buildUserProfile(UserAccount user) {
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("fullName", user.fullName());
        profile.put("username", user.username());
        profile.put("seeded", user.seeded());
        return profile;
    }

    private List<Map<String, Object>> getVisibleUsers(String currentUsername) {
        return usersByUsername.values().stream()
            .filter(user -> !user.username().equals(currentUsername))
            .sorted(Comparator.comparing(UserAccount::fullName))
            .map(this::buildUserProfile)
            .toList();
    }

    private List<Map<String, Object>> getSeededMembers() {
        return usersByUsername.values().stream()
            .filter(UserAccount::seeded)
            .sorted(Comparator.comparing(UserAccount::fullName))
            .map(user -> {
                Map<String, Object> map = buildUserProfile(user);
                map.put("passwordHint", DEFAULT_PASSWORD);
                return map;
            })
            .toList();
    }

    private List<Map<String, Object>> getVisibleShares(String currentUsername) {
        return sharesById.values().stream()
            .filter(share -> share.senderUsername().equals(currentUsername) || share.recipientUsername().equals(currentUsername))
            .sorted(Comparator.comparing(ChatShare::createdAt).reversed())
            .map(share -> buildSharePayload(share, currentUsername))
            .toList();
    }

    private Map<String, Object> buildSharePayload(ChatShare share, String currentUsername) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", share.id());
        map.put("senderUsername", share.senderUsername());
        map.put("senderName", share.senderName());
        map.put("recipientUsername", share.recipientUsername());
        map.put("recipientName", share.recipientName());
        map.put("messagePreview", "Hidden Message");
        map.put("createdAt", share.createdAt());
        map.put("useCompression", share.useCompression());
        map.put("stegoImageBase64", share.stegoImageBase64());
        map.put("mine", share.senderUsername().equals(currentUsername));
        map.put("canDecrypt", share.recipientUsername().equals(currentUsername));
        map.put("decrypted", share.decrypted());
        map.put("decryptedMessage", share.decryptedMessage());
        map.put("decryptInfo", share.decryptInfo());
        return map;
    }

    public record UserAccount(
        String fullName,
        String username,
        String passwordHash,
        boolean seeded,
        PublicKey publicKey,
        PrivateKey privateKey
    ) {}

    public record PreparedShare(
        String recipientUsername,
        boolean useCompression,
        byte[] stegoImageBytes
    ) {}

    private record ChatShare(
        long id,
        String senderUsername,
        String senderName,
        String recipientUsername,
        String recipientName,
        String messagePlaintext,
        boolean useCompression,
        String stegoImageBase64,
        String createdAt,
        boolean decrypted,
        String decryptedMessage,
        Map<String, Object> decryptInfo
    ) {
        private ChatShare withDecryption(boolean nextDecrypted, String nextMessage, Map<String, Object> nextInfo) {
            return new ChatShare(
                id,
                senderUsername,
                senderName,
                recipientUsername,
                recipientName,
                messagePlaintext,
                useCompression,
                stegoImageBase64,
                createdAt,
                nextDecrypted,
                nextMessage,
                nextInfo
            );
        }
    }
}

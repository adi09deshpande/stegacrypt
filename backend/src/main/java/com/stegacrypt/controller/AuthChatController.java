package com.stegacrypt.controller;

import com.stegacrypt.service.AuthChatService;
import com.stegacrypt.service.CompressionService;
import com.stegacrypt.service.ImageProcessingService;
import com.stegacrypt.service.SteganographyService;
import com.stegacrypt.util.AESUtil;
import com.stegacrypt.util.RSAUtil;
import com.stegacrypt.util.ValidationUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthChatController {

    private final AuthChatService authChatService;
    private final CompressionService compressionService;
    private final ImageProcessingService imageProcessingService;
    private final SteganographyService steganographyService;

    public AuthChatController(
        AuthChatService authChatService,
        CompressionService compressionService,
        ImageProcessingService imageProcessingService,
        SteganographyService steganographyService
    ) {
        this.authChatService = authChatService;
        this.compressionService = compressionService;
        this.imageProcessingService = imageProcessingService;
        this.steganographyService = steganographyService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(
        @RequestParam("fullName") String fullName,
        @RequestParam("username") String username,
        @RequestParam("password") String password
    ) {
        try {
            return ResponseEntity.ok(authChatService.register(fullName, username, password));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error(e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
        @RequestParam("username") String username,
        @RequestParam("password") String password
    ) {
        try {
            return ResponseEntity.ok(authChatService.login(username, password));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error(e.getMessage()));
        }
    }

    @GetMapping("/chat")
    public ResponseEntity<?> chatBootstrap(@RequestHeader("X-Auth-Token") String token) {
        try {
            return ResponseEntity.ok(authChatService.getChatBootstrap(token));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error(e.getMessage()));
        }
    }

    @GetMapping("/members")
    public ResponseEntity<?> members() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("members", authChatService.getAllUsers());
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/chat/send")
    public ResponseEntity<?> sendChatShare(
        @RequestHeader("X-Auth-Token") String token,
        @RequestParam("recipientUsernames") List<String> recipientUsernames,
        @RequestParam("message") String message,
        @RequestParam(value = "useCompression", defaultValue = "true") boolean useCompression,
        @RequestParam("image") MultipartFile image
    ) {
        try {
            AuthChatService.UserAccount sender = authChatService.requireUser(token);
            ValidationUtil.validateMessage(message);
            List<String> normalizedRecipients = authChatService.normalizeRecipientUsernames(recipientUsernames);
            byte[] carrierImageBytes = image.getBytes();
            List<AuthChatService.PreparedShare> preparedShares = new ArrayList<>();

            for (String recipientUsername : normalizedRecipients) {
                PublicKey recipientPublicKey = authChatService.getRecipientPublicKey(recipientUsername);
                String seedMaterial = RSAUtil.getKeyFingerprint(recipientPublicKey);
                BufferedImage carrierImage = imageProcessingService.loadImageFromBytes(carrierImageBytes);
                String securePayload = buildChatPayload(sender.username(), recipientUsername, message);

                boolean compressed = useCompression && compressionService.shouldCompress(securePayload);
                byte[] dataToEncrypt = compressed
                    ? compressionService.compress(securePayload)
                    : securePayload.getBytes(StandardCharsets.UTF_8);

                byte[] encryptedPayload = AESUtil.encrypt(dataToEncrypt, recipientPublicKey, compressed);
                steganographyService.embedData(carrierImage, encryptedPayload, seedMaterial);
                byte[] stegoImageBytes = imageProcessingService.saveImageAsPNG(carrierImage);

                preparedShares.add(new AuthChatService.PreparedShare(recipientUsername, compressed, stegoImageBytes));
            }

            return ResponseEntity.ok(authChatService.sendShares(token, message, preparedShares));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error(e.getMessage()));
        }
    }

    @PostMapping("/chat/decrypt/{shareId}")
    public ResponseEntity<?> decryptChatShare(
        @RequestHeader("X-Auth-Token") String token,
        @PathVariable("shareId") long shareId
    ) {
        try {
            AuthChatService.UserAccount currentUser = authChatService.requireUser(token);
            byte[] imageBytes = authChatService.getShareImageBytes(token, shareId);
            BufferedImage image = imageProcessingService.loadImageFromBytes(imageBytes);
            String seedMaterial = RSAUtil.getKeyFingerprint(RSAUtil.derivePublicKey(currentUser.privateKey()));
            byte[] encryptedData = steganographyService.extractData(image, seedMaterial);
            AESUtil.DecryptionResult decrypted = AESUtil.decrypt(encryptedData, currentUser.privateKey());
            String payloadText = decrypted.isCompressed()
                ? compressionService.decompress(decrypted.getPlainData())
                : new String(decrypted.getPlainData(), StandardCharsets.UTF_8);
            ChatPayload chatPayload = parseChatPayload(payloadText);

            if (!chatPayload.recipientUsername().equals(currentUser.username())) {
                throw new IllegalArgumentException("Message can't be extracted with the selected users. Wrong keys.");
            }

            return ResponseEntity.ok(
                authChatService.decryptShare(
                    token,
                    shareId,
                    chatPayload.message(),
                    encryptedData.length,
                    decrypted.getWrappedKeyLength(),
                    decrypted.isCompressed()
                )
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error(normalizeExtractionError(e)));
        }
    }

    @PostMapping("/extract-shared-image")
    public ResponseEntity<?> extractSharedImage(
        @RequestParam("image") MultipartFile image,
        @RequestParam("recipientUsername") String recipientUsername,
        @RequestParam(value = "senderUsername", required = false) String senderUsername
    ) {
        try {
            if (senderUsername != null && !senderUsername.isBlank()) {
                authChatService.getUserByUsername(senderUsername);
            }

            PrivateKey recipientPrivateKey = authChatService.getUserByUsername(recipientUsername).privateKey();
            String seedMaterial = RSAUtil.getKeyFingerprint(RSAUtil.derivePublicKey(recipientPrivateKey));
            BufferedImage stegoImage = imageProcessingService.loadImage(image);
            byte[] encryptedData = steganographyService.extractData(stegoImage, seedMaterial);
            AESUtil.DecryptionResult decrypted = AESUtil.decrypt(encryptedData, recipientPrivateKey);

            String payloadText = decrypted.isCompressed()
                ? compressionService.decompress(decrypted.getPlainData())
                : new String(decrypted.getPlainData(), StandardCharsets.UTF_8);
            ChatPayload chatPayload = parseChatPayload(payloadText);

            if (senderUsername != null
                && !senderUsername.isBlank()
                && !chatPayload.senderUsername().equalsIgnoreCase(senderUsername.trim())) {
                throw new IllegalArgumentException("Message can't be extracted with the selected users. Wrong keys.");
            }
            if (!chatPayload.recipientUsername().equalsIgnoreCase(recipientUsername.trim())) {
                throw new IllegalArgumentException("Message can't be extracted with the selected users. Wrong keys.");
            }

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", true);
            response.put("message", chatPayload.message());
            response.put("encryptedSize", encryptedData.length);
            response.put("messageLength", chatPayload.message().length());
            response.put("wrappedKeyLength", decrypted.getWrappedKeyLength());
            response.put("usedCompression", decrypted.isCompressed());
            response.put("encryptionMode", "RSA-OAEP + AES-256-GCM");
            response.put("senderUsername", chatPayload.senderUsername());
            response.put("recipientUsername", chatPayload.recipientUsername());
            response.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error(normalizeExtractionError(e)));
        }
    }

    private Map<String, Object> error(String message) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", false);
        response.put("message", message);
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }

    private String normalizeExtractionError(Exception e) {
        String message = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
        if (message.contains("invalid data length")
            || message.contains("padding")
            || message.contains("tag mismatch")
            || message.contains("unable to authenticate")
            || message.contains("final block")
            || message.contains("bad decrypt")
            || message.contains("unsupported stego payload format")
            || message.contains("unsupported payload version")
            || message.contains("payload length mismatch")
            || message.contains("invalid payload metadata")
            || message.contains("wrong keys")) {
            return "Message can't be extracted with the selected users. Wrong keys or wrong extraction mode.";
        }
        return e.getMessage();
    }

    private String buildChatPayload(String senderUsername, String recipientUsername, String message) {
        return "STEGA_CHAT_V1\n"
            + "sender=" + senderUsername + "\n"
            + "recipient=" + recipientUsername + "\n\n"
            + message;
    }

    private ChatPayload parseChatPayload(String payload) {
        String normalized = payload == null ? "" : payload.replace("\r\n", "\n");
        String marker = "STEGA_CHAT_V1\n";
        if (!normalized.startsWith(marker)) {
            throw new IllegalArgumentException("Message can't be extracted with the selected users. Wrong keys.");
        }

        int bodySeparator = normalized.indexOf("\n\n");
        if (bodySeparator < 0) {
            throw new IllegalArgumentException("Message can't be extracted with the selected users. Wrong keys.");
        }

        String header = normalized.substring(0, bodySeparator);
        String body = normalized.substring(bodySeparator + 2);
        String senderUsername = null;
        String recipientUsername = null;

        for (String line : header.split("\n")) {
            if (line.startsWith("sender=")) {
                senderUsername = line.substring("sender=".length()).trim();
            } else if (line.startsWith("recipient=")) {
                recipientUsername = line.substring("recipient=".length()).trim();
            }
        }

        if (senderUsername == null || senderUsername.isBlank() || recipientUsername == null || recipientUsername.isBlank()) {
            throw new IllegalArgumentException("Message can't be extracted with the selected users. Wrong keys.");
        }

        return new ChatPayload(senderUsername, recipientUsername, body);
    }

    private record ChatPayload(String senderUsername, String recipientUsername, String message) {}
}

package com.dnxt.globaladmin.aigateway.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class AiConfigEncryption {

    private static final Logger log = LoggerFactory.getLogger(AiConfigEncryption.class);
    private static final String CIPHER_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_BITS = 128;
    private static final int IV_LENGTH = 12;
    private static final byte[] SALT = "dnxt-global-admin-ai-salt-v1".getBytes(StandardCharsets.UTF_8);
    private static final int PBKDF2_ITERATIONS = 65_536;

    @Value("${admin.ai-config.secret:dnxt-global-admin-ai-default-local-dev-secret-change-me}")
    private String secret;

    private SecretKeySpec keySpec;
    private final SecureRandom random = new SecureRandom();

    @PostConstruct
    public void init() {
        try {
            PBEKeySpec spec = new PBEKeySpec(secret.toCharArray(), SALT, PBKDF2_ITERATIONS, 256);
            byte[] keyBytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
            this.keySpec = new SecretKeySpec(keyBytes, "AES");
            if (secret.contains("default-local-dev")) {
                log.warn("AiConfigEncryption using default dev secret. Set ADMIN_AI_CONFIG_SECRET in production!");
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to derive AI config encryption key", e);
        }
    }

    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) return null;
        try {
            byte[] iv = new byte[IV_LENGTH];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] cipherText = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            int tagBytes = GCM_TAG_BITS / 8;
            byte[] body = new byte[cipherText.length - tagBytes];
            byte[] tag  = new byte[tagBytes];
            System.arraycopy(cipherText, 0, body, 0, body.length);
            System.arraycopy(cipherText, body.length, tag, 0, tagBytes);
            return Base64.getEncoder().encodeToString(iv) + ":" +
                   Base64.getEncoder().encodeToString(tag) + ":" +
                   Base64.getEncoder().encodeToString(body);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to encrypt AI credential", e);
        }
    }

    public String decrypt(String envelope) {
        if (envelope == null || envelope.isEmpty()) return null;
        String[] parts = envelope.split(":");
        if (parts.length != 3) { log.warn("Malformed AI credential envelope"); return null; }
        try {
            byte[] iv   = Base64.getDecoder().decode(parts[0]);
            byte[] tag  = Base64.getDecoder().decode(parts[1]);
            byte[] body = Base64.getDecoder().decode(parts[2]);
            byte[] cipherText = new byte[body.length + tag.length];
            System.arraycopy(body, 0, cipherText, 0, body.length);
            System.arraycopy(tag, 0, cipherText, body.length, tag.length);
            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_BITS, iv));
            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Failed to decrypt AI credential: {}", e.getMessage());
            return null;
        }
    }
}

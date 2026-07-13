package com.unza.clinic.service;

import com.unza.clinic.model.RefreshToken;
import com.unza.clinic.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepo;

    @Value("${app.refresh-token.ttl-days:7}")
    private int ttlDays;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepo) {
        this.refreshTokenRepo = refreshTokenRepo;
    }

    /** Create a new refresh token for the given user.  Returns the raw (unhashed) token. */
    public String createToken(String userId, String userEmail) {
        String rawToken = UUID.randomUUID().toString() + "-" + UUID.randomUUID().toString();
        String tokenId  = UUID.randomUUID().toString();

        RefreshToken rt = new RefreshToken();
        rt.setTokenId(tokenId);
        rt.setUserId(userId);
        rt.setUserEmail(userEmail);
        rt.setTokenHash(hash(rawToken));
        rt.setIssuedAt(LocalDateTime.now());
        rt.setExpiresAt(LocalDateTime.now().plusDays(ttlDays));
        rt.setRevoked(false);
        refreshTokenRepo.save(rt);

        // Return tokenId:rawToken so the client sends it back and we can look up by tokenId
        return tokenId + ":" + rawToken;
    }

    /**
     * Validate and rotate a refresh token.
     * Returns the userId if valid; throws IllegalArgumentException otherwise.
     */
    public String rotateToken(String compositeToken, String newAccessTokenWillBeIssuedForUserId) {
        String[] parts = compositeToken.split(":", 2);
        if (parts.length != 2) throw new IllegalArgumentException("Invalid refresh token format");

        String tokenId  = parts[0];
        String rawToken = parts[1];

        RefreshToken rt = refreshTokenRepo.findByTokenId(tokenId)
                .orElseThrow(() -> new IllegalArgumentException("Refresh token not found"));

        if (rt.isRevoked()) throw new IllegalArgumentException("Refresh token has been revoked");
        if (rt.getExpiresAt().isBefore(LocalDateTime.now())) throw new IllegalArgumentException("Refresh token has expired");
        if (!hash(rawToken).equals(rt.getTokenHash())) throw new IllegalArgumentException("Refresh token signature mismatch");

        // Revoke the old token
        rt.setRevoked(true);
        rt.setRevokedAt(LocalDateTime.now());
        refreshTokenRepo.save(rt);

        return rt.getUserId();
    }

    public void revokeAll(String userId) {
        refreshTokenRepo.revokeAllByUserId(userId);
    }

    private String hash(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (Exception e) {
            throw new RuntimeException("Hashing failed", e);
        }
    }
}

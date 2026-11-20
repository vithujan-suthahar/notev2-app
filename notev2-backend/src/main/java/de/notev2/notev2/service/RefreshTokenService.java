package de.notev2.notev2.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

import org.springframework.stereotype.Service;

import de.notev2.notev2.entity.RefreshToken;
import de.notev2.notev2.entity.User;
import de.notev2.notev2.repos.RefreshTokenRepository;

@Service
public class RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    private String hashToken(String rawToken) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 ist nicht verfügbar", e);
        }
    }

    public String createRefreshToken(User user) {

        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);

        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        String tokenHash = hashToken(rawToken);

        RefreshToken rftoken = new RefreshToken();
        rftoken.setUser(user);
        Instant now = Instant.now();
        rftoken.setCreatedAt(now);
        rftoken.setExpiresAt(now.plus(Duration.ofDays(7)));
        rftoken.setTokenHash(tokenHash);

        refreshTokenRepository.save(rftoken);

        return rawToken;

    }

    public RefreshToken validateRefreshToken(String rawToken) {
        String tokenHash = hashToken(rawToken);

        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new IllegalStateException("Refresh Token nicht gefunden"));


        if (!refreshToken.getExpiresAt().isAfter(Instant.now())) {
            throw new IllegalStateException("Refresh Token ist abgelaufen");
        }

        if (refreshToken.getRevokedAt() != null) {
            throw new IllegalStateException("Refresh Token wurde widerrufen");
        }

        return refreshToken;
    }

}

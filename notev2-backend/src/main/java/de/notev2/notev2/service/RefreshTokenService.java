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

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }


    public String createRefreshToken(User user) {
        SecureRandom secureRandom = new SecureRandom();

        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);

        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        MessageDigest digest;
        String tokenhash;
        try {
            digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            tokenhash = HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 ist nicht verfügbar", e);
        }


        RefreshToken rftoken = new RefreshToken();
        rftoken.setUser(user);
        Instant now = Instant.now();
        rftoken.setCreatedAt(now);
        rftoken.setExpiresAt(now.plus(Duration.ofDays(7)));
        rftoken.setTokenHash(tokenhash);

        refreshTokenRepository.save(rftoken);
        

        return rawToken;
        
    }
    
}

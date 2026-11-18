package de.notev2.notev2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.notev2.notev2.entity.RefreshToken;
import de.notev2.notev2.entity.User;
import de.notev2.notev2.repos.RefreshTokenRepository;
import de.notev2.notev2.service.RefreshTokenService;

@ExtendWith(MockitoExtension.class)
public class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    @Test
    void createRefreshToken_shouldSaveTokenHashAndReturnRawToken() {
        User user = new User();
        user.setUsername("deniz");

        refreshTokenService.createRefreshToken(user);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);

        verify(refreshTokenRepository).save(captor.capture());

        RefreshToken savedToken = captor.getValue();

        assertEquals(user, savedToken.getUser());
        assertNotNull(savedToken.getTokenHash());
        assertFalse(savedToken.getTokenHash().isBlank());

    }

    @Test
    void createRefreshToken_shouldGenerateDifferentTokens() {

        User user = new User();
        user.setUsername("deniz");

        refreshTokenService.createRefreshToken(user);
        refreshTokenService.createRefreshToken(user);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);

        verify(refreshTokenRepository, times(2)).save(captor.capture());

        List<RefreshToken> savedTokens = captor.getAllValues();

        assertNotEquals(
                savedTokens.get(0).getTokenHash(),
                savedTokens.get(1).getTokenHash());
    }

    @Test
    void createRefreshToken_shouldSetExpirationDate() {

        User user = new User();
        user.setUsername("deniz");

        refreshTokenService.createRefreshToken(user);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);

        verify(refreshTokenRepository).save(captor.capture());

        RefreshToken savedToken = captor.getValue();

        Instant created_at = savedToken.getCreatedAt();
        Instant exprires_at = savedToken.getExpiresAt();

        Duration duration = Duration.between(created_at, exprires_at);

        assertEquals(Duration.ofDays(7), duration);
    }

    @Test
    void createRefreshToken_shouldStoreHashInsteadOfRawToken() throws NoSuchAlgorithmException {
        User user = new User();

        String rawToken = refreshTokenService.createRefreshToken(user);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);

        verify(refreshTokenRepository).save(captor.capture());

        String savedHash = captor.getValue().getTokenHash();

        String expectedHash = HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256")
                        .digest(rawToken.getBytes(StandardCharsets.UTF_8)));

        assertNotEquals(rawToken, savedHash);
        assertEquals(expectedHash, savedHash);
    }
}

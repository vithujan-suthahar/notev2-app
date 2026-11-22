package de.notev2.notev2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.notev2.notev2.entity.RefreshToken;
import de.notev2.notev2.entity.User;
import de.notev2.notev2.exception.InvalidRefreshTokenException;
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

        @Test
        void validateRefreshToken_shouldReturnToken_whenTokenIsValid() throws NoSuchAlgorithmException {
                String rawToken = "test-refresh-token";

                String tokenHash = HexFormat.of().formatHex(
                                MessageDigest.getInstance("SHA-256")
                                                .digest(rawToken.getBytes(StandardCharsets.UTF_8)));

                User user = new User();

                RefreshToken refreshToken = new RefreshToken();
                refreshToken.setUser(user);
                refreshToken.setTokenHash(tokenHash);
                refreshToken.setCreatedAt(Instant.now());
                refreshToken.setExpiresAt(Instant.now().plus(Duration.ofDays(7)));
                refreshToken.setRevokedAt(null);

                when(refreshTokenRepository.findByTokenHash(tokenHash))
                                .thenReturn(Optional.of(refreshToken));

                RefreshToken result = refreshTokenService.validateRefreshToken(rawToken);

                assertEquals(refreshToken, result);
                verify(refreshTokenRepository).findByTokenHash(tokenHash);

        }

        @Test
        void validateRefreshToken_shouldThrowException_whenTokenNotFound()
                        throws NoSuchAlgorithmException {

                String rawToken = "test-refresh-token";

                String tokenHash = HexFormat.of().formatHex(
                                MessageDigest.getInstance("SHA-256")
                                                .digest(rawToken.getBytes(StandardCharsets.UTF_8)));

                when(refreshTokenRepository.findByTokenHash(tokenHash))
                                .thenReturn(Optional.empty());

                InvalidRefreshTokenException ex = assertThrows(
                                InvalidRefreshTokenException.class,
                                () -> refreshTokenService.validateRefreshToken(rawToken));

                assertEquals("Refresh Token nicht gefunden", ex.getMessage());

                verify(refreshTokenRepository).findByTokenHash(tokenHash);
        }

        @Test
        void validateRefreshToken_shouldThrowException_whenTokenExpired() throws NoSuchAlgorithmException {
                String rawToken = "test-refresh-token";

                String tokenHash = HexFormat.of().formatHex(
                                MessageDigest.getInstance("SHA-256")
                                                .digest(rawToken.getBytes(StandardCharsets.UTF_8)));

                User user = new User();

                RefreshToken refreshToken = new RefreshToken();
                refreshToken.setUser(user);
                refreshToken.setTokenHash(tokenHash);
                refreshToken.setCreatedAt(Instant.now());
                refreshToken.setExpiresAt(Instant.now().minus(Duration.ofDays(1)));
                refreshToken.setRevokedAt(null);

                when(refreshTokenRepository.findByTokenHash(tokenHash))
                                .thenReturn(Optional.of(refreshToken));

                InvalidRefreshTokenException ex = assertThrows(
                                InvalidRefreshTokenException.class,
                                () -> refreshTokenService.validateRefreshToken(rawToken));

                assertEquals("Refresh Token ist abgelaufen", ex.getMessage());

                verify(refreshTokenRepository).findByTokenHash(tokenHash);

        }

        @Test
        void validateRefreshToken_shouldThrowException_whenTokenRevoked() throws NoSuchAlgorithmException {
                String rawToken = "test-refresh-token";

                String tokenHash = HexFormat.of().formatHex(
                                MessageDigest.getInstance("SHA-256")
                                                .digest(rawToken.getBytes(StandardCharsets.UTF_8)));

                User user = new User();

                RefreshToken refreshToken = new RefreshToken();
                refreshToken.setUser(user);
                refreshToken.setTokenHash(tokenHash);
                refreshToken.setCreatedAt(Instant.now());
                refreshToken.setExpiresAt(Instant.now().plus(Duration.ofDays(7)));
                refreshToken.setRevokedAt(Instant.now());

                when(refreshTokenRepository.findByTokenHash(tokenHash))
                                .thenReturn(Optional.of(refreshToken));

                InvalidRefreshTokenException ex = assertThrows(
                                InvalidRefreshTokenException.class,
                                () -> refreshTokenService.validateRefreshToken(rawToken));

                assertEquals("Refresh Token wurde widerrufen", ex.getMessage());

                verify(refreshTokenRepository).findByTokenHash(tokenHash);

        }

        @Test
        void revokeRefreshToken_shouldSetRevokedAt_whenTokenExists() throws NoSuchAlgorithmException {
                String rawToken = "test-refresh-token";

                String tokenHash = HexFormat.of().formatHex(
                                MessageDigest.getInstance("SHA-256")
                                                .digest(rawToken.getBytes(StandardCharsets.UTF_8)));

                User user = new User();

                RefreshToken refreshToken = new RefreshToken();
                refreshToken.setUser(user);
                refreshToken.setTokenHash(tokenHash);
                refreshToken.setCreatedAt(Instant.now());
                refreshToken.setExpiresAt(Instant.now().plus(Duration.ofDays(7)));
                refreshToken.setRevokedAt(null);

                when(refreshTokenRepository.findByTokenHash(tokenHash))
                                .thenReturn(Optional.of(refreshToken));

                refreshTokenService.revokeRefreshToken(rawToken);

                assertNotNull(refreshToken.getRevokedAt());

                verify(refreshTokenRepository).findByTokenHash(tokenHash);
                verify(refreshTokenRepository).save(refreshToken);

        }

        @Test
        void revokeRefreshToken_shouldThrowException_whenTokenNotFound() throws NoSuchAlgorithmException {
                String rawToken = "test-refresh-token";

                String tokenHash = HexFormat.of().formatHex(
                                MessageDigest.getInstance("SHA-256")
                                                .digest(rawToken.getBytes(StandardCharsets.UTF_8)));

                when(refreshTokenRepository.findByTokenHash(tokenHash))
                                .thenReturn(Optional.empty());

                InvalidRefreshTokenException ex = assertThrows(
                                InvalidRefreshTokenException.class,
                                () -> refreshTokenService.revokeRefreshToken(rawToken));

                assertEquals("Refresh Token nicht gefunden", ex.getMessage());

                verify(refreshTokenRepository).findByTokenHash(tokenHash);
                verify(refreshTokenRepository, never()).save(any(RefreshToken.class));

        }

        @Test
        void rotateRefreshToken_shouldCreateNewTokenAndRevokeOldToken() {

                String rawToken = "test-refresh-token";

                User user = new User();

                RefreshToken refreshToken = new RefreshToken();
                refreshToken.setUser(user);
                refreshToken.setTokenHash("old-hash");
                refreshToken.setCreatedAt(Instant.now());
                refreshToken.setExpiresAt(Instant.now().plus(Duration.ofDays(7)));
                refreshToken.setRevokedAt(null);

                String newRawToken = refreshTokenService.rotateRefreshToken(refreshToken);

                assertNotNull(newRawToken);
                assertNotEquals(rawToken, newRawToken);

                ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);

                verify(refreshTokenRepository, times(2)).save(captor.capture());
                verify(refreshTokenRepository, never()).findByTokenHash(any(String.class));

                List<RefreshToken> savedTokens = captor.getAllValues();

                RefreshToken alt = savedTokens.get(0);
                RefreshToken neu = savedTokens.get(1);

                assertEquals(user, alt.getUser());
                assertNotNull(alt.getRevokedAt());

                assertNotNull(neu.getTokenHash());
                assertNotEquals(alt.getTokenHash(), neu.getTokenHash());
                assertEquals(user, neu.getUser());
                assertNull(neu.getRevokedAt());
        }

}

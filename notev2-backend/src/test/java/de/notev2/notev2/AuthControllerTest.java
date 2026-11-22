package de.notev2.notev2;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional; // WICHTIG!

import com.fasterxml.jackson.databind.ObjectMapper;

import de.notev2.notev2.entity.RefreshToken;
import de.notev2.notev2.entity.User;
import de.notev2.notev2.repos.RefreshTokenRepository;
import de.notev2.notev2.repos.UserRepository;
import de.notev2.notev2.service.RefreshTokenService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

// Falls du ein Passwort-Encoder nutzt, aktiviere den Import:
// import org.springframework.security.crypto.password.PasswordEncoder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void login_shouldReturnTokens() throws Exception {

        User user = new User();
        user.setUsername("deniz");
        user.setPassword(passwordEncoder.encode("123"));
        userRepository.save(user);

        String request = """
                {
                    "username": "deniz",
                    "password": "123"
                }
                """;

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.message").value("Login erfolgreich"));
    }

    @Test
    void login_withInvalidCredentials_shouldReturnUnauthorized() throws Exception {

        User user = new User();
        user.setUsername("deniz");
        user.setPassword(passwordEncoder.encode("123"));
        userRepository.save(user);

        String request = """
                {
                    "username": "deniz",
                    "password": "wrongpassword"
                }
                """;

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_withNonExistingUser_shouldReturnUnauthorized() throws Exception {

        String request = """
                {
                    "username": "nonexistinguser",
                    "password": "123"
                }
                """;

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_withEmptyUsername_shouldReturnBadRequest() throws Exception {

        String request = """
                {
                    "username": "",
                    "password": "123"
                }
                """;

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validierung fehlgeschlagen"))
                .andExpect(jsonPath("$.errors.username").exists());
    }

    @Test
    void login_withEmptyPassword_shouldReturnBadRequest() throws Exception {

        String request = """
                {
                    "username": "deniz",
                    "password": ""
                }
                """;

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validierung fehlgeschlagen"))
                .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    void login_withEmptyUsernameAndPassword_shouldReturnBadRequest() throws Exception {

        String request = """
                {
                    "username": "",
                    "password": ""
                }
                """;

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validierung fehlgeschlagen"))
                .andExpect(jsonPath("$.errors.username").exists())
                .andExpect(jsonPath("$.errors.password").exists());

    }

    @Test
    void login_withMaxLengthUsername_shouldReturnBadRequest() throws Exception {
        String username = "a".repeat(51);
        String request = """
                {
                    "username": "%s",
                    "password": "123"
                }
                """.formatted(username);

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validierung fehlgeschlagen"))
                .andExpect(jsonPath("$.errors.username").exists());
    }

    @Test
    void login_withMaxLengthPassword_shouldReturnBadRequest() throws Exception {
        String password = "a".repeat(51);
        String request = """
                {
                    "username": "deniz",
                    "password": "%s"
                }
                """.formatted(password);

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validierung fehlgeschlagen"))
                .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    void register_withExistingUsername_shouldReturnBadRequest() throws Exception {

        String request = """
                {
                    "username": "deniz",
                    "password": "123"
                }
                """;

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isOk());

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isConflict());
    }

    @Test
    void register_withEmptyUsername_shouldReturnBadRequest() throws Exception {
        String request = """
                {
                    "username" : "",
                    "password" : "123"
                }
                """;
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Validierung fehlgeschlagen"))
                .andExpect(jsonPath("$.errors.username")
                        .exists());

    }

    @Test
    void register_withEmptyPassword_shouldReturnBadRequest() throws Exception {
        String request = """
                {
                    "username" : "deniz",
                    "password" : ""
                }
                """;
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Validierung fehlgeschlagen"))
                .andExpect(jsonPath("$.errors.password")
                        .exists());

    }

    @Test
    void register_withEmptyUsernamePassword_shouldReturnBadRequest() throws Exception {
        String request = """
                {
                    "username" : "",
                    "password" : ""
                }
                """;
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Validierung fehlgeschlagen"))
                .andExpect(jsonPath("$.errors.username")
                        .exists())
                .andExpect(jsonPath("$.errors.password")
                        .exists());

    }

    @Test
    void register_withEmptyUsernameAndwithSpacesPassword_shouldReturnBadRequest() throws Exception {
        String request = """
                {
                    "username" : "",
                    "password" : "   "
                }
                """;
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Validierung fehlgeschlagen"))
                .andExpect(jsonPath("$.errors.username")
                        .exists())
                .andExpect(jsonPath("$.errors.password")
                        .exists());

    }

    @Test
    void register_withSpacesUsernameAndwithEmptyPassword_shouldReturnBadRequest() throws Exception {
        String request = """
                {
                    "username" : "   ",
                    "password" : ""
                }
                """;
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Validierung fehlgeschlagen"))
                .andExpect(jsonPath("$.errors.username")
                        .exists())
                .andExpect(jsonPath("$.errors.password")
                        .exists());

    }

    @Test
    void register_withMaxLengthUsername_shouldReturnBadRequest() throws Exception {
        String longUsername = "a".repeat(51);
        String request = """
                {
                    "username" : "%s",
                    "password" : "123"
                }
                """.formatted(longUsername);
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Validierung fehlgeschlagen"))
                .andExpect(jsonPath("$.errors.username")
                        .exists());

    }

    @Test
    void register_withMaxLengthPassword_shouldReturnBadRequest() throws Exception {
        String longPassword = "a".repeat(51);
        String request = """
                {
                    "username" : "deniz",
                    "password" : "%s"
                }
                """.formatted(longPassword);
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Validierung fehlgeschlagen"))
                .andExpect(jsonPath("$.errors.password")
                        .exists());

    }

    @Test
    void refreshToken_shouldReturnOk() throws Exception {

        User user = new User();
        user.setUsername("deniz");
        user.setPassword(passwordEncoder.encode("123"));
        userRepository.save(user);

        String refreshToken = refreshTokenService.createRefreshToken(user);

        String request = """
                {
                    "refreshToken": "%s"
                }
                """.formatted(refreshToken);

        mockMvc.perform(post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.message").value("Token erneuert"));
    }

    @Test
    void refreshTokenExpired_shouldReturnUnauthorizedRequest() throws Exception {

        User user = new User();
        user.setUsername("deniz");
        user.setPassword(passwordEncoder.encode("123"));
        userRepository.save(user);

        String rawToken = refreshTokenService.createRefreshToken(user);

        String hash = refreshTokenService.hashToken(rawToken);

        RefreshToken token = refreshTokenRepository.findByTokenHash(hash).orElseThrow();
        token.setExpiresAt(Instant.now().minus(Duration.ofDays(1)));
        refreshTokenRepository.save(token);

        String request = """
                {
                    "refreshToken": "%s"
                }
                """.formatted(rawToken);

        mockMvc.perform(post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isUnauthorized());
        assertNull(token.getRevokedAt());
        assertEquals(1, refreshTokenRepository.count());
    }

    @Test
    void refreshTokenRevoked_shouldReturnUnauthorizedRequest() throws Exception {

        User user = new User();
        user.setUsername("deniz");
        user.setPassword(passwordEncoder.encode("123"));
        userRepository.save(user);

        String rawToken = refreshTokenService.createRefreshToken(user);

        String hash = refreshTokenService.hashToken(rawToken);

        RefreshToken token = refreshTokenRepository.findByTokenHash(hash).orElseThrow();
        token.setRevokedAt(Instant.now());
        refreshTokenRepository.save(token);

        String request = """
                {
                    "refreshToken": "%s"
                }
                """.formatted(rawToken);

        Instant revokedAt = token.getRevokedAt();

        mockMvc.perform(post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isUnauthorized());

        assertEquals(revokedAt, token.getRevokedAt());
        assertEquals(1, refreshTokenRepository.count());
    }

    @Test
    void refreshTokenNotFound_shouldReturnUnauthorizedRequest() throws Exception {

        String rawToken = "test-raw-token";

        String request = """
                {
                    "refreshToken": "%s"
                }
                """.formatted(rawToken);

        mockMvc.perform(post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshToken_shouldRotateTokens_whenTokenIsValid() throws Exception {

        User user = new User();
        user.setUsername("deniz");
        user.setPassword(passwordEncoder.encode("123"));
        userRepository.save(user);

        String rawToken = refreshTokenService.createRefreshToken(user);

        String hash = refreshTokenService.hashToken(rawToken);

        String request = """
                {
                    "refreshToken": "%s"
                }
                """.formatted(rawToken);

        MvcResult result = mockMvc.perform(post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.message").value("Token erneuert"))
                .andReturn();

        RefreshToken oldToken = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow();

        assertNotNull(oldToken.getRevokedAt());

        String responseBody = result.getResponse().getContentAsString();
        String newRawToken = objectMapper.readTree(responseBody)
                .get("refreshToken").asText();

        assertEquals(2, refreshTokenRepository.count());

        String newHash = refreshTokenService.hashToken(newRawToken);

        RefreshToken newToken = refreshTokenRepository.findByTokenHash(newHash)
                .orElseThrow();

        assertNotEquals(hash, newHash);
        assertEquals(user.getId(), newToken.getUser().getId());
        assertNull(newToken.getRevokedAt());
    }

    @Test
    void logout_shouldRevokeRefreshToken_whenTokenIsValid() throws Exception {
        User user = new User();
        user.setUsername("deniz");
        user.setPassword(passwordEncoder.encode("123"));
        userRepository.save(user);

        String rawToken = refreshTokenService.createRefreshToken(user);

        String hash = refreshTokenService.hashToken(rawToken);

        String request = """
                {
                    "refreshToken": "%s"
                }
                """.formatted(rawToken);

        mockMvc.perform(post("/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isEmpty())
                .andExpect(jsonPath("$.refreshToken").isEmpty())
                .andExpect(jsonPath("$.message").value("Token entfernt"));

        RefreshToken token = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow();

        assertNotNull(token.getRevokedAt());
    }

    @Test
    void logout_shouldReturnUnauthorized_whenTokenNotFound() throws Exception {

        String rawToken = "test-refresh-token";

        String request = """
                {
                    "refreshToken": "%s"
                }
                """.formatted(rawToken);

        mockMvc.perform(post("/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logout_shouldReturnUnauthorized_whenTokenAlreadyRevoked() throws Exception {

        User user = new User();
        user.setUsername("deniz");
        user.setPassword(passwordEncoder.encode("123"));
        userRepository.save(user);

        String rawToken = refreshTokenService.createRefreshToken(user);

        String hash = refreshTokenService.hashToken(rawToken);
        RefreshToken token = refreshTokenRepository.findByTokenHash(hash).orElseThrow();
        token.setRevokedAt(Instant.now());
        refreshTokenRepository.save(token);

        String request = """
                {
                    "refreshToken": "%s"
                }
                """.formatted(rawToken);

        mockMvc.perform(post("/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isUnauthorized());
    }

}
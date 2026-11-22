package de.notev2.notev2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import de.notev2.notev2.config.CustomUserDetailsService;
import de.notev2.notev2.config.JwtUtil;
import de.notev2.notev2.dto.AuthLoginRequest;
import de.notev2.notev2.dto.AuthRegisterRequest;
import de.notev2.notev2.dto.AuthResponse;
import de.notev2.notev2.entity.User;
import de.notev2.notev2.repos.UserRepository;
import de.notev2.notev2.service.AuthService;
import de.notev2.notev2.service.RefreshTokenService;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private CustomUserDetailsService userDetailsService; // ← fehlt, hinzufügen

    @InjectMocks
    private AuthService authService;

    // =========================
    // REGISTER TESTS
    // =========================

    @Test
    void shouldRegisterUser() {
        AuthRegisterRequest user = new AuthRegisterRequest();
        user.setUsername("deniz");
        user.setPassword("123");

        when(userRepository.findByUsername("deniz")).thenReturn(null);
        when(passwordEncoder.encode("123")).thenReturn("hashed_123");

        String result = authService.register(user);

        assertEquals("Registrierung erfolgreich", result);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void shouldNotRegisterUserWithExistingUsername() {
        User existingUser = new User();
        existingUser.setUsername("deniz");

        AuthRegisterRequest newUser = new AuthRegisterRequest();
        newUser.setUsername("deniz");
        newUser.setPassword("123");

        when(userRepository.findByUsername("deniz")).thenReturn(existingUser);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.register(newUser);
        });

        assertEquals("Username bereits vergeben", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    // =========================
    // LOGIN TESTS
    // =========================

    @Test
    void shouldLoginUser() {
        AuthLoginRequest requestUser = new AuthLoginRequest();
        requestUser.setUsername("deniz");
        requestUser.setPassword("123");

        User dbUser = new User();
        dbUser.setUsername("deniz");
        dbUser.setPassword("hashed_password");
        dbUser.setAdmin(false); // ← hinzufügen

        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername("deniz")
                .password("hashed_password")
                .authorities(new SimpleGrantedAuthority("ROLE_USER"))
                .build();

        when(userRepository.findByUsername("deniz")).thenReturn(dbUser);
        when(passwordEncoder.matches("123", "hashed_password")).thenReturn(true);
        when(userDetailsService.loadUserByUsername("deniz")).thenReturn(userDetails); // ← neu
        when(jwtUtil.generateToken(any(UserDetails.class))).thenReturn("test_access_token");
        when(refreshTokenService.createRefreshToken(dbUser)).thenReturn("test_refresh_token");

        AuthResponse result = authService.login(requestUser);
        assertEquals("test_access_token", result.getAccessToken());
        assertEquals("test_refresh_token", result.getRefreshToken());
        verify(refreshTokenService).createRefreshToken(dbUser);
    }

    @Test
    void shouldNotLoginWithWrongPassword() {
        AuthLoginRequest requestUser = new AuthLoginRequest();
        requestUser.setUsername("deniz");
        requestUser.setPassword("wrong");

        User dbUser = new User();
        dbUser.setUsername("deniz");
        dbUser.setPassword("hashed_password");

        when(userRepository.findByUsername("deniz")).thenReturn(dbUser);
        when(passwordEncoder.matches("wrong", "hashed_password")).thenReturn(false);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.login(requestUser);
        });

        assertEquals("Benutzername oder Passwort falsch", exception.getMessage());
    }

    @Test
    void shouldNotLoginWithNonExistingUser() {
        AuthLoginRequest user = new AuthLoginRequest();
        user.setUsername("deniz");
        user.setPassword("123");

        when(userRepository.findByUsername("deniz")).thenReturn(null);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.login(user);
        });

        assertEquals("Benutzername oder Passwort falsch", exception.getMessage());
    }

    @Test
    void shouldNotLoginWithEmptyUsername() {
        AuthLoginRequest user = new AuthLoginRequest();
        user.setUsername("");
        user.setPassword("123");

        when(userRepository.findByUsername("")).thenReturn(null);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.login(user);
        });

        assertEquals("Benutzername oder Passwort falsch", exception.getMessage());
    }

    @Test
    void shouldNotLoginWithEmptyPassword() {
        AuthLoginRequest requestUser = new AuthLoginRequest();
        requestUser.setUsername("deniz");
        requestUser.setPassword("");

        User dbUser = new User();
        dbUser.setUsername("deniz");
        dbUser.setPassword("hashed_password");

        when(userRepository.findByUsername("deniz")).thenReturn(dbUser);
        when(passwordEncoder.matches("", "hashed_password")).thenReturn(false);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.login(requestUser);
        });

        assertEquals("Benutzername oder Passwort falsch", exception.getMessage());
    }

}
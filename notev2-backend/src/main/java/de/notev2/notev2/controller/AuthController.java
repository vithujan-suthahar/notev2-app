package de.notev2.notev2.controller;

import java.time.Duration;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.notev2.notev2.service.AuthService;
import de.notev2.notev2.dto.AuthLoginRequest;
import de.notev2.notev2.dto.AuthRefreshRequest;
import de.notev2.notev2.dto.AuthRegisterRequest;
import de.notev2.notev2.dto.AuthResponse;
import de.notev2.notev2.exception.InvalidRefreshTokenException;

import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
public class AuthController {

        private final AuthService authService;

        public AuthController(AuthService authService) {
                this.authService = authService;
        }

        @Operation(summary = "Register", description = "Registriert einen neuen Benutzer")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Registrierung erfolgreich"),
                        @ApiResponse(responseCode = "400", description = "Fehlende oder ungültige Felder"),
                        @ApiResponse(responseCode = "409", description = "Benutzername bereits vergeben")
        })
        @PostMapping("/register")
        public ResponseEntity<AuthResponse> register(@Valid @RequestBody AuthRegisterRequest request) {
                String message = authService.register(request);
                return ResponseEntity.ok(new AuthResponse(message));

        }

        @Operation(summary = "Login", description = "Authentifiziert einen Benutzer und gibt ein JWT zurück")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Login erfolgreich, JWT im Response-Body enthalten"),
                        @ApiResponse(responseCode = "401", description = "Ungültige Benutzername oder Passwort")

        })
        @PostMapping("/login")
        public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthLoginRequest request) {

                AuthResponse authResponse = authService.login(request);

                ResponseCookie refreshCookie = ResponseCookie.from(
                                "refreshToken",
                                authResponse.getRefreshToken())
                                .httpOnly(true)
                                .secure(false)
                                .sameSite("Lax")
                                .path("/auth")
                                .maxAge(Duration.ofDays(7))
                                .build();

                AuthResponse response = new AuthResponse(
                                authResponse.getAccessToken(),
                                null,
                                authResponse.getMessage());

                return ResponseEntity.ok()
                                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                                .body(response);
        }

        @Operation(summary = "Get User Info", description = "Ruft Informationen über den angemeldeten Benutzer ab")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Benutzerinformationen erfolgreich abgerufen"),
                        @ApiResponse(responseCode = "401", description = "Ungültige oder fehlende Authentifizierung")
        })
        @GetMapping("/me")
        public ResponseEntity<AuthResponse> getUser(Authentication auth) {
                String message = authService.getUserInfo(auth);
                return ResponseEntity.ok(new AuthResponse(message));
        }

        @PostMapping("/refresh")
        public ResponseEntity<AuthResponse> refreshToken(
                        @CookieValue(name = "refreshToken", required = false) String rawToken) {

                if (rawToken == null || rawToken.isBlank()) {
                        throw new InvalidRefreshTokenException("Refresh Token fehlt");
                }

                AuthResponse authResponse = authService.refreshToken(
                                new AuthRefreshRequest(rawToken));

                ResponseCookie refreshCookie = ResponseCookie.from(
                                "refreshToken",
                                authResponse.getRefreshToken())
                                .httpOnly(true)
                                .secure(false)
                                .sameSite("Lax")
                                .path("/auth")
                                .maxAge(Duration.ofDays(7))
                                .build();

                AuthResponse response = new AuthResponse(
                                authResponse.getAccessToken(),
                                null,
                                authResponse.getMessage());

                return ResponseEntity.ok()
                                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                                .body(response);
        }

        @PostMapping("/logout")
        public ResponseEntity<AuthResponse> logout(
                        @CookieValue(name = "refreshToken", required = false) String rawToken) {

                if (rawToken == null || rawToken.isBlank()) {
                        throw new InvalidRefreshTokenException("Refresh Token fehlt");
                }

                AuthResponse authResponse = authService.logout(new AuthRefreshRequest(rawToken));

                ResponseCookie clearCookie = ResponseCookie.from("refreshToken", "")
                                .httpOnly(true)
                                .secure(false)
                                .sameSite("Lax")
                                .path("/auth")
                                .maxAge(Duration.ZERO)
                                .build();

                return ResponseEntity.ok()
                                .header(HttpHeaders.SET_COOKIE, clearCookie.toString())
                                .body(authResponse);
        }

}

package de.notev2.notev2.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public class AuthResponse {

    @Schema(description = "JWT-Token für die Authentifizierung", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyMSIsImlhdCI6MTY4ODQyODAwMCwiZXhwIjoxNjg4NDMyNjAwfQ.abc123def456ghi789jkl012mno345pqr678stu901vwx234yz567890")
    private String accessToken;

    @Schema(description = "Refresh-Token für die Authentifizierung", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyMSIsImlhdCI6MTY4ODQyODAwMCwiZXhwIjoxNjg4NDMyNjAwfQ.abc123def456ghi789jkl012mno345pqr678stu901vwx234yz567890")
    private String refreshToken;

    @Schema(description = "Nachricht zur Darstellung im Frontend", example = "Login erfolgreich")
    private String message;

    public AuthResponse(String message) {
        this.message = message;
    }
    
    public AuthResponse(String accessToken, String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }

    public AuthResponse(String accessToken, String refreshToken, String message) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.message = message;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

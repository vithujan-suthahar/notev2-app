package de.notev2.notev2.dto;

public class AuthRefreshRequest {
    private String refreshToken;

    public AuthRefreshRequest() {
    }

    public AuthRefreshRequest(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
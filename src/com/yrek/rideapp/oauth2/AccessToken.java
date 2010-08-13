package com.yrek.rideapp.oauth2;

import java.io.Serializable;

public class AccessToken implements Serializable {
    private static final long serialVersionUID = 0L;

    private final String accessToken;
    private final long expiresAt;
    private final String refreshToken;

    public AccessToken(String accessToken, long expiresAt, String refreshToken) {
        this.accessToken = accessToken;
        this.expiresAt = expiresAt;
        this.refreshToken = refreshToken;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    @Override
    public String toString() {
        return "access_token="+accessToken+"&expires_at="+expiresAt+"&refresh_token="+refreshToken;
    }
}

package ru.operatorsha.ziovpolabi.auth;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        long accessExpiresInSeconds,
        long refreshExpiresInSeconds
) {
}

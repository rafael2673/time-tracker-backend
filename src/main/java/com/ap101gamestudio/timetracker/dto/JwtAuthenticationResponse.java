package com.ap101gamestudio.timetracker.dto;

public record JwtAuthenticationResponse(
        String accessToken,
        String tokenType,
        Long expiresIn
) {
    public JwtAuthenticationResponse(String accessToken, Long expiresIn) {
        this(accessToken, "Bearer", expiresIn);
    }
}

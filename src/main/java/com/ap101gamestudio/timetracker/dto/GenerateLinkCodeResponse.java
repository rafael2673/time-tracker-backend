package com.ap101gamestudio.timetracker.dto;

public record GenerateLinkCodeResponse(
        String code,
        long expiresInSeconds
) {}
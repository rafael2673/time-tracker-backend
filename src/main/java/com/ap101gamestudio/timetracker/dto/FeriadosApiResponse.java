package com.ap101gamestudio.timetracker.dto;

public record FeriadosApiResponse(
        String date,
        String name,
        String type
) {}
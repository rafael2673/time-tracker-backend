package com.ap101gamestudio.timetracker.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FeriadosApiResponse(
        @JsonProperty("data") String date,
        @JsonProperty("nome") String name,
        @JsonProperty("tipo") String type
) {}
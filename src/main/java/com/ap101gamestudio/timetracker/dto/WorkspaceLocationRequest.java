package com.ap101gamestudio.timetracker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WorkspaceLocationRequest(
                @NotBlank(message = "error.workspace.name_required") String name,

                @Size(min = 2, max = 2, message = "error.workspace.state_invalid") String stateUf,

                @Size(min = 7, max = 7, message = "error.workspace.ibge_invalid") String ibgeCode) {
}

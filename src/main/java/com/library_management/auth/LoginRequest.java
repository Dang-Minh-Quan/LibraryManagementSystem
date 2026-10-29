package com.library_management.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank @Size(max = 50) @Schema(example = "dangminhquan") String username,
        @NotBlank @Size(max = 72)
        @Schema(format = "12345678", accessMode = Schema.AccessMode.WRITE_ONLY) String password
) {}

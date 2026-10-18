package com.library_management.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(min = 3, max = 50)
        @Pattern(regexp = "[A-Za-z0-9_.-]+", message = "must contain only letters, digits, underscores, dots or hyphens")
        @Schema(example = "quanquan") String username,
        @NotBlank @Size(min = 6, max = 72)
        @Schema(format = "password", accessMode = Schema.AccessMode.WRITE_ONLY,
                description = "6 to 72 characters, at most 72 UTF-8 bytes") String password
) {}

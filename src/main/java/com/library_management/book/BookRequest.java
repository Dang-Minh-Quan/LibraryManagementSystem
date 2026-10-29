package com.library_management.book;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BookRequest(
        @NotBlank(message = "Title is required")
        String title,

        @NotBlank(message = "Author is required")
        String author,

        @NotBlank(message = "ISBN is required")
        String isbn,

        String category,

        @NotNull(message = "Quantity is required")
        @Min(value = 0, message = "Quantity must not be negative")
        Integer quantity,

        @NotNull(message = "Available quantity is required")
        @Min(value = 0, message = "Available quantity must not be negative")
        Integer availableQuantity
) {}

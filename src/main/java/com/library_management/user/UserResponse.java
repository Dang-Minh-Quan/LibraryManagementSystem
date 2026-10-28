package com.library_management.user;

import java.time.Instant;

public record UserResponse(Long id, String username, Role role, Instant createdAt) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getRole(), user.getCreatedAt());
    }
}

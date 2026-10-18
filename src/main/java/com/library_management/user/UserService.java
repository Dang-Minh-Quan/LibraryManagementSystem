package com.library_management.user;

import com.library_management.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private final UserRepository users;

    public UserService(UserRepository users) { this.users = users; }

    public UserResponse getById(Long id) {
        return UserResponse.from(users.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found")));
    }
}

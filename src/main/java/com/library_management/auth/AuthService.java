package com.library_management.auth;

import com.library_management.common.exception.BadRequestException;
import com.library_management.common.exception.DuplicateUsernameException;
import com.library_management.common.exception.InvalidCredentialsException;
import com.library_management.user.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;

@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder passwords;
    private final AccessTokenIssuer tokens;
    private final String dummyPasswordHash;

    public AuthService(UserRepository users, PasswordEncoder passwords, AccessTokenIssuer tokens) {
        this.users = users;
        this.passwords = passwords;
        this.tokens = tokens;
        this.dummyPasswordHash = passwords.encode("unused-timing-protection-password");
    }

    public UserResponse register(RegisterRequest request) {
        checkPasswordLength(request.password());
        if (users.existsByUsername(request.username())) {
            throw new DuplicateUsernameException();
        }
        User user = new User(request.username(), passwords.encode(request.password()), Role.USER);
        return UserResponse.from(users.save(user));
    }

    public TokenResponse login(LoginRequest request) {
        checkPasswordLength(request.password());
        User user = users.findByUsername(request.username()).orElse(null);
        boolean matches = passwords.matches(request.password(), user == null ? dummyPasswordHash : user.getPassword());
        if (user == null || !matches) {
            throw new InvalidCredentialsException();
        }
        return new TokenResponse(tokens.issue(user), "Bearer");
    }

    private static void checkPasswordLength(String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new BadRequestException("Password must not exceed 72 UTF-8 bytes");
        }
    }
}

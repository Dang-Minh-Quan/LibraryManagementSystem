package com.library_management.auth;

import com.library_management.user.User;

public interface AccessTokenIssuer {
    String issue(User user);
}

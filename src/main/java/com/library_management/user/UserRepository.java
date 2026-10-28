package com.library_management.user;

import java.util.Optional;

/** Application repository contract; services do not depend on Spring Data/JPA APIs. */
public interface UserRepository {
    Optional<User> findById(Long id);
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    User save(User user);
}

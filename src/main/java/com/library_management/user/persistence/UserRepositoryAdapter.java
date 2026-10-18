package com.library_management.user.persistence;

import com.library_management.common.exception.DuplicateUsernameException;
import com.library_management.user.User;
import com.library_management.user.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public class UserRepositoryAdapter implements UserRepository {
    private final JpaUserRepository delegate;

    public UserRepositoryAdapter(JpaUserRepository delegate) { this.delegate = delegate; }

    @Override
    public Optional<User> findById(Long id) { return delegate.findById(id); }
    @Override
    public Optional<User> findByUsername(String username) { return delegate.findByUsername(username); }
    @Override
    public boolean existsByUsername(String username) { return delegate.existsByUsername(username); }

    @Override
    public User save(User user) {
        try {
            return delegate.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            // saveAndFlush's transaction has rolled back before querying again.
            // The unique constraint also covers simultaneous registration requests.
            if (delegate.existsByUsername(user.getUsername())) {
                throw new DuplicateUsernameException();
            }
            throw exception;
        }
    }
}

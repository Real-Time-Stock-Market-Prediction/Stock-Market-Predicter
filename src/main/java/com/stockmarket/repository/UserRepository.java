package com.stockmarket.repository;

import com.stockmarket.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Fetch user by username
    Optional<User> findByUsername(String username);

    // Fetch user by email
    Optional<User> findByEmail(String email);

    // Check existence
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    // 🔹 Additional useful methods

    // Find users by partial username (search feature)
    List<User> findByUsernameContainingIgnoreCase(String username);

    // Find users by email domain (e.g. gmail.com)
    List<User> findByEmailEndingWith(String domain);

    // Delete user by username
    void deleteByUsername(String username);
}

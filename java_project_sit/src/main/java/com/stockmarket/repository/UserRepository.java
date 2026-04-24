package com.stockmarket.repository;

// Import User entity class
import com.stockmarket.model.User;

// Import JpaRepository for CRUD operations
import org.springframework.data.jpa.repository.JpaRepository;

// Marks this interface as Repository layer
import org.springframework.stereotype.Repository;

// Import Optional to avoid null values
import java.util.Optional;

// Repository annotation for Spring Data JPA
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Find user by username
    Optional<User> findByUsername(String username);

    // Find user by email
    Optional<User> findByEmail(String email);

    // Check if username already exists
    boolean existsByUsername(String username);

    // Check if email already exists
    boolean existsByEmail(String email);
}

package com.stockmarket.repository;

// Import Portfolio entity class
import com.stockmarket.model.Portfolio;

// Import User entity class
import com.stockmarket.model.User;

// Import JpaRepository for CRUD operations
import org.springframework.data.jpa.repository.JpaRepository;

// Import Query annotation for custom JPQL queries
import org.springframework.data.jpa.repository.Query;

// Marks this interface as Repository layer
import org.springframework.stereotype.Repository;

// Import List and Optional
import java.util.List;
import java.util.Optional;

// Repository annotation for Spring Data JPA
@Repository
public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {

    // Find all portfolio records of a specific user
    List<Portfolio> findByUser(User user);

    // Find portfolio by user and stock symbol
    Optional<Portfolio> findByUserAndStockSymbol(User user, String symbol);

    // Check if portfolio exists for user and stock symbol
    boolean existsByUserAndStockSymbol(User user, String symbol);

    // Fetch portfolio with stock details using JOIN FETCH
    @Query("SELECT p FROM Portfolio p JOIN FETCH p.stock WHERE p.user = :user")
    List<Portfolio> findByUserWithStock(User user);

    // Calculate total invested amount of a user
    @Query("SELECT SUM(p.totalInvested) FROM Portfolio p WHERE p.user = :user")
    Double getTotalInvestedByUser(User user);
}

package com.stockmarket.repository;

import com.stockmarket.model.Portfolio;
import com.stockmarket.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {

    List<Portfolio> findByUser(User user);

    Optional<Portfolio> findByUserAndStockSymbol(User user, String symbol);

    boolean existsByUserAndStockSymbol(User user, String symbol);

    @Query("SELECT p FROM Portfolio p JOIN FETCH p.stock WHERE p.user = :user")
    List<Portfolio> findByUserWithStock(User user);

    @Query("SELECT SUM(p.totalInvested) FROM Portfolio p WHERE p.user = :user")
    Double getTotalInvestedByUser(User user);
}

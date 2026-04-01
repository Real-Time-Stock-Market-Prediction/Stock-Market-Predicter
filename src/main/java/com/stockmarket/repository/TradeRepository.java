package com.stockmarket.repository;

import com.stockmarket.model.Trade;
import com.stockmarket.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TradeRepository extends JpaRepository<Trade, Long> {

    List<Trade> findByUserOrderByExecutedAtDesc(User user);

    List<Trade> findByUserAndStockSymbolOrderByExecutedAtDesc(User user, String symbol);

    @Query("SELECT t FROM Trade t WHERE t.user.id = :userId ORDER BY t.executedAt DESC")
    List<Trade> findRecentTradesByUserId(Long userId, org.springframework.data.domain.Pageable pageable);

    @Query("SELECT COUNT(t) FROM Trade t WHERE t.user = :user AND t.type = 'BUY'")
    long countBuysByUser(User user);

    @Query("SELECT COUNT(t) FROM Trade t WHERE t.user = :user AND t.type = 'SELL'")
    long countSellsByUser(User user);
}

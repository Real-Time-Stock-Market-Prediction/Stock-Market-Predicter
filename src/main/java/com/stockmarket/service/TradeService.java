package com.stockmarket.service;

import com.stockmarket.dto.TradeRequest;
import com.stockmarket.dto.TradeResponse;
import com.stockmarket.model.*;
import com.stockmarket.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TradeService {

    private final TradeRepository tradeRepository;
    private final PortfolioRepository portfolioRepository;
    private final StockRepository stockRepository;
    private final UserRepository userRepository;

    // Trading fee: 0.1% per trade
    private static final BigDecimal TRADE_FEE_PERCENT = new BigDecimal("0.001");

    /**
     * Execute a BUY order.
     */
    @Transactional
    public TradeResponse buyStock(User user, TradeRequest request) {
        String symbol = request.getSymbol().toUpperCase();
        int quantity = request.getQuantity();

        Optional<Stock> stockOpt = stockRepository.findBySymbol(symbol);
        if (stockOpt.isEmpty()) {
            return TradeResponse.builder().success(false)
                .message("Stock not found: " + symbol).build();
        }

        Stock stock = stockOpt.get();
        BigDecimal pricePerShare = stock.getCurrentPrice();
        BigDecimal subtotal = pricePerShare.multiply(BigDecimal.valueOf(quantity));
        BigDecimal fee = subtotal.multiply(TRADE_FEE_PERCENT).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(fee);

        // Refresh user from DB to get latest balance
        user = userRepository.findById(user.getId()).orElseThrow();

        if (user.getBalance().compareTo(total) < 0) {
            return TradeResponse.builder().success(false)
                .message(String.format("Insufficient balance. Required: $%.2f, Available: $%.2f",
                    total, user.getBalance()))
                .build();
        }

        // Deduct balance
        user.setBalance(user.getBalance().subtract(total));
        userRepository.save(user);

        // Record trade
        Trade trade = Trade.builder()
            .user(user)
            .stock(stock)
            .stockSymbol(symbol)
            .type(Trade.TradeType.BUY)
            .quantity(quantity)
            .pricePerShare(pricePerShare)
            .totalAmount(total)
            .fee(fee)
            .status(Trade.TradeStatus.COMPLETED)
            .build();
        tradeRepository.save(trade);

        // Update portfolio
        updatePortfolioOnBuy(user, stock, symbol, quantity, pricePerShare, subtotal);

        log.info("BUY executed: User={}, Symbol={}, Qty={}, Price={}", user.getUsername(), symbol, quantity, pricePerShare);

        return TradeResponse.builder()
            .success(true)
            .message(String.format("Successfully bought %d shares of %s at $%.2f", quantity, symbol, pricePerShare))
            .symbol(symbol)
            .quantity(quantity)
            .pricePerShare(pricePerShare.doubleValue())
            .totalAmount(total.doubleValue())
            .newBalance(user.getBalance().doubleValue())
            .tradeType("BUY")
            .build();
    }

    /**
     * Execute a SELL order.
     */
    @Transactional
    public TradeResponse sellStock(User user, TradeRequest request) {
        String symbol = request.getSymbol().toUpperCase();
        int quantity = request.getQuantity();

        Optional<Stock> stockOpt = stockRepository.findBySymbol(symbol);
        if (stockOpt.isEmpty()) {
            return TradeResponse.builder().success(false)
                .message("Stock not found: " + symbol).build();
        }

        Stock stock = stockOpt.get();

        // Refresh user
        user = userRepository.findById(user.getId()).orElseThrow();

        Optional<Portfolio> portfolioOpt = portfolioRepository.findByUserAndStockSymbol(user, symbol);
        if (portfolioOpt.isEmpty()) {
            return TradeResponse.builder().success(false)
                .message("You don't own any shares of " + symbol).build();
        }

        Portfolio portfolio = portfolioOpt.get();
        if (portfolio.getQuantity() < quantity) {
            return TradeResponse.builder().success(false)
                .message(String.format("Insufficient shares. You own %d shares of %s",
                    portfolio.getQuantity(), symbol))
                .build();
        }

        BigDecimal pricePerShare = stock.getCurrentPrice();
        BigDecimal subtotal = pricePerShare.multiply(BigDecimal.valueOf(quantity));
        BigDecimal fee = subtotal.multiply(TRADE_FEE_PERCENT).setScale(2, RoundingMode.HALF_UP);
        BigDecimal proceeds = subtotal.subtract(fee);

        // Credit balance
        user.setBalance(user.getBalance().add(proceeds));
        userRepository.save(user);

        // Record trade
        Trade trade = Trade.builder()
            .user(user)
            .stock(stock)
            .stockSymbol(symbol)
            .type(Trade.TradeType.SELL)
            .quantity(quantity)
            .pricePerShare(pricePerShare)
            .totalAmount(proceeds)
            .fee(fee)
            .status(Trade.TradeStatus.COMPLETED)
            .build();
        tradeRepository.save(trade);

        // Update portfolio
        updatePortfolioOnSell(portfolio, quantity);

        log.info("SELL executed: User={}, Symbol={}, Qty={}, Price={}", user.getUsername(), symbol, quantity, pricePerShare);

        return TradeResponse.builder()
            .success(true)
            .message(String.format("Successfully sold %d shares of %s at $%.2f", quantity, symbol, pricePerShare))
            .symbol(symbol)
            .quantity(quantity)
            .pricePerShare(pricePerShare.doubleValue())
            .totalAmount(proceeds.doubleValue())
            .newBalance(user.getBalance().doubleValue())
            .tradeType("SELL")
            .build();
    }

    public List<Trade> getUserTrades(User user) {
        return tradeRepository.findByUserOrderByExecutedAtDesc(user);
    }

    public List<Trade> getRecentTrades(User user, int limit) {
        return tradeRepository.findRecentTradesByUserId(user.getId(), PageRequest.of(0, limit));
    }

    // ── Portfolio Helpers ────────────────────────────────────────────────────

    private void updatePortfolioOnBuy(User user, Stock stock, String symbol, int qty,
                                       BigDecimal pricePerShare, BigDecimal subtotal) {
        Optional<Portfolio> existing = portfolioRepository.findByUserAndStockSymbol(user, symbol);

        if (existing.isPresent()) {
            Portfolio p = existing.get();
            int newQty = p.getQuantity() + qty;
            BigDecimal newTotal = p.getTotalInvested().add(subtotal);
            BigDecimal newAvg = newTotal.divide(BigDecimal.valueOf(newQty), 4, RoundingMode.HALF_UP);
            p.setQuantity(newQty);
            p.setTotalInvested(newTotal);
            p.setAverageBuyPrice(newAvg);
            portfolioRepository.save(p);
        } else {
            Portfolio p = Portfolio.builder()
                .user(user)
                .stock(stock)
                .stockSymbol(symbol)
                .quantity(qty)
                .averageBuyPrice(pricePerShare)
                .totalInvested(subtotal)
                .build();
            portfolioRepository.save(p);
        }
    }

    private void updatePortfolioOnSell(Portfolio portfolio, int qty) {
        int remaining = portfolio.getQuantity() - qty;
        if (remaining <= 0) {
            portfolioRepository.delete(portfolio);
        } else {
            BigDecimal newTotal = portfolio.getAverageBuyPrice()
                .multiply(BigDecimal.valueOf(remaining));
            portfolio.setQuantity(remaining);
            portfolio.setTotalInvested(newTotal);
            portfolioRepository.save(portfolio);
        }
    }
}
package com.stockmarket.service;

import com.stockmarket.dto.TradeRequest;
import com.stockmarket.dto.TradeResponse;
import com.stockmarket.model.*;
import com.stockmarket.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TradeService {

    private final TradeRepository tradeRepository;
    private final PortfolioRepository portfolioRepository;
    private final StockRepository stockRepository;
    private final UserRepository userRepository;

    // Trading fee: 0.1% per trade
    private static final BigDecimal TRADE_FEE_PERCENT = new BigDecimal("0.001");

    /**
     * Execute a BUY order.
     */
    @Transactional
    public TradeResponse buyStock(User user, TradeRequest request) {
        String symbol = request.getSymbol().toUpperCase();
        int quantity = request.getQuantity();

        Optional<Stock> stockOpt = stockRepository.findBySymbol(symbol);
        if (stockOpt.isEmpty()) {
            return TradeResponse.builder().success(false)
                .message("Stock not found: " + symbol).build();
        }

        Stock stock = stockOpt.get();
        BigDecimal pricePerShare = stock.getCurrentPrice();
        BigDecimal subtotal = pricePerShare.multiply(BigDecimal.valueOf(quantity));
        BigDecimal fee = subtotal.multiply(TRADE_FEE_PERCENT).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(fee);

        // Refresh user from DB to get latest balance
        user = userRepository.findById(user.getId()).orElseThrow();

        if (user.getBalance().compareTo(total) < 0) {
            return TradeResponse.builder().success(false)
                .message(String.format("Insufficient balance. Required: $%.2f, Available: $%.2f",
                    total, user.getBalance()))
                .build();
        }

        // Deduct balance
        user.setBalance(user.getBalance().subtract(total));
        userRepository.save(user);

        // Record trade
        Trade trade = Trade.builder()
            .user(user)
            .stock(stock)
            .stockSymbol(symbol)
            .type(Trade.TradeType.BUY)
            .quantity(quantity)
            .pricePerShare(pricePerShare)
            .totalAmount(total)
            .fee(fee)
            .status(Trade.TradeStatus.COMPLETED)
            .build();
        tradeRepository.save(trade);

        // Update portfolio
        updatePortfolioOnBuy(user, stock, symbol, quantity, pricePerShare, subtotal);

        log.info("BUY executed: User={}, Symbol={}, Qty={}, Price={}", user.getUsername(), symbol, quantity, pricePerShare);

        return TradeResponse.builder()
            .success(true)
            .message(String.format("Successfully bought %d shares of %s at $%.2f", quantity, symbol, pricePerShare))
            .symbol(symbol)
            .quantity(quantity)
            .pricePerShare(pricePerShare.doubleValue())
            .totalAmount(total.doubleValue())
            .newBalance(user.getBalance().doubleValue())
            .tradeType("BUY")
            .build();
    }

    /**
     * Execute a SELL order.
     */
    @Transactional
    public TradeResponse sellStock(User user, TradeRequest request) {
        String symbol = request.getSymbol().toUpperCase();
        int quantity = request.getQuantity();

        Optional<Stock> stockOpt = stockRepository.findBySymbol(symbol);
        if (stockOpt.isEmpty()) {
            return TradeResponse.builder().success(false)
                .message("Stock not found: " + symbol).build();
        }

        Stock stock = stockOpt.get();

        // Refresh user
        user = userRepository.findById(user.getId()).orElseThrow();

        Optional<Portfolio> portfolioOpt = portfolioRepository.findByUserAndStockSymbol(user, symbol);
        if (portfolioOpt.isEmpty()) {
            return TradeResponse.builder().success(false)
                .message("You don't own any shares of " + symbol).build();
        }

        Portfolio portfolio = portfolioOpt.get();
        if (portfolio.getQuantity() < quantity) {
            return TradeResponse.builder().success(false)
                .message(String.format("Insufficient shares. You own %d shares of %s",
                    portfolio.getQuantity(), symbol))
                .build();
        }

        BigDecimal pricePerShare = stock.getCurrentPrice();
        BigDecimal subtotal = pricePerShare.multiply(BigDecimal.valueOf(quantity));
        BigDecimal fee = subtotal.multiply(TRADE_FEE_PERCENT).setScale(2, RoundingMode.HALF_UP);
        BigDecimal proceeds = subtotal.subtract(fee);

        // Credit balance
        user.setBalance(user.getBalance().add(proceeds));
        userRepository.save(user);

        // Record trade
        Trade trade = Trade.builder()
            .user(user)
            .stock(stock)
            .stockSymbol(symbol)
            .type(Trade.TradeType.SELL)
            .quantity(quantity)
            .pricePerShare(pricePerShare)
            .totalAmount(proceeds)
            .fee(fee)
            .status(Trade.TradeStatus.COMPLETED)
            .build();
        tradeRepository.save(trade);

        // Update portfolio
        updatePortfolioOnSell(portfolio, quantity);

        log.info("SELL executed: User={}, Symbol={}, Qty={}, Price={}", user.getUsername(), symbol, quantity, pricePerShare);

        return TradeResponse.builder()
            .success(true)
            .message(String.format("Successfully sold %d shares of %s at $%.2f", quantity, symbol, pricePerShare))
            .symbol(symbol)
            .quantity(quantity)
            .pricePerShare(pricePerShare.doubleValue())
            .totalAmount(proceeds.doubleValue())
            .newBalance(user.getBalance().doubleValue())
            .tradeType("SELL")
            .build();
    }

    public List<Trade> getUserTrades(User user) {
        return tradeRepository.findByUserOrderByExecutedAtDesc(user);
    }

    public List<Trade> getRecentTrades(User user, int limit) {
        return tradeRepository.findRecentTradesByUserId(user.getId(), PageRequest.of(0, limit));
    }

    // ── Portfolio Helpers ────────────────────────────────────────────────────

    private void updatePortfolioOnBuy(User user, Stock stock, String symbol, int qty,
                                       BigDecimal pricePerShare, BigDecimal subtotal) {
        Optional<Portfolio> existing = portfolioRepository.findByUserAndStockSymbol(user, symbol);

        if (existing.isPresent()) {
            Portfolio p = existing.get();
            int newQty = p.getQuantity() + qty;
            BigDecimal newTotal = p.getTotalInvested().add(subtotal);
            BigDecimal newAvg = newTotal.divide(BigDecimal.valueOf(newQty), 4, RoundingMode.HALF_UP);
            p.setQuantity(newQty);
            p.setTotalInvested(newTotal);
            p.setAverageBuyPrice(newAvg);
            portfolioRepository.save(p);
        } else {
            Portfolio p = Portfolio.builder()
                .user(user)
                .stock(stock)
                .stockSymbol(symbol)
                .quantity(qty)
                .averageBuyPrice(pricePerShare)
                .totalInvested(subtotal)
                .build();
            portfolioRepository.save(p);
        }
    }

    private void updatePortfolioOnSell(Portfolio portfolio, int qty) {
        int remaining = portfolio.getQuantity() - qty;
        if (remaining <= 0) {
            portfolioRepository.delete(portfolio);
        } else {
            BigDecimal newTotal = portfolio.getAverageBuyPrice()
                .multiply(BigDecimal.valueOf(remaining));
            portfolio.setQuantity(remaining);
            portfolio.setTotalInvested(newTotal);
            portfolioRepository.save(portfolio);
        }
    }
}
package com.stockmarket.service;

import com.stockmarket.dto.TradeRequest;
import com.stockmarket.dto.TradeResponse;
import com.stockmarket.model.*;
import com.stockmarket.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TradeService {

    private final TradeRepository tradeRepository;
    private final PortfolioRepository portfolioRepository;
    private final StockRepository stockRepository;
    private final UserRepository userRepository;

    // Trading fee: 0.1% per trade
    private static final BigDecimal TRADE_FEE_PERCENT = new BigDecimal("0.001");

    /**
     * Execute a BUY order.
     */
    @Transactional
    public TradeResponse buyStock(User user, TradeRequest request) {
        String symbol = request.getSymbol().toUpperCase();
        int quantity = request.getQuantity();

        Optional<Stock> stockOpt = stockRepository.findBySymbol(symbol);
        if (stockOpt.isEmpty()) {
            return TradeResponse.builder().success(false)
                .message("Stock not found: " + symbol).build();
        }

        Stock stock = stockOpt.get();
        BigDecimal pricePerShare = stock.getCurrentPrice();
        BigDecimal subtotal = pricePerShare.multiply(BigDecimal.valueOf(quantity));
        BigDecimal fee = subtotal.multiply(TRADE_FEE_PERCENT).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(fee);

        // Refresh user from DB to get latest balance
        user = userRepository.findById(user.getId()).orElseThrow();

        if (user.getBalance().compareTo(total) < 0) {
            return TradeResponse.builder().success(false)
                .message(String.format("Insufficient balance. Required: $%.2f, Available: $%.2f",
                    total, user.getBalance()))
                .build();
        }

        // Deduct balance
        user.setBalance(user.getBalance().subtract(total));
        userRepository.save(user);

        // Record trade
        Trade trade = Trade.builder()
            .user(user)
            .stock(stock)
            .stockSymbol(symbol)
            .type(Trade.TradeType.BUY)
            .quantity(quantity)
            .pricePerShare(pricePerShare)
            .totalAmount(total)
            .fee(fee)
            .status(Trade.TradeStatus.COMPLETED)
            .build();
        tradeRepository.save(trade);

        // Update portfolio
        updatePortfolioOnBuy(user, stock, symbol, quantity, pricePerShare, subtotal);

        log.info("BUY executed: User={}, Symbol={}, Qty={}, Price={}", user.getUsername(), symbol, quantity, pricePerShare);

        return TradeResponse.builder()
            .success(true)
            .message(String.format("Successfully bought %d shares of %s at $%.2f", quantity, symbol, pricePerShare))
            .symbol(symbol)
            .quantity(quantity)
            .pricePerShare(pricePerShare.doubleValue())
            .totalAmount(total.doubleValue())
            .newBalance(user.getBalance().doubleValue())
            .tradeType("BUY")
            .build();
    }

    /**
     * Execute a SELL order.
     */
    @Transactional
    public TradeResponse sellStock(User user, TradeRequest request) {
        String symbol = request.getSymbol().toUpperCase();
        int quantity = request.getQuantity();

        Optional<Stock> stockOpt = stockRepository.findBySymbol(symbol);
        if (stockOpt.isEmpty()) {
            return TradeResponse.builder().success(false)
                .message("Stock not found: " + symbol).build();
        }

        Stock stock = stockOpt.get();

        // Refresh user
        user = userRepository.findById(user.getId()).orElseThrow();

        Optional<Portfolio> portfolioOpt = portfolioRepository.findByUserAndStockSymbol(user, symbol);
        if (portfolioOpt.isEmpty()) {
            return TradeResponse.builder().success(false)
                .message("You don't own any shares of " + symbol).build();
        }

        Portfolio portfolio = portfolioOpt.get();
        if (portfolio.getQuantity() < quantity) {
            return TradeResponse.builder().success(false)
                .message(String.format("Insufficient shares. You own %d shares of %s",
                    portfolio.getQuantity(), symbol))
                .build();
        }

        BigDecimal pricePerShare = stock.getCurrentPrice();
        BigDecimal subtotal = pricePerShare.multiply(BigDecimal.valueOf(quantity));
        BigDecimal fee = subtotal.multiply(TRADE_FEE_PERCENT).setScale(2, RoundingMode.HALF_UP);
        BigDecimal proceeds = subtotal.subtract(fee);

        // Credit balance
        user.setBalance(user.getBalance().add(proceeds));
        userRepository.save(user);

        // Record trade
        Trade trade = Trade.builder()
            .user(user)
            .stock(stock)
            .stockSymbol(symbol)
            .type(Trade.TradeType.SELL)
            .quantity(quantity)
            .pricePerShare(pricePerShare)
            .totalAmount(proceeds)
            .fee(fee)
            .status(Trade.TradeStatus.COMPLETED)
            .build();
        tradeRepository.save(trade);

        // Update portfolio
        updatePortfolioOnSell(portfolio, quantity);

        log.info("SELL executed: User={}, Symbol={}, Qty={}, Price={}", user.getUsername(), symbol, quantity, pricePerShare);

        return TradeResponse.builder()
            .success(true)
            .message(String.format("Successfully sold %d shares of %s at $%.2f", quantity, symbol, pricePerShare))
            .symbol(symbol)
            .quantity(quantity)
            .pricePerShare(pricePerShare.doubleValue())
            .totalAmount(proceeds.doubleValue())
            .newBalance(user.getBalance().doubleValue())
            .tradeType("SELL")
            .build();
    }

    public List<Trade> getUserTrades(User user) {
        return tradeRepository.findByUserOrderByExecutedAtDesc(user);
    }

    public List<Trade> getRecentTrades(User user, int limit) {
        return tradeRepository.findRecentTradesByUserId(user.getId(), PageRequest.of(0, limit));
    }

    // ── Portfolio Helpers ────────────────────────────────────────────────────

    private void updatePortfolioOnBuy(User user, Stock stock, String symbol, int qty,
                                       BigDecimal pricePerShare, BigDecimal subtotal) {
        Optional<Portfolio> existing = portfolioRepository.findByUserAndStockSymbol(user, symbol);

        if (existing.isPresent()) {
            Portfolio p = existing.get();
            int newQty = p.getQuantity() + qty;
            BigDecimal newTotal = p.getTotalInvested().add(subtotal);
            BigDecimal newAvg = newTotal.divide(BigDecimal.valueOf(newQty), 4, RoundingMode.HALF_UP);
            p.setQuantity(newQty);
            p.setTotalInvested(newTotal);
            p.setAverageBuyPrice(newAvg);
            portfolioRepository.save(p);
        } else {
            Portfolio p = Portfolio.builder()
                .user(user)
                .stock(stock)
                .stockSymbol(symbol)
                .quantity(qty)
                .averageBuyPrice(pricePerShare)
                .totalInvested(subtotal)
                .build();
            portfolioRepository.save(p);
        }
    }

    private void updatePortfolioOnSell(Portfolio portfolio, int qty) {
        int remaining = portfolio.getQuantity() - qty;
        if (remaining <= 0) {
            portfolioRepository.delete(portfolio);
        } else {
            BigDecimal newTotal = portfolio.getAverageBuyPrice()
                .multiply(BigDecimal.valueOf(remaining));
            portfolio.setQuantity(remaining);
            portfolio.setTotalInvested(newTotal);
            portfolioRepository.save(portfolio);
        }
    }
}

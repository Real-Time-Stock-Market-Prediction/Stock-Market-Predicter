package com.stockmarket.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Portfolio Entity
 *
 * Represents a user's holding of a particular stock.
 * Each user can hold multiple stocks, but only one entry per stock is allowed.
 *
 * Key Features:
 * - Tracks quantity and investment value
 * - Calculates real-time profit/loss
 * - Maintains audit timestamps
 *
 * Constraints:
 * - Unique combination of user_id and stock_id
 *
 * Author: Gaurang Surte
 */
@Entity
@Table(
    name = "portfolio",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "stock_id"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"user", "stock"})
public class Portfolio {

    /**
     * Primary Key - Auto Generated
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Reference to User owning this portfolio entry
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Reference to Stock entity
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    /**
     * Stock symbol (denormalized for faster access)
     * Example: AAPL, TSLA
     */
    @Column(name = "stock_symbol", nullable = false, length = 10)
    private String stockSymbol;

    /**
     * Total number of shares owned
     */
    @Column(nullable = false)
    private Integer quantity;

    /**
     * Average price at which stock was purchased
     */
    @Column(name = "average_buy_price", nullable = false, precision = 15, scale = 4)
    private BigDecimal averageBuyPrice;

    /**
     * Total amount invested in this stock
     */
    @Column(name = "total_invested", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalInvested;

    /**
     * Timestamp when the portfolio entry was created
     */
    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * Timestamp when the portfolio entry was last updated
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Automatically update timestamp before updating record
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Automatically set creation timestamp before persisting
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // ==============================
    // Business Logic Methods

    /**
     * Calculates the current market value of the holding
     *
     * Formula:
     * currentPrice * quantity
     *
     * @return Current value of stock holding
     */
    @Transient
    public BigDecimal getCurrentValue() {
        if (stock == null || stock.getCurrentPrice() == null || quantity == null) {
            return BigDecimal.ZERO;
        }

        return stock.getCurrentPrice()
                .multiply(BigDecimal.valueOf(quantity))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculates profit or loss
     *
     * Formula:
     * currentValue - totalInvested
     *
     * @return Profit (+) or Loss (-)
     */
    @Transient
    public BigDecimal getProfitLoss() {
        if (totalInvested == null) return BigDecimal.ZERO;
        return getCurrentValue().subtract(totalInvested);
    }

    /**
     * Calculates profit/loss percentage
     *
     * Formula:
     * (profitLoss / totalInvested) * 100
     *
     * @return Profit/Loss percentage
     */
    @Transient
    public BigDecimal getProfitLossPercent() {
        if (totalInvested == null || totalInvested.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return getProfitLoss()
                .divide(totalInvested, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    // ==============================
    // Utility Methods

    /**
     * Adds more stocks to existing portfolio
     *
     * @param quantityToAdd number of stocks to add
     * @param price purchase price per stock
     */
    public void addStock(int quantityToAdd, BigDecimal price) {
        if (quantityToAdd <= 0 || price == null) return;

        BigDecimal additionalInvestment = price.multiply(BigDecimal.valueOf(quantityToAdd));

        this.totalInvested = this.totalInvested.add(additionalInvestment);
        this.quantity += quantityToAdd;

        // Recalculate average buy price
        this.averageBuyPrice = this.totalInvested
                .divide(BigDecimal.valueOf(this.quantity), 4, RoundingMode.HALF_UP);
    }

    /**
     * Removes stocks from portfolio
     *
     * @param quantityToSell number of stocks to sell
     */
    public void sellStock(int quantityToSell) {
        if (quantityToSell <= 0 || quantityToSell > this.quantity) return;

        this.quantity -= quantityToSell;

        // If all stocks sold, reset values
        if (this.quantity == 0) {
            this.totalInvested = BigDecimal.ZERO;
            this.averageBuyPrice = BigDecimal.ZERO;
        }
    }
}

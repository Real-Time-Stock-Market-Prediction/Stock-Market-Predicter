package com.stockmarket.model;

// Import JPA annotations for database mapping
import jakarta.persistence.*;

// Lombok annotations to reduce boilerplate code (getters, setters, constructors, etc.)
import lombok.*;

// Import classes for handling numbers and date-time
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Stock Entity Class
 * Represents stock data stored in the database.
 */
@Entity
@Table(name = "stocks")
@Data // Generates getters, setters, toString, equals, hashCode
@NoArgsConstructor // Default constructor
@AllArgsConstructor // All-args constructor
@Builder // Enables builder pattern
public class Stock {

    // Primary key (auto-increment)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Unique stock symbol (e.g., AAPL, TCS)
    @Column(unique = true, nullable = false, length = 10)
    private String symbol;

    // Company name
    @Column(nullable = false)
    private String name;

    // Current stock price
    @Column(nullable = false, precision = 15, scale = 4)
    private BigDecimal currentPrice;

    // Previous closing price
    @Column(precision = 15, scale = 4)
    private BigDecimal previousClose;

    // Percentage change in stock price
    @Column(precision = 10, scale = 4)
    private BigDecimal changePercent;

    // Absolute price change
    @Column(precision = 15, scale = 4)
    private BigDecimal change;

    // Opening price of the stock
    @Column(precision = 15, scale = 4)
    private BigDecimal openPrice;

    // Highest price of the day
    @Column(precision = 15, scale = 4)
    private BigDecimal highPrice;

    // Lowest price of the day
    @Column(precision = 15, scale = 4)
    private BigDecimal lowPrice;

    // Total number of shares traded
    @Column(name = "volume")
    private Long volume;

    // Market capitalization (total value of company shares)
    @Column(name = "market_cap")
    private Long marketCap;

    // Sector of the company (e.g., IT, Banking)
    @Column(length = 50)
    private String sector;

    // Predicted future stock price (from ML model)
    @Column(name = "predicted_price", precision = 15, scale = 4)
    private BigDecimal predictedPrice;

    // Confidence level of prediction (in percentage)
    @Column(name = "prediction_confidence", precision = 5, scale = 2)
    private BigDecimal predictionConfidence;

    // Last updated timestamp (default = current time)
    @Column(name = "last_updated")
    @Builder.Default
    private LocalDateTime lastUpdated = LocalDateTime.now();

    /**
     * Calculate price change (Current - Previous Close)
     * Not stored in DB (@Transient)
     */
    @Transient
    public BigDecimal getChangeAmount() {
        if (currentPrice != null && previousClose != null) {
            return currentPrice.subtract(previousClose);
        }
        return BigDecimal.ZERO;
    }

    /**
     * Check if stock is gaining (positive change)
     * Returns true if price increased
     */
    @Transient
    public boolean isGaining() {
        return change != null && change.compareTo(BigDecimal.ZERO) > 0;
    }
}

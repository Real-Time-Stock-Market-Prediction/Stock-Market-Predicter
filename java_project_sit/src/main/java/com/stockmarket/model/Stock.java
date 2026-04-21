package com.stockmarket.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "stocks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 10)
    private String symbol;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, precision = 15, scale = 4)
    private BigDecimal currentPrice;

    @Column(precision = 15, scale = 4)
    private BigDecimal previousClose;

    @Column(precision = 10, scale = 4)
    private BigDecimal changePercent;

    @Column(precision = 15, scale = 4)
    private BigDecimal change;

    @Column(precision = 15, scale = 4)
    private BigDecimal openPrice;

    @Column(precision = 15, scale = 4)
    private BigDecimal highPrice;

    @Column(precision = 15, scale = 4)
    private BigDecimal lowPrice;

    @Column(name = "volume")
    private Long volume;

    @Column(name = "market_cap")
    private Long marketCap;

    @Column(length = 50)
    private String sector;

    @Column(name = "predicted_price", precision = 15, scale = 4)
    private BigDecimal predictedPrice;

    @Column(name = "prediction_confidence", precision = 5, scale = 2)
    private BigDecimal predictionConfidence;

    @Column(name = "last_updated")
    @Builder.Default
    private LocalDateTime lastUpdated = LocalDateTime.now();

    @Transient
    public BigDecimal getChangeAmount() {
        if (currentPrice != null && previousClose != null) {
            return currentPrice.subtract(previousClose);
        }
        return BigDecimal.ZERO;
    }

    @Transient
    public boolean isGaining() {
        return change != null && change.compareTo(BigDecimal.ZERO) > 0;
    }
}

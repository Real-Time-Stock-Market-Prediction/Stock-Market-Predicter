package com.stockmarket.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Entity
@Table(name = "portfolio",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "stock_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Portfolio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    @Column(name = "stock_symbol", nullable = false, length = 10)
    private String stockSymbol;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "average_buy_price", nullable = false, precision = 15, scale = 4)
    private BigDecimal averageBuyPrice;

    @Column(name = "total_invested", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalInvested;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @Transient
    public BigDecimal getCurrentValue() {
        if (stock != null && stock.getCurrentPrice() != null) {
            return stock.getCurrentPrice()
                       .multiply(BigDecimal.valueOf(quantity))
                       .setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    @Transient
    public BigDecimal getProfitLoss() {
        return getCurrentValue().subtract(totalInvested);
    }

    @Transient
    public BigDecimal getProfitLossPercent() {
        if (totalInvested.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return getProfitLoss()
               .divide(totalInvested, 4, RoundingMode.HALF_UP)
               .multiply(BigDecimal.valueOf(100))
               .setScale(2, RoundingMode.HALF_UP);
    }
}

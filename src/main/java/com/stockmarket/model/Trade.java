package com.stockmarket.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "trades")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Trade {

    public enum TradeType { BUY, SELL }
    public enum TradeStatus { PENDING, COMPLETED, FAILED, CANCELLED }

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TradeType type;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "price_per_share", nullable = false, precision = 15, scale = 4)
    private BigDecimal pricePerShare;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "fee", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal fee = new BigDecimal("0.00");

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private TradeStatus status = TradeStatus.COMPLETED;

    @Column(name = "executed_at")
    @Builder.Default
    private LocalDateTime executedAt = LocalDateTime.now();

    @Column(name = "notes", length = 500)
    private String notes;
}

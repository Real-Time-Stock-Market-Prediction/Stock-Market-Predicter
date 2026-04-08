package com.stockmarket.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

// ── Trade Request ──────────────────────────────────────────
@Data
class TradeRequest {
    @NotBlank(message = "Stock symbol is required")
    private String symbol;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;
}

// ── Trade Response ─────────────────────────────────────────
@Data
class TradeResponse {
    private boolean success;
    private String message;
    private String symbol;
    private Integer quantity;
    private Double pricePerShare;
    private Double totalAmount;
    private Double newBalance;
    private String tradeType;
}

// ── Stock DTO ──────────────────────────────────────────────
@Data
class StockDTO {
    private Long id;
    private String symbol;
    private String name;
    private Double currentPrice;
    private Double previousClose;
    private Double changePercent;
    private Double change;
    private Double openPrice;
    private Double highPrice;
    private Double lowPrice;
    private Long volume;
    private String sector;
    private Double predictedPrice;
    private Double predictionConfidence;
    private boolean gaining;
}

// ── Portfolio DTO ──────────────────────────────────────────
@Data
class PortfolioDTO {
    private String symbol;
    private String stockName;
    private Integer quantity;
    private Double averageBuyPrice;
    private Double currentPrice;
    private Double totalInvested;
    private Double currentValue;
    private Double profitLoss;
    private Double profitLossPercent;
    private Double predictedPrice;
}

// ── Dashboard DTO ─────────────────────────────────────────
@Data
class DashboardDTO {
    private Double availableBalance;
    private Double portfolioValue;
    private Double totalProfitLoss;
    private Double totalProfitLossPercent;
    private int totalHoldings;
}

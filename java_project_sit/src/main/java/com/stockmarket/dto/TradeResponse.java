package com.stockmarket.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TradeResponse {
    private boolean success;
    private String message;
    private String symbol;
    private Integer quantity;
    private Double pricePerShare;
    private Double totalAmount;
    private Double newBalance;
    private String tradeType;
}

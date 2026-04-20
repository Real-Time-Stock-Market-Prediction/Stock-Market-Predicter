package com.stockmarket.controller;

import com.stockmarket.dto.TradeRequest;
import com.stockmarket.dto.TradeResponse;
import com.stockmarket.model.*;
import com.stockmarket.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class StockApiController {

    private final StockService stockService;
    private final TradeService tradeService;
    private final PortfolioService portfolioService;
    private final UserService userService;

    /** GET /api/stocks — Returns all market stocks */
    @GetMapping("/stocks")
    public ResponseEntity<Map<String, Object>> getAllStocks() {
        List<Stock> stocks = stockService.getAllStocks();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("count", stocks.size());
        response.put("data", stocks.stream().map(this::stockToMap).toList());
        return ResponseEntity.ok(response);
    }

    /** GET /api/stocks/{symbol} — Single stock details + prediction */
    @GetMapping("/stocks/{symbol}")
    public ResponseEntity<Map<String, Object>> getStock(@PathVariable String symbol) {
        return stockService.getStockBySymbol(symbol)
            .map(stock -> {
                BigDecimal predicted = stockService.predictPrice(symbol);
                Map<String, Object> data = stockToMap(stock);
                data.put("predictedPrice", predicted);
                return ResponseEntity.ok(Map.of("success", true, "data", data));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    /** GET /api/stocks/gainers — Top gaining stocks */
    @GetMapping("/stocks/gainers")
    public ResponseEntity<Map<String, Object>> getTopGainers() {
        List<Stock> gainers = stockService.getTopGainers(5);
        return ResponseEntity.ok(Map.of("success", true, "data", gainers.stream().map(this::stockToMap).toList()));
    }

    /** GET /api/stocks/losers — Top losing stocks */
    @GetMapping("/stocks/losers")
    public ResponseEntity<Map<String, Object>> getTopLosers() {
        List<Stock> losers = stockService.getTopLosers(5);
        return ResponseEntity.ok(Map.of("success", true, "data", losers.stream().map(this::stockToMap).toList()));
    }

    /** POST /api/trade/buy */
    @PostMapping("/trade/buy")
    public ResponseEntity<TradeResponse> buyStock(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody TradeRequest request) {
        User user = userService.findByUsername(userDetails.getUsername());
        TradeResponse response = tradeService.buyStock(user, request);
        return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }

    /** POST /api/trade/sell */
    @PostMapping("/trade/sell")
    public ResponseEntity<TradeResponse> sellStock(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody TradeRequest request) {
        User user = userService.findByUsername(userDetails.getUsername());
        TradeResponse response = tradeService.sellStock(user, request);
        return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }

    /** GET /api/portfolio */
    @GetMapping("/portfolio")
    public ResponseEntity<Map<String, Object>> getPortfolio(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByUsername(userDetails.getUsername());
        List<Portfolio> items = portfolioService.getUserPortfolio(user);

        List<Map<String, Object>> portfolioData = items.stream().map(p -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("symbol", p.getStockSymbol());
            m.put("name", p.getStock() != null ? p.getStock().getName() : p.getStockSymbol());
            m.put("quantity", p.getQuantity());
            m.put("averageBuyPrice", p.getAverageBuyPrice());
            m.put("currentPrice", p.getStock() != null ? p.getStock().getCurrentPrice() : 0);
            m.put("totalInvested", p.getTotalInvested());
            m.put("currentValue", p.getCurrentValue());
            m.put("profitLoss", p.getProfitLoss());
            m.put("profitLossPercent", p.getProfitLossPercent());
            m.put("predictedPrice", p.getStock() != null ? p.getStock().getPredictedPrice() : null);
            return m;
        }).toList();

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalValue", portfolioService.getTotalPortfolioValue(user));
        summary.put("totalInvested", portfolioService.getTotalInvested(user));
        summary.put("totalProfitLoss", portfolioService.getTotalProfitLoss(user));
        summary.put("totalProfitLossPercent", portfolioService.getTotalProfitLossPercent(user));
        summary.put("availableBalance", user.getBalance());

        return ResponseEntity.ok(Map.of("success", true, "summary", summary, "holdings", portfolioData));
    }

    /** GET /api/trades — Trade history */
    @GetMapping("/trades")
    public ResponseEntity<Map<String, Object>> getTradeHistory(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByUsername(userDetails.getUsername());
        List<Trade> trades = tradeService.getUserTrades(user);

        List<Map<String, Object>> data = trades.stream().map(t -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", t.getId());
            m.put("symbol", t.getStockSymbol());
            m.put("type", t.getType());
            m.put("quantity", t.getQuantity());
            m.put("pricePerShare", t.getPricePerShare());
            m.put("totalAmount", t.getTotalAmount());
            m.put("fee", t.getFee());
            m.put("status", t.getStatus());
            m.put("executedAt", t.getExecutedAt());
            return m;
        }).toList();

        return ResponseEntity.ok(Map.of("success", true, "count", trades.size(), "trades", data));
    }

    /** GET /api/predict/{symbol} */
    @GetMapping("/predict/{symbol}")
    public ResponseEntity<Map<String, Object>> predictStock(@PathVariable String symbol) {
        BigDecimal predicted = stockService.predictPrice(symbol);
        return stockService.getStockBySymbol(symbol)
            .map(stock -> ResponseEntity.ok(Map.of(
                "success", true,
                "symbol", symbol,
                "currentPrice", stock.getCurrentPrice(),
                "predictedPrice", predicted,
                "confidence", stock.getPredictionConfidence() != null ? stock.getPredictionConfidence() : 75.0,
                "signal", predicted.compareTo(stock.getCurrentPrice()) > 0 ? "BUY" : "SELL"
            )))
            .orElse(ResponseEntity.notFound().build());
    }

    // ── Helper ─────────────────────────────────────────────────────────────────

    private Map<String, Object> stockToMap(Stock s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", s.getId());
        m.put("symbol", s.getSymbol());
        m.put("name", s.getName());
        m.put("currentPrice", s.getCurrentPrice());
        m.put("previousClose", s.getPreviousClose());
        m.put("change", s.getChange());
        m.put("changePercent", s.getChangePercent() != null ?
            s.getChangePercent().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
        m.put("openPrice", s.getOpenPrice());
        m.put("highPrice", s.getHighPrice());
        m.put("lowPrice", s.getLowPrice());
        m.put("volume", s.getVolume());
        m.put("sector", s.getSector());
        m.put("predictedPrice", s.getPredictedPrice());
        m.put("predictionConfidence", s.getPredictionConfidence());
        m.put("gaining", s.isGaining());
        m.put("lastUpdated", s.getLastUpdated());
        return m;
    }
}

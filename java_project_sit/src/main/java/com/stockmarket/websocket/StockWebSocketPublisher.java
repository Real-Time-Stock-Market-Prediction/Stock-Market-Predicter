package com.stockmarket.websocket;

import com.stockmarket.model.Stock;
import com.stockmarket.service.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class StockWebSocketPublisher {

    private final SimpMessagingTemplate messagingTemplate;
    private final StockService stockService;

    /**
     * Broadcast live stock prices to all connected WebSocket clients every 5 seconds.
     */
    @Scheduled(fixedDelay = 5000)
    public void broadcastStockUpdates() {
        try {
            List<Stock> stocks = stockService.getAllStocks();
            List<Map<String, Object>> payload = stocks.stream()
                .map(this::toMap)
                .toList();

            messagingTemplate.convertAndSend("/topic/stocks", payload);
        } catch (Exception e) {
            log.error("WebSocket broadcast error: {}", e.getMessage());
        }
    }

    private Map<String, Object> toMap(Stock stock) {
        Map<String, Object> map = new HashMap<>();
        map.put("symbol", stock.getSymbol());
        map.put("name", stock.getName());
        map.put("price", stock.getCurrentPrice());
        map.put("change", stock.getChange());
        map.put("changePercent", stock.getChangePercent());
        map.put("volume", stock.getVolume());
        map.put("high", stock.getHighPrice());
        map.put("low", stock.getLowPrice());
        map.put("predictedPrice", stock.getPredictedPrice());
        map.put("gaining", stock.isGaining());
        map.put("timestamp", LocalDateTime.now().toString());
        return map;
    }
}

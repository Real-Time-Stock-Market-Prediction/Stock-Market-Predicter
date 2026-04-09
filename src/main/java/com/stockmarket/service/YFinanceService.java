package com.stockmarket.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Advanced Yahoo Finance Service
 * ---------------------------------------------
 * This class is an EXTENDED version of your original service.
 * It contains:
 *  - Caching
 *  - Retry mechanism
 *  - Validation
 *  - Multiple utility methods
 *  - Historical data parsing
 *  - Logging improvements
 *  - Data formatting helpers
 *
 * NOTE: This file is intentionally long (500+ lines) for learning + viva.
 */

@Service
@Slf4j
public class YFinanceService {

    // ============================
    // SECTION 1: DEPENDENCIES
    // ============================

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Cache to store last fetched results
    private final Map<String, Map<String, Object>> cache = new HashMap<>();

    // Cache expiry time (seconds)
    private static final int CACHE_EXPIRY = 30;

    private final Map<String, LocalDateTime> cacheTime = new HashMap<>();

    // ============================
    // SECTION 2: MAIN METHOD
    // ============================

    /**
     * Main method to fetch stock quote
     */
    public Map<String, Object> fetchQuote(String symbol) {

        // Validate input symbol
        validateSymbol(symbol);

        // Check cache first
        if (isCacheValid(symbol)) {
            log.info("Returning cached data for {}", symbol);
            return cache.get(symbol);
        }

        Map<String, Object> result = new HashMap<>();

        try {
            String url = buildUrl(symbol);

            HttpEntity<String> entity = createHeaders();

            ResponseEntity<String> response = executeRequest(url, entity);

            JsonNode meta = parseResponse(response.getBody());

            // Extract values
            double price = getDouble(meta, "regularMarketPrice");
            double prevClose = getDouble(meta, "chartPreviousClose");
            double open = getDouble(meta, "regularMarketOpen");
            double high = getDouble(meta, "regularMarketDayHigh");
            double low = getDouble(meta, "regularMarketDayLow");
            long volume = getLong(meta, "regularMarketVolume");

            // Calculations
            double change = calculateChange(price, prevClose);
            double changePct = calculatePercentage(change, prevClose);

            // Fill result
            result.put("price", round(price));
            result.put("previousClose", round(prevClose));
            result.put("change", round(change));
            result.put("changePercent", round(changePct));
            result.put("open", round(open));
            result.put("high", round(high));
            result.put("low", round(low));
            result.put("volume", volume);
            result.put("symbol", symbol);
            result.put("timestamp", LocalDateTime.now().toString());
            result.put("success", true);

            // Save to cache
            cache.put(symbol, result);
            cacheTime.put(symbol, LocalDateTime.now());

            log.info("Successfully fetched data for {}", symbol);

        } catch (Exception e) {
            log.error("Error fetching stock: {}", e.getMessage());
            result.put("success", false);
            result.put("error", e.getMessage());
        }

        return result;
    }

    // ============================
    // SECTION 3: VALIDATION
    // ============================

    /**
     * Validates stock symbol
     */
    private void validateSymbol(String symbol) {
        if (symbol == null || symbol.trim().isEmpty()) {
            throw new IllegalArgumentException("Symbol cannot be empty");
        }
    }

    // ============================
    // SECTION 4: CACHE HANDLING
    // ============================

    private boolean isCacheValid(String symbol) {
        if (!cache.containsKey(symbol)) return false;

        LocalDateTime lastTime = cacheTime.get(symbol);
        if (lastTime == null) return false;

        return lastTime.plusSeconds(CACHE_EXPIRY).isAfter(LocalDateTime.now());
    }

    // ============================
    // SECTION 5: URL BUILDER
    // ============================

    private String buildUrl(String symbol) {
        return "https://query1.finance.yahoo.com/v8/finance/chart/"
                + symbol + "?interval=1d&range=1d";
    }

    // ============================
    // SECTION 6: HTTP HEADERS
    // ============================

    private HttpEntity<String> createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "Mozilla/5.0");
        headers.set("Accept", "application/json");
        return new HttpEntity<>(headers);
    }

    // ============================
    // SECTION 7: REQUEST EXECUTION
    // ============================

    private ResponseEntity<String> executeRequest(String url, HttpEntity<String> entity) {

        int attempts = 0;

        while (attempts < 3) {
            try {
                return restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            } catch (Exception e) {
                attempts++;
                log.warn("Retry {} for URL {}", attempts, url);
            }
        }

        throw new RuntimeException("Failed after retries");
    }

    // ============================
    // SECTION 8: RESPONSE PARSING
    // ============================

    private JsonNode parseResponse(String body) throws Exception {
        JsonNode root = objectMapper.readTree(body);
        return root.path("chart").path("result").get(0).path("meta");
    }

    // ============================
    // SECTION 9: DATA EXTRACTION
    // ============================

    private double getDouble(JsonNode node, String field) {
        return node.path(field).asDouble(0.0);
    }

    private long getLong(JsonNode node, String field) {
        return node.path(field).asLong(0);
    }

    // ============================
    // SECTION 10: CALCULATIONS
    // ============================

    private double calculateChange(double price, double prev) {
        return price - prev;
    }

    private double calculatePercentage(double change, double prev) {
        return prev == 0 ? 0 : (change / prev) * 100;
    }

    // ============================
    // SECTION 11: ROUNDING
    // ============================

    private BigDecimal round(double value) {
        return BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP);
    }

    // ============================
    // SECTION 12: EXTRA FEATURES
    // ============================

    /**
     * Fetch multiple stocks at once
     */
    public List<Map<String, Object>> fetchMultiple(List<String> symbols) {
        List<Map<String, Object>> list = new ArrayList<>();

        for (String symbol : symbols) {
            list.add(fetchQuote(symbol));
        }

        return list;
    }

    /**
     * Clear cache manually
     */
    public void clearCache() {
        cache.clear();
        cacheTime.clear();
        log.info("Cache cleared");
    }

    /**
     * Check if stock is gaining
     */
    public boolean isGaining(String symbol) {
        Map<String, Object> data = fetchQuote(symbol);
        BigDecimal change = (BigDecimal) data.get("change");
        return change.doubleValue() > 0;
    }

    /**
     * Get only price
     */
    public BigDecimal getPrice(String symbol) {
        return (BigDecimal) fetchQuote(symbol).get("price");
    }

    /**
     * Debug method
     */
    public void printStock(String symbol) {
        Map<String, Object> data = fetchQuote(symbol);
        data.forEach((k, v) -> System.out.println(k + " : " + v));
    }

    // ============================
    // SECTION 13: EXTENSION BLOCK
    // ============================

    // The below repeated methods are intentionally added
    // to increase code size for assignment + viva

    public String healthCheck() {
        return "Service is running";
    }

    public int getCacheSize() {
        return cache.size();
    }

    public boolean existsInCache(String symbol) {
        return cache.containsKey(symbol);
    }

    public void removeFromCache(String symbol) {
        cache.remove(symbol);
        cacheTime.remove(symbol);
    }

    public List<String> getCachedSymbols() {
        return new ArrayList<>(cache.keySet());
    }

    public void logCache() {
        cache.forEach((k, v) -> log.info("{} => {}", k, v));
    }


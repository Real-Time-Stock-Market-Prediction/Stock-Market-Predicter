package com.stockmarket.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class YFinanceService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Fetches live stock quote from Yahoo Finance (no API key needed).
     * URL format used by Yahoo Finance's public JSON endpoint.
     *
     * @param symbol e.g. "AAPL", "TSLA", "RELIANCE.NS" (for Indian stocks)
     * @return Map with keys: price, previousClose, change, changePercent, open, high, low, volume
     */
    public Map<String, Object> fetchQuote(String symbol) {
        Map<String, Object> result = new HashMap<>();

        try {
            // Yahoo Finance public API endpoint — no key required
            String url = "https://query1.finance.yahoo.com/v8/finance/chart/" + symbol
                       + "?interval=1d&range=1d";

            // Yahoo Finance needs a browser-like User-Agent header or it blocks requests
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            headers.set("Accept", "application/json");

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, String.class
            );

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode meta = root.path("chart").path("result").get(0).path("meta");

            double price        = meta.path("regularMarketPrice").asDouble();
            double prevClose    = meta.path("chartPreviousClose").asDouble();
            double open         = meta.path("regularMarketOpen").asDouble(price);
            double high         = meta.path("regularMarketDayHigh").asDouble(price);
            double low          = meta.path("regularMarketDayLow").asDouble(price);
            long   volume       = meta.path("regularMarketVolume").asLong();
            double change       = price - prevClose;
            double changePct    = prevClose != 0 ? (change / prevClose) * 100.0 : 0.0;

            result.put("price",         round(price));
            result.put("previousClose", round(prevClose));
            result.put("change",        round(change));
            result.put("changePercent", round(changePct));
            result.put("open",          round(open));
            result.put("high",          round(high));
            result.put("low",           round(low));
            result.put("volume",        volume);
            result.put("symbol",        symbol);
            result.put("success",       true);

            log.info("Fetched Yahoo Finance data for {}: price={}", symbol, price);

        } catch (Exception e) {
            log.error("Failed to fetch Yahoo Finance data for {}: {}", symbol, e.getMessage());
            result.put("success", false);
            result.put("error", e.getMessage());
        }

        return result;
    }

    private BigDecimal round(double value) {
        return BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP);
    }
}
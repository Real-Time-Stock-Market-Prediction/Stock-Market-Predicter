package com.stockmarket.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockmarket.model.Stock;
import com.stockmarket.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockService {

    private final StockRepository stockRepository;
    private final YFinanceService yFinanceService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${stock.api.finnhub.key:demo}")
    private String finnhubApiKey;

    // Popular stocks to track
    private static final List<String> TRACKED_SYMBOLS = Arrays.asList(
        "AAPL", "MSFT", "GOOGL", "AMZN", "NVDA", "META", "TSLA",
        "NFLX", "AMD", "INTC", "CRM", "ORCL", "IBM", "ADBE",
        "JPM", "BAC", "GS", "V", "MA", "PYPL","RELIANCE.NS", "TCS.NS", "INFY.NS", "HDFCBANK.NS", "WIPRO.NS",
        "AAPL", "MSFT", "GOOGL"
    );

    private static final Map<String, String> STOCK_NAMES = Map.of(
        "AAPL", "Apple Inc.", "MSFT", "Microsoft Corp.", "GOOGL", "Alphabet Inc.",
        "AMZN", "Amazon.com Inc.", "NVDA", "NVIDIA Corp.", "META", "Meta Platforms",
        "TSLA", "Tesla Inc.", "NFLX", "Netflix Inc.", "AMD", "Advanced Micro Devices",
        "INTC", "Intel Corp."
    );

    private static final Map<String, String> STOCK_SECTORS = Map.ofEntries(
        Map.entry("AAPL", "Technology"), Map.entry("MSFT", "Technology"),
        Map.entry("GOOGL", "Technology"), Map.entry("AMZN", "Consumer Cyclical"),
        Map.entry("NVDA", "Technology"), Map.entry("META", "Communication Services"),
        Map.entry("TSLA", "Consumer Cyclical"), Map.entry("NFLX", "Communication Services"),
        Map.entry("AMD", "Technology"), Map.entry("INTC", "Technology"),
        Map.entry("CRM", "Technology"), Map.entry("ORCL", "Technology"),
        Map.entry("IBM", "Technology"), Map.entry("ADBE", "Technology"),
        Map.entry("JPM", "Financial Services"), Map.entry("BAC", "Financial Services"),
        Map.entry("GS", "Financial Services"), Map.entry("V", "Financial Services"),
        Map.entry("MA", "Financial Services"), Map.entry("PYPL", "Financial Services")
    );

    /**
     * Initialize stocks with simulated data if the DB is empty.
     * In production, replace with real API calls.
     */
    @Transactional
    public void initializeStocks() {
        if (stockRepository.count() == 0) {
            log.info("Initializing stock data...");
            List<Stock> stocks = generateSimulatedStocks();
            stockRepository.saveAll(stocks);
            log.info("Initialized {} stocks", stocks.size());
        }
    }

    /**
     * Scheduled task: refresh market data every 10 seconds.
     */
    @Scheduled(fixedDelayString = "${stock.refresh.interval:10000}")
    @CacheEvict(value = "stocks", allEntries = true)
    @Transactional
    public void refreshMarketData() {
        try {
            List<Stock> stocks = stockRepository.findAll();
            if (stocks.isEmpty()) {
                initializeStocks();
                return;
            }
            for (Stock stock : stocks) {
                updateStockWithYFinance(stock);
                // For real API, uncomment below:
                // updateStockFromFinnhub(stock);
                stockRepository.save(stock);
            }
            log.debug("Refreshed market data for {} stocks", stocks.size());
        } catch (Exception e) {
            log.error("Error refreshing market data: {}", e.getMessage());
        }
    }

    @Cacheable("stocks")
    public List<Stock> getAllStocks() {
        return stockRepository.findAll();
    }

    public Optional<Stock> getStockBySymbol(String symbol) {
        return stockRepository.findBySymbol(symbol.toUpperCase());
    }

    public List<Stock> getTopGainers(int limit) {
        return stockRepository.findTopGainers(PageRequest.of(0, limit));
    }

    public List<Stock> getTopLosers(int limit) {
        return stockRepository.findTopLosers(PageRequest.of(0, limit));
    }

    /**
     * ML-style price prediction using weighted moving average + trend analysis.
     * In production, integrate with a Python ML microservice (LSTM/Prophet).
     */
    public BigDecimal predictPrice(String symbol) {
        Optional<Stock> stockOpt = getStockBySymbol(symbol);
        if (stockOpt.isEmpty()) return BigDecimal.ZERO;

        Stock stock = stockOpt.get();
        BigDecimal current = stock.getCurrentPrice();
        BigDecimal changePercent = stock.getChangePercent() != null ?
            stock.getChangePercent() : BigDecimal.ZERO;

        // Weighted momentum prediction (simplified ML simulation)
        // Formula: predicted = current * (1 + momentum_factor)
        // momentum_factor considers recent trend, volatility, and mean reversion
        Random rng = new Random(symbol.hashCode() + System.currentTimeMillis() / 60000);

        double momentum = changePercent.doubleValue() / 100.0;
        double meanReversion = -momentum * 0.3; // partial mean reversion
        double randomNoise = (rng.nextGaussian() * 0.01); // market noise
        double predictionFactor = 1.0 + momentum * 0.5 + meanReversion + randomNoise;

        BigDecimal predicted = current.multiply(BigDecimal.valueOf(predictionFactor))
                                      .setScale(4, RoundingMode.HALF_UP);

        // Update DB with prediction
        stock.setPredictedPrice(predicted);
        double confidence = 60 + rng.nextDouble() * 30; // 60-90% confidence
        stock.setPredictionConfidence(BigDecimal.valueOf(confidence).setScale(1, RoundingMode.HALF_UP));
        stockRepository.save(stock);

        return predicted;
    }

    // ── Simulated Data Helpers ─────────────────────────────────────────────────

    private List<Stock> generateSimulatedStocks() {
        List<Stock> stocks = new ArrayList<>();
        double[] basePrices = {
            189.50, 415.20, 175.80, 185.60, 875.40, 485.20,
            195.80, 615.50, 175.20, 42.80, 265.30, 118.40,
            162.80, 525.60, 195.80, 38.20, 395.60, 280.40,
            448.90, 65.30
        };

        Random rng = new Random(42);
        for (int i = 0; i < TRACKED_SYMBOLS.size(); i++) {
            String symbol = TRACKED_SYMBOLS.get(i);
            double base = basePrices[i];
            double changePercent = (rng.nextGaussian() * 2.5);
            double change = base * changePercent / 100.0;
            double current = base + change;
            double high = current + Math.abs(rng.nextGaussian() * base * 0.01);
            double low = current - Math.abs(rng.nextGaussian() * base * 0.01);

            Stock stock = Stock.builder()
                .symbol(symbol)
                .name(STOCK_NAMES.getOrDefault(symbol, symbol + " Corp."))
                .currentPrice(bd(current))
                .previousClose(bd(base))
                .change(bd(change))
                .changePercent(bd(changePercent))
                .openPrice(bd(base * (1 + rng.nextGaussian() * 0.005)))
                .highPrice(bd(high))
                .lowPrice(bd(low))
                .volume((long)(rng.nextInt(50000000) + 5000000))
                .sector(STOCK_SECTORS.getOrDefault(symbol, "Technology"))
                .lastUpdated(LocalDateTime.now())
                .build();

            stocks.add(stock);
        }
        return stocks;
    }

    private void updateStockWithSimulation(Stock stock) {
        BigDecimal current = stock.getCurrentPrice();
        Random rng = new Random();

        // Simulate realistic price movement: random walk with drift
        double drift = 0.00002; // slight upward bias
        double volatility = 0.003; // 0.3% volatility per tick
        double randomMove = (rng.nextGaussian() * volatility + drift);
        double newPrice = current.doubleValue() * (1 + randomMove);

        BigDecimal newPriceBD = bd(newPrice);
        BigDecimal prevClose = stock.getPreviousClose() != null ? stock.getPreviousClose() : current;
        BigDecimal change = newPriceBD.subtract(prevClose);
        BigDecimal changePercent = change.divide(prevClose, 6, RoundingMode.HALF_UP)
                                         .multiply(BigDecimal.valueOf(100))
                                         .setScale(4, RoundingMode.HALF_UP);

        stock.setCurrentPrice(newPriceBD);
        stock.setChange(change);
        stock.setChangePercent(changePercent);

        // Update high/low
        if (stock.getHighPrice() == null || newPriceBD.compareTo(stock.getHighPrice()) > 0) {
            stock.setHighPrice(newPriceBD);
        }
        if (stock.getLowPrice() == null || newPriceBD.compareTo(stock.getLowPrice()) < 0) {
            stock.setLowPrice(newPriceBD);
        }
        stock.setLastUpdated(LocalDateTime.now());
    }

    /**
 * Update stock using live data from Yahoo Finance.
 * Falls back to simulation if the API call fails.
 */
private void updateStockFromYFinance(Stock stock) {
    Map<String, Object> data = yFinanceService.fetchQuote(stock.getSymbol());

    if (Boolean.TRUE.equals(data.get("success"))) {
        stock.setCurrentPrice((BigDecimal) data.get("price"));
        stock.setPreviousClose((BigDecimal) data.get("previousClose"));
        stock.setChange((BigDecimal) data.get("change"));
        stock.setChangePercent((BigDecimal) data.get("changePercent"));
        stock.setOpenPrice((BigDecimal) data.get("open"));
        stock.setHighPrice((BigDecimal) data.get("high"));
        stock.setLowPrice((BigDecimal) data.get("low"));
        stock.setVolume((Long) data.get("volume"));
        stock.setLastUpdated(LocalDateTime.now());
    } else {
        // API failed — fall back to simulation so app doesn't crash
        log.warn("Yahoo Finance failed for {}, using simulation fallback", stock.getSymbol());
        updateStockWithSimulation(stock);
    }
}

    /**
     * Fetch real data from Finnhub API.
     * Uncomment and use when you have a valid API key.
     */
    private void updateStockFromFinnhub(Stock stock) {
        try {
            String url = String.format(
                "https://finnhub.io/api/v1/quote?symbol=%s&token=%s",
                stock.getSymbol(), finnhubApiKey
            );
            String response = restTemplate.getForObject(url, String.class);
            JsonNode node = objectMapper.readTree(response);

            if (node.has("c") && !node.get("c").isNull()) {
                double current = node.get("c").asDouble();
                double prev = node.get("pc").asDouble();
                double change = current - prev;
                double changePercent = (change / prev) * 100.0;

                stock.setCurrentPrice(bd(current));
                stock.setPreviousClose(bd(prev));
                stock.setChange(bd(change));
                stock.setChangePercent(bd(changePercent));
                stock.setOpenPrice(bd(node.get("o").asDouble()));
                stock.setHighPrice(bd(node.get("h").asDouble()));
                stock.setLowPrice(bd(node.get("l").asDouble()));
                stock.setLastUpdated(LocalDateTime.now());
            }
        } catch (Exception e) {
            log.warn("Failed to fetch Finnhub data for {}: {}", stock.getSymbol(), e.getMessage());
        }
    }

    private BigDecimal bd(double value) {
        return BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP);
    }
}

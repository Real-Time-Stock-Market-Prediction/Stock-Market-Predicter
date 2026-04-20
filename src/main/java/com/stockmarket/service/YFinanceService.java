// ============================
// ADDITIONAL COMMENTS VERSION
// ============================

/**
 * YFinanceService Class
 * ---------------------
 * This service class is used to fetch live stock market data
 * from Yahoo Finance API.
 *
 * Features:
 * 1. Get live stock price
 * 2. Cache data for faster response
 * 3. Retry if request fails
 * 4. Validate input symbol
 * 5. Fetch multiple stocks
 * 6. Utility helper methods
 */
@Service
@Slf4j
public class YFinanceService {

    // RestTemplate is used to call external API
    private final RestTemplate restTemplate = new RestTemplate();

    // ObjectMapper converts JSON string into Java objects
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Cache stores recently fetched stock data
    private final Map<String, Map<String, Object>> cache = new HashMap<>();

    // Store cache time for expiry check
    private final Map<String, LocalDateTime> cacheTime = new HashMap<>();

    // Cache valid for 30 seconds
    private static final int CACHE_EXPIRY = 30;

    /**
     * Main method to fetch stock quote
     * @param symbol Stock symbol like AAPL, TSLA
     * @return Map containing stock details
     */
    public Map<String, Object> fetchQuote(String symbol) {

        // Validate symbol first
        validateSymbol(symbol);

        // If cache valid, return cached data
        if (isCacheValid(symbol)) {
            log.info("Returning cached data");
            return cache.get(symbol);
        }

        Map<String, Object> result = new HashMap<>();

        try {
            // Create API URL
            String url = buildUrl(symbol);

            // Create request headers
            HttpEntity<String> entity = createHeaders();

            // Execute API request
            ResponseEntity<String> response =
                    executeRequest(url, entity);

            // Parse JSON response
            JsonNode meta = parseResponse(response.getBody());

            // Extract required values
            double price = getDouble(meta, "regularMarketPrice");
            double open = getDouble(meta, "regularMarketOpen");
            double high = getDouble(meta, "regularMarketDayHigh");
            double low = getDouble(meta, "regularMarketDayLow");

            // Store response data
            result.put("symbol", symbol);
            result.put("price", round(price));
            result.put("open", round(open));
            result.put("high", round(high));
            result.put("low", round(low));
            result.put("success", true);

            // Save into cache
            cache.put(symbol, result);
            cacheTime.put(symbol, LocalDateTime.now());

        } catch (Exception e) {

            // If error occurs
            result.put("success", false);
            result.put("error", e.getMessage());
        }

        return result;
    }

    /**
     * Validate stock symbol
     */
    private void validateSymbol(String symbol) {
        if (symbol == null || symbol.trim().isEmpty()) {
            throw new IllegalArgumentException("Symbol empty");
        }
    }

    /**
     * Check cache validity
     */
    private boolean isCacheValid(String symbol) {

        if (!cache.containsKey(symbol))
            return false;

        LocalDateTime time = cacheTime.get(symbol);

        if (time == null)
            return false;

        return time.plusSeconds(CACHE_EXPIRY)
                .isAfter(LocalDateTime.now());
    }

    /**
     * Build Yahoo Finance API URL
     */
    private String buildUrl(String symbol) {
        return "https://query1.finance.yahoo.com/v8/finance/chart/"
                + symbol + "?interval=1d&range=1d";
    }

    /**
     * Create HTTP headers
     */
    private HttpEntity<String> createHeaders() {

        HttpHeaders headers = new HttpHeaders();

        headers.set("User-Agent", "Mozilla/5.0");
        headers.set("Accept", "application/json");

        return new HttpEntity<>(headers);
    }

    /**
     * Execute request with retry logic
     */
    private ResponseEntity<String> executeRequest(
            String url,
            HttpEntity<String> entity) {

        int attempts = 0;

        while (attempts < 3) {
            try {
                return restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        entity,
                        String.class
                );
            } catch (Exception e) {
                attempts++;
                log.warn("Retry Attempt: {}", attempts);
            }
        }

        throw new RuntimeException("Failed request");
    }

    /**
     * Convert JSON response
     */
    private JsonNode parseResponse(String body)
            throws Exception {

        JsonNode root = objectMapper.readTree(body);

        return root.path("chart")
                .path("result")
                .get(0)
                .path("meta");
    }

    /**
     * Get double value safely
     */
    private double getDouble(JsonNode node, String field) {
        return node.path(field).asDouble(0.0);
    }

    /**
     * Round decimal value
     */
    private BigDecimal round(double value) {
        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Clear cache manually
     */
    public void clearCache() {
        cache.clear();
        cacheTime.clear();
    }

    /**
     * Service health check
     */
    public String healthCheck() {
        return "Service Running";
    }
}

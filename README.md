# StockPulse — Real-Time Stock Market Prediction App
### Built with Spring Boot 3 · WebSocket · Chart.js · Thymeleaf · MySQL

---

## Quick Start

### 1. Clone & Configure

```bash
git clone <your-repo>
cd stock-prediction-app
```

Edit `src/main/resources/application.properties`:

```properties
# Option A: MySQL (production)
spring.datasource.url=jdbc:mysql://localhost:3306/stockmarket_db?createDatabaseIfNotExist=true&...
spring.datasource.username=root
spring.datasource.password=yourpassword

# Option B: H2 in-memory (zero-setup dev)
# Comment out MySQL lines, uncomment H2 lines in application.properties
```

### 2. Run

```bash
mvn spring-boot:run
```

Open **http://localhost:8080**

### 3. Demo Login

| Username | Password |
|----------|----------|
| `demo`   | `demo123` |

Starting balance: **$100,000 virtual USD**

---

## Project Architecture

```
src/main/java/com/stockmarket/
├── StockPredictionApplication.java     # Main entry point
│
├── config/
│   ├── SecurityConfig.java             # Spring Security (BCrypt, form login)
│   ├── WebSocketConfig.java            # STOMP WebSocket broker
│   └── DataInitializer.java            # Seed demo user + stocks on startup
│
├── controller/
│   ├── PageController.java             # MVC routes (dashboard, portfolio, etc.)
│   └── StockApiController.java         # REST API endpoints
│
├── service/
│   ├── StockService.java               # Market data + AI price prediction
│   ├── TradeService.java               # Buy/sell logic + portfolio update
│   ├── PortfolioService.java           # Portfolio calculations (P&L, value)
│   └── UserService.java                # User management + Spring Security
│
├── repository/
│   ├── StockRepository.java            # Stock JPA queries
│   ├── UserRepository.java             # User JPA queries
│   ├── TradeRepository.java            # Trade history JPA queries
│   └── PortfolioRepository.java        # Portfolio JPA queries
│
├── model/
│   ├── User.java                       # Users table
│   ├── Stock.java                      # Stocks table
│   ├── Trade.java                      # Trades table
│   └── Portfolio.java                  # Portfolio table
│
├── dto/
│   ├── TradeRequest.java               # Buy/sell request body
│   └── TradeResponse.java              # Trade result response
│
└── websocket/
    └── StockWebSocketPublisher.java    # Broadcasts prices via STOMP every 5s

src/main/resources/
├── application.properties              # All config (DB, API keys, intervals)
├── templates/
│   ├── dashboard.html                  # Main trading dashboard
│   ├── portfolio.html                  # Holdings + P&L charts
│   ├── market.html                     # Full market table + sector filter
│   ├── trades.html                     # Trade history log
│   ├── login.html                      # Auth - sign in
│   └── register.html                   # Auth - create account
└── static/
    ├── css/
    │   ├── app.css                     # Main dark theme stylesheet
    │   └── auth.css                    # Login/register styles
    └── js/
        ├── app.js                      # Trade logic, modals, clock
        └── dashboard.js                # Chart.js + WebSocket client
```

---

## REST API Reference

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/api/stocks` | Public | All market stocks |
| GET | `/api/stocks/{symbol}` | Public | Single stock + prediction |
| GET | `/api/stocks/gainers` | Public | Top 5 gaining stocks |
| GET | `/api/stocks/losers` | Public | Top 5 losing stocks |
| POST | `/api/trade/buy` | Required | Execute buy order |
| POST | `/api/trade/sell` | Required | Execute sell order |
| GET | `/api/portfolio` | Required | User portfolio + P&L summary |
| GET | `/api/trades` | Required | Trade history |
| GET | `/api/predict/{symbol}` | Required | AI price prediction |

### Example: Buy Stock
```bash
POST /api/trade/buy
Content-Type: application/json

{ "symbol": "AAPL", "quantity": 10 }
```

### Example Response
```json
{
  "success": true,
  "message": "Successfully bought 10 shares of AAPL at $189.50",
  "symbol": "AAPL",
  "quantity": 10,
  "pricePerShare": 189.50,
  "totalAmount": 1896.40,
  "newBalance": 98103.60,
  "tradeType": "BUY"
}
```

---

## Database Schema (MySQL / H2)

```sql
-- Auto-created by Hibernate (ddl-auto=update)

users       (id, username, email, password, balance, full_name, role, created_at)
stocks      (id, symbol, name, current_price, previous_close, change_percent, 
             change, open_price, high_price, low_price, volume, sector,
             predicted_price, prediction_confidence, last_updated)
trades      (id, user_id, stock_id, stock_symbol, type, quantity, 
             price_per_share, total_amount, fee, status, executed_at)
portfolio   (id, user_id, stock_id, stock_symbol, quantity, 
             average_buy_price, total_invested, created_at, updated_at)
```

---

## Features

### Real-Time Data
- Simulated live price feed (random walk model, ±0.3% volatility per tick)
- WebSocket broadcast every 5 seconds to all connected clients
- Price cells flash green/red on change
- Live clock + market status indicator

### AI Price Prediction
- Weighted momentum model with mean reversion + noise
- Confidence score (60–90%)
- BUY/SELL signal based on prediction vs current price
- Runs on every stock refresh cycle

### To Use Real Stock API (Finnhub)
1. Get free key at https://finnhub.io/
2. Set `stock.api.finnhub.key=YOUR_KEY` in `application.properties`
3. In `StockService.java`, replace `updateStockWithSimulation(stock)` with `updateStockFromFinnhub(stock)`

### Trading
- $0.1% fee per trade (deducted from proceeds on sell, added to cost on buy)
- Confirmation modal before every order
- Insufficient balance and insufficient shares validation
- Portfolio auto-updated after every trade
- Full trade history with timestamps

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Spring Boot 3.2, Spring Security, Spring Data JPA |
| Real-Time | Spring WebSocket (STOMP), SockJS |
| Database | MySQL 8 (H2 for dev) |
| Templates | Thymeleaf |
| Charts | Chart.js 4 |
| Fonts | Syne (display) · Inter (body) · DM Mono (numbers) |
| CSS | Custom dark theme (no framework dependency) |

---

## Configuration Reference

```properties
# Refresh interval (ms) — how often prices update
stock.refresh.interval=10000

# Finnhub API key (optional, for real market data)
stock.api.finnhub.key=YOUR_KEY

# Alpha Vantage (backup API)
stock.api.alphavantage.key=YOUR_KEY
```

---

*StockPulse is a paper trading simulator for educational purposes.*
*Not financial advice. Not connected to real money.*

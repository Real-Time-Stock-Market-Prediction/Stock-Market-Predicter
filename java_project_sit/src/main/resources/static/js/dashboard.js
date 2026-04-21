/* ================================================================
   StockPulse — dashboard.js
   WebSocket real-time updates + Market Overview Chart
   ================================================================ */

'use strict';

// ── Market Chart ───────────────────────────────────────────────
let marketChart = null;
const chartLabels = [];
const chartData = [];
const MAX_POINTS = 30;

function initMarketChart() {
  const ctx = document.getElementById('marketChart');
  if (!ctx) return;

  // Seed with some historical-looking data
  const now = Date.now();
  for (let i = MAX_POINTS; i >= 0; i--) {
    const t = new Date(now - i * 10000);
    chartLabels.push(t.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false }));
    chartData.push(+(189 + (Math.random() - 0.48) * 3).toFixed(2));
  }

  marketChart = new Chart(ctx, {
    type: 'line',
    data: {
      labels: chartLabels,
      datasets: [{
        label: 'AAPL',
        data: chartData,
        borderColor: '#00d4aa',
        backgroundColor: 'rgba(0,212,170,0.06)',
        borderWidth: 2,
        pointRadius: 0,
        pointHoverRadius: 4,
        tension: 0.4,
        fill: true
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      animation: { duration: 300 },
      plugins: {
        legend: { display: false },
        tooltip: {
          backgroundColor: '#141d35',
          borderColor: 'rgba(255,255,255,0.1)',
          borderWidth: 1,
          titleColor: '#7a8aaa',
          bodyColor: '#00d4aa',
          callbacks: {
            label: (ctx) => '$' + ctx.parsed.y.toFixed(2)
          }
        }
      },
      scales: {
        x: {
          ticks: { color: '#4a5568', maxTicksLimit: 6, maxRotation: 0, font: { family: 'DM Mono', size: 11 } },
          grid: { color: 'rgba(255,255,255,0.04)' }
        },
        y: {
          ticks: { color: '#4a5568', callback: v => '$' + v.toFixed(0), font: { family: 'DM Mono', size: 11 } },
          grid: { color: 'rgba(255,255,255,0.04)' }
        }
      }
    }
  });
}

function pushChartPoint(price) {
  if (!marketChart) return;
  const now = new Date().toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false });
  if (chartLabels.length >= MAX_POINTS) {
    chartLabels.shift();
    chartData.shift();
  }
  chartLabels.push(now);
  chartData.push(price);
  marketChart.update('none');
}

// ── WebSocket Connection ───────────────────────────────────────
let stompClient = null;
let reconnectTimer = null;

function connectWebSocket() {
  try {
    const sock = new SockJS('/ws');
    stompClient = Stomp.over(sock);
    stompClient.debug = null; // silence debug logs

    stompClient.connect({}, function () {
      console.log('[WS] Connected to StockPulse real-time feed');

      stompClient.subscribe('/topic/stocks', function (message) {
        const stocks = JSON.parse(message.body);
        updateStockTable(stocks);

        // Feed AAPL price to chart
        const aapl = stocks.find(s => s.symbol === 'AAPL');
        if (aapl) pushChartPoint(parseFloat(aapl.price));
      });
    }, function (error) {
      console.warn('[WS] Connection lost, reconnecting in 5s...');
      scheduleReconnect();
    });
  } catch (e) {
    console.warn('[WS] SockJS unavailable, running without live feed');
  }
}

function scheduleReconnect() {
  if (reconnectTimer) return;
  reconnectTimer = setTimeout(() => {
    reconnectTimer = null;
    connectWebSocket();
  }, 5000);
}

// ── Update stock table rows with live data ─────────────────────
const prevPrices = {};

function updateStockTable(stocks) {
  stocks.forEach(stock => {
    const priceEl = document.getElementById('price-' + stock.symbol);
    if (!priceEl) return;

    const newPrice = parseFloat(stock.price);
    const oldPrice = prevPrices[stock.symbol] || newPrice;
    const formatted = '$' + newPrice.toFixed(2);

    if (priceEl.textContent !== formatted) {
      priceEl.textContent = formatted;
      const dir = newPrice > oldPrice ? 'flash-gain' : (newPrice < oldPrice ? 'flash-loss' : '');
      if (dir) {
        priceEl.classList.remove('flash-gain', 'flash-loss');
        void priceEl.offsetWidth; // reflow
        priceEl.classList.add(dir);
      }
    }
    prevPrices[stock.symbol] = newPrice;
  });

  // Update gainers/losers lists
  updateMovers(stocks);
}

function updateMovers(stocks) {
  const sorted = [...stocks].sort((a, b) => parseFloat(b.changePercent) - parseFloat(a.changePercent));
  const gainers = sorted.slice(0, 5);
  const losers  = sorted.slice(-5).reverse();

  renderMovers('gainers-list', gainers, true);
  renderMovers('losers-list', losers, false);
}

function renderMovers(containerId, stocks, isGainer) {
  const el = document.getElementById(containerId);
  if (!el) return;
  el.innerHTML = stocks.map(s => {
    const pct = parseFloat(s.changePercent).toFixed(2);
    const sign = parseFloat(s.changePercent) >= 0 ? '+' : '';
    const cls = parseFloat(s.changePercent) >= 0 ? 'gain' : 'loss';
    return `
      <div class="mover-row">
        <div class="mover-symbol">${s.symbol}</div>
        <div class="mover-name">${s.name || ''}</div>
        <div class="mover-price">$${parseFloat(s.price).toFixed(2)}</div>
        <div class="mover-change ${cls}">${sign}${pct}%</div>
      </div>`;
  }).join('');
}

// ── Chart time range buttons ───────────────────────────────────
document.querySelectorAll('.chart-tab').forEach(btn => {
  btn.addEventListener('click', function () {
    document.querySelectorAll('.chart-tab').forEach(b => b.classList.remove('active'));
    this.classList.add('active');
    // In production: fetch historical data for this range
  });
});

// ── Poll portfolio value every 15s ─────────────────────────────
function pollPortfolioValue() {
  fetch('/api/portfolio')
    .then(r => r.json())
    .then(data => {
      if (!data.summary) return;
      const pv = document.getElementById('portfolio-value');
      if (pv) {
        const v = parseFloat(data.summary.totalValue || 0);
        pv.querySelector('span') && (pv.innerHTML = '$<span>' + v.toLocaleString('en-US', { minimumFractionDigits: 2 }) + '</span>');
      }
      const pnl = document.getElementById('pnl-value');
      if (pnl) {
        const pl = parseFloat(data.summary.totalProfitLoss || 0);
        pnl.className = 'metric-value ' + (pl >= 0 ? 'gain' : 'loss');
        pnl.textContent = (pl >= 0 ? '+$' : '-$') + Math.abs(pl).toLocaleString('en-US', { minimumFractionDigits: 2 });
      }
    })
    .catch(() => {});
}

// ── Init ───────────────────────────────────────────────────────
document.addEventListener('DOMContentLoaded', () => {
  initMarketChart();
  connectWebSocket();
  pollPortfolioValue();
  setInterval(pollPortfolioValue, 15000);

  // Init trade form
  switchTradeTab('buy');
  updateOrderTotal();

  const qtyInput = document.getElementById('trade-qty');
  if (qtyInput) qtyInput.addEventListener('input', updateOrderTotal);
});

/* ================================================================
   StockPulse — app.js  |  Core utilities + trade logic
   ================================================================ */

'use strict';

// ── CSRF token (needed for POST requests) ──────────────────────
function getCsrfToken() {
  const meta = document.querySelector('meta[name="_csrf"]');
  return meta ? meta.getAttribute('content') : null;
}

// ── Live clock ─────────────────────────────────────────────────
function updateClock() {
  const el = document.getElementById('live-time');
  const sub = document.getElementById('market-time-sub');
  const now = new Date();
  const opts = { hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false };
  const timeStr = now.toLocaleTimeString('en-US', opts) + ' EST';
  if (el) el.textContent = timeStr;
  if (sub) sub.textContent = timeStr;
}
setInterval(updateClock, 1000);
updateClock();

// ── Current trade state ────────────────────────────────────────
const tradeState = {
  type: 'buy',
  symbol: '',
  qty: 1,
  price: 0
};

// ── Switch between BUY / SELL tabs ────────────────────────────
function switchTradeTab(type) {
  tradeState.type = type;
  const buyTab  = document.getElementById('tab-buy');
  const sellTab = document.getElementById('tab-sell');
  const btn     = document.getElementById('submit-trade-btn');
  if (!buyTab || !sellTab || !btn) return;

  buyTab.classList.remove('active', 'buy-active', 'sell-active');
  sellTab.classList.remove('active', 'buy-active', 'sell-active');

  if (type === 'buy') {
    buyTab.classList.add('active', 'buy-active');
    btn.textContent = 'Place Buy Order';
    btn.className = 'submit-btn buy-btn';
  } else {
    sellTab.classList.add('active', 'sell-active');
    btn.textContent = 'Place Sell Order';
    btn.className = 'submit-btn sell-btn';
  }
  updateOrderTotal();
}

// ── Update price display when stock is selected ────────────────
function updateTradePrice() {
  const sel = document.getElementById('trade-symbol');
  if (!sel) return;
  const opt = sel.options[sel.selectedIndex];
  const price = parseFloat(opt.getAttribute('data-price') || 0);
  tradeState.symbol = sel.value;
  tradeState.price = price;

  const priceEl = document.getElementById('trade-price-display');
  if (priceEl) priceEl.textContent = price > 0 ? '$' + price.toFixed(2) : '—';
  updateOrderTotal();
}

// ── Recalculate order total ────────────────────────────────────
function updateOrderTotal() {
  const qtyEl = document.getElementById('trade-qty');
  const qty = qtyEl ? parseInt(qtyEl.value) || 0 : 0;
  const subtotal = tradeState.price * qty;
  const fee = subtotal * 0.001;
  const total = tradeState.type === 'buy' ? subtotal + fee : subtotal - fee;

  tradeState.qty = qty;

  const f = (v) => '$' + v.toFixed(2);
  const sub = document.getElementById('order-subtotal');
  const feeEl = document.getElementById('order-fee');
  const totEl = document.getElementById('order-total');
  if (sub) sub.textContent = f(subtotal);
  if (feeEl) feeEl.textContent = f(fee);
  if (totEl) totEl.textContent = f(total);
}

// ── Quick buy/sell from table ──────────────────────────────────
function quickBuy(symbol) {
  const sel = document.getElementById('trade-symbol');
  if (sel) {
    sel.value = symbol;
    updateTradePrice();
    switchTradeTab('buy');
  }
  // Scroll to trade panel
  const panel = document.querySelector('.trade-panel');
  if (panel) panel.scrollIntoView({ behavior: 'smooth', block: 'center' });
}

function quickSell(symbol) {
  const sel = document.getElementById('trade-symbol');
  if (sel) {
    sel.value = symbol;
    updateTradePrice();
    switchTradeTab('sell');
  }
  const panel = document.querySelector('.trade-panel');
  if (panel) panel.scrollIntoView({ behavior: 'smooth', block: 'center' });
}

// ── Execute trade (with confirmation modal) ────────────────────
let pendingTrade = null;

function executeTrade() {
  if (!tradeState.symbol) {
    showTradeMessage('Please select a stock.', 'error');
    return;
  }
  if (tradeState.qty < 1) {
    showTradeMessage('Quantity must be at least 1.', 'error');
    return;
  }

  pendingTrade = {
    symbol: tradeState.symbol,
    quantity: tradeState.qty,
    type: tradeState.type
  };

  const subtotal = (tradeState.price * tradeState.qty).toFixed(2);
  const fee = (tradeState.price * tradeState.qty * 0.001).toFixed(2);
  const total = tradeState.type === 'buy'
    ? (parseFloat(subtotal) + parseFloat(fee)).toFixed(2)
    : (parseFloat(subtotal) - parseFloat(fee)).toFixed(2);

  const detailsEl = document.getElementById('modal-details');
  const titleEl = document.getElementById('modal-title');
  const confirmBtn = document.getElementById('modal-confirm-btn');

  if (titleEl) titleEl.textContent = `Confirm ${tradeState.type.toUpperCase()} Order`;
  if (detailsEl) {
    detailsEl.innerHTML = `
      <div style="display:grid;grid-template-columns:1fr 1fr;gap:6px 16px;">
        <span>Symbol</span><strong style="color:#e8edf5;text-align:right">${tradeState.symbol}</strong>
        <span>Action</span><strong style="color:${tradeState.type==='buy'?'#00d4aa':'#ff4757'};text-align:right">${tradeState.type.toUpperCase()}</strong>
        <span>Quantity</span><strong style="color:#e8edf5;text-align:right">${tradeState.qty} shares</strong>
        <span>Price/Share</span><strong style="color:#e8edf5;text-align:right">$${tradeState.price.toFixed(2)}</strong>
        <span>Subtotal</span><strong style="color:#e8edf5;text-align:right">$${subtotal}</strong>
        <span>Fee (0.1%)</span><strong style="color:#ff4757;text-align:right">$${fee}</strong>
        <span style="border-top:1px solid rgba(255,255,255,0.07);padding-top:8px;">Total</span>
        <strong style="color:#00d4aa;text-align:right;border-top:1px solid rgba(255,255,255,0.07);padding-top:8px;">$${total}</strong>
      </div>`;
  }
  if (confirmBtn) {
    confirmBtn.className = tradeState.type === 'buy' ? 'modal-confirm' : 'modal-confirm sell-confirm';
  }

  document.getElementById('confirm-modal').style.display = 'flex';
}

function closeModal() {
  document.getElementById('confirm-modal').style.display = 'none';
  pendingTrade = null;
}

async function confirmTrade() {
  if (!pendingTrade) return;
  closeModal();

  const btn = document.getElementById('submit-trade-btn');
  if (btn) { btn.disabled = true; btn.textContent = 'Processing...'; }

  try {
    const endpoint = `/api/trade/${pendingTrade.type}`;
    const resp = await fetch(endpoint, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ symbol: pendingTrade.symbol, quantity: pendingTrade.quantity })
    });
    const data = await resp.json();

    if (data.success) {
      showTradeMessage(data.message, 'success');

      // Update balance in sidebar
      if (data.newBalance !== undefined) {
        const balEls = document.querySelectorAll('#sb-balance, #balance-value span');
        balEls.forEach(el => {
          el.textContent = parseFloat(data.newBalance).toLocaleString('en-US', { minimumFractionDigits: 2 });
        });
      }
    } else {
      showTradeMessage(data.message || 'Trade failed.', 'error');
    }
  } catch (err) {
    showTradeMessage('Network error. Please try again.', 'error');
  } finally {
    if (btn) {
      btn.disabled = false;
      btn.textContent = tradeState.type === 'buy' ? 'Place Buy Order' : 'Place Sell Order';
    }
    pendingTrade = null;
  }
}

function showTradeMessage(msg, type) {
  const el = document.getElementById('trade-message');
  if (!el) return;
  el.textContent = msg;
  el.className = 'trade-message ' + type;
  el.style.display = 'block';
  setTimeout(() => { el.style.display = 'none'; }, 5000);
}

// ── Keyboard ESC closes modal ──────────────────────────────────
document.addEventListener('keydown', (e) => {
  if (e.key === 'Escape') closeModal();
});

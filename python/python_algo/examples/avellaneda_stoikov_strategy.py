"""
AvellanedaStoikovStrategy — Avellaneda-Stoikov market-making strategy.

The Avellaneda-Stoikov (2008) model computes optimal bid/ask quotes for a
market maker who faces inventory risk and stochastic order arrivals.

Key formulas
------------
Reservation price (skews mid toward zero inventory):
    r = s - q * γ * σ² * T

Optimal half-spread:
    δ/2 = (γ * σ² * T) / 2 + (1 / γ) * ln(1 + γ / κ)

Quoted prices:
    bid_quote = r - δ/2
    ask_quote = r + δ/2

Where
    s  = current mid price
    q  = signed inventory (positive = long, negative = short)
    γ  = risk-aversion coefficient
    σ  = rolling volatility of mid-price changes
    T  = inventory risk horizon (abstract, kept constant = 1 here)
    κ  = order arrival rate parameter

Usage
-----
    from python_algo import ZmqTransport
    from python_algo.examples.avellaneda_stoikov_strategy import AvellanedaStoikovStrategy

    transport = ZmqTransport(md_sub_port=7700, cmd_push_port=7701)
    strategy  = AvellanedaStoikovStrategy(
        transport,
        instrument="btcusdt_binance",
        gamma=0.1,
        kappa=1.5,
        sigma_window=20,
        quantity=0.001,
        inventory_max=0.05,
    )
    strategy.run()
"""

from __future__ import annotations

import collections
import logging
import math
from typing import Deque, Dict, Optional

from python_algo.messages import (
    CandleMsg,
    DepthMsg,
    ExecutionReportMsg,
    QuoteRequestCmd,
    TradeMsg,
)
from python_algo.strategy import PythonStrategy
from python_algo.transport import Transport

log = logging.getLogger(__name__)


class AvellanedaStoikovStrategy(PythonStrategy):
    """
    Avellaneda-Stoikov market maker.

    On every depth update the strategy:
    1. Updates the rolling volatility estimate from mid-price changes.
    2. Computes the reservation price using current inventory.
    3. Computes the optimal spread from the A-S formula.
    4. Sends a ``QuoteRequestCmd`` to place symmetric two-sided quotes.

    Parameters
    ----------
    transport :
        Connected ZmqTransport (or any Transport).
    instrument :
        Instrument primary key to trade (e.g. ``"btcusdt_binance"``).
    gamma :
        Risk-aversion coefficient γ ∈ (0, ∞).  Higher → tighter quotes,
        stronger inventory penalty.
    kappa :
        Market order arrival rate κ > 0.  Higher → tighter spread.
    sigma_window :
        Number of mid-price changes used to estimate rolling volatility σ.
    quantity :
        Size of each side of the quote.
    inventory_max :
        Absolute inventory limit.  When |q| ≥ inventory_max the corresponding
        one-sided quote is withdrawn (quantity set to 0).
    min_half_spread :
        Floor on the half-spread to avoid quoting inside the touch.
    alpha :
        Optional directional alpha added to the reservation price.
        Positive → shade quotes higher (bullish), negative → lower (bearish).
        The RL subclass writes to this attribute each step.
    """

    def __init__(
        self,
        transport: Transport,
        instrument: Optional[str] = None,
        gamma: float = 0.1,
        kappa: float = 1.5,
        sigma_window: int = 20,
        quantity: float = 0.001,
        inventory_max: float = 0.05,
        min_half_spread: float = 0.0,
        alpha: float = 0.0,
        **kwargs,
    ) -> None:
        super().__init__(transport, **kwargs)
        self._instrument    = instrument
        self._gamma         = gamma
        self._kappa         = kappa
        self._sigma_window  = sigma_window
        self._quantity      = quantity
        self._inventory_max = inventory_max
        self._min_half_spread = min_half_spread
        self.alpha          = alpha   # public: RL layer can overwrite per step

        # Per-instrument state
        self._inventory: Dict[str, float]     = collections.defaultdict(float)
        self._mids:      Dict[str, Deque[float]] = {}
        self._last_mid:  Dict[str, float]     = {}

    # ------------------------------------------------------------------
    # PythonStrategy callbacks
    # ------------------------------------------------------------------

    def on_depth(self, depth: DepthMsg) -> None:
        instrument = depth.instrument
        if self._instrument is not None and instrument != self._instrument:
            return
        if math.isnan(depth.mid) or math.isnan(depth.best_bid) or math.isnan(depth.best_ask):
            return

        mid = depth.mid
        self._update_mid(instrument, mid)

        sigma = self._sigma(instrument)
        q     = self._inventory[instrument]

        bid_price, ask_price = self._compute_quotes(mid, q, sigma, depth)

        bid_qty = self._quantity if q < self._inventory_max  else 0.0
        ask_qty = self._quantity if q > -self._inventory_max else 0.0

        if bid_qty == 0.0 and ask_qty == 0.0:
            log.debug("inventory limit reached for %s q=%.6f", instrument, q)
            return

        cmd = QuoteRequestCmd(
            instrument=instrument,
            bid_price=bid_price,
            bid_quantity=bid_qty,
            ask_price=ask_price,
            ask_quantity=ask_qty,
        )
        log.debug(
            "quote %s  bid=%.6f ask=%.6f  q=%.6f σ=%.6f",
            instrument, bid_price, ask_price, q, sigma,
        )
        self.send_quote(cmd)

    def on_trade(self, trade: TradeMsg) -> None:
        pass

    def on_execution_report(self, er: ExecutionReportMsg) -> None:
        if er.status not in ("CompletellyFilled", "PartialFilled"):
            return
        delta = er.last_quantity if er.last_quantity > 0 else er.quantity_fill
        instrument = er.instrument
        if er.verb.lower() in ("buy", "b"):
            self._inventory[instrument] += delta
        else:
            self._inventory[instrument] -= delta
        log.info(
            "fill %s %s qty=%.6f  inventory=%.6f",
            instrument, er.verb, delta, self._inventory[instrument],
        )

    def on_candle(self, candle: CandleMsg) -> None:
        pass

    # ------------------------------------------------------------------
    # Public helpers (used by RL layer)
    # ------------------------------------------------------------------

    def inventory(self, instrument: Optional[str] = None) -> float:
        """Return current inventory for the instrument."""
        inst = instrument or self._instrument or ""
        return self._inventory[inst]

    def volatility(self, instrument: Optional[str] = None) -> float:
        """Return current σ estimate."""
        inst = instrument or self._instrument or ""
        return self._sigma(inst)

    def last_mid(self, instrument: Optional[str] = None) -> float:
        """Return last mid price (nan if not yet seen)."""
        inst = instrument or self._instrument or ""
        return self._last_mid.get(inst, float("nan"))

    # ------------------------------------------------------------------
    # Internal helpers
    # ------------------------------------------------------------------

    def _update_mid(self, instrument: str, mid: float) -> None:
        if instrument not in self._mids:
            self._mids[instrument] = collections.deque(maxlen=self._sigma_window + 1)
        self._mids[instrument].append(mid)
        self._last_mid[instrument] = mid

    def _sigma(self, instrument: str) -> float:
        buf = self._mids.get(instrument)
        if buf is None or len(buf) < 2:
            return 1e-6  # tiny default avoids divide-by-zero at startup
        changes = [buf[i] - buf[i - 1] for i in range(1, len(buf))]
        n = len(changes)
        mean = sum(changes) / n
        variance = sum((c - mean) ** 2 for c in changes) / n
        return math.sqrt(variance) if variance > 0 else 1e-6

    def _compute_quotes(
        self,
        mid: float,
        q: float,
        sigma: float,
        depth: DepthMsg,
    ) -> tuple[float, float]:
        gamma, kappa, T = self._gamma, self._kappa, 1.0

        # Reservation price
        r = mid - q * gamma * sigma ** 2 * T + self.alpha

        # Optimal half-spread from A-S formula
        inventory_term = gamma * sigma ** 2 * T / 2.0
        arrival_term   = (1.0 / gamma) * math.log(1.0 + gamma / kappa) if kappa > 0 else 0.0
        half_spread    = max(inventory_term + arrival_term, self._min_half_spread)

        bid_price = r - half_spread
        ask_price = r + half_spread

        # Ensure quotes don't cross the touch
        bid_price = min(bid_price, depth.best_bid)
        ask_price = max(ask_price, depth.best_ask)

        return bid_price, ask_price

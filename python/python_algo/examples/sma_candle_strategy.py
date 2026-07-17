"""
SmaCandleStrategy — simple moving-average crossover strategy driven by candles.

The strategy subscribes to candle events published by PythonAlgorithm and
generates directional orders when the fast SMA crosses the slow SMA:

  * fast SMA crosses above slow SMA → BUY  (go / stay long)
  * fast SMA crosses below slow SMA → SELL (go / stay short)

A market order is placed on every crossover.  It is intentionally simple and
serves as a runnable example of how to build candle-driven strategies with the
python_algo bridge.

Usage
-----
    from python_algo import ZmqTransport
    from python_algo.examples.sma_candle_strategy import SmaCandleStrategy

    transport = ZmqTransport(md_sub_port=7700, cmd_push_port=7701)
    strategy  = SmaCandleStrategy(
        transport,
        instrument="btcusdt_binance",
        quantity=0.001,
        fast_period=5,
        slow_period=20,
    )
    strategy.run()
"""

from __future__ import annotations

import collections
import logging
from typing import Deque, Dict, Optional

from python_algo.messages import (
    CandleMsg,
    DepthMsg,
    ExecutionReportMsg,
    OrderRequestCmd,
    TradeMsg,
)
from python_algo.strategy import PythonStrategy
from python_algo.transport import Transport

log = logging.getLogger(__name__)


class SmaCandleStrategy(PythonStrategy):
    """
    SMA crossover strategy.

    Parameters
    ----------
    transport :
        ZmqTransport (or any Transport) connected to a running PythonAlgorithm.
    instrument :
        Instrument primary key to trade (e.g. ``"btcusdt_binance"``).
        If ``None``, the strategy responds to the first instrument seen.
    quantity :
        Order size per trade.
    fast_period :
        Number of candles for the fast SMA.
    slow_period :
        Number of candles for the slow SMA.
    """

    def __init__(
        self,
        transport: Transport,
        instrument: Optional[str] = None,
        quantity: float = 0.001,
        fast_period: int = 5,
        slow_period: int = 20,
        **kwargs,
    ) -> None:
        if fast_period >= slow_period:
            raise ValueError(
                f"fast_period ({fast_period}) must be less than slow_period ({slow_period})"
            )
        # Pass a per-instrument subscription when an instrument is known so
        # that the SUB socket only receives events for that symbol.  If no
        # instrument is given the base-class default (subscribe-all) is used.
        if instrument is not None:
            kwargs.setdefault("instruments", [instrument])
        super().__init__(transport, **kwargs)
        self._instrument = instrument
        self._quantity = quantity
        self._fast_period = fast_period
        self._slow_period = slow_period

        # Rolling close buffer per instrument
        self._closes: Dict[str, Deque[float]] = collections.defaultdict(
            lambda: collections.deque(maxlen=slow_period)
        )
        # Last crossover direction: +1 long, -1 short, 0 none
        self._position: Dict[str, int] = collections.defaultdict(int)
        self._last_depth: Dict[str, DepthMsg] = {}

    # ------------------------------------------------------------------
    # Mandatory callbacks
    # ------------------------------------------------------------------

    def on_depth(self, depth: DepthMsg) -> None:
        self._last_depth[depth.instrument] = depth

    def on_trade(self, trade: TradeMsg) -> None:
        pass

    def on_execution_report(self, er: ExecutionReportMsg) -> None:
        log.info(
            "execution_report instrument=%s status=%s verb=%s qty=%s price=%s",
            er.instrument,
            er.status,
            er.verb,
            er.quantity_fill,
            er.price,
        )

    def on_candle(self, candle: CandleMsg) -> None:
        instrument = candle.instrument
        if self._instrument is not None and instrument != self._instrument:
            return

        closes = self._closes[instrument]
        closes.append(candle.close)

        if len(closes) < self._slow_period:
            log.debug(
                "candle %s close=%.6f  buffering (%d/%d)",
                instrument,
                candle.close,
                len(closes),
                self._slow_period,
            )
            return

        fast_sma = sum(list(closes)[-self._fast_period :]) / self._fast_period
        slow_sma = sum(closes) / self._slow_period

        log.debug(
            "candle %s close=%.6f fast_sma=%.6f slow_sma=%.6f",
            instrument,
            candle.close,
            fast_sma,
            slow_sma,
        )

        current_position = self._position[instrument]

        if fast_sma > slow_sma and current_position <= 0:
            self._place_order(instrument, "Buy")
            self._position[instrument] = 1
        elif fast_sma < slow_sma and current_position >= 0:
            self._place_order(instrument, "Sell")
            self._position[instrument] = -1

    # ------------------------------------------------------------------
    # Internal helpers
    # ------------------------------------------------------------------

    def _place_order(self, instrument: str, verb: str) -> None:
        depth = self._last_depth.get(instrument)
        if depth is not None:
            price = depth.best_bid if verb == "Sell" else depth.best_ask
        else:
            price = 0.0  # market order fallback

        order_type = "Limit" if price != 0.0 else "Market"
        cmd = OrderRequestCmd(
            instrument=instrument,
            verb=verb,
            order_type=order_type,
            quantity=self._quantity,
            price=price,
        )
        log.info(
            "placing %s %s %s qty=%.6f price=%.6f",
            order_type,
            verb,
            instrument,
            self._quantity,
            price,
        )
        self.send_order(cmd)

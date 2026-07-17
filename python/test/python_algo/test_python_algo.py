"""
Tests for the python_algo bridge — verifying:
  - OrderRequestCmd / QuoteRequestCmd serialisation round-trip
  - PythonStrategy subscribes to all topics by default (instruments=None)
  - PythonStrategy subscribes to specific topics when instruments are given
  - SmaCandleStrategy subscribes to its instrument
  - Basic PythonStrategy event dispatch (depth, trade, candle, execution_report)
"""

import json
import threading
import unittest
from typing import Optional, List
from unittest.mock import MagicMock, call, patch

import sys
import os

# Ensure the python/ source root is importable without installing the package
_HERE = os.path.dirname(os.path.abspath(__file__))
_PYTHON_ROOT = os.path.abspath(os.path.join(_HERE, "..", ".."))
if _PYTHON_ROOT not in sys.path:
    sys.path.insert(0, _PYTHON_ROOT)

from python_algo.codec import JsonCodec
from python_algo.messages import (
    CandleMsg,
    DepthMsg,
    Envelope,
    ExecutionReportMsg,
    OrderRequestCmd,
    QuoteRequestCmd,
    RequestInfoCmd,
    TradeMsg,
)
from python_algo.strategy import PythonStrategy
from python_algo.transport import Transport


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

class _MockTransport(Transport):
    """In-memory transport for testing."""

    def __init__(self):
        self.subscriptions: List[str] = []
        self.sent: List[bytes] = []
        self._queue: List[bytes] = []

    def subscribe(self, topic: str = "") -> None:
        self.subscriptions.append(topic)

    def recv(self, timeout_ms: int = 200) -> Optional[bytes]:
        return self._queue.pop(0) if self._queue else None

    def send(self, data: bytes) -> None:
        self.sent.append(data)

    def close(self) -> None:
        pass

    def push(self, raw: bytes) -> None:
        """Simulate an inbound message from Java."""
        self._queue.append(raw)


def _make_envelope(msg_type: str, instrument: str, data: dict) -> bytes:
    env = {"v": 1, "type": msg_type, "instrument": instrument, "data": data}
    return JsonCodec().encode(env)


class _ConcreteStrategy(PythonStrategy):
    """Minimal concrete PythonStrategy for unit testing."""

    def __init__(self, transport, **kwargs):
        super().__init__(transport, **kwargs)
        self.depths: List[DepthMsg] = []
        self.trades: List[TradeMsg] = []
        self.ers: List[ExecutionReportMsg] = []
        self.candles: List[CandleMsg] = []

    def on_depth(self, depth): self.depths.append(depth)
    def on_trade(self, trade): self.trades.append(trade)
    def on_execution_report(self, er): self.ers.append(er)
    def on_candle(self, candle): self.candles.append(candle)


# ---------------------------------------------------------------------------
# Message serialisation tests
# ---------------------------------------------------------------------------

class TestOrderRequestCmdSerialisation(unittest.TestCase):

    def test_to_envelope_contains_required_keys(self):
        cmd = OrderRequestCmd(
            instrument="btcusdt_binance",
            verb="Buy",
            order_type="Limit",
            quantity=0.001,
            price=12345.0,
        )
        env = cmd.to_envelope()
        self.assertEqual(env["type"], "order_request")
        self.assertEqual(env["v"], 1)
        data = env["data"]
        self.assertEqual(data["instrument"], "btcusdt_binance")
        self.assertEqual(data["verb"], "Buy")
        self.assertEqual(data["orderType"], "Limit")
        self.assertAlmostEqual(data["quantity"], 0.001)
        self.assertAlmostEqual(data["price"], 12345.0)
        self.assertEqual(data["orderRequestAction"], "Send")

    def test_to_bytes_is_valid_json(self):
        cmd = OrderRequestCmd(
            instrument="btcusdt_binance",
            verb="Sell",
            order_type="Market",
            quantity=0.01,
            price=0.0,
        )
        raw = cmd.to_bytes()
        parsed = json.loads(raw.decode("utf-8"))
        self.assertEqual(parsed["type"], "order_request")
        self.assertEqual(parsed["data"]["verb"], "Sell")

    def test_to_json_round_trip(self):
        cmd = OrderRequestCmd(
            instrument="ethusdt_binance",
            verb="Buy",
            order_type="Limit",
            quantity=1.0,
            price=3000.0,
            client_order_id="ord-001",
        )
        js = cmd.to_json()
        parsed = json.loads(js)
        self.assertEqual(parsed["data"]["clientOrderId"], "ord-001")


class TestQuoteRequestCmdSerialisation(unittest.TestCase):

    def test_to_envelope_contains_required_keys(self):
        cmd = QuoteRequestCmd(
            instrument="btcusdt_binance",
            bid_price=100.0,
            bid_quantity=0.01,
            ask_price=101.0,
            ask_quantity=0.01,
        )
        env = cmd.to_envelope()
        self.assertEqual(env["type"], "quote_request")
        data = env["data"]
        self.assertAlmostEqual(data["bidPrice"], 100.0)
        self.assertAlmostEqual(data["askPrice"], 101.0)


# ---------------------------------------------------------------------------
# PythonStrategy subscription tests
# ---------------------------------------------------------------------------

class TestPythonStrategySubscription(unittest.TestCase):

    def test_no_instruments_subscribes_to_all(self):
        """When instruments=None, strategy must subscribe to '' (all topics)."""
        transport = _MockTransport()
        strategy = _ConcreteStrategy(transport)
        self.assertIn("", transport.subscriptions,
                      "PythonStrategy must subscribe to '' when no instruments given")

    def test_specific_instruments_subscribe_by_name(self):
        """When instruments=['btcusdt_binance'], strategy subscribes to that prefix."""
        transport = _MockTransport()
        strategy = _ConcreteStrategy(transport, instruments=["btcusdt_binance"])
        self.assertIn("btcusdt_binance", transport.subscriptions)
        # Should NOT also add empty-string subscribe-all
        self.assertNotIn("", transport.subscriptions)

    def test_multiple_instruments_each_subscribed(self):
        transport = _MockTransport()
        strategy = _ConcreteStrategy(
            transport, instruments=["btcusdt_binance", "ethusdt_binance"]
        )
        self.assertIn("btcusdt_binance", transport.subscriptions)
        self.assertIn("ethusdt_binance", transport.subscriptions)


# ---------------------------------------------------------------------------
# PythonStrategy dispatch tests
# ---------------------------------------------------------------------------

class TestPythonStrategyDispatch(unittest.TestCase):

    def _make_strategy(self):
        transport = _MockTransport()
        strategy = _ConcreteStrategy(transport)
        return strategy, transport

    def test_dispatch_depth(self):
        strategy, transport = self._make_strategy()
        depth_data = {
            "instrument": "btcusdt_binance",
            "timestamp": 1000,
            "bids": [100.0, 99.0],
            "asks": [101.0, 102.0],
            "bidsQuantities": [1.0, 2.0],
            "asksQuantities": [1.0, 2.0],
            "levels": 2,
        }
        transport.push(_make_envelope("depth", "btcusdt_binance", depth_data))
        strategy.step()
        self.assertEqual(len(strategy.depths), 1)
        self.assertEqual(strategy.depths[0].instrument, "btcusdt_binance")
        self.assertAlmostEqual(strategy.depths[0].best_bid, 100.0)
        self.assertAlmostEqual(strategy.depths[0].best_ask, 101.0)

    def test_dispatch_trade(self):
        strategy, transport = self._make_strategy()
        trade_data = {
            "instrument": "btcusdt_binance",
            "timestamp": 1000,
            "price": 100.5,
            "quantity": 0.5,
        }
        transport.push(_make_envelope("trade", "btcusdt_binance", trade_data))
        strategy.step()
        self.assertEqual(len(strategy.trades), 1)
        self.assertAlmostEqual(strategy.trades[0].price, 100.5)

    def test_dispatch_candle(self):
        strategy, transport = self._make_strategy()
        candle_data = {
            "instrumentPk": "btcusdt_binance",
            "timestamp": 1000,
            "open": 99.0,
            "high": 102.0,
            "low": 98.0,
            "close": 101.0,
        }
        transport.push(_make_envelope("candle", "btcusdt_binance", candle_data))
        strategy.step()
        self.assertEqual(len(strategy.candles), 1)
        self.assertAlmostEqual(strategy.candles[0].close, 101.0)

    def test_dispatch_execution_report(self):
        strategy, transport = self._make_strategy()
        er_data = {
            "instrument": "btcusdt_binance",
            "clientOrderId": "ord-001",
            "executionReportStatus": "CompletelyFilled",
            "verb": "Buy",
            "price": 100.5,
            "quantity": 0.001,
            "quantityFill": 0.001,
            "lastQuantity": 0.001,
            "timestampCreation": 1000,
        }
        transport.push(_make_envelope("execution_report", "btcusdt_binance", er_data))
        strategy.step()
        self.assertEqual(len(strategy.ers), 1)
        self.assertEqual(strategy.ers[0].client_order_id, "ord-001")

    def test_step_returns_false_on_no_message(self):
        strategy, transport = self._make_strategy()
        result = strategy.step(timeout_ms=0)
        self.assertFalse(result)

    def test_step_returns_true_on_message(self):
        strategy, transport = self._make_strategy()
        trade_data = {"instrument": "x", "timestamp": 0, "price": 1.0, "quantity": 1.0}
        transport.push(_make_envelope("trade", "x", trade_data))
        result = strategy.step()
        self.assertTrue(result)


# ---------------------------------------------------------------------------
# PythonStrategy send_order tests
# ---------------------------------------------------------------------------

class TestPythonStrategySendOrder(unittest.TestCase):

    def test_send_order_puts_bytes_on_transport(self):
        transport = _MockTransport()
        strategy = _ConcreteStrategy(transport)
        cmd = OrderRequestCmd(
            instrument="btcusdt_binance",
            verb="Buy",
            order_type="Limit",
            quantity=0.001,
            price=12345.0,
        )
        strategy.send_order(cmd)
        self.assertEqual(len(transport.sent), 1)
        parsed = json.loads(transport.sent[0].decode("utf-8"))
        self.assertEqual(parsed["type"], "order_request")
        self.assertEqual(parsed["data"]["instrument"], "btcusdt_binance")

    def test_send_quote_puts_bytes_on_transport(self):
        transport = _MockTransport()
        strategy = _ConcreteStrategy(transport)
        cmd = QuoteRequestCmd(
            instrument="btcusdt_binance",
            bid_price=99.0,
            bid_quantity=0.01,
            ask_price=101.0,
            ask_quantity=0.01,
        )
        strategy.send_quote(cmd)
        self.assertEqual(len(transport.sent), 1)
        parsed = json.loads(transport.sent[0].decode("utf-8"))
        self.assertEqual(parsed["type"], "quote_request")


# ---------------------------------------------------------------------------
# SmaCandleStrategy subscription test
# ---------------------------------------------------------------------------

class TestSmaCandleStrategySubscription(unittest.TestCase):

    def test_subscribes_to_instrument_when_provided(self):
        """SmaCandleStrategy must subscribe to its instrument topic."""
        from python_algo.examples.sma_candle_strategy import SmaCandleStrategy

        transport = _MockTransport()
        strategy = SmaCandleStrategy(
            transport,
            instrument="btcusdt_binance",
            quantity=0.001,
            fast_period=3,
            slow_period=10,
        )
        self.assertIn("btcusdt_binance", transport.subscriptions,
                      "SmaCandleStrategy should subscribe to its instrument")
        # Should NOT also add empty-string subscribe-all when instrument is given
        self.assertNotIn("", transport.subscriptions)

    def test_subscribes_to_all_when_no_instrument(self):
        """SmaCandleStrategy without an explicit instrument → subscribe to all."""
        from python_algo.examples.sma_candle_strategy import SmaCandleStrategy

        transport = _MockTransport()
        strategy = SmaCandleStrategy(
            transport,
            quantity=0.001,
            fast_period=3,
            slow_period=10,
        )
        self.assertIn("", transport.subscriptions)


if __name__ == "__main__":
    unittest.main()

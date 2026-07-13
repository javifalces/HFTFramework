"""
PythonStrategy — base class for pure-Python trading strategies.

Usage
-----
class MyStrategy(PythonStrategy):
    def on_depth(self, depth: DepthMsg) -> None:
        if depth.spread < 0.01:
            self.send_order(OrderRequestCmd(
                instrument=depth.instrument,
                verb="Buy",
                order_type="Limit",
                quantity=0.01,
                price=depth.best_bid,
            ))

    def on_trade(self, trade: TradeMsg) -> None: ...
    def on_execution_report(self, er: ExecutionReportMsg) -> None: ...

# TCP + JSON (default)
transport = ZmqTransport(md_sub_port=7700, cmd_push_port=7701)
strategy  = MyStrategy(transport, instruments=["btcusdt_binance"])
strategy.run()           # blocking event loop

# IPC + MessagePack (local, lower latency)
from python_algo.codec import MsgpackCodec
transport = ZmqTransport(transport_type="ipc", codec=MsgpackCodec())
strategy  = MyStrategy(transport)
strategy.run()

# Backtest with debugger support — Java blocks until Python ACKs each event,
# so hitting a breakpoint naturally pauses the whole backtest.
transport = ZmqTransport(md_sub_port=7700, cmd_push_port=7701, backtest_sync=True)
strategy  = MyStrategy(transport, instruments=["btcusdt_binance"])
strategy.run()
"""

from __future__ import annotations

import abc
import logging
from typing import TYPE_CHECKING, Optional, List

from python_algo.messages import (
    Envelope,
    DepthMsg,
    TradeMsg,
    ExecutionReportMsg,
    CandleMsg,
    OrderRequestCmd,
    QuoteRequestCmd,
    RequestInfoCmd,
    PortfolioSnapshotRequestCmd,
    PortfolioSnapshotMsg,
)
from python_algo.transport import Transport

if TYPE_CHECKING:
    from python_algo.codec import Codec

log = logging.getLogger(__name__)

_TYPE_DEPTH = "depth"
_TYPE_TRADE = "trade"
_TYPE_ER    = "execution_report"
_TYPE_CANDLE = "candle"


class PythonStrategy(abc.ABC):
    """
    Abstract base for pure-Python trading strategies.

    Subclass and implement on_depth / on_trade / on_execution_report.
    Call run() to start the blocking event loop, or use step() for
    manual / Gymnasium-driven iteration.
    """

    def __init__(
        self,
        transport: Transport,
        instruments: [List[str]] = None,
        codec: Optional["Codec"] = None,
    ) -> None:
        self._transport = transport
        self._running   = False
        # Use the transport's codec if it exposes one, falling back to the
        # explicitly supplied codec, then None (which defaults to JsonCodec).
        self._codec = getattr(transport, "codec", None) or codec

        # Subscribe to all or specific instrument topics.
        # When no instruments are specified, subscribe to all messages so that
        # the SUB socket receives every topic published by Java.
        if instruments:
            for inst in instruments:
                transport.subscribe(inst)
        else:
            transport.subscribe("")


    # -----------------------------------------------------------------------
    # Abstract callbacks — implement in subclass
    # -----------------------------------------------------------------------

    @abc.abstractmethod
    def on_depth(self, depth: DepthMsg) -> None:
        """Called on every depth update from Java."""

    @abc.abstractmethod
    def on_trade(self, trade: TradeMsg) -> None:
        """Called on every trade update from Java."""

    @abc.abstractmethod
    def on_execution_report(self, er: ExecutionReportMsg) -> None:
        """Called on every execution report from Java."""

    @abc.abstractmethod
    def on_candle(self, candle: CandleMsg) -> None:
        """Called on every completed candle published by Java."""

    # -----------------------------------------------------------------------
    # Optional hook — override to react to unknown message types
    # -----------------------------------------------------------------------

    def on_unknown(self, envelope: Envelope) -> None:
        log.debug("unknown message type=%s instrument=%s", envelope.type, envelope.instrument)

    # -----------------------------------------------------------------------
    # Order / quote helpers
    # -----------------------------------------------------------------------

    def send_order(self, cmd: OrderRequestCmd) -> None:
        """
        Send an order request to Java (asynchronous).
        
        Args:
            cmd: OrderRequestCmd with order details (instrument, verb, type, quantity, price, etc.)
            
        Example:
            self.send_order(OrderRequestCmd(
                instrument="btcusdt_binance",
                verb="Buy",
                order_type="Limit",
                quantity=0.01,
                price=50000.0
            ))
        """
        self._transport.send(cmd.to_bytes(self._codec))

    def send_quote(self, cmd: QuoteRequestCmd) -> None:
        """
        Send a quote request to Java for market making (asynchronous).
        
        Args:
            cmd: QuoteRequestCmd with two-sided quote details
            
        Example:
            self.send_quote(QuoteRequestCmd(
                instrument="btcusdt_binance",
                bid_price=49950.0,
                bid_quantity=0.1,
                ask_price=50050.0,
                ask_quantity=0.1
            ))
        """
        self._transport.send(cmd.to_bytes(self._codec))

    def request_info(self, info: str) -> None:
        """
        Send an informational request to Java (asynchronous).
        
        Args:
            info: Information identifier string
            
        Note:
            The response handling depends on the Java implementation's
            onInfoUpdate() method.
        """
        self._transport.send(RequestInfoCmd(info).to_bytes(self._codec))

    def get_portfolio_snapshot(self, timeout_ms: int = 5000) -> Optional[PortfolioSnapshotMsg]:
        """
        Synchronously request the current portfolio snapshot from Java.
        
        This method blocks until a response is received from the Java PythonAlgorithm
        or the timeout expires. It uses a REQ/REP socket pattern for reliable
        request-response communication.
        
        Args:
            timeout_ms: Maximum time to wait for response in milliseconds.
                       Default is 5000ms (5 seconds). Must be positive.
            
        Returns:
            PortfolioSnapshotMsg containing:
                - algorithm_info: Algorithm identifier
                - net_investment: Total capital invested
                - realized_pnl: Realized profit and loss
                - unrealized_pnl: Unrealized profit and loss
                - total_pnl: Total P&L (realized + unrealized)
                - total_fees: All trading fees incurred
                - realized_fees: Fees from closed positions
                - unrealized_fees: Fees from open positions
                - net_position: Net position across all instruments
                - last_timestamp_update: Timestamp of last update (epoch ms)
                - instrument_pnl_snapshots: Per-instrument P&L breakdown (dict)
            
            Returns None if:
                - Request times out
                - Java side is not running
                - Response cannot be parsed
                
        Example:
            snapshot = self.get_portfolio_snapshot(timeout_ms=3000)
            if snapshot:
                print(f"Total P&L: ${snapshot.total_pnl:.2f}")
                print(f"Net Position: {snapshot.net_position:.4f}")
                
                # Check individual instruments
                for instrument, pnl in snapshot.instrument_pnl_snapshots.items():
                    print(f"{instrument}: {pnl}")
            else:
                print("Failed to retrieve portfolio snapshot")
                
        Warning:
            This method blocks the event loop. Avoid calling it in high-frequency
            paths. Consider caching snapshots or calling at controlled intervals.
            
        Note:
            - The Java side must have the REP socket enabled (default port 7703)
            - Requires Java parameter: python_rep_port=7703 (TCP) or
              python_ipc_rep_path=/tmp/python_algo_req (IPC)
            - On timeout, the REQ socket is reset to maintain proper state
        """
        request_bytes = PortfolioSnapshotRequestCmd().to_bytes(self._codec)
        response_bytes = self._transport.request(request_bytes, timeout_ms)
        
        if response_bytes is None:
            log.warning("Portfolio snapshot request timed out")
            return None
            
        try:
            envelope = Envelope.parse(response_bytes, self._codec)
            if envelope.type == "portfolio_snapshot":
                return PortfolioSnapshotMsg.from_dict(envelope.data)
            else:
                log.warning("Unexpected response type: %s", envelope.type)
                return None
        except Exception as e:
            log.error("Error parsing portfolio snapshot response: %s", e)
            return None

    # -----------------------------------------------------------------------
    # Event loop
    # -----------------------------------------------------------------------

    def step(self, timeout_ms: int = 200) -> bool:
        """
        Process one inbound message (non-blocking with timeout).
        Returns True if a message was processed, False on timeout.
        Suitable for Gymnasium step() or manual driving.

        In backtest-sync mode the transport's ``send_ack()`` is called after
        dispatching so that Java unblocks and advances to the next event.
        """
        raw = self._transport.recv(timeout_ms)
        if raw is None:
            return False
        try:
            env = Envelope.parse(raw, self._codec)
            self._dispatch(env)
        except Exception as e:
            log.warning("error processing message: %s", e)
        finally:
            self._transport.send_ack()
        return True

    def run(self, poll_ms: int = 200) -> None:
        """Blocking event loop. Returns when stop() is called."""
        self._running = True
        try:
            while self._running:
                self.step(poll_ms)
        finally:
            self._transport.close()

    def stop(self) -> None:
        """Signal the run() loop to exit."""
        self._running = False

    # -----------------------------------------------------------------------
    # Internal dispatch
    # -----------------------------------------------------------------------

    def _dispatch(self, env: Envelope) -> None:
        if env.type == _TYPE_DEPTH:
            self.on_depth(env.as_depth())
        elif env.type == _TYPE_TRADE:
            self.on_trade(env.as_trade())
        elif env.type == _TYPE_ER:
            self.on_execution_report(env.as_execution_report())
        elif env.type == _TYPE_CANDLE:
            self.on_candle(env.as_candle())
        else:
            self.on_unknown(env)

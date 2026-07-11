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
    OrderRequestCmd,
    QuoteRequestCmd,
    RequestInfoCmd,
)
from python_algo.transport import Transport

if TYPE_CHECKING:
    from python_algo.codec import Codec

log = logging.getLogger(__name__)

_TYPE_DEPTH = "depth"
_TYPE_TRADE = "trade"
_TYPE_ER    = "execution_report"


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
        instruments: Optional[List[str]] = None,
        codec: Optional["Codec"] = None,
    ) -> None:
        self._transport = transport
        self._running   = False
        # Use the transport's codec if it exposes one, falling back to the
        # explicitly supplied codec, then None (which defaults to JsonCodec).
        self._codec = getattr(transport, "codec", None) or codec

        # Subscribe to all or specific instrument topics
        if instruments:
            for inst in instruments:
                transport.subscribe(inst)
        else:
            transport.subscribe("")  # all topics

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

    # -----------------------------------------------------------------------
    # Optional hook — override to react to unknown message types
    # -----------------------------------------------------------------------

    def on_unknown(self, envelope: Envelope) -> None:
        log.debug("unknown message type=%s instrument=%s", envelope.type, envelope.instrument)

    # -----------------------------------------------------------------------
    # Order / quote helpers
    # -----------------------------------------------------------------------

    def send_order(self, cmd: OrderRequestCmd) -> None:
        self._transport.send(cmd.to_bytes(self._codec))

    def send_quote(self, cmd: QuoteRequestCmd) -> None:
        self._transport.send(cmd.to_bytes(self._codec))

    def request_info(self, info: str) -> None:
        self._transport.send(RequestInfoCmd(info).to_bytes(self._codec))

    # -----------------------------------------------------------------------
    # Event loop
    # -----------------------------------------------------------------------

    def step(self, timeout_ms: int = 200) -> bool:
        """
        Process one inbound message (non-blocking with timeout).
        Returns True if a message was processed, False on timeout.
        Suitable for Gymnasium step() or manual driving.
        """
        raw = self._transport.recv(timeout_ms)
        if raw is None:
            return False
        try:
            env = Envelope.parse(raw, self._codec)
            self._dispatch(env)
        except Exception as e:
            log.warning("error processing message: %s", e)
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
        else:
            self.on_unknown(env)

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
    JavaParametersUpdateMsg,
    OrderRequestCmd,
    QuoteRequestCmd,
    RequestInfoCmd,
    PortfolioSnapshotRequestCmd,
    PortfolioSnapshotMsg,
    GetParametersRequestCmd,
    SetParametersRequestCmd,
    ParametersUpdateCmd,
    ReadyCmd,
)
from python_algo.transport import Transport

if TYPE_CHECKING:
    from python_algo.codec import Codec

log = logging.getLogger(__name__)

_TYPE_DEPTH = "depth"
_TYPE_TRADE = "trade"
_TYPE_ER    = "execution_report"
_TYPE_CANDLE = "candle"
_TYPE_JAVA_PARAMS_UPDATE = "java_parameters_update"


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
        
        # Strategy parameters dictionary
        self._parameters = {}


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
    
    def on_java_parameters_update(self, msg: JavaParametersUpdateMsg) -> None:
        """
        Called when Java sends parameter updates via PUB socket.
        
        Override this method to react to parameter changes from Java.
        By default, parameters are merged into self._parameters.
        
        Args:
            msg: JavaParametersUpdateMsg containing the updated parameters dictionary
        """
        log.info("[PythonStrategy] received %d parameter(s) from Java", len(msg.parameters))
        for key, value in msg.parameters.items():
            log.info("[PythonStrategy]   parameter: %s = %s", key, value)
        
        # Merge Java parameters into Python parameters
        self._parameters.update(msg.parameters)
        log.debug("[PythonStrategy] parameters merged, total count: %d", len(self._parameters))


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

    def send_ready(self, version: str = "1.0", strategy_name: str = None) -> None:
        """
        Signal to Java that Python strategy is initialized and ready to receive events.
        
        Args:
            version: Strategy version string (default "1.0")
            strategy_name: Strategy class name (default: class name)
            
        Example:
            self.send_ready(version="2.1", strategy_name="MyCustomStrategy")
            
        Note:
            This should be called after initialization is complete, typically
            at the start of run() or after subscribing to instruments.
            Java will log a warning for events published before ready signal.
        """
        if strategy_name is None:
            strategy_name = self.__class__.__name__
        
        log.debug(f"[PythonStrategy] send_ready() - signaling Java: strategy={strategy_name} version={version}")
        
        try:
            cmd = ReadyCmd(version=version, strategy=strategy_name)
            cmd_bytes = cmd.to_bytes(self._codec)
            log.debug(f"[PythonStrategy] send_ready() - encoded ready signal ({len(cmd_bytes)} bytes)")
            
            self._transport.send(cmd_bytes)
            log.info(f"[PythonStrategy] send_ready() - READY signal sent to Java: strategy={strategy_name} version={version}")
        except Exception as e:
            log.error(f"[PythonStrategy] send_ready() - failed to send ready signal: {e}", exc_info=True)

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
    # Parameter management
    # -----------------------------------------------------------------------

    def get_parameters(self) -> dict:
        """
        Get the Python strategy parameters.
        
        Returns:
            Dictionary of strategy parameters
        """
        params = self._parameters.copy()
        log.debug(f"[PythonStrategy] get_parameters() - returning {len(params)} parameters")
        if params:
            log.debug(f"[PythonStrategy] get_parameters() - parameter keys: {list(params.keys())}")
        return params

    def set_parameters(self, parameters: dict) -> None:
        """
        Set Python strategy parameters and optionally notify Java.
        
        During initialization (before run()), parameters are stored but NOT sent.
        After run() is called, parameters are sent immediately.
        
        This ensures parameters are sent AFTER the READY signal, so Java's
        command loop is active and ready to receive them.
        
        Args:
            parameters: Dictionary of parameters to set
        """
        if not parameters:
            log.info("[PythonStrategy] set_parameters() - received empty parameters (no update)")
            return
            
        log.debug(f"[PythonStrategy] set_parameters() - updating {len(parameters)} parameters")
        log.debug(f"[PythonStrategy] set_parameters() - parameter keys: {list(parameters.keys())}")
        
        original_size = len(self._parameters)
        self._parameters.update(parameters)
        new_size = len(self._parameters)
        
        log.info(f"[PythonStrategy] set_parameters() - parameters updated (size: {original_size} -> {new_size})")
        
        # Only send to Java if strategy is already running (after READY signal sent)
        if self._running:
            log.debug("[PythonStrategy] set_parameters() - strategy is running, notifying Java via PUSH socket")
            try:
                cmd_bytes = ParametersUpdateCmd(self._parameters).to_bytes(self._codec)
                log.debug(f"[PythonStrategy] set_parameters() - encoded {len(cmd_bytes)} bytes")
                self._transport.send(cmd_bytes)
                log.info(f"[PythonStrategy] set_parameters() - successfully sent {new_size} parameters to Java")
            except Exception as e:
                log.error(f"[PythonStrategy] set_parameters() - failed to send to Java: {e}", exc_info=True)
        else:
            log.debug("[PythonStrategy] set_parameters() - strategy not running yet, parameters will be sent after READY signal")

    def get_java_parameters(self, timeout_ms: int = 5000) -> Optional[dict]:
        """
        Synchronously request parameters from Java PythonAlgorithm.
        
        Args:
            timeout_ms: Maximum time to wait for response in milliseconds.
                       Default is 5000ms (5 seconds).
            
        Returns:
            Dictionary of Java-side parameters, or None if request fails
        """
        log.debug(f"[PythonStrategy] get_java_parameters() - requesting parameters from Java (timeout: {timeout_ms}ms)")
        
        try:
            request_bytes = GetParametersRequestCmd().to_bytes(self._codec)
            log.debug(f"[PythonStrategy] get_java_parameters() - encoded request ({len(request_bytes)} bytes)")
            
            response_bytes = self._transport.request(request_bytes, timeout_ms)
            
            if response_bytes is None:
                log.warning(f"[PythonStrategy] get_java_parameters() - request timed out after {timeout_ms}ms")
                return None
            
            log.debug(f"[PythonStrategy] get_java_parameters() - received response ({len(response_bytes)} bytes)")
                
            envelope = Envelope.parse(response_bytes, self._codec)
            log.debug(f"[PythonStrategy] get_java_parameters() - response type: {envelope.type}")
            
            if envelope.type == "get_parameters_response":
                if isinstance(envelope.data, dict):
                    params = envelope.data
                    log.info(f"[PythonStrategy] get_java_parameters() - successfully received {len(params)} parameters from Java")
                    log.debug(f"[PythonStrategy] get_java_parameters() - parameter keys: {list(params.keys())}")
                    return params
                else:
                    log.warning(f"[PythonStrategy] get_java_parameters() - response data is not a dict: {type(envelope.data).__name__}")
                    return {}
            else:
                log.warning(f"[PythonStrategy] get_java_parameters() - unexpected response type: {envelope.type} (expected: get_parameters_response)")
                return None
        except Exception as e:
            log.error(f"[PythonStrategy] get_java_parameters() - error requesting parameters: {e}", exc_info=True)
            return None

    def set_java_parameters(self, parameters: dict, timeout_ms: int = 5000) -> bool:
        """
        Synchronously send parameters to Java PythonAlgorithm.
        
        Args:
            parameters: Dictionary of parameters to set in Java
            timeout_ms: Maximum time to wait for response in milliseconds.
                       Default is 5000ms (5 seconds).
            
        Returns:
            True if parameters were successfully set, False otherwise
        """
        if not parameters:
            log.info("[PythonStrategy] set_java_parameters() - no parameters to send (empty dict)")
            return True
            
        log.debug(f"[PythonStrategy] set_java_parameters() - sending {len(parameters)} parameters to Java (timeout: {timeout_ms}ms)")
        log.debug(f"[PythonStrategy] set_java_parameters() - parameter keys: {list(parameters.keys())}")
        
        try:
            request_bytes = SetParametersRequestCmd(parameters).to_bytes(self._codec)
            log.debug(f"[PythonStrategy] set_java_parameters() - encoded request ({len(request_bytes)} bytes)")
            
            response_bytes = self._transport.request(request_bytes, timeout_ms)
            
            if response_bytes is None:
                log.warning(f"[PythonStrategy] set_java_parameters() - request timed out after {timeout_ms}ms")
                return False
            
            log.debug(f"[PythonStrategy] set_java_parameters() - received response ({len(response_bytes)} bytes)")
                
            envelope = Envelope.parse(response_bytes, self._codec)
            log.debug(f"[PythonStrategy] set_java_parameters() - response type: {envelope.type}")
            
            if envelope.type == "set_parameters_response":
                if isinstance(envelope.data, dict):
                    success = envelope.data.get("success", False)
                    if success:
                        log.info(f"[PythonStrategy] set_java_parameters() - successfully sent {len(parameters)} parameters to Java")
                    else:
                        error = envelope.data.get("error", "Unknown error")
                        log.warning(f"[PythonStrategy] set_java_parameters() - Java rejected parameters: {error}")
                    return success
                else:
                    log.warning(f"[PythonStrategy] set_java_parameters() - response data is not a dict: {type(envelope.data).__name__}")
                    return False
            else:
                log.warning(f"[PythonStrategy] set_java_parameters() - unexpected response type: {envelope.type} (expected: set_parameters_response)")
                return False
        except Exception as e:
            log.error(f"[PythonStrategy] set_java_parameters() - error sending parameters: {e}", exc_info=True)
            return False

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
        """
        Blocking event loop. Returns when stop() is called.
        
        Automatically sends READY signal to Java, then sends parameters,
        before starting the event loop.
        """
        # Signal to Java that we're ready to receive events
        self.send_ready()
        
        # Send parameters to Java after READY signal so Java knows to expect them
        # This ensures Java's command loop is active and ready to receive parameters
        if self._parameters:
            log.debug(f"[PythonStrategy] run() - sending {len(self._parameters)} parameters to Java after READY")
            try:
                self._transport.send(ParametersUpdateCmd(self._parameters).to_bytes(self._codec))
                log.info(f"[PythonStrategy] run() - successfully sent {len(self._parameters)} parameters to Java")
            except Exception as e:
                log.error(f"[PythonStrategy] run() - failed to send parameters: {e}", exc_info=True)
        else:
            log.debug("[PythonStrategy] run() - no parameters to send")
        
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
        elif env.type == _TYPE_JAVA_PARAMS_UPDATE:
            self.on_java_parameters_update(env.as_java_parameters_update())
        else:
            self.on_unknown(env)

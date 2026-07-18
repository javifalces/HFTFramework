"""
Message schemas for the Java ↔ Python bridge.

Wire envelope:
  {"v": 1, "type": "<type>", "instrument": "<pk>", "data": {...}}

The encoding of that envelope (JSON vs MessagePack) is determined by the
:class:`~python_algo.codec.Codec` in use.  The default is
:class:`~python_algo.codec.JsonCodec`.

Market-data types (Java → Python):
  depth             DepthMsg
  trade             TradeMsg
  execution_report  ExecutionReportMsg
  candle            CandleMsg
  java_parameters_update    JavaParametersUpdateMsg

Command types (Python → Java):
  order_request              OrderRequestCmd
  quote_request              QuoteRequestCmd
  request_info               RequestInfoCmd
  portfolio_snapshot_request PortfolioSnapshotRequestCmd

Response types (Java → Python):
  portfolio_snapshot         PortfolioSnapshotMsg
"""

from __future__ import annotations

import json
from dataclasses import dataclass, field
from typing import TYPE_CHECKING, List, Optional

if TYPE_CHECKING:
    from python_algo.codec import Codec

SCHEMA_VERSION = 1

# Lazy default so that importing messages.py does not force codec.py to run.
def _default_codec() -> "Codec":
    from python_algo.codec import JsonCodec
    return JsonCodec()


# ---------------------------------------------------------------------------
# Inbound (Java → Python)
# ---------------------------------------------------------------------------

@dataclass
class DepthMsg:
    instrument: str
    timestamp: int                  # epoch ms
    bids: List[float]
    asks: List[float]
    bids_quantities: List[float]
    asks_quantities: List[float]
    levels: int = 0

    @staticmethod
    def from_dict(d: dict) -> "DepthMsg":
        return DepthMsg(
            instrument=d["instrument"],
            timestamp=int(d["timestamp"]),
            bids=d.get("bids", []),
            asks=d.get("asks", []),
            bids_quantities=d.get("bidsQuantities", []),
            asks_quantities=d.get("asksQuantities", []),
            levels=int(d.get("levels", 0)),
        )

    @property
    def best_bid(self) -> float:
        return self.bids[0] if self.bids else float("nan")

    @property
    def best_ask(self) -> float:
        return self.asks[0] if self.asks else float("nan")

    @property
    def mid(self) -> float:
        return (self.best_bid + self.best_ask) / 2.0

    @property
    def spread(self) -> float:
        return self.best_ask - self.best_bid


@dataclass
class TradeMsg:
    instrument: str
    timestamp: int
    price: float
    quantity: float

    @staticmethod
    def from_dict(d: dict) -> "TradeMsg":
        return TradeMsg(
            instrument=d["instrument"],
            timestamp=int(d["timestamp"]),
            price=float(d["price"]),
            quantity=float(d["quantity"]),
        )


@dataclass
class ExecutionReportMsg:
    instrument: str
    client_order_id: str
    status: str          # ExecutionReportStatus enum name
    verb: str            # buy / sell
    price: float
    quantity: float
    quantity_fill: float
    last_quantity: float
    timestamp_creation: int
    algorithm_info: str = ""
    reject_reason: str = ""

    @staticmethod
    def from_dict(d: dict) -> "ExecutionReportMsg":
        return ExecutionReportMsg(
            instrument=d.get("instrument", ""),
            client_order_id=d.get("clientOrderId", ""),
            status=d.get("executionReportStatus", ""),
            verb=d.get("verb", ""),
            price=float(d.get("price", 0.0)),
            quantity=float(d.get("quantity", 0.0)),
            quantity_fill=float(d.get("quantityFill", 0.0)),
            last_quantity=float(d.get("lastQuantity", 0.0)),
            timestamp_creation=int(d.get("timestampCreation", 0)),
            algorithm_info=d.get("algorithmInfo", ""),
            reject_reason=d.get("rejectReason", ""),
        )


@dataclass
class CandleMsg:
    instrument: str
    timestamp: int       # epoch ms
    open: float
    high: float
    low: float
    close: float
    candle_type: str = ""
    open_volume: float = 0.0
    high_volume: float = 0.0
    low_volume: float = 0.0
    close_volume: float = 0.0

    @staticmethod
    def from_dict(d: dict) -> "CandleMsg":
        return CandleMsg(
            instrument=d.get("instrumentPk", ""),
            timestamp=int(d.get("timestamp", 0)),
            open=float(d.get("open", 0.0)),
            high=float(d.get("high", 0.0)),
            low=float(d.get("low", 0.0)),
            close=float(d.get("close", 0.0)),
            candle_type=d.get("candleType", ""),
            open_volume=float(d.get("openVolume", 0.0)),
            high_volume=float(d.get("highVolume", 0.0)),
            low_volume=float(d.get("lowVolume", 0.0)),
            close_volume=float(d.get("closeVolume", 0.0)),
        )


@dataclass
class JavaParametersUpdateMsg:
    """
    Java-side parameter update pushed to Python via PUB socket.
    
    This message is sent by Java when parameters are updated, allowing
    Python to react to configuration changes.
    
    Wire format:
        {"v": 1, "type": "java_parameters_update", "instrument": "", "data": {...}}
    """
    parameters: dict
    
    @staticmethod
    def from_dict(d: dict) -> "JavaParametersUpdateMsg":
        return JavaParametersUpdateMsg(parameters=d)


@dataclass
class Envelope:
    """Parsed inbound envelope from Java."""
    version: int
    type: str
    instrument: str
    data: dict

    @staticmethod
    def parse(raw: bytes, codec: Optional["Codec"] = None) -> "Envelope":
        d = (codec or _default_codec()).decode(raw)
        return Envelope(
            version=int(d.get("v", 1)),
            type=d["type"],
            instrument=d.get("instrument", ""),
            data=d["data"],
        )

    def as_depth(self) -> DepthMsg:
        return DepthMsg.from_dict(self.data)

    def as_trade(self) -> TradeMsg:
        return TradeMsg.from_dict(self.data)

    def as_execution_report(self) -> ExecutionReportMsg:
        return ExecutionReportMsg.from_dict(self.data)

    def as_candle(self) -> CandleMsg:
        return CandleMsg.from_dict(self.data)
    
    def as_java_parameters_update(self) -> JavaParametersUpdateMsg:
        return JavaParametersUpdateMsg.from_dict(self.data)


# ---------------------------------------------------------------------------
# Outbound (Python → Java)
# ---------------------------------------------------------------------------

@dataclass
class OrderRequestCmd:
    instrument: str
    verb: str                    # "Buy" | "Sell"
    order_type: str              # "Limit" | "Market"
    quantity: float
    price: float = 0.0
    order_request_action: str = "Send"   # "Send" | "Cancel" | "Modify"
    client_order_id: str = ""
    orig_client_order_id: str = ""
    algorithm_info: str = ""

    def to_envelope(self) -> dict:
        data = {
            "instrument": self.instrument,
            "verb": self.verb,
            "orderType": self.order_type,
            "quantity": self.quantity,
            "price": self.price,
            "orderRequestAction": self.order_request_action,
            "clientOrderId": self.client_order_id,
            "origClientOrderId": self.orig_client_order_id,
            "algorithmInfo": self.algorithm_info,
        }
        return {"v": SCHEMA_VERSION, "type": "order_request", "data": data}

    def to_bytes(self, codec: Optional["Codec"] = None) -> bytes:
        return (codec or _default_codec()).encode(self.to_envelope())

    def to_json(self) -> str:
        return json.dumps(self.to_envelope())


@dataclass
class QuoteRequestCmd:
    instrument: str
    bid_price: float
    bid_quantity: float
    ask_price: float
    ask_quantity: float
    quote_request_action: str = "On"  # "On" | "Off"
    algorithm_info: str = ""

    def to_envelope(self) -> dict:
        data = {
            "instrument": self.instrument,
            "bidPrice": self.bid_price,
            "bidQuantity": self.bid_quantity,
            "askPrice": self.ask_price,
            "askQuantity": self.ask_quantity,
            "quoteRequestAction": self.quote_request_action,
            "algorithmInfo": self.algorithm_info,
        }
        return {"v": SCHEMA_VERSION, "type": "quote_request", "data": data}

    def to_bytes(self, codec: Optional["Codec"] = None) -> bytes:
        return (codec or _default_codec()).encode(self.to_envelope())

    def to_json(self) -> str:
        return json.dumps(self.to_envelope())


@dataclass
class RequestInfoCmd:
    info: str

    def to_envelope(self) -> dict:
        return {"v": SCHEMA_VERSION, "type": "request_info", "data": {"info": self.info}}

    def to_bytes(self, codec: Optional["Codec"] = None) -> bytes:
        return (codec or _default_codec()).encode(self.to_envelope())

    def to_json(self) -> str:
        return json.dumps(self.to_envelope())


@dataclass
class PortfolioSnapshotRequestCmd:
    """
    Command to request the current portfolio snapshot from Java (synchronous).
    
    This command is sent via the REQ socket and blocks waiting for a
    PortfolioSnapshotMsg response. Use PythonStrategy.get_portfolio_snapshot()
    instead of constructing this directly.
    
    Wire format:
        {"v": 1, "type": "portfolio_snapshot_request", "data": {}}
    
    Example:
        # Don't use directly - use the strategy method instead:
        snapshot = strategy.get_portfolio_snapshot(timeout_ms=5000)
    """

    def to_envelope(self) -> dict:
        """Convert to wire envelope dict."""
        return {"v": SCHEMA_VERSION, "type": "portfolio_snapshot_request", "data": {}}

    def to_bytes(self, codec: Optional["Codec"] = None) -> bytes:
        """Encode to bytes using the specified codec (or default JsonCodec)."""
        return (codec or _default_codec()).encode(self.to_envelope())

    def to_json(self) -> str:
        """Encode to JSON string."""
        return json.dumps(self.to_envelope())


@dataclass
class GetParametersRequestCmd:
    """
    Command to request Java-side parameters (synchronous).
    
    This command is sent via the REQ socket to retrieve parameters from the
    Java PythonAlgorithm instance.
    
    Wire format:
        {"v": 1, "type": "get_parameters_request", "data": {}}
    """

    def to_envelope(self) -> dict:
        """Convert to wire envelope dict."""
        return {"v": SCHEMA_VERSION, "type": "get_parameters_request", "data": {}}

    def to_bytes(self, codec: Optional["Codec"] = None) -> bytes:
        """Encode to bytes using the specified codec (or default JsonCodec)."""
        return (codec or _default_codec()).encode(self.to_envelope())

    def to_json(self) -> str:
        """Encode to JSON string."""
        return json.dumps(self.to_envelope())


@dataclass
class SetParametersRequestCmd:
    """
    Command to send parameters to Java (synchronous).
    
    This command is sent via the REQ socket to update parameters in the
    Java PythonAlgorithm instance.
    
    Wire format:
        {"v": 1, "type": "set_parameters_request", "data": {...}}
    """
    parameters: dict

    def to_envelope(self) -> dict:
        """Convert to wire envelope dict."""
        return {"v": SCHEMA_VERSION, "type": "set_parameters_request", "data": self.parameters}

    def to_bytes(self, codec: Optional["Codec"] = None) -> bytes:
        """Encode to bytes using the specified codec (or default JsonCodec)."""
        return (codec or _default_codec()).encode(self.to_envelope())

    def to_json(self) -> str:
        """Encode to JSON string."""
        return json.dumps(self.to_envelope())


@dataclass
class ParametersUpdateCmd:
    """
    Command to push Python parameters to Java (asynchronous).
    
    This command is sent via the PUSH socket to inform Java of Python-side
    parameter changes. Java will merge these with its own parameters.
    
    Wire format:
        {"v": 1, "type": "parameters_update", "data": {...}}
    """
    parameters: dict

    def to_envelope(self) -> dict:
        """Convert to wire envelope dict."""
        return {"v": SCHEMA_VERSION, "type": "parameters_update", "data": self.parameters}

    def to_bytes(self, codec: Optional["Codec"] = None) -> bytes:
        """Encode to bytes using the specified codec (or default JsonCodec)."""
        return (codec or _default_codec()).encode(self.to_envelope())

    def to_json(self) -> str:
        """Encode to JSON string."""
        return json.dumps(self.to_envelope())


@dataclass
class ReadyCmd:
    """
    Command to signal Python strategy is initialized and ready (asynchronous).
    
    This command is sent via the PUSH socket to inform Java that the Python
    strategy is fully initialized and ready to receive market data events.
    
    Wire format:
        {"v": 1, "type": "ready", "data": {"version": "...", "strategy": "..."}}
    """
    version: str = "1.0"
    strategy: str = "PythonStrategy"

    def to_envelope(self) -> dict:
        """Convert to wire envelope dict."""
        return {
            "v": SCHEMA_VERSION,
            "type": "ready",
            "data": {
                "version": self.version,
                "strategy": self.strategy,
            }
        }

    def to_bytes(self, codec: Optional["Codec"] = None) -> bytes:
        """Encode to bytes using the specified codec (or default JsonCodec)."""
        return (codec or _default_codec()).encode(self.to_envelope())

    def to_json(self) -> str:
        """Encode to JSON string."""
        return json.dumps(self.to_envelope())


@dataclass
class PortfolioSnapshotMsg:
    """
    Portfolio snapshot response from Java.
    
    Contains aggregated portfolio-level metrics and per-instrument P&L breakdown.
    This is the response to a PortfolioSnapshotRequestCmd.
    
    Attributes:
        algorithm_info: Algorithm identifier
        net_investment: Total capital invested across all positions
        realized_pnl: Profit/loss from closed positions
        unrealized_pnl: Profit/loss from open positions (mark-to-market)
        total_pnl: Total P&L (realized + unrealized)
        total_fees: All trading fees incurred (realized + unrealized)
        realized_fees: Fees from closed positions
        unrealized_fees: Fees from open positions
        net_position: Net position size across all instruments
        last_timestamp_update: Timestamp of last update in epoch milliseconds
        instrument_pnl_snapshots: Per-instrument P&L breakdown (dict)
    
    Example:
        snapshot = strategy.get_portfolio_snapshot()
        if snapshot:
            print(f"Total P&L: ${snapshot.total_pnl:.2f}")
            print(f"Net Position: {snapshot.net_position:.4f}")
            
            # Per-instrument breakdown
            for instrument, pnl_data in snapshot.instrument_pnl_snapshots.items():
                print(f"{instrument}: {pnl_data}")
    """
    algorithm_info: str
    net_investment: float
    realized_pnl: float
    unrealized_pnl: float
    total_pnl: float
    total_fees: float
    realized_fees: float
    unrealized_fees: float
    net_position: float
    last_timestamp_update: int
    instrument_pnl_snapshots: dict = field(default_factory=dict)

    @staticmethod
    def from_dict(d: dict) -> "PortfolioSnapshotMsg":
        """
        Parse from Java response data dict.
        
        Args:
            d: Response data dict from Java with camelCase field names
            
        Returns:
            PortfolioSnapshotMsg instance with all fields populated
        """
        return PortfolioSnapshotMsg(
            algorithm_info=d.get("algorithmInfo", ""),
            net_investment=float(d.get("netInvestment", 0.0)),
            realized_pnl=float(d.get("realizedPnl", 0.0)),
            unrealized_pnl=float(d.get("unrealizedPnl", 0.0)),
            total_pnl=float(d.get("totalPnl", 0.0)),
            total_fees=float(d.get("totalFees", 0.0)),
            realized_fees=float(d.get("realizedFees", 0.0)),
            unrealized_fees=float(d.get("unrealizedFees", 0.0)),
            net_position=float(d.get("netPosition", 0.0)),
            last_timestamp_update=int(d.get("lastTimestampUpdate", 0)),
            instrument_pnl_snapshots=d.get("instrumentPnlSnapshotMap", {}),
        )

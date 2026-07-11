"""
Message schemas for the Java ↔ Python bridge.

All wire messages are JSON with envelope:
  {"v": 1, "type": "<type>", "instrument": "<pk>", "data": {...}}

Market-data types (Java → Python):
  depth             DepthMsg
  trade             TradeMsg
  execution_report  ExecutionReportMsg

Command types (Python → Java):
  order_request     OrderRequestCmd
  quote_request     QuoteRequestCmd
  request_info      RequestInfoCmd
"""

from __future__ import annotations

import json
from dataclasses import dataclass, asdict, field
from typing import List, Optional

SCHEMA_VERSION = 1


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
class Envelope:
    """Parsed inbound envelope from Java."""
    version: int
    type: str
    instrument: str
    data: dict

    @staticmethod
    def parse(raw: bytes) -> "Envelope":
        d = json.loads(raw.decode("utf-8"))
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

    def to_json(self) -> str:
        return json.dumps(self.to_envelope())


@dataclass
class RequestInfoCmd:
    info: str

    def to_envelope(self) -> dict:
        return {"v": SCHEMA_VERSION, "type": "request_info", "data": {"info": self.info}}

    def to_json(self) -> str:
        return json.dumps(self.to_envelope())

"""
Codec abstraction for the Java ↔ Python bridge.

A Codec serialises the envelope dict to bytes (for sending commands) and
deserialises bytes back to an envelope dict (for receiving market-data).

Provided implementations
------------------------
JsonCodec    – UTF-8 JSON (default, always available)
MsgpackCodec – binary MessagePack; requires ``pip install msgpack``

Usage
-----
from python_algo.codec import MsgpackCodec

transport = ZmqTransport(..., codec=MsgpackCodec())
"""

from __future__ import annotations

import abc
import json
from typing import Any


class Codec(abc.ABC):
    """Serialise / deserialise envelope dicts to/from bytes."""

    @abc.abstractmethod
    def encode(self, data: dict) -> bytes:
        """Encode an envelope dict to bytes for transmission."""

    @abc.abstractmethod
    def decode(self, raw: bytes) -> dict:
        """Decode received bytes into an envelope dict."""


class JsonCodec(Codec):
    """UTF-8 JSON codec (default)."""

    def encode(self, data: dict) -> bytes:
        return json.dumps(data, separators=(",", ":")).encode("utf-8")

    def decode(self, raw: bytes) -> dict:
        return json.loads(raw.decode("utf-8"))


class MsgpackCodec(Codec):
    """
    Binary MessagePack codec.

    Install: ``pip install msgpack``

    ~3× faster to parse than JSON; smaller wire footprint for numeric-heavy
    market-data messages.
    """

    def __init__(self, use_bin_type: bool = True) -> None:
        try:
            import msgpack as _msgpack  # noqa: F401
        except ImportError as exc:
            raise ImportError(
                "msgpack is not installed. Run: pip install msgpack"
            ) from exc
        self._use_bin_type = use_bin_type

    def encode(self, data: dict) -> bytes:
        import msgpack
        return msgpack.packb(data, use_bin_type=self._use_bin_type)

    def decode(self, raw: bytes) -> dict:
        import msgpack
        return msgpack.unpackb(raw, raw=False)

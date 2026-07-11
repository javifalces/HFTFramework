"""
Transport abstraction for the Java ↔ Python bridge.

Interface
---------
Transport (ABC)
  recv() -> bytes | None
  send(data: bytes) -> None
  subscribe(topic: str) -> None
  close() -> None

ZmqTransport
  SUB socket: subscribes to Java PUB (md_sub_host:md_sub_port)
  PUSH socket: pushes commands to Java PULL (cmd_push_host:cmd_push_port)
"""

from __future__ import annotations

import abc
from typing import Optional


class Transport(abc.ABC):
    """Minimal bidirectional transport for market-data and commands."""

    @abc.abstractmethod
    def recv(self, timeout_ms: int = 200) -> Optional[bytes]:
        """
        Receive the next inbound message payload (without topic frame).
        Returns None on timeout.
        """

    @abc.abstractmethod
    def send(self, data: bytes) -> None:
        """Send an outbound command payload."""

    @abc.abstractmethod
    def subscribe(self, topic: str) -> None:
        """Add a subscription filter (empty string = all topics)."""

    @abc.abstractmethod
    def close(self) -> None:
        """Release all resources."""


class ZmqTransport(Transport):
    """
    ZeroMQ implementation:
      SUB socket ← Java PUB  (market data)
      PUSH socket → Java PULL (commands)

    Parameters
    ----------
    md_sub_host   : host where Java PUB socket is running
    md_sub_port   : port of Java PUB socket   (default 7700)
    cmd_push_host : host where Java PULL socket is running
    cmd_push_port : port of Java PULL socket  (default 7701)
    """

    def __init__(
        self,
        md_sub_host: str = "localhost",
        md_sub_port: int = 7700,
        cmd_push_host: str = "localhost",
        cmd_push_port: int = 7701,
    ) -> None:
        import zmq

        self._ctx = zmq.Context.instance()

        self._sub = self._ctx.socket(zmq.SUB)
        self._sub.connect(f"tcp://{md_sub_host}:{md_sub_port}")

        self._push = self._ctx.socket(zmq.PUSH)
        self._push.setsockopt(zmq.LINGER, 0)
        self._push.connect(f"tcp://{cmd_push_host}:{cmd_push_port}")

        self._poller = zmq.Poller()
        self._poller.register(self._sub, zmq.POLLIN)

    def subscribe(self, topic: str = "") -> None:
        self._sub.setsockopt_string(4, topic)  # zmq.SUBSCRIBE = 4

    def recv(self, timeout_ms: int = 200) -> Optional[bytes]:
        socks = dict(self._poller.poll(timeout_ms))
        if self._sub not in socks:
            return None
        # multipart: [topic, payload]
        parts = self._sub.recv_multipart()
        if len(parts) < 2:
            return None
        return parts[1]

    def send(self, data: bytes) -> None:
        self._push.send(data, copy=False)

    def close(self) -> None:
        self._sub.close()
        self._push.close()

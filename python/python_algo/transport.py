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
  Supports two endpoint types:
    tcp  – TCP sockets (cross-host or local)
    ipc  – Unix-domain sockets via ZeroMQ IPC (same host, lower latency)

  SUB socket: subscribes to Java PUB
  PUSH socket: pushes commands to Java PULL

  Accepts an optional :class:`~python_algo.codec.Codec` for
  serialisation.  Pass ``codec=MsgpackCodec()`` to use binary MessagePack
  instead of the default JSON.
"""

from __future__ import annotations

import abc
from typing import TYPE_CHECKING, Optional

if TYPE_CHECKING:
    from python_algo.codec import Codec


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
    ZeroMQ implementation with TCP or IPC endpoints.

    TCP mode (default)
    ------------------
    md_sub_host / md_sub_port     – host:port of the Java PUB socket
    cmd_push_host / cmd_push_port – host:port of the Java PULL socket

    IPC mode (same-host, lower latency)
    ------------------------------------
    Set ``transport_type="ipc"`` and supply Unix socket paths:

      ipc_md_path  (default "/tmp/python_algo_md")
      ipc_cmd_path (default "/tmp/python_algo_cmd")

    The host / port parameters are ignored in IPC mode.

    Codec
    -----
    Pass ``codec=MsgpackCodec()`` to use binary MessagePack encoding.
    The Java side must be configured with ``python_codec=msgpack`` to match.
    """

    _IPC_MD_DEFAULT  = "/tmp/python_algo_md"
    _IPC_CMD_DEFAULT = "/tmp/python_algo_cmd"

    def __init__(
        self,
        *,
        transport_type: str = "tcp",
        # TCP parameters
        md_sub_host: str = "localhost",
        md_sub_port: int = 7700,
        cmd_push_host: str = "localhost",
        cmd_push_port: int = 7701,
        # IPC parameters
        ipc_md_path: str = _IPC_MD_DEFAULT,
        ipc_cmd_path: str = _IPC_CMD_DEFAULT,
        # Serialisation
        codec: Optional["Codec"] = None,
    ) -> None:
        import zmq

        if transport_type not in ("tcp", "ipc"):
            raise ValueError(f"transport_type must be 'tcp' or 'ipc', got {transport_type!r}")

        self._codec = codec  # None → use Codec default (JsonCodec) in messages layer

        self._ctx = zmq.Context.instance()

        self._sub = self._ctx.socket(zmq.SUB)
        self._push = self._ctx.socket(zmq.PUSH)
        self._push.setsockopt(zmq.LINGER, 0)

        if transport_type == "ipc":
            self._sub.connect(f"ipc://{ipc_md_path}")
            self._push.connect(f"ipc://{ipc_cmd_path}")
        else:
            self._sub.connect(f"tcp://{md_sub_host}:{md_sub_port}")
            self._push.connect(f"tcp://{cmd_push_host}:{cmd_push_port}")

        self._poller = zmq.Poller()
        self._poller.register(self._sub, zmq.POLLIN)

    @property
    def codec(self) -> Optional["Codec"]:
        """The codec used for message serialisation, or None for the default (JSON)."""
        return self._codec

    def subscribe(self, topic: str = "") -> None:
        import zmq
        self._sub.setsockopt(zmq.SUBSCRIBE, topic.encode())

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

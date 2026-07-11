"""
python_algo — pure-Python strategy bridge for the Java HFTFramework.

Quickstart (TCP + JSON, default)
---------------------------------
from python_algo import PythonStrategy, ZmqTransport

class MyStrategy(PythonStrategy):
    def on_depth(self, depth): ...
    def on_trade(self, trade): ...
    def on_execution_report(self, er): ...

transport = ZmqTransport(md_sub_port=7700, cmd_push_port=7701)
strategy  = MyStrategy(transport)
strategy.run()

Local IPC + MessagePack (lower latency, same host only)
-------------------------------------------------------
from python_algo import PythonStrategy, ZmqTransport, MsgpackCodec

transport = ZmqTransport(transport_type="ipc", codec=MsgpackCodec())
strategy  = MyStrategy(transport)
strategy.run()
# Java side: python_transport_type=ipc, python_codec=msgpack
"""

from python_algo.codec import Codec, JsonCodec, MsgpackCodec
from python_algo.messages import (
    Envelope, DepthMsg, TradeMsg, ExecutionReportMsg,
    OrderRequestCmd, QuoteRequestCmd, RequestInfoCmd,
)
from python_algo.transport import Transport, ZmqTransport
from python_algo.strategy import PythonStrategy
from python_algo.live_env import PythonAlgoEnv

__all__ = [
    "Codec",
    "JsonCodec",
    "MsgpackCodec",
    "Envelope",
    "DepthMsg",
    "TradeMsg",
    "ExecutionReportMsg",
    "OrderRequestCmd",
    "QuoteRequestCmd",
    "RequestInfoCmd",
    "Transport",
    "ZmqTransport",
    "PythonStrategy",
    "PythonAlgoEnv",
]

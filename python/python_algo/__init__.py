"""
python_algo — pure-Python strategy bridge for the Java HFTFramework.

Quickstart
----------
from python_algo import PythonStrategy, ZmqTransport

class MyStrategy(PythonStrategy):
    def on_depth(self, depth): ...
    def on_trade(self, trade): ...
    def on_execution_report(self, er): ...

transport = ZmqTransport(md_sub_port=7700, cmd_push_port=7701)
strategy  = MyStrategy(transport)
strategy.run()
"""

from python_algo.messages import (
    Envelope, DepthMsg, TradeMsg, ExecutionReportMsg,
    OrderRequestCmd, QuoteRequestCmd, RequestInfoCmd,
)
from python_algo.transport import Transport, ZmqTransport
from python_algo.strategy import PythonStrategy
from python_algo.live_env import PythonAlgoEnv

__all__ = [
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

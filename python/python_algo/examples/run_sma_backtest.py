"""
run_sma_backtest.py — main entry point for the SMA candle strategy backtest.

This script:
1. Launches the Java backtest engine (Backtest.jar) configured to use
   ``PythonAlgorithm`` as the algorithm.  The Java side streams depth,
   trade, execution_report, and candle events via a ZeroMQ PUB socket,
   and accepts order commands on a PULL socket.
2. Connects the :class:`SmaCandleStrategy` to those sockets and runs it
   until the backtest completes.

Configuration
-------------
Edit the constants below to match your environment:
  INSTRUMENT  — instrument primary key (e.g. ``"btcusdt_binance"``)
  START_DATE  — backtest start (datetime)
  END_DATE    — backtest end   (datetime)
  QUANTITY    — order size per trade
  FAST_PERIOD — fast SMA period (candles)
  SLOW_PERIOD — slow SMA period (candles)
  MD_PUB_PORT — Java PUB socket port (must match ``python_md_pub_port``)
  CMD_PULL_PORT — Java PULL socket port (must match ``python_cmd_pull_port``)

Prerequisites
-------------
* A ``Backtest.jar`` built from the repository (see ``java/Backtest/``).
* Market data for the selected instrument available under ``$LAMBDA_DATA_PATH``.
  The backtest reads raw tick data; the Java algorithm generates candles
  from ticks using ``CandleFromTickUpdater`` (default period: 60 s).

Usage
-----
    python -m python_algo.examples.run_sma_backtest

or simply:

    python python/python_algo/examples/run_sma_backtest.py
"""

from __future__ import annotations

import datetime
import logging
import os
import sys
import threading
import time

# ---------------------------------------------------------------------------
# Make sure the python/ directory is on sys.path when run directly
# ---------------------------------------------------------------------------
_HERE = os.path.dirname(os.path.abspath(__file__))
_PYTHON_ROOT = os.path.abspath(os.path.join(_HERE, "..", "..", ".."))
if _PYTHON_ROOT not in sys.path:
    sys.path.insert(0, os.path.join(_PYTHON_ROOT, "python"))

# ---------------------------------------------------------------------------
# Configuration — adjust to your environment
# ---------------------------------------------------------------------------
INSTRUMENT    = "btcusdt_binance"
START_DATE    = datetime.datetime(2024, 11, 9, 7, 0, 0)
END_DATE      = datetime.datetime(2024, 11, 9, 15, 0, 0)
QUANTITY      = 0.001
FAST_PERIOD   = 5
SLOW_PERIOD   = 20
MD_PUB_PORT   = 7700
CMD_PULL_PORT = 7701

# Candle period used by PythonAlgorithm (seconds). Default in the framework is 56 s.
CANDLE_SECONDS = 60

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)s %(name)s — %(message)s",
)
log = logging.getLogger(__name__)


def _build_backtest_launcher():
    """Return a configured BacktestLauncher for PythonAlgorithm."""
    from backtest.backtest_launcher import BacktestLauncher
    from backtest.input_configuration import (
        AlgorithmConfiguration,
        BacktestConfiguration,
        InputConfiguration,
        JAR_PATH,
        MultiThreadConfiguration,
    )

    backtest_cfg = BacktestConfiguration(
        start_date=START_DATE,
        end_date=END_DATE,
        instrument_pk=INSTRUMENT,
        delay_order_ms=0,
        multithread_configuration=MultiThreadConfiguration.singlethread,
        fees_commissions_included=False,
        search_match_market_trades=False,
    )

    algorithm_cfg = AlgorithmConfiguration(
        algorithm_name="PythonAlgorithm_sma",
        parameters={
            "python_md_pub_port":   str(MD_PUB_PORT),
            "python_cmd_pull_port": str(CMD_PULL_PORT),
            "python_transport_type": "tcp",
            "python_codec": "json",
            # Candle period forwarded to CandleFromTickUpdater via Algorithm base class
            "candle_seconds": str(CANDLE_SECONDS),
        },
    )

    input_cfg = InputConfiguration(
        backtest_configuration=backtest_cfg,
        algorithm_configuration=algorithm_cfg,
    )

    return BacktestLauncher(
        input_configuration=input_cfg,
        id="PythonAlgorithm_sma",
        jar_path=JAR_PATH,
    )


def main() -> None:
    from python_algo import ZmqTransport
    from python_algo.examples.sma_candle_strategy import SmaCandleStrategy

    launcher = _build_backtest_launcher()
    log.info("Starting Java backtest process …")
    launcher.start()

    # Give the Java process a moment to bind its sockets before connecting.
    time.sleep(3.0)

    transport = ZmqTransport(
        md_sub_port=MD_PUB_PORT,
        cmd_push_port=CMD_PULL_PORT,
    )
    strategy = SmaCandleStrategy(
        transport,
        instrument=INSTRUMENT,
        quantity=QUANTITY,
        fast_period=FAST_PERIOD,
        slow_period=SLOW_PERIOD,
    )

    def _stop_when_done():
        launcher.join()
        log.info("Backtest process finished — stopping strategy.")
        strategy.stop()

    watcher = threading.Thread(target=_stop_when_done, daemon=True)
    watcher.start()

    log.info(
        "Strategy running: instrument=%s fast=%d slow=%d quantity=%s",
        INSTRUMENT,
        FAST_PERIOD,
        SLOW_PERIOD,
        QUANTITY,
    )
    strategy.run()  # blocks until stop() is called

    log.info("Done.")


if __name__ == "__main__":
    main()

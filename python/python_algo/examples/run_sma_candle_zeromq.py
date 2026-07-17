"""
Run SMA candle strategy through the ZeroMQ launcher.

This example generates an algorithm settings JSON for the legacy
`python/zeromq_trading/algotrading_zeromq_launcher.py` flow and then starts
`AlgoTradingZeroMqLauncher`.

Usage
-----
From repository root:

    python -m python_algo.examples.run_sma_candle_zeromq

Optional overrides:

    python -m python_algo.examples.run_sma_candle_zeromq \
      --instrument btcusdt_binance \
      --jvm "-Xmx2048M"
"""

from __future__ import annotations

import argparse
import json
import os
import tempfile
from datetime import datetime


def _build_settings(instrument: str) -> dict:
    now_tag = datetime.utcnow().strftime("%Y%m%d_%H%M%S")
    algorithm_name = f"sma_candle_strategy_{instrument}_{now_tag}"

    # Minimal shape consumed by AlgoTradingZeroMqLauncher:
    # - algorithm.algorithmName is required
    # - rl_gym_configuration is optional; omitted here
    return {
        "algorithm": {
            "algorithmName": algorithm_name,
            "parameters": {
                "instrument": instrument,
                "quantity": "0.001",
                "fast_period": "5",
                "slow_period": "20",
                "candle_type": "mid",
                "seconds_candles": "60",
            },
        }
    }


def main() -> None:
    parser = argparse.ArgumentParser(description="Run sma_candle_strategy via zeromq_trading launcher")
    parser.add_argument("--instrument", default="btcusdt_binance")
    parser.add_argument("--jvm", default="-Xmx2048M")
    args = parser.parse_args()

    from zeromq_trading.algotrading_zeromq_launcher import AlgoTradingZeroMqLauncher

    settings = _build_settings(args.instrument)

    with tempfile.NamedTemporaryFile("w", suffix=".json", delete=False, encoding="utf-8") as tmp:
        json.dump(settings, tmp, indent=2)
        settings_path = tmp.name

    print(f"settings_file={settings_path}")
    print("starting AlgoTradingZeroMqLauncher ...")

    launcher = AlgoTradingZeroMqLauncher(
        algorithm_settings_path=settings_path,
        jvm_options=f"-Duser.timezone=GMT {args.jvm}",
    )
    launcher.run()

    # keep file for reproducibility
    print(f"launcher started, settings kept at: {os.path.abspath(settings_path)}")


if __name__ == "__main__":
    main()

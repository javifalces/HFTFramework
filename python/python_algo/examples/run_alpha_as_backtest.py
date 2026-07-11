"""
run_alpha_as_backtest.py — train and/or evaluate the Alpha Avellaneda-Stoikov agent.

Two modes
---------
1. **Train** (default): runs the Java backtest while the SB3 SAC agent
   collects experience and learns.  At the end the policy is saved to
   ``MODEL_SAVE_PATH``.

2. **Evaluate** (``--eval``): loads a previously saved model and runs one
   evaluation episode through the backtest without updating the weights.

Configuration
-------------
Edit the constants below to match your environment.

Usage
-----
    # Train
    python -m python_algo.examples.run_alpha_as_backtest

    # Evaluate a saved model
    python -m python_algo.examples.run_alpha_as_backtest --eval

Prerequisites
-------------
* ``pip install stable-baselines3 gymnasium pyzmq``
* A ``Backtest.jar`` built from the repository (see ``java/Backtest/``).
* Market data for the selected instrument under ``$LAMBDA_DATA_PATH``.
"""

from __future__ import annotations

import argparse
import datetime
import logging
import os
import sys
import threading
import time

# ---------------------------------------------------------------------------
# Ensure python/ is on sys.path when run directly
# ---------------------------------------------------------------------------
_HERE        = os.path.dirname(os.path.abspath(__file__))
_PYTHON_ROOT = os.path.abspath(os.path.join(_HERE, "..", "..", ".."))
if _PYTHON_ROOT not in sys.path:
    sys.path.insert(0, os.path.join(_PYTHON_ROOT, "python"))

# ---------------------------------------------------------------------------
# Configuration — adjust to your environment
# ---------------------------------------------------------------------------
INSTRUMENT    = "btcusdt_binance"
START_DATE    = datetime.datetime(2024, 11, 9, 7, 0, 0)
END_DATE      = datetime.datetime(2024, 11, 9, 15, 0, 0)

# A-S parameters
GAMMA         = 0.1      # risk-aversion coefficient
KAPPA         = 1.5      # order arrival rate
SIGMA_WINDOW  = 20       # rolling volatility window (depth ticks)
QUANTITY      = 0.001    # quote size per side
INVENTORY_MAX = 0.05     # max absolute inventory before one-sided quote
MAX_ALPHA     = 5e-4     # RL action range [−MAX_ALPHA, +MAX_ALPHA]
INV_PENALTY   = 0.1      # λ in reward = ΔPnL − λ·q²·σ²

# Training
TOTAL_TIMESTEPS = 200_000
MODEL_SAVE_PATH = "alpha_as_sac"

# ZeroMQ ports
MD_PUB_PORT   = 7700
CMD_PULL_PORT = 7701

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)s %(name)s — %(message)s",
)
log = logging.getLogger(__name__)


# ---------------------------------------------------------------------------
# Backtest launcher helper
# ---------------------------------------------------------------------------

def _build_backtest_launcher():
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
        algorithm_name="PythonAlgorithm_alpha_as",
        parameters={
            "python_md_pub_port":    str(MD_PUB_PORT),
            "python_cmd_pull_port":  str(CMD_PULL_PORT),
            "python_transport_type": "tcp",
            "python_codec":          "json",
        },
    )

    input_cfg = InputConfiguration(
        backtest_configuration=backtest_cfg,
        algorithm_configuration=algorithm_cfg,
    )

    return BacktestLauncher(
        input_configuration=input_cfg,
        id="PythonAlgorithm_alpha_as",
        jar_path=JAR_PATH,
    )


# ---------------------------------------------------------------------------
# Train
# ---------------------------------------------------------------------------

def train() -> None:
    from stable_baselines3 import SAC
    from python_algo import ZmqTransport
    from python_algo.examples.alpha_as_env import AlphaASEnv

    launcher = _build_backtest_launcher()
    log.info("Starting Java backtest process …")
    launcher.start()
    time.sleep(3.0)  # wait for Java sockets to bind

    transport = ZmqTransport(md_sub_port=MD_PUB_PORT, cmd_push_port=CMD_PULL_PORT)
    env = AlphaASEnv(
        transport=transport,
        instrument=INSTRUMENT,
        gamma=GAMMA,
        kappa=KAPPA,
        sigma_window=SIGMA_WINDOW,
        quantity=QUANTITY,
        inventory_max=INVENTORY_MAX,
        max_alpha=MAX_ALPHA,
        inventory_risk_penalty=INV_PENALTY,
    )

    model = SAC(
        "MlpPolicy",
        env,
        verbose=1,
        learning_rate=3e-4,
        buffer_size=100_000,
        batch_size=256,
        gamma=0.99,
        tau=0.005,
        ent_coef="auto",
    )

    done = threading.Event()

    def _stop_when_done():
        launcher.join()
        log.info("Backtest process finished — stopping training loop.")
        done.set()

    watcher = threading.Thread(target=_stop_when_done, daemon=True)
    watcher.start()

    log.info(
        "Training SAC agent: instrument=%s timesteps=%d", INSTRUMENT, TOTAL_TIMESTEPS
    )

    # learn() is blocking; we rely on the Java process finishing or the
    # total_timesteps limit, whichever comes first.
    model.learn(total_timesteps=TOTAL_TIMESTEPS, reset_num_timesteps=True)

    env.close()
    model.save(MODEL_SAVE_PATH)
    log.info("Model saved to %s", MODEL_SAVE_PATH)


# ---------------------------------------------------------------------------
# Evaluate
# ---------------------------------------------------------------------------

def evaluate() -> None:
    from stable_baselines3 import SAC
    from python_algo import ZmqTransport
    from python_algo.examples.alpha_as_env import AlphaASEnv

    launcher = _build_backtest_launcher()
    log.info("Starting Java backtest process …")
    launcher.start()
    time.sleep(3.0)

    transport = ZmqTransport(md_sub_port=MD_PUB_PORT, cmd_push_port=CMD_PULL_PORT)
    env = AlphaASEnv(
        transport=transport,
        instrument=INSTRUMENT,
        gamma=GAMMA,
        kappa=KAPPA,
        sigma_window=SIGMA_WINDOW,
        quantity=QUANTITY,
        inventory_max=INVENTORY_MAX,
        max_alpha=MAX_ALPHA,
        inventory_risk_penalty=INV_PENALTY,
    )

    model = SAC.load(MODEL_SAVE_PATH, env=env)
    log.info("Loaded model from %s", MODEL_SAVE_PATH)

    obs, _ = env.reset()
    total_reward = 0.0
    steps = 0

    running = True
    while running:
        action, _ = model.predict(obs, deterministic=True)
        obs, reward, terminated, truncated, info = env.step(action)
        total_reward += reward
        steps += 1
        if terminated or truncated or info.get("timeout"):
            running = False

    env.close()
    log.info("Evaluation done: steps=%d total_reward=%.6f", steps, total_reward)


# ---------------------------------------------------------------------------
# Entry point
# ---------------------------------------------------------------------------

def main() -> None:
    parser = argparse.ArgumentParser(
        description="Alpha Avellaneda-Stoikov RL agent — train or evaluate."
    )
    parser.add_argument(
        "--eval",
        action="store_true",
        help="Load a saved model and run one evaluation episode (no training).",
    )
    args = parser.parse_args()

    if args.eval:
        evaluate()
    else:
        train()


if __name__ == "__main__":
    main()

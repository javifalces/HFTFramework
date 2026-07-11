"""
AlphaASEnv — Gymnasium environment for RL-enhanced Avellaneda-Stoikov.

The agent learns a directional **alpha** signal that skews the A-S
reservation price, effectively learning when to lean the book in one
direction based on current market context.

Observation (Box, float32)
--------------------------
Index  Feature
  0    Normalised inventory   q / inventory_max  ∈ [-1, 1]
  1    Spread / mid                               ∈ [0, ∞)
  2    Rolling σ (volatility estimate)
  3–7  Last 5 normalised mid-price changes        ∈ [-1, 1] (clipped)
  8    Normalised PnL change since last step

Action (Box, float32)
---------------------
  [0]  alpha ∈ [-max_alpha, +max_alpha]
       Positive → shift reservation price up (bullish lean).
       Negative → shift reservation price down (bearish lean).

Reward
------
  r = ΔPnL − λ * q² * σ²
  ΔPnL = Δcash + Δinventory_value (mark-to-market)
  λ     = inventory_risk_penalty (controls risk aversion in reward)

Usage
-----
    from stable_baselines3 import SAC
    from python_algo import ZmqTransport
    from python_algo.examples.alpha_as_env import AlphaASEnv

    env = AlphaASEnv(
        transport=ZmqTransport(md_sub_port=7700, cmd_push_port=7701),
        instrument="btcusdt_binance",
        gamma=0.1,
        kappa=1.5,
        quantity=0.001,
        inventory_max=0.05,
        max_alpha=0.0005,
        inventory_risk_penalty=0.1,
    )
    model = SAC("MlpPolicy", env, verbose=1)
    model.learn(total_timesteps=200_000)
    model.save("alpha_as_sac")
"""

from __future__ import annotations

import collections
import logging
import math
from typing import Any, Dict, Optional, Tuple

import numpy as np
import gymnasium
from gymnasium import spaces

from python_algo.messages import (
    CandleMsg,
    DepthMsg,
    Envelope,
    ExecutionReportMsg,
    TradeMsg,
    QuoteRequestCmd,
    RequestInfoCmd,
)
from python_algo.transport import Transport

log = logging.getLogger(__name__)

_OBS_DIM = 9          # see docstring above
_PRICE_CHANGE_HIST = 5


class AlphaASEnv(gymnasium.Env):
    """
    Gymnasium environment wrapping an Avellaneda-Stoikov market maker.

    The Java backtest engine streams depth and execution-report events;
    on every depth tick the RL agent outputs an alpha value that is
    applied to the A-S reservation price before quoting.

    Works identically in backtest and live mode.
    """

    metadata = {"render_modes": []}

    def __init__(
        self,
        transport: Transport,
        instrument: str,
        gamma: float = 0.1,
        kappa: float = 1.5,
        sigma_window: int = 20,
        quantity: float = 0.001,
        inventory_max: float = 0.05,
        min_half_spread: float = 0.0,
        max_alpha: float = 5e-4,
        inventory_risk_penalty: float = 0.1,
        max_wait_ms: int = 5_000,
        codec=None,
    ) -> None:
        super().__init__()

        self.observation_space = spaces.Box(
            low=-np.inf, high=np.inf, shape=(_OBS_DIM,), dtype=np.float32
        )
        self.action_space = spaces.Box(
            low=np.array([-max_alpha], dtype=np.float32),
            high=np.array([max_alpha], dtype=np.float32),
            dtype=np.float32,
        )

        self._transport   = transport
        self._instrument  = instrument
        self._gamma       = gamma
        self._kappa       = kappa
        self._sigma_window = sigma_window
        self._quantity    = quantity
        self._inventory_max = inventory_max
        self._min_half_spread = min_half_spread
        self._max_alpha   = max_alpha
        self._inv_penalty = inventory_risk_penalty
        self._max_wait_ms = max_wait_ms
        self._codec       = getattr(transport, "codec", None) or codec

        transport.subscribe("")  # all topics

        self._reset_state()

    # ------------------------------------------------------------------
    # Gymnasium API
    # ------------------------------------------------------------------

    def reset(
        self,
        *,
        seed: Optional[int] = None,
        options: Optional[Dict] = None,
    ) -> Tuple[np.ndarray, Dict]:
        super().reset(seed=seed)
        self._reset_state()

        depth = self._wait_for_depth()
        if depth is None:
            return np.zeros(_OBS_DIM, dtype=np.float32), {"timeout": True}

        obs = self._observe(depth)
        return obs, {}

    def step(self, action: Any) -> Tuple[np.ndarray, float, bool, bool, Dict]:
        alpha = float(np.clip(action[0], -self._max_alpha, self._max_alpha))

        # Apply alpha and send quote based on last known depth
        if self._last_depth is not None:
            self._quote(self._last_depth, alpha)

        depth = self._wait_for_depth()
        if depth is None:
            obs = np.zeros(_OBS_DIM, dtype=np.float32)
            return obs, 0.0, True, False, {"timeout": True}

        rew = self._reward(depth)
        obs = self._observe(depth)
        self._step_count += 1
        return obs, rew, False, False, {}

    def close(self) -> None:
        self._transport.close()
        super().close()

    # ------------------------------------------------------------------
    # Internal: quoting
    # ------------------------------------------------------------------

    def _quote(self, depth: DepthMsg, alpha: float) -> None:
        mid   = depth.mid
        q     = self._inventory
        sigma = self._sigma()

        gamma, kappa, T = self._gamma, self._kappa, 1.0

        r            = mid - q * gamma * sigma ** 2 * T + alpha
        half_spread  = max(
            gamma * sigma ** 2 * T / 2.0 + (1.0 / gamma) * math.log(1.0 + gamma / kappa),
            self._min_half_spread,
        )

        bid_price = min(r - half_spread, depth.best_bid)
        ask_price = max(r + half_spread, depth.best_ask)

        bid_qty = self._quantity if q < self._inventory_max  else 0.0
        ask_qty = self._quantity if q > -self._inventory_max else 0.0

        if bid_qty == 0.0 and ask_qty == 0.0:
            return

        cmd = QuoteRequestCmd(
            instrument=self._instrument,
            bid_price=bid_price,
            bid_quantity=bid_qty,
            ask_price=ask_price,
            ask_quantity=ask_qty,
        )
        self._transport.send(cmd.to_bytes(self._codec))

    # ------------------------------------------------------------------
    # Internal: observation / reward
    # ------------------------------------------------------------------

    def _observe(self, depth: DepthMsg) -> np.ndarray:
        inv_norm = np.clip(self._inventory / self._inventory_max, -1.0, 1.0)
        spread_norm = depth.spread / depth.mid if depth.mid != 0 else 0.0
        sigma = self._sigma()

        changes = list(self._mid_changes)
        # Pad / clip to exactly _PRICE_CHANGE_HIST
        while len(changes) < _PRICE_CHANGE_HIST:
            changes.insert(0, 0.0)
        changes = changes[-_PRICE_CHANGE_HIST:]
        # Normalise by current sigma
        norm_changes = [np.clip(c / (sigma + 1e-10), -1.0, 1.0) for c in changes]

        pnl_delta = (self._pnl - self._prev_pnl) / (depth.mid + 1e-10)

        obs = np.array(
            [inv_norm, spread_norm, sigma] + norm_changes + [pnl_delta],
            dtype=np.float32,
        )
        self._prev_pnl = self._pnl
        return obs

    def _reward(self, depth: DepthMsg) -> float:
        # Mark-to-market PnL: cash + inventory * current mid
        mtm = self._cash + self._inventory * depth.mid
        delta_pnl = mtm - self._prev_mtm
        self._prev_mtm = mtm
        self._pnl = delta_pnl

        sigma = self._sigma()
        penalty = self._inv_penalty * (self._inventory ** 2) * (sigma ** 2)

        return float(delta_pnl - penalty)

    # ------------------------------------------------------------------
    # Internal: state management
    # ------------------------------------------------------------------

    def _reset_state(self) -> None:
        self._inventory: float  = 0.0
        self._cash: float       = 0.0
        self._pnl: float        = 0.0
        self._prev_pnl: float   = 0.0
        self._prev_mtm: float   = 0.0
        self._step_count: int   = 0
        self._last_depth: Optional[DepthMsg]             = None
        self._last_er:    Optional[ExecutionReportMsg]   = None
        self._mids:   collections.deque = collections.deque(maxlen=self._sigma_window + 1)
        self._mid_changes: collections.deque = collections.deque(maxlen=_PRICE_CHANGE_HIST)

    def _sigma(self) -> float:
        if len(self._mids) < 2:
            return 1e-6
        changes = [self._mids[i] - self._mids[i - 1] for i in range(1, len(self._mids))]
        n = len(changes)
        mean = sum(changes) / n
        variance = sum((c - mean) ** 2 for c in changes) / n
        return math.sqrt(variance) if variance > 0 else 1e-6

    # ------------------------------------------------------------------
    # Internal: message loop
    # ------------------------------------------------------------------

    def _wait_for_depth(self) -> Optional[DepthMsg]:
        waited, limit, poll = 0, self._max_wait_ms, min(200, self._max_wait_ms)
        while waited < limit:
            raw = self._transport.recv(poll)
            waited += poll
            if raw is None:
                continue
            try:
                env = Envelope.parse(raw, self._codec)
            except Exception as exc:
                log.warning("parse error: %s", exc)
                continue

            if env.type == "depth":
                depth = env.as_depth()
                if depth.instrument != self._instrument:
                    continue
                mid = depth.mid
                if not math.isnan(mid):
                    if self._mids:
                        self._mid_changes.append(mid - self._mids[-1])
                    self._mids.append(mid)
                self._last_depth = depth
                return depth

            elif env.type == "execution_report":
                er = env.as_execution_report()
                if er.instrument == self._instrument and er.status in (
                    "CompletellyFilled", "PartialFilled"
                ):
                    delta = er.last_quantity if er.last_quantity > 0 else er.quantity_fill
                    if er.verb.lower() in ("buy", "b"):
                        self._inventory += delta
                        self._cash -= delta * er.price
                    else:
                        self._inventory -= delta
                        self._cash += delta * er.price
                    self._last_er = er

        log.warning("_wait_for_depth timed out after %d ms", limit)
        return None

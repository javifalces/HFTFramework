"""
PythonAlgoEnv — Gymnasium environment wrapper for Stable-Baselines3.

Wraps a PythonStrategy subclass so that:
  - reset() subscribes and waits for the first depth event
  - step(action) passes the action to the strategy and waits for the next depth
  - observation is derived from the last DepthMsg via observe()
  - reward is derived from the last DepthMsg / ExecutionReport via reward()

Subclass PythonAlgoEnv and implement:
  observe(depth: DepthMsg, er: Optional[ExecutionReportMsg]) -> np.ndarray
  reward(depth: DepthMsg, er: Optional[ExecutionReportMsg]) -> float
  act(action, depth: DepthMsg) -> None

Example
-------
class MyEnv(PythonAlgoEnv):
    def observe(self, depth, er):
        return np.array([depth.mid, depth.spread])

    def reward(self, depth, er):
        return 0.0

    def act(self, action, depth):
        # send an order based on the action
        self.send_order(OrderRequestCmd(...))

env = MyEnv(
    observation_space=spaces.Box(...),
    action_space=spaces.Discrete(3),
    transport=ZmqTransport(md_sub_port=7700, cmd_push_port=7701),
)
model = PPO("MlpPolicy", env, verbose=1)
model.learn(total_timesteps=100_000)
"""

from __future__ import annotations

import abc
import logging
from typing import Optional, Tuple, Any, Dict

import numpy as np
import gymnasium
from gymnasium import spaces

from python_algo.messages import (
    Envelope,
    DepthMsg,
    TradeMsg,
    ExecutionReportMsg,
    OrderRequestCmd,
    QuoteRequestCmd,
    RequestInfoCmd,
)
from python_algo.transport import Transport

log = logging.getLogger(__name__)

_TYPE_DEPTH = "depth"
_TYPE_TRADE = "trade"
_TYPE_ER    = "execution_report"


class PythonAlgoEnv(gymnasium.Env):
    """
    Gymnasium environment that drives a Python strategy connected to the
    Java HFTFramework via ZeroMQ.

    Works identically in backtest and live mode — the Java side decides
    the pace of market data delivery.
    """

    metadata = {"render_modes": []}

    def __init__(
        self,
        observation_space: spaces.Space,
        action_space: spaces.Space,
        transport: Transport,
        instruments: Optional[list] = None,
        max_wait_ms: int = 5_000,
    ) -> None:
        super().__init__()
        self.observation_space = observation_space
        self.action_space      = action_space
        self._transport        = transport
        self._max_wait_ms      = max_wait_ms

        self._last_depth: Optional[DepthMsg]           = None
        self._last_er:    Optional[ExecutionReportMsg] = None
        self._done        = False
        self._step_count  = 0

        if instruments:
            for inst in instruments:
                transport.subscribe(inst)
        else:
            transport.subscribe("")

    # -----------------------------------------------------------------------
    # Abstract interface — implement in subclass
    # -----------------------------------------------------------------------

    @abc.abstractmethod
    def observe(
        self,
        depth: DepthMsg,
        er: Optional[ExecutionReportMsg],
    ) -> np.ndarray:
        """Build the observation vector from the latest market state."""

    @abc.abstractmethod
    def reward(
        self,
        depth: DepthMsg,
        er: Optional[ExecutionReportMsg],
    ) -> float:
        """Compute the reward signal for the current step."""

    @abc.abstractmethod
    def act(self, action: Any, depth: DepthMsg) -> None:
        """Translate the RL action into order/quote commands."""

    def is_done(self, depth: DepthMsg, er: Optional[ExecutionReportMsg]) -> bool:
        """Override to signal episode termination (default: never)."""
        return False

    # -----------------------------------------------------------------------
    # Gymnasium API
    # -----------------------------------------------------------------------

    def reset(
        self,
        *,
        seed: Optional[int] = None,
        options: Optional[Dict] = None,
    ) -> Tuple[np.ndarray, Dict]:
        super().reset(seed=seed)
        self._last_er   = None
        self._done      = False
        self._step_count = 0

        depth = self._wait_for_depth()
        if depth is None:
            obs = np.zeros(self.observation_space.shape, dtype=np.float32)
            return obs, {"timeout": True}
        obs = self.observe(depth, None)
        return np.asarray(obs, dtype=np.float32), {}

    def step(self, action: Any) -> Tuple[np.ndarray, float, bool, bool, Dict]:
        if self._last_depth is not None:
            self.act(action, self._last_depth)

        depth = self._wait_for_depth()
        if depth is None:
            obs = np.zeros(self.observation_space.shape, dtype=np.float32)
            return obs, 0.0, True, False, {"timeout": True}

        rew       = self.reward(depth, self._last_er)
        terminated = self.is_done(depth, self._last_er)
        self._step_count += 1
        obs = self.observe(depth, self._last_er)
        return np.asarray(obs, dtype=np.float32), rew, terminated, False, {}

    def close(self) -> None:
        self._transport.close()
        super().close()

    # -----------------------------------------------------------------------
    # Order helpers (delegate to transport)
    # -----------------------------------------------------------------------

    def send_order(self, cmd: OrderRequestCmd) -> None:
        self._transport.send(cmd.to_json().encode("utf-8"))

    def send_quote(self, cmd: QuoteRequestCmd) -> None:
        self._transport.send(cmd.to_json().encode("utf-8"))

    def request_info(self, info: str) -> None:
        self._transport.send(RequestInfoCmd(info).to_json().encode("utf-8"))

    # -----------------------------------------------------------------------
    # Internal helpers
    # -----------------------------------------------------------------------

    def _wait_for_depth(self, timeout_ms: Optional[int] = None) -> Optional[DepthMsg]:
        """
        Poll until a depth message arrives or timeout expires.
        Side-effect: updates self._last_depth and self._last_er.
        """
        waited = 0
        limit  = timeout_ms or self._max_wait_ms
        poll   = min(200, limit)

        while waited < limit:
            raw = self._transport.recv(poll)
            waited += poll
            if raw is None:
                continue
            try:
                env = Envelope.parse(raw)
            except Exception as e:
                log.warning("parse error: %s", e)
                continue

            if env.type == _TYPE_DEPTH:
                self._last_depth = env.as_depth()
                return self._last_depth
            elif env.type == _TYPE_TRADE:
                pass  # discard; depth is the step trigger
            elif env.type == _TYPE_ER:
                self._last_er = env.as_execution_report()

        log.warning("_wait_for_depth timed out after %d ms", limit)
        return None

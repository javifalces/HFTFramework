# Reinforcement Learning

These algorithms are designed to emulate a Reinforcement Learning (RL) environment in Java, which is capable of
receiving actions from Python and returning the subsequent state and reward via the ZeroMQ Request/Response protocol. In
Java, these algorithms extend the abstract.
class [SingleInstrumentRLAlgorithm](../java/algorithmic_trading_framework/src/main/java/com/lambda/investing/algorithmic_trading/reinforcement_learning/SingleInstrumentRLAlgorithm.java).
In Python, the environment algorithms
extend [RlAlgorithm](../python/trading_algorithms/reinforcement_learning/rl_algorithm.py).

The system integrates Python and Java to execute a backtest with an RL algorithm. The Python AI gym, based on stable
baselines 3, is open to integration with other frameworks. The Java backtest connects each step with a ZeroMQ
request-response protocol.

![BacktestRlGym](../fig/GymCommunication.jpg?raw=true "BacktestRlGym")

The algorithm is trained from Python and initiates the backtest with an **rlHost** and **rlPort** property.
If a SingleInstrumentRLAlgorithm is launched and the host and port are configured, an OrdinaryBacktestRLGym is
initiated. This will include a ZeroMqServer that responds to the Python gym with the subsequent state and reward.

**Please note that it is not possible to initiate a backtest or live trading solely with Java when using a Reinforcement
Learning (RL) algorithm. The Java launchers that have been defined are intended solely for testing purposes, 
utilizing a dummyAgent**


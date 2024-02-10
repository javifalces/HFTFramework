# Alpha Avellaneda Stoikov

The focus of my research is the implementation of the Avellaneda Stoikov market making model, which is managed by
[Reinforcement Learning (RL)](reinforcement_learning.md). The RL is designed to dynamically configure the various
parameters of the Avellaneda
Stoikov model, taking into account both market and private states.

* [Java](../java/trading_algorithms/src/main/java/com/lambda/investing/algorithmic_trading/market_making/avellaneda_stoikov/AlphaAvellanedaStoikov.java)
* [Python](../python/trading_algorithms/market_making/alpha_avellaneda_stoikov.py)

![Alpha AS](../fig/AlphaAS_functional.jpg?raw=true "Alpha AS")

The Avellaneda Stoikov market making model, when used in conjunction with Reinforcement Learning (RL), offers several
advantages:

1. Dynamic Parameter Configuration: RL can dynamically adjust the parameters of the Avellaneda Stoikov model based on
   the current market and private states. This allows the model to adapt to changing market conditions, potentially
   improving its performance.
2. Risk Management: The Avellaneda Stoikov model is designed to minimize inventory risk, which is a significant concern
   in market making. By using RL to adjust the model's parameters, it's possible to further optimize this risk
   management.
3. Learning from Experience: RL algorithms learn from their past actions and the resulting rewards or penalties. This
   means that over time, the RL-enhanced Avellaneda Stoikov model can improve its performance by learning from its past
   trades.
5. Potential for Improved Performance: The combination of the Avellaneda Stoikov model with RL has the potential to
   outperform traditional market making strategies, as demonstrated in academic research.
6. Flexibility: The RL can be trained with different types of algorithms (like DQN, PPO, etc.), providing flexibility in
   choosing the most suitable one for the specific market conditions and trading objectives.

### Avellaneda Stoikov implementation

* [Java](../java/trading_algorithms/src/main/java/com/lambda/investing/algorithmic_trading/market_making/avellaneda_stoikov/AvellanedaStoikov.java)
* [Python](../python/trading_algorithms/market_making/avellaneda_stoikov.py)
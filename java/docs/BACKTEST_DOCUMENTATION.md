# Backtest System Documentation

## Overview

The Backtest system is a comprehensive framework for simulating trading algorithms using historical market data. It
supports traditional algorithmic trading strategies as well as reinforcement learning approaches, with powerful
configuration options, performance metrics, and visualization capabilities.

## Configuration Structure

### JSON Configuration Format

Backtest configurations are defined in JSON files with two main sections:

- `backtest`: Defines simulation parameters
- `algorithm` (legacy): Defines one algorithm and its parameters
- `algorithms` (new): Defines multiple algorithms to run simultaneously

Example:

```json
{
  "backtest": {
    "startDate": "20250407 9:00:00",
    "endDate": "20250407 12:00:00",
    "instrument": "btceur_kraken",
    "delayOrderMs": 0,
    "feesCommissionsIncluded": false,
    "multithreadConfiguration": "single_thread"
  },
  "algorithm": {
    "algorithmName": "AvellanedaStoikov_test",
    "parameters": {
      "riskAversion": 0.00006,
      "quantity": 0.001,
      "firstHour": 7.0,
      "lastHour": 19.0
      // other algorithm-specific parameters
    }
  }
}
```

Multiple algorithms example:

```json
{
  "backtest": {
    "startDate": "20250407 9:00:00",
    "endDate": "20250407 12:00:00",
    "delayOrderMs": 0,
    "feesCommissionsIncluded": false,
    "multithreadConfiguration": "single_thread"
  },
  "algorithms": [
    {
      "algorithmName": "AvellanedaStoikov_test",
      "parameters": {
        "instrumentPks": ["btceur_kraken"],
        "quantity": 0.001
      }
    },
    {
      "algorithmName": "ConstantSpread_test",
      "parameters": {
        "instrumentPks": ["btceur_kraken"],
        "quantity": 0.001
      }
    }
  ]
}
```

### Backtest Parameters

- `startDate`/`endDate`: Time range for simulation (format: "YYYYMMDD HH:MM:SS")
- `instrument`: Trading instrument identifier (e.g., "btceur_kraken")
- `delayOrderMs`: Simulated order processing delay in milliseconds
- `feesCommissionsIncluded`: Whether to include trading fees in P&L calculations
- `multithreadConfiguration`: Threading model ("single_thread" or "multi_thread")
- `bucleRun`: Run in loop mode (for continuous backtesting)

### Algorithm Parameters

- `algorithmName`: Name of algorithm to instantiate
- `parameters`: Map of algorithm-specific parameters
    - Common parameters include:
        - `quantity`: Trading size
        - `firstHour`/`lastHour`: Operating hours (UTC)
        - `ui`: Enable visualization (1=on, 0=off)

## Execution Flow

### Initialization

1. `App.java` loads the JSON configuration
2. Configuration is parsed into `InputConfiguration` objects
3. `BacktestConfiguration` is created with algorithm and market data settings
4. The backtest engine is initialized with the configuration

### Data Flow

1. Historical market data is loaded from parquet files (from `DATA_PATH`)
2. Data is fed to the algorithm in chronological order
3. Algorithm processes market events and generates orders
4. Simulated execution is applied and execution reports returned
5. P&L and positions are tracked throughout the simulation

### Termination

1. When `endDate` is reached, backtest is marked as complete
2. Summary statistics are calculated and displayed
3. Trade data and performance metrics are saved to output files
4. Optional visualization of results is presented

## Reinforcement Learning Integration

### Configuration

RL algorithms require additional parameters:

- `dummyAgent`: Enable dummy agent mode for testing (1=on, 0=off)
- `reinforcementLearningActionType`: "discrete" or "continuous"
- `rlHost`/`rlPort`: ZeroMQ connection parameters
- `stateColumnsFilter`: Features to include in state representation
- `actionColumns`: Number of action dimensions

### Communication

The backtest communicates with RL agents through ZeroMQ:

1. Agent receives states from the environment
2. Agent sends actions to the environment
3. Environment returns rewards and next states
4. Process continues until terminal state is reached

### Dummy Agent

For testing, the framework includes a `DummyRlAgent` that:

- Connects to the backtest via ZeroMQ
- Generates random actions
- Receives state/reward data
- Simulates an RL agent without actual learning

## Output and Analysis

### Runtime Output

- Trade execution details
- Position updates
- P&L snapshots
- Market data statistics

### Saved Files

- Trade tables: CSV files with all executed trades
- P&L snapshots: Performance at different time points
- Summary statistics: Overall performance metrics

### Visualization

When enabled (`ui: 1`):

- Price charts with trade markers
- P&L evolution over time
- Position changes
- Custom metrics visualization

## Common Operations

### Running a Backtest

```bash
java -jar backtest.jar path/to/config.json
```

### Creating a New Algorithm

1. Extend the `Algorithm` class
2. Implement required methods like `onDepthUpdate`, `onTradeUpdate`
3. Define algorithm parameters
4. Create configuration JSON with algorithm name and parameters
5. Add it to TradingAlgorithmsProvider.java

### Analyzing Results

Results are saved to the `OUTPUT_PATH` directory:

- `trades_table_{algorithmName}_{instrument}.csv`: All trades
- Summary statistics are printed to console and logs

## Reinforcement Learning Workflow

### Training Process

1. Configure backtest with RL algorithm and parameters
2. Connect RL agent to backtest through ZeroMQ
3. Run backtest with `dummyAgent: 0`
4. Agent receives states and rewards, learns policy
5. Save trained model

### Evaluation Process

1. Load trained model
2. Configure backtest with same parameters
3. Run backtest with loaded model
4. Analyze performance metrics

## Important Implementation Notes

- The backtest uses a simulated clock that advances with market data timestamps
- Position tracking is automatic but can be overridden
- Market data is immutable once loaded
- Order execution is simulated based on available liquidity
- Fees and commissions are optional and configurable
- Trading hours can be restricted using `firstHour` and `lastHour`

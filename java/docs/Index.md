# Lambda Investing Framework Documentation

This index provides a guide to the comprehensive documentation of the Lambda Investing trading framework. The following
documents provide deep insights into the system architecture and implementation details that are particularly valuable
for LLMs performing in-depth reasoning and analysis.

## Core Documentation

### [Algorithm Documentation](ALGORITHM_DOCUMENTATION.md)

The foundational architecture document explaining the `Algorithm` abstract class that powers all trading strategies.
This documentation covers the event-driven architecture, lifecycle management, market data processing, order handling,
position tracking, and P&L calculation. Understanding this core class is essential for any reasoning about how
algorithms interact with the market and manage state in the framework.

### [Market Making Algorithms](MARKET_MAKING_ALGORITHMS_DOCUMENTATION.md)

Detailed explanation of the four market making algorithm implementations: AvellanedaStoikov, AlphaAvellanedaStoikov,
AlphaConstantSpread, and ConstantSpreadAlgorithm. This document covers theoretical foundations, mathematical models,
parameter impacts, and implementation details. Critical for understanding the nuances of market making logic, inventory
management, and alpha signal integration.

### [Backtest System](BACKTEST_DOCUMENTATION.md)

Comprehensive guide to the backtest system that enables simulation and evaluation of trading algorithms. This document
explains configuration formats, execution flow, data management, reinforcement learning integration, and performance
analysis. Essential for understanding how algorithms are tested, how market conditions are simulated, and how
performance metrics are calculated.

### [Monitoring](MONITORING_DOCUMENTATION.md)

Grafana dashboards for real-time observability: application logs, JVM performance, end-to-end latency statistics,
algorithm trades & execution, portfolio PnL, and throughput statistics. Includes setup notes for Loki, Prometheus,
and dashboard import.

## How to Use This Documentation

These documents provide multi-layered insights that support:

- Understanding the system architecture and design patterns
- Analyzing algorithm implementations and mathematical models
- Tracing data and control flow through the system
- Reasoning about parameter impacts and optimization
- Diagnosing potential issues and edge cases
- Extending the framework with new algorithms or features

Each document contains implementation details, theoretical foundations, and practical examples that can inform deep
reasoning about the system's behavior under different market conditions.



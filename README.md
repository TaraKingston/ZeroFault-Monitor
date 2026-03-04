# ZeroFault Monitor

A lightweight, real-time system monitoring agent written in Java that collects CPU, RAM, network, and disk metrics using the OSHI library. It provides alerting for CPU spikes and memory leaks, an HTTP API for metrics access, and optional CSV export.

## Features

- **Real-time Metrics Collection**: Monitors CPU usage, RAM usage, network throughput (download/upload KB/s), and disk usage.
- **Moving Average Calculations**: Maintains 60-second rolling averages for CPU and RAM.
- **Intelligent Alerts**:
  - CPU spike detection: Alerts when CPU exceeds average by a configurable delta for a sustained period.
  - Memory leak detection: Alerts when RAM rises by a configurable threshold over the 60-second window.
- **HTTP API Server**: Exposes metrics via REST endpoints, including Prometheus-compatible format.
- **CSV Export**: Optionally exports metrics to a CSV file for historical analysis.
- **Top Processes**: Tracks top CPU and memory-consuming processes.
- **Configurable**: All settings via `monitor.properties` file.
- **Console Mode**: Simple console output for quick monitoring.

## Requirements

- Java 17 or higher (tested with Java 25)
- Maven 3.6+ (for building)

## Building

Clone the repository and build with Maven:

```bash
git clone <repository-url>
cd ZeroFault-Monitor
mvn clean compile
```

## Running

### Agent Mode (Full Features)

Run the main agent with API server and CSV export:

```bash
mvn exec:java -Dexec.mainClass="com.zerofault.monitor.AgentMain"
```

This starts:
- Metrics collection every 1 second
- HTTP API server on port 8080
- CSV export to `metrics.csv` (if enabled)

### Console Mode (Simple)

For basic console monitoring without API:

```bash
mvn exec:java -Dexec.mainClass="com.zerofault.monitor.Main"
```

## Configuration

Edit `src/main/resources/monitor.properties`:

```properties
# Sampling interval in milliseconds
sample.interval.ms=1000

# Number of samples to keep in memory (600 = 10 minutes at 1s intervals)
store.capacity=600

# CPU Alert: Trigger if current exceeds average by 30.0%
cpu.spike.delta=30.0
# CPU Alert: Spike must last 3 seconds
cpu.spike.seconds=3

# RAM Alert: Trigger if RAM increases 5.0% over 60 seconds
ram.rise.threshold=5.0

# Alert cooldown: Wait 10 seconds between same alerts
alert.cooldown.ms=10000

# HTTP API port
api.port=8080

# CSV Export settings
export.csv.enabled=true
export.csv.path=metrics.csv
export.csv.flushEvery=5
```

## API Endpoints

- `GET /metrics/latest`: Latest metrics snapshot
- `GET /metrics/history?seconds=60`: Historical metrics for the last N seconds
- `GET /alerts`: List of recent alerts
- `GET /metrics/prometheus`: Metrics in Prometheus format
- `GET /processes/top`: Top CPU and memory processes

Example:

```bash
curl http://localhost:8080/metrics/latest
```

## CSV Export

When enabled, metrics are appended to `metrics.csv` with columns:
- timestamp
- cpuNow
- cpuAvg60
- ramNow
- ramAvg60
- downKBps
- upKBps

## Alerts

Alerts are printed to console and stored in memory. Access via API or check console output.

## Architecture

- `AgentMain`: Main entry point, starts all services
- `MetricsCollector`: Uses OSHI to gather system metrics
- `MetricsStore`: In-memory storage for metrics and alerts
- `SamplerService`: Background thread for periodic sampling and alerting
- `ApiServer`: HTTP server for API endpoints
- `MetricsCSV`: Handles CSV export
- `MonitorConfig`: Loads configuration from properties file

## Dependencies

- [OSHI](https://github.com/oshi/oshi): Cross-platform system information library

## License

[Add your license here]

## Contributing

[Add contribution guidelines]</content>
<parameter name="filePath">C:\Users\tarak\IdeaProjects\ZeroFault-Monitor\README.md

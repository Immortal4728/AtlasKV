# AtlasKV CLI

Official command-line interface for the [AtlasKV](https://github.com/rishikesh-suvarna/atlaskv) distributed key-value store.

Built on [Picocli](https://picocli.info/) and the AtlasKV Java SDK.

## Installation

### Build from Source

```bash
cd atlaskv-cli
mvn clean package -DskipTests
```

The executable fat JAR is produced at:

```
atlaskv-cli/target/atlaskv-cli-0.1.0-SNAPSHOT.jar
```

### Run

```bash
java -jar atlaskv-cli/target/atlaskv-cli-0.1.0-SNAPSHOT.jar --help
```

### Alias (optional)

```bash
# Bash / Zsh
alias atlaskv="java -jar /path/to/atlaskv-cli-0.1.0-SNAPSHOT.jar"

# PowerShell
Set-Alias atlaskv "java -jar D:\path\to\atlaskv-cli-0.1.0-SNAPSHOT.jar"
```

## Configuration

The CLI reads configuration from `~/.atlaskv/config.yml`:

```yaml
host: localhost
port: 8080
timeout: 5

# Authentication (optional)
authentication:
  type: bearer
  token: your-token-here

# Or use basic auth:
# authentication:
#   type: basic
#   username: admin
#   password: secret
```

### Config Commands

```bash
atlaskv config init     # Create default config file
atlaskv config show     # Display current configuration
atlaskv config path     # Show config file location
```

## CLI Reference

### Key-Value Operations

```bash
# Store a key-value pair
atlaskv put <key> <value>

# Retrieve a value
atlaskv get <key>

# Delete a key
atlaskv delete <key>

# Check if a key exists
atlaskv exists <key>

# Compare-and-swap update
atlaskv cas <key> <value> <expected-version>

# Prefix scan with pagination
atlaskv prefix <prefix> [--offset 0] [--limit 100]
```

### History & Rollback

```bash
# Show full revision history
atlaskv history <key>

# Show a specific revision
atlaskv history <key> <revision>

# Rollback to a specific revision
atlaskv rollback <key> <revision>
```

### Lease Management

```bash
# Create a lease
atlaskv lease create --ttl 30s [--id custom-id]

# Renew a lease
atlaskv lease renew <lease-id>

# Revoke a lease
atlaskv lease revoke <lease-id>

# List all active leases
atlaskv lease list
```

### Watch (Live Events)

```bash
# Watch a single key
atlaskv watch <key>

# Watch all keys with a prefix
atlaskv watch <prefix> --prefix
```

Press `Ctrl+C` to stop watching.

### Cluster Operations

```bash
# Show cluster node status
atlaskv cluster status

# Show current leader
atlaskv cluster leader

# List cluster members
atlaskv cluster members
```

### Metrics

```bash
# Show performance metrics
atlaskv metrics
```

### Global Options

All commands support:

| Flag | Description |
|------|-------------|
| `-H, --host` | Override server host |
| `-p, --port` | Override server port |
| `-h, --help` | Show help for any command |
| `-V, --version` | Show version |

## Examples

### Basic CRUD Session

```bash
$ atlaskv put user/alice "Alice Smith"
✓ Key stored successfully
  Key:            user/alice
  Value:          Alice Smith
  Version:        1
  Created:        2026-07-19 12:00:00
  Updated:        2026-07-19 12:00:00

$ atlaskv get user/alice
─── Key-Value ───
  Key:            user/alice
  Value:          Alice Smith
  Version:        1
  Created:        2026-07-19 12:00:00
  Updated:        2026-07-19 12:00:00

$ atlaskv exists user/alice
✓ Key exists: user/alice

$ atlaskv delete user/alice
✓ Deleted key: user/alice
```

### CAS (Optimistic Locking)

```bash
$ atlaskv cas config/db-url "postgres://new-host:5432" 1
✓ CAS update succeeded
  Key:            config/db-url
  Value:          postgres://new-host:5432
  Version:        2
  Updated:        2026-07-19 12:01:00

$ atlaskv cas config/db-url "wrong" 1
✗ Error: CAS conflict: Version mismatch
  Expected:       1
  Current:        2
```

### Watch Session

```bash
$ atlaskv watch sensor/temp --prefix
✓ Watching prefix: sensor/temp
ℹ Press Ctrl+C to stop
[ PUT ] sensor/temp/1 = 22.5
[ PUT ] sensor/temp/2 = 23.1
[ DEL ] sensor/temp/1 = (deleted)
^C
ℹ Watch stopped
```

### Cluster Status

```bash
$ atlaskv cluster status
─── Cluster Status ───
  Node ID:        node-1
  Role:           LEADER
  Term:           5
  Commit Index:   142
  Last Applied:   142
  Leader:         node-1
  Healthy:        true
  Uptime:         2h 15m
  State:          STARTED
  gRPC Port:      9090
  Peer Count:     2
```

### Remote Connection

```bash
atlaskv -H 10.0.0.5 -p 9090 get mykey
```

## Architecture

```
atlaskv-cli/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/atlaskv/cli/
    │   │   ├── AtlasKVCli.java          # Entry point
    │   │   ├── CliConfig.java           # YAML config loader
    │   │   ├── ClientFactory.java       # SDK client factory
    │   │   ├── OutputFormatter.java     # Colored output + tables
    │   │   └── commands/
    │   │       ├── ConnectionMixin.java # Shared --host/--port
    │   │       ├── PutCommand.java
    │   │       ├── GetCommand.java
    │   │       ├── DeleteCommand.java
    │   │       ├── ExistsCommand.java
    │   │       ├── CasCommand.java
    │   │       ├── PrefixCommand.java
    │   │       ├── HistoryCommand.java
    │   │       ├── RollbackCommand.java
    │   │       ├── LeaseCommand.java
    │   │       ├── WatchCommand.java
    │   │       ├── ClusterCommand.java
    │   │       ├── MetricsCommand.java
    │   │       └── ConfigCommand.java
    │   └── resources/
    │       └── logback.xml
    └── test/
        └── java/com/atlaskv/cli/
            ├── AtlasKVCliTest.java
            ├── CliConfigTest.java
            └── OutputFormatterTest.java
```

## Tech Stack

- **Java 21**
- **Picocli 4.7.6** — CLI framework with auto-help, auto-completion
- **AtlasKV Java SDK** — HTTP client with retry + leader redirection
- **SnakeYAML 2.3** — Config file parsing
- **JUnit 5 + AssertJ + Mockito** — Testing
- **Maven Shade Plugin** — Executable fat JAR

## License

MIT — see [LICENSE](../LICENSE).

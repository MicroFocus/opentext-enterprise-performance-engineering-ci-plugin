# LRE Actions - Usage Guide

## Overview

The LRE Actions application provides a unified interface to interact with LoadRunner Enterprise (LRE). It supports two main actions:

1. **ExecuteLreTest** - Execute load tests on LRE
2. **WorkspaceSync** - Synchronize local workspace scripts to LRE

## Building the Application

```bash
mvn clean package
```

This creates:
- `target/lre-actions-1.1-SNAPSHOT.jar` - Regular JAR
- `target/lre-actions-1.1-SNAPSHOT-jar-with-dependencies.jar` - Fat JAR with all dependencies

## Running the Application

### Using Configuration File

```bash
java -jar target/lre-actions-1.1-SNAPSHOT-jar-with-dependencies.jar config.json
```

### Using Environment Variables (Harness CI/CD)

Set environment variables with `PLUGIN_` prefix:
```bash
export PLUGIN_LRE_ACTION=WorkspaceSync
export PLUGIN_LRE_SERVER=lre.example.com
export PLUGIN_LRE_TENANT=mytenant
export PLUGIN_LRE_USERNAME=myuser
export PLUGIN_LRE_PASSWORD=mypassword
# ... other variables

java -jar target/lre-actions-1.1-SNAPSHOT-jar-with-dependencies.jar
```

## Configuration Examples

### 1. Workspace Sync Configuration

Create a file named `workspace-sync-config.json`:

```json
{
  "lre_action": "WorkspaceSync",
  "lre_server": "lre.example.com",
  "lre_port": "443",
  "lre_tenant": "mytenant",
  "lre_https_protocol": true,
  "lre_domain": "DEFAULT",
  "lre_project": "MyProject",
  "lre_proxy_out_url": "",
  "workspace_path": "C:/workspace/scripts",
  "lre_runtime_only": false
}
```

**Required Credentials (from environment):**
```bash
export PLUGIN_LRE_USERNAME=your_username
export PLUGIN_LRE_PASSWORD=your_password
```

**Run:**
```bash
java -jar target/lre-actions-1.1-SNAPSHOT-jar-with-dependencies.jar workspace-sync-config.json
```

**What it does:**
- Scans `workspace_path` for script folders (containing .usr, .jmx, .scala, .java, or main.js+rts.yml)
- Compresses each script folder to a ZIP file
- Uploads ZIP files to LRE maintaining the folder structure
- Creates log file at: `<workspace_path>/logs/lre_workspace_sync_<timestamp>.log`

---

### 2. Test Execution Configuration

Create a file named `run-test-config.json`:

```json
{
  "lre_action": "ExecuteLreTest",
  "lre_description": "Automated test run from CI/CD",
  "lre_server": "lre.example.com",
  "lre_https_protocol": true,
  "lre_authenticate_with_token": false,
  "lre_domain": "DEFAULT",
  "lre_project": "MyProject",
  "lre_test": "123",
  "lre_test_instance": "AUTO",
  "lre_timeslot_duration_hours": "1",
  "lre_timeslot_duration_minutes": "30",
  "lre_post_run_action": "Collate and Analyze",
  "lre_vuds_mode": false,
  "lre_trend_report": "",
  "lre_proxy_out_url": "",
  "lre_search_timeslot": false,
  "lre_status_by_sla": false,
  "lre_workspace_dir": "C:/workspace",
  "lre_retry": "3",
  "lre_retry_delay": "5",
  "lre_retry_occurrences": "3",
  "lre_trend_report_wait_time": "30",
  "lre_enable_stacktrace": false
}
```

**Required Credentials (from environment):**
```bash
export PLUGIN_LRE_USERNAME=your_username
export PLUGIN_LRE_PASSWORD=your_password
```

**Run:**
```bash
java -jar target/lre-actions-1.1-SNAPSHOT-jar-with-dependencies.jar run-test-config.json
```

**What it does:**
- Executes test ID `123` on LRE
- Waits for timeslot (1 hour 30 minutes)
- Monitors test execution
- Collates and analyzes results
- Downloads artifacts to workspace
- Creates log file at: `<workspace_dir>/artifacts/lre_run_test_<timestamp>.log`

---

## Configuration Parameters

### Common Parameters (Both Actions)

| Parameter | Required | Default | Description |
|-----------|----------|---------|-------------|
| `lre_action` | Yes | "ExecuteLreTest" | Action type: "ExecuteLreTest" or "WorkspaceSync" |
| `lre_server` | Yes | - | LRE server hostname or IP |
| `lre_https_protocol` | No | false | Use HTTPS (true) or HTTP (false) |
| `lre_domain` | Yes | - | LRE domain name |
| `lre_project` | Yes | - | LRE project name |
| `lre_proxy_out_url` | No | "" | Outbound proxy URL |

### WorkspaceSync Specific

| Parameter | Required | Default | Description |
|-----------|----------|---------|-------------|
| `lre_port` | No | "" | LRE server port (if not default) |
| `lre_tenant` | Yes | - | LRE tenant name |
| `workspace_path` | Yes | - | Local workspace path to scan |
| `lre_runtime_only` | No | false | Upload runtime-only scripts |

### ExecuteLreTest Specific

| Parameter | Required | Default | Description |
|-----------|----------|---------|-------------|
| `lre_test` | Yes | - | Test ID (number) or YAML file path |
| `lre_test_instance` | No | "AUTO" | Test instance ID or "AUTO" |
| `lre_timeslot_duration_hours` | No | "0" | Timeslot duration (hours) |
| `lre_timeslot_duration_minutes` | No | "30" | Timeslot duration (minutes) |
| `lre_post_run_action` | Yes | "Collate and Analyze" | Action after test: "Collate and Analyze", "Collate Results", or "Do Nothing" |
| `lre_vuds_mode` | No | false | Use VUD mode |
| `lre_search_timeslot` | No | false | Search for available timeslot |
| `lre_status_by_sla` | No | false | Determine status by SLA |
| `lre_retry` | No | "1" | Number of retry attempts |
| `lre_retry_delay` | No | "1" | Delay between retries (minutes) |
| `lre_enable_stacktrace` | No | false | Enable detailed stack traces in logs |
| `lre_workspace_dir` | Yes | - | Working directory for artifacts |

### Environment Variables (Credentials)

These should be set as environment variables for security:

| Environment Variable | Config Parameter | Description |
|---------------------|------------------|-------------|
| `PLUGIN_LRE_USERNAME` | `lre_username` | LRE username |
| `PLUGIN_LRE_PASSWORD` | `lre_password` | LRE password |
| `PLUGIN_LRE_USERNAME_PROXY` | `lre_username_proxy` | Proxy username (if needed) |
| `PLUGIN_LRE_PASSWORD_PROXY` | `lre_password_proxy` | Proxy password (if needed) |

## Exit Codes

- `0` - Success
- `1` - Failure (validation error, execution error, or test failed)
- `2` - Invalid usage

## Logging

### WorkspaceSync
- **Location:** `<workspace_path>/logs/lre_workspace_sync_<timestamp>.log`
- **Console:** Yes
- **Stack traces:** Always enabled

### ExecuteLreTest
- **Location:** `<workspace_dir>/artifacts/lre_run_test_<timestamp>.log`
- **Console:** Yes
- **Stack traces:** Controlled by `lre_enable_stacktrace`

## Troubleshooting

### "Another instance is already running"
Only one instance can run at a time (port 57396). Wait for the previous instance to finish.

### "Missing required config value: <key>"
Ensure all required parameters are provided in the config file or as environment variables.

### Authentication Failures
- Verify credentials are correctly set in environment variables
- Check server URL and HTTPS protocol settings
- Verify proxy settings if behind a corporate firewall

### Script Upload Failures (WorkspaceSync)
- Ensure workspace_path is accessible
- Verify LRE credentials have permissions to upload scripts
- Check network connectivity and proxy settings
- Review log file for detailed error messages

### Test Execution Failures (ExecuteLreTest)
- Verify test ID exists in the project
- Check timeslot availability
- Ensure workspace directory is writable
- Review log file for detailed error messages

## CI/CD Integration

### Harness CI/CD Example

```yaml
- step:
    type: Plugin
    name: LRE Workspace Sync
    identifier: lre_sync
    spec:
      connectorRef: <+input>
      image: openjdk:17
      settings:
        lre_action: WorkspaceSync
        lre_server: lre.example.com:443/LoadTest/mytenant
        lre_https_protocol: "true"
        lre_authenticate_with_token: "false"
        lre_domain: DEFAULT
        lre_project: MyProject
        lre_runtime_only: "false"
        lre_username: <+secrets.getValue("lre_username")>
        lre_password: <+secrets.getValue("lre_password")>
```

## Developer Notes

- Main entry point: `com.opentext.lre.actions.Main`
- Action routing is determined by `lre_action` parameter
- Both actions share common authentication and REST client infrastructure
- See `ARCHITECTURE.md` for detailed architecture documentation


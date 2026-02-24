# Architecture Overview

## Main Entry Point: `Main.java`

The `Main` class serves as the **unified entry point** for the LRE Actions application. It supports two distinct operational modes:

1. **ExecuteLreTest** - Executes load tests on LoadRunner Enterprise (LRE)
2. **WorkspaceSync** - Synchronizes local workspace scripts to LRE

## How It Works

### 1. Configuration Loading (`InputRetriever`)

The `InputRetriever` class is responsible for reading configuration from:
- **JSON configuration file** (passed as command-line argument)
- **Environment variables** (with `PLUGIN_` prefix for Harness CI/CD)

It provides methods to retrieve:
- `getLreAction()` - Returns the action type ("ExecuteLreTest" or "WorkspaceSync")
- `getLreTestRunModel()` - Returns configuration for test execution (or null)
- `getLreWorkspaceSyncModel()` - Returns configuration for workspace sync (or null)

### 2. Action Routing (`Main.main()`)

The main flow:
```
1. Check for running instance (single instance lock)
2. Initialize environment variables via InputRetriever
3. Determine action type from config
4. Route to appropriate executor
5. Exit with appropriate code
```

### 3. Execution Paths

#### ExecuteLreTest Path
```
Main.executeTestRun()
  ├── Setup logging (lre_run_test_<timestamp>.log)
  ├── Initialize Log4j2 with stack trace control
  ├── Create LreTestRunBuilder
  └── Execute test run
```

**Key Components:**
- `LreTestRunModel` - Configuration model for test execution
- `LreTestRunBuilder` - Orchestrates the test execution
- `LreTestRunClient` - Communicates with LRE REST API

#### WorkspaceSync Path
```
Main.executeWorkspaceSync()
  ├── Setup logging (lre_workspace_sync_<timestamp>.log)
  ├── Initialize Log4j2 with stack trace enabled
  ├── Create LreWorkspaceSyncTask
  └── Execute synchronization
```

**Key Components:**
- `LreWorkspaceSyncModel` - Configuration model for workspace sync
- `LreWorkspaceSyncTask` - Orchestrates the synchronization
- `WorkspaceScriptFolderScanner` - Scans for script folders
- `ZipFolderCompressor` - Compresses script folders
- REST client for uploading scripts to LRE

## Configuration Format

### For ExecuteLreTest
```json
{
  "lre_action": "ExecuteLreTest",
  "lre_server": "lre.example.com",
  "lre_https_protocol": true,
  "lre_domain": "DEFAULT",
  "lre_project": "MyProject",
  "lre_test": "123",
  "lre_timeslot_duration_hours": "1",
  "lre_timeslot_duration_minutes": "30",
  "lre_post_run_action": "Collate and Analyze",
  "lre_workspace_dir": "C:/workspace"
}
```

### For WorkspaceSync
```json
{
  "lre_action": "WorkspaceSync",
  "lre_server": "lre.example.com:443/LoadTest/mytenant",
  "lre_https_protocol": true,
  "lre_authenticate_with_token": false,
  "lre_domain": "DEFAULT",
  "lre_project": "MyProject",
  "lre_workspace_path": "C:/workspace/scripts",
  "lre_runtime_only": false
}
```

## Security Considerations

Sensitive parameters (credentials) are retrieved from environment variables:
- `PLUGIN_LRE_USERNAME` or `lre_username` (from config as fallback)
- `PLUGIN_LRE_PASSWORD` or `lre_password` (from config as fallback)
- `PLUGIN_LRE_USERNAME_PROXY`
- `PLUGIN_LRE_PASSWORD_PROXY`

## Logging

Both execution paths initialize logging independently:
- **Log file location:**
  - ExecuteLreTest: `<workspace>/artifacts/lre_run_test_<timestamp>.log`
  - WorkspaceSync: `<workspace>/logs/lre_workspace_sync_<timestamp>.log`
- **Log format:** Timestamped with console and file output
- **Stack traces:** Controlled by configuration (test run) or always enabled (workspace sync)

## Exit Codes

- `0` - Success
- `1` - Failure (execution error, validation error, or build failure)
- `2` - Invalid usage (for standalone runners)

## Design Benefits

1. **Single Entry Point** - Simplifies CI/CD integration
2. **Unified Configuration** - Consistent parameter naming and retrieval
3. **Action Routing** - Easy to add new actions in the future
4. **Separation of Concerns** - Each action has its own model, runner, and logic
5. **Environment Flexibility** - Works with both JSON config files and environment variables


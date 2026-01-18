![OpenText Logo](https://upload.wikimedia.org/wikipedia/commons/1/1b/OpenText_logo.svg)

# OpenText Enterprise Performance Engineering – CI/CD Plugin

## Overview

This repository is used to build Docker image providing a **CI/CD-friendly plugin** for triggering, monitoring, and collecting results from **OpenText Enterprise Performance Engineering (formerly LoadRunner Enterprise)**.

The docker image is designed to run **non-interactively** inside CI/CD pipelines (Harness, GitHub Actions, GitLab CI, Jenkins, Kubernetes jobs, etc.) using **environment variables for configuration and outputs**.

---

## What This Plugin Does

At runtime, the container:

1. Authenticates with an OpenText Enterprise Performance Engineering server
2. Triggers a performance test execution
3. Allocates or reuses a timeslot (based on configuration)
4. Monitors test execution until completion
5. Optionally collates and analyzes results
6. Writes logs and result artifacts to configured directories
7. Exits with a deterministic status code suitable for pipeline gating

This behavior allows the plugin to be used as a **pipeline quality gate**.

---

## Requirements

- Docker runner (local, Kubernetes, or Harness delegate)
- Network access to the OpenText Enterprise Performance Engineering server
- Valid OpenText Enterprise Performance Engineering credentials  
  (username/password or token)
- Writable workspace and output directories (mounted volumes)

---

## Inputs & Outputs (Configuration)

### Inputs (Environment Variables)

All configuration is provided **exclusively** via environment variables.

#### Required Inputs

| Variable | Description |
|--------|-------------|
| **PLUGIN_LRE_SERVER** | LRE server URL, including optional port and tenant. Default tenant is used if not specified. |
| **PLUGIN_LRE_USERNAME** | Username (unless token authentication is used) |
| **PLUGIN_LRE_PASSWORD** | Password (unless token authentication is used) |
| **PLUGIN_LRE_DOMAIN** | Domain (case-sensitive) |
| **PLUGIN_LRE_PROJECT** | Project (case-sensitive) |
| **PLUGIN_LRE_TEST** | Test ID **or** relative path to a YAML file defining a new test |

\* Either username/password **or** token authentication must be used.

---

#### Optional & Advanced Inputs

| Environment Variable | Description | Default |
|--------------------|-------------|---------|
| **PLUGIN_LRE_ACTION** | Action to execute. Currently supported: `ExecuteLreTest` | `ExecuteLreTest` |
| **PLUGIN_LRE_DESCRIPTION** | Description displayed in console logs | |
| **PLUGIN_LRE_HTTPS_PROTOCOL** | Use HTTPS (`true` / `false`) | `false` |
| **PLUGIN_LRE_AUTHENTICATE_WITH_TOKEN** | Use token authentication (required for SSO) | `false` |
| **PLUGIN_LRE_TEST_INSTANCE** | `AUTO` or specific instance ID | `AUTO` |
| **PLUGIN_LRE_TIMESLOT_DURATION_HOURS** | Timeslot duration (hours) | `0` |
| **PLUGIN_LRE_TIMESLOT_DURATION_MINUTES** | Timeslot duration (minutes) | `30` |
| **PLUGIN_LRE_POST_RUN_ACTION** | `Collate Results`, `Collate and Analyze`, `Do Not Collate` | `Do Not Collate` |
| **PLUGIN_LRE_VUDS_MODE** | Use VUDS licenses | `false` |
| **PLUGIN_LRE_TREND_REPORT** | Trend report behavior or report ID | |
| **PLUGIN_LRE_SEARCH_TIMESLOT** | Reuse matching timeslot instead of creating new | `false` |
| **PLUGIN_LRE_STATUS_BY_SLA** | Report success based on SLA | `false` |
| **PLUGIN_LRE_PROXY_OUT_URL** | Proxy URL | |
| **PLUGIN_LRE_USERNAME_PROXY** | Proxy username | |
| **PLUGIN_LRE_PASSWORD_PROXY** | Proxy password | |
| **PLUGIN_LRE_ENABLE_STACKTRACE** | Print stacktrace on errors | `false` |

---

### Outputs

| Path | Purpose |
|-----|---------|
| **PLUGIN_LRE_OUTPUT_DIR** | Result files and summarized outputs |
| **PLUGIN_LRE_WORKSPACE_DIR** | Workspace for logs, reports, and checkout |

Defaults:
- `PLUGIN_LRE_OUTPUT_DIR`: `$HARNESS_STEP_OUTPUTS_PATH` or `$HARNESS_WORKSPACE`
- `PLUGIN_LRE_WORKSPACE_DIR`: `$HARNESS_WORKSPACE`

These directories **must be writable** and are typically mounted volumes in CI environments.

---

### Expected Output
During execution, the condole logs provides the progression (waiting for run to finish, analysis report generation, ...)
After execution, the plugin writes:
- `results.xml` – summarized test results
- `LreResult/` – execution logs (lre_run_test__<date>.log)
- Optional analysis files if `PLUGIN_LRE_POST_RUN_ACTION` is `Collate and Analyze` (under LreResult/LreReports/HtmlReport)
- Optional trend report if PLUGIN_LRE_TREND_REPORT is set with existing trend report (and LreResult/LreReports/TrendReports)
Exit code `0` indicates success; any non-zero value indicates failure.

---
## Execution Model

- Non-interactive, blocking execution
- The container handles polling and status monitoring internally
- No state is preserved between runs
- Designed to be idempotent and reproducible

---

## Usage

### Run Locally with Docker

```powershell
$imageBase = "danieldanan/opentext-enterprise-performance-engineering-test"
$imageVersion = "latest"
$imageName = "${imageBase}:${imageVersion}"

docker run -it --rm `
  -v C:/temp/harness/output:/harness/output `
  -v C:/temp/harness/workspace:/harness/workspace `
  -e PLUGIN_LRE_ACTION="ExecuteLreTest" `
  -e PLUGIN_LRE_DESCRIPTION="running new test" `
  -e PLUGIN_LRE_SERVER="http://<Server>/?tenant=<tenant-id>" `
  -e PLUGIN_LRE_USERNAME="<lreusername>" `
  -e PLUGIN_LRE_PASSWORD="<lrepassword>" `
  -e PLUGIN_LRE_DOMAIN="<domain>" `
  -e PLUGIN_LRE_PROJECT="<project>" `
  -e PLUGIN_LRE_TEST="<testid>" `
  -e PLUGIN_LRE_POST_RUN_ACTION="Collate and Analyze" `
  -e PLUGIN_LRE_OUTPUT_DIR="/harness/output" `
  -e PLUGIN_LRE_WORKSPACE_DIR="/harness/workspace" `
  $imageName
```


### Harness pipeline example (this is where many READMEs fail)

Harness users **expect YAML**.
---
## Harness pipeline example

```yaml
steps:
  - step:
      name: Run LRE Test
      type: Container
      spec:
        image: danieldanan/opentext-enterprise-performance-engineering-test:latest
        shell: Sh
        envVariables:
          PLUGIN_LRE_ACTION: ExecuteLreTest
          PLUGIN_LRE_DESCRIPTION: running new test
          PLUGIN_LRE_SERVER: http://<Server>/?tenant=<tenant-id>
          PLUGIN_LRE_HTTPS_PROTOCOL: false
          PLUGIN_LRE_AUTHENTICATE_WITH_TOKEN: false
          PLUGIN_LRE_USERNAME: <+secrets.getValue("lre_username")>
          PLUGIN_LRE_PASSWORD: <+secrets.getValue("lre_password")>
          PLUGIN_LRE_DOMAIN: <domain>
          PLUGIN_LRE_PROJECT: <project>
          PLUGIN_LRE_TEST: "<testid>"
          PLUGIN_LRE_TIMESLOT_DURATION_MINUTES: "30"
          PLUGIN_LRE_POST_RUN_ACTION: Collate and Analyze
          PLUGIN_LRE_OUTPUT_DIR: /harness/output
          PLUGIN_LRE_WORKSPACE_DIR: /harness/workspace
        resources:
          limits:
            memory: 2Gi
            cpu: 1
```
---
## Configuration parameters

All configuration is provided using environment variables.

## Output

The plugin writes execution logs and result files to:

- Output directory: `$PLUGIN_LRE_OUTPUT_DIR`
- Workspace directory: `$PLUGIN_LRE_WORKSPACE_DIR`

These directories should be mapped to Harness workspace paths
to allow artifact collection and post-processing.
---
## Authentication

Supported authentication modes:
- Username / password
- Token-based authentication (required when SSO is enabled)
Enable token authentication by setting:
PLUGIN_LRE_AUTHENTICATE_WITH_TOKEN=true
---
## Troubleshooting
- Ensure network connectivity from the container to the LRE server
- Validate tenant ID, domain, and project values
- Verify credentials or token permissions
- Enable stack traces by setting:
```text
  PLUGIN_LRE_ENABLE_STACKTRACE=true
```
---
## Related plugins
This plugin uses the same core logic and configuration model as:
- GitHub Action: https://github.com/MicroFocus/lre-gh-action
- GitLab CI Plugin: https://gitlab.com/loadrunner-enterprise/lre-gitlab-action
---
## Design Goals
This image follows CI/CD plugin best practices:
- Declarative configuration
- Environment-variable contract
- Deterministic exit codes
- Platform-agnostic execution
- No CI vendor lock-in
---
## License
This project is licensed under the MIT License.
See the [LICENSE](LICENSE) file for details.
---

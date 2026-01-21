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
| **PLUGIN_LRE_SERVER** | Server, port (when not mentionned, default is 80 or 433 for secured) and tenant (when not mentionned, default is ?tenant=fa128c06-5436-413d-9cfa-9f04bb738df3). e.g.: myserver.mydomain.com:81/?tenant=fa128c06-5436-413d-9cfa-9f04bb738df3' |
| **PLUGIN_LRE_USERNAME** | Username (or ID part of token) |
| **PLUGIN_LRE_PASSWORD** | Password (or secret part of token) |
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
| **PLUGIN_LRE_HTTPS_PROTOCOL** | Use secured protocol for connecting to the server (`true` / `false`) | `false` |
| **PLUGIN_LRE_AUTHENTICATE_WITH_TOKEN** | Use token authentication (required for SSO). (`true` / `false`) | `false` |
| **PLUGIN_LRE_TEST_INSTANCE** | `AUTO` or specific instance ID | `AUTO` |
| **PLUGIN_LRE_TIMESLOT_DURATION_HOURS** | Timeslot duration (hours) | `0` |
| **PLUGIN_LRE_TIMESLOT_DURATION_MINUTES** | Timeslot duration (minutes) | `30` |
| **PLUGIN_LRE_POST_RUN_ACTION** | `Collate Results`, `Collate and Analyze`, `Do Not Collate` | `Do Not Collate` |
| **PLUGIN_LRE_VUDS_MODE** | Use VUDS licenses (`true` / `false`) | `false` |
| **PLUGIN_LRE_TREND_REPORT** |  `ASSOCIATED` - the trend report defined in the test design will be used', Valid report ID - Report ID will be used for trend, No value or not defined - no trend monitoring. | |
| **PLUGIN_LRE_SEARCH_TIMESLOT** | Experimental: Search for matching timeslot instead of creating a new timeslot (`true` / `false`) | `false` |
| **PLUGIN_LRE_STATUS_BY_SLA** | Report success based on SLA (`true` / `false`) | `false` |
| **PLUGIN_LRE_PROXY_OUT_URL** | Proxy URL | |
| **PLUGIN_LRE_USERNAME_PROXY** | Proxy username | |
| **PLUGIN_LRE_PASSWORD_PROXY** | Proxy password | |
| **PLUGIN_LRE_ENABLE_STACKTRACE** | Print stacktrace on errors (`true` / `false`) | `false` |

---

### Outputs

| Path | Purpose |
|-----|---------|
| **PLUGIN_LRE_OUTPUT_DIR** | Result files and summarized outputs |
| **PLUGIN_LRE_WORKSPACE_DIR** | Workspace for logs, reports, and checkout |

These directories **must be writable** and are typically mounted volumes in CI environments.

---

### Expected Output
During execution, the condole logs provides the progression (waiting for run to finish, analysis report generation, ...)
After execution, the plugin writes:
- Under PLUGIN_LRE_OUTPUT_DIR path, the file `results<timestamp>.xml` – summarized test results
- Under PLUGIN_LRE_WORKSPACE_DIR path, `LreResult/*.*` – execution logs (lre_run_test__<date>.log)
- Under PLUGIN_LRE_WORKSPACE_DIR path, optional analysis files if `PLUGIN_LRE_POST_RUN_ACTION` is `Collate and Analyze` (under LreResult/LreReports/HtmlReport)
- Under PLUGIN_LRE_WORKSPACE_DIR path, optional trend report if PLUGIN_LRE_TREND_REPORT is set with existing trend report (and LreResult/LreReports/TrendReports)
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
Assuming the operating systen can pull docker images and run them as linux container.

#### powershell
create a ps1 file with the following content (provide valid values to environment variables) and run it:
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
  -e PLUGIN_LRE_USERNAME="<username>" `
  -e PLUGIN_LRE_PASSWORD="<password>" `
  -e PLUGIN_LRE_DOMAIN="<domain>" `
  -e PLUGIN_LRE_PROJECT="<project>" `
  -e PLUGIN_LRE_TEST="<testid>" `
  -e PLUGIN_LRE_POST_RUN_ACTION="Collate and Analyze" `
  -e PLUGIN_LRE_OUTPUT_DIR="/harness/output" `
  -e PLUGIN_LRE_WORKSPACE_DIR="/harness/workspace" `
  $imageName
```

#### Python
create a testImage.py file with following content (provide valid values to parameters in env_vars) and run it.

```python
import subprocess
from pathlib import Path

# -----------------------------
# Configurable environment variables
# -----------------------------
image_base = "danieldanan/opentext-enterprise-performance-engineering-test"
image_version = "latest"
image_name = f"{image_base}:{image_version}"

# Test-specific environment variables
env_vars = {
    "PLUGIN_LRE_ACTION": "ExecuteLreTest",
    "PLUGIN_LRE_DESCRIPTION": "running new test",
    "PLUGIN_LRE_SERVER": "http://<Server>/?tenant=<tenant-id>",
    "PLUGIN_LRE_HTTPS_PROTOCOL": "false",
    "PLUGIN_LRE_AUTHENTICATE_WITH_TOKEN": "false",
    "PLUGIN_LRE_USERNAME": "<username>",
    "PLUGIN_LRE_PASSWORD": "<password>",
    "PLUGIN_LRE_DOMAIN": "<domain>",
    "PLUGIN_LRE_PROJECT": "<project>",
    "PLUGIN_LRE_TEST": "<testid>",
    "PLUGIN_LRE_TEST_INSTANCE": "",
    "PLUGIN_LRE_TIMESLOT_DURATION_HOURS": "0",
    "PLUGIN_LRE_TIMESLOT_DURATION_MINUTES": "30",
    "PLUGIN_LRE_POST_RUN_ACTION": "Collate and Analyze",
    "PLUGIN_LRE_VUDS_MODE": "false",
    "PLUGIN_LRE_TREND_REPORT": "5",
    "PLUGIN_LRE_SEARCH_TIMESLOT": "false",
    "PLUGIN_LRE_STATUS_BY_SLA": "false",
    "PLUGIN_LRE_OUTPUT_DIR": "/harness/output",
    "PLUGIN_LRE_WORKSPACE_DIR": "/harness",
    "PLUGIN_LRE_ENABLE_STACKTRACE": "false",
}


def windows_path_for_docker(path: str) -> str:
    """Convert Windows path to Docker-friendly forward slash path"""
    return Path(path).resolve().as_posix()


def run_test_container():
    print(f"Beginning running a test using container of image {image_name} ...")

    # Docker volume mappings
    volumes = [
        f"{path_for_docker('./harness/output')}:{env_vars['PLUGIN_LRE_OUTPUT_DIR']}",
        f"{path_for_docker('./harness/workspace')}:{env_vars['PLUGIN_LRE_WORKSPACE_DIR']}",
    ]

    # Build docker run command
    cmd = ["docker", "run", "-it", "--rm"]

    # Add volume mappings
    for vol in volumes:
        cmd += ["-v", vol]

    # Add environment variables
    for key, value in env_vars.items():
        cmd += ["-e", f"{key}={value}"]

    # Add image name
    cmd.append(image_name)

    # Run the docker command
    print("> Running Docker container...")
    subprocess.run(cmd, shell=True, check=True)

    print(f"Finished running a test using container of image {image_name} ...")


if __name__ == "__main__":
    run_test_container()

```

### Harness pipeline example

Harness users **expect YAML** as step (the value of connectorRef should be customized to your harness configuation).
values can be stored as secrets in harness and referenced.

```yaml
...
            steps:
              - step:
                  type: Run
                  name: Run_1
                  identifier: Run_1
                  spec:
                    connectorRef: DockerRegistry1
                    image: danieldanan/opentext-enterprise-performance-engineering-test:latest
                    shell: Sh
                    envVariables:
                      PLUGIN_LRE_ACTION: ExecuteLreTest
                      PLUGIN_LRE_DESCRIPTION: running new test
                      PLUGIN_LRE_SERVER: <+secrets.getValue("lre_server")>
                      PLUGIN_LRE_HTTPS_PROTOCOL: "true"
                      PLUGIN_LRE_AUTHENTICATE_WITH_TOKEN: "false"
                      PLUGIN_LRE_USERNAME: <+secrets.getValue("lre_username")>
                      PLUGIN_LRE_PASSWORD: <+secrets.getValue("lre_password")>
                      PLUGIN_LRE_DOMAIN: <+secrets.getValue("lre_domain")>
                      PLUGIN_LRE_PROJECT: <+secrets.getValue("lre_project")>
                      PLUGIN_LRE_TEST: <+secrets.getValue("lre_test")>
                      PLUGIN_LRE_TIMESLOT_DURATION_HOURS: "0"
                      PLUGIN_LRE_TIMESLOT_DURATION_MINUTES: "30"
                      PLUGIN_LRE_POST_RUN_ACTION: Collate and Analyze
                      PLUGIN_LRE_VUDS_MODE: "false"
                      PLUGIN_LRE_TREND_REPORT: "5"
                      PLUGIN_LRE_SEARCH_TIMESLOT: "false"
                      PLUGIN_LRE_STATUS_BY_SLA: "false"
                      PLUGIN_LRE_OUTPUT_DIR: /harness/output
                      PLUGIN_LRE_WORKSPACE_DIR: /harness
                      PLUGIN_LRE_ENABLE_STACKTRACE: "false"
...
```

### Configuration parameters

All configuration is provided using environment variables.

### Output

The plugin writes execution logs and result files to:

- Output directory: `$PLUGIN_LRE_OUTPUT_DIR`
- Workspace directory: `$PLUGIN_LRE_WORKSPACE_DIR`

These directories should be mapped to Harness workspace paths
to allow artifact collection and post-processing.
---
## Authentication

Supported authentication modes:
- Username / password
- Token authentication by setting:
-- PLUGIN_LRE_AUTHENTICATE_WITH_TOKEN=true
-- PLUGIN_LRE_USERNAME=<Id part of the token>
-- PLUGIN_LRE_PASSWORD=<Secret part of the token>

---
## Troubleshooting
- Ensure network connectivity from the container to the server
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
## License
This project is licensed under the MIT License.
See the [LICENSE](LICENSE) file for details.
---

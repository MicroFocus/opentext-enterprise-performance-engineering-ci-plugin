import os
import subprocess
import json
from pathlib import Path

# -----------------------------
# Docker image metadata
# -----------------------------
image_name = os.getenv("IMAGE_NAME", "opentext-enterprise-performance-engineering-ci-plugin:latest")

# -----------------------------
# Load JSON config into environment variables (safe mode)
# -----------------------------
def load_json_config_to_env(json_path: str):
    """Safely load JSON config file and convert properties to PLUGIN_* env vars.
    Never raises exceptions. Safe if file missing or invalid."""
    
    if not os.path.isfile(json_path):
        print(f"[INFO] Config file '{json_path}' not found. Skipping JSON load.")
        return

    try:
        with open(json_path, "r", encoding="utf-8") as f:
            data = json.load(f)

        if not isinstance(data, dict):
            print(f"[WARNING] Config file '{json_path}' is not a JSON object. Skipping.")
            return

        print(f"[INFO] Loading configuration from {json_path} ...")

        for key, value in data.items():
            env_key = f"PLUGIN_{key.upper()}"

            if isinstance(value, bool):
                value = str(value).lower()
            else:
                value = str(value)

            os.environ[env_key] = value
            print(f"[INFO] Set {env_key}={value}")

    except Exception as e:
        print(f"[WARNING] Failed to load config '{json_path}': {e}")
        print("[INFO] Continuing without JSON configuration.")
        
# -----------------------------
# Docker volume helper
# -----------------------------
def path_for_docker(p: str) -> str:
    """Convert path to Docker-friendly string (Linux/Windows)"""
    return str(Path(p).resolve())

# -----------------------------
# Run container
# -----------------------------
def run_test_container():
    print(f"Beginning running a test using container {image_name} ...")

    # Build env_vars AFTER JSON is loaded
    env_vars = {
        key: os.environ.get(key, "")
        for key in [
            "PLUGIN_LRE_ACTION",
            "PLUGIN_LRE_DESCRIPTION",
            "PLUGIN_LRE_SERVER",
            "PLUGIN_LRE_HTTPS_PROTOCOL",
            "PLUGIN_LRE_AUTHENTICATE_WITH_TOKEN",
            "PLUGIN_LRE_USERNAME",
            "PLUGIN_LRE_PASSWORD",
            "PLUGIN_LRE_DOMAIN",
            "PLUGIN_LRE_PROJECT",
            "PLUGIN_LRE_TEST",
            "PLUGIN_LRE_TEST_INSTANCE",
            "PLUGIN_LRE_TIMESLOT_DURATION_HOURS",
            "PLUGIN_LRE_TIMESLOT_DURATION_MINUTES",
            "PLUGIN_LRE_POST_RUN_ACTION",
            "PLUGIN_LRE_VUDS_MODE",
            "PLUGIN_LRE_TREND_REPORT",
            "PLUGIN_LRE_SEARCH_TIMESLOT",
            "PLUGIN_LRE_STATUS_BY_SLA",
            "PLUGIN_LRE_OUTPUT_DIR",
            "PLUGIN_LRE_WORKSPACE_DIR",
            "PLUGIN_LRE_ENABLE_STACKTRACE",
            "PLUGIN_LRE_RUNTIME_ONLY",
        ]
    }

    # Validate required volume vars
    output_dir = env_vars.get("PLUGIN_LRE_OUTPUT_DIR")
    workspace_dir = env_vars.get("PLUGIN_LRE_WORKSPACE_DIR")

    if not output_dir or not workspace_dir:
        raise RuntimeError(
            "PLUGIN_LRE_OUTPUT_DIR and PLUGIN_LRE_WORKSPACE_DIR must be set "
            "(via environment variables or JSON config)."
        )

    volumes = [
        f"{path_for_docker('./harness/output')}:{env_vars['PLUGIN_LRE_OUTPUT_DIR']}",
        f"{path_for_docker('./harness/workspace')}:{env_vars['PLUGIN_LRE_WORKSPACE_DIR']}",
    ]

    cmd = ["docker", "run", "--rm"]

    for vol in volumes:
        cmd += ["-v", vol]

    for key, value in env_vars.items():
        if value:
            cmd += ["-e", f"{key}={value}"]

    cmd.append(image_name)

    print("> Running Docker container...")
    subprocess.run(cmd, check=True)

    print(f"Finished running container {image_name}.")

# -----------------------------
# Main
# -----------------------------
if __name__ == "__main__":
    # Load JSON config first
    load_json_config_to_env("java-app/run-config-WorkspaceSync2.json")

    os.makedirs("./harness/output", exist_ok=True)
    os.makedirs("./harness/workspace", exist_ok=True)
    run_test_container()

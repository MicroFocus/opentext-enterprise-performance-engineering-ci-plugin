# OpenText Enterprise Performance Engineering CI Plugin

This Harness custom step plugin runs load tests using OpenText Enterprise Performance Engineering.

## Usage

- Select step in Harness pipeline
- Provide required parameters: LRE_SERVER, LRE_USERNAME, LRE_PASSWORD, LRE_DOMAIN, LRE_PROJECT, LRE_TEST
- Optional parameters: LRE_OUTPUT_DIR, LRE_TIMESLOT_DURATION_HOURS, etc.
- Plugin runs your Docker image and reports status

## Notes

- Requires Docker-enabled Harness delegate
- Boolean parameters are toggles
- Trend report is ignored when post-run action is `Do Not Collate`

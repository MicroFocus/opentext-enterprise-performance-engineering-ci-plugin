'use strict';

const { execSync } = require('child_process');

// Uncomment for local testing if needed
// require('dotenv').config();

// -----------------------------------------------------------------------------
// Constants
// -----------------------------------------------------------------------------
const DEFAULT_WORKSPACE = '/harness';
const IMAGE =
  'performancetesting/opentext-enterprise-performance-engineering-ci-plugin:latest';

// -----------------------------------------------------------------------------
// Read inputs from Harness (all values arrive as strings)
// -----------------------------------------------------------------------------
const {
  LRE_ACTION,
  LRE_DESCRIPTION,
  LRE_SERVER,
  LRE_HTTPS_PROTOCOL,
  LRE_USERNAME,
  LRE_PASSWORD,
  LRE_AUTHENTICATE_WITH_TOKEN,
  LRE_DOMAIN,
  LRE_PROJECT,
  LRE_TEST,
  LRE_TEST_INSTANCE,
  LRE_TIMESLOT_DURATION_HOURS,
  LRE_TIMESLOT_DURATION_MINUTES,
  LRE_POST_RUN_ACTION,
  LRE_VUDS_MODE,
  LRE_TREND_REPORT,
  LRE_SEARCH_TIMESLOT,
  LRE_STATUS_BY_SLA,
  LRE_PROXY_OUT_URL,
  LRE_USERNAME_PROXY,
  LRE_PASSWORD_PROXY,
  LRE_ENABLE_STACKTRACE,
  LRE_OUTPUT_DIR,
  LRE_WORKSPACE_DIR
} = process.env;

// -----------------------------------------------------------------------------
// Resolve workspace & output directories (user value wins, defaults otherwise)
// -----------------------------------------------------------------------------
const resolvedWorkspaceDir =
  LRE_WORKSPACE_DIR && LRE_WORKSPACE_DIR.trim() !== ''
    ? LRE_WORKSPACE_DIR
    : DEFAULT_WORKSPACE;

const resolvedOutputDir =
  LRE_OUTPUT_DIR && LRE_OUTPUT_DIR.trim() !== ''
    ? LRE_OUTPUT_DIR
    : `${resolvedWorkspaceDir}/output`;

// -----------------------------------------------------------------------------
// Helpers
// -----------------------------------------------------------------------------
function env(name, value) {
  return value !== undefined && value !== ''
    ? `-e ${name}=${value}`
    : '';
}

// -----------------------------------------------------------------------------
// Execution
// -----------------------------------------------------------------------------
try {
  console.log(
    'Starting OpenText Enterprise Performance Engineering Docker container...'
  );

  // Basic validation (UI enforces this, but YAML users may bypass it)
  if (!LRE_SERVER || !LRE_USERNAME || !LRE_PASSWORD) {
    throw new Error(
      'Missing required parameters: LRE_SERVER, LRE_USERNAME, or LRE_PASSWORD'
    );
  }

  const envArgs = [
    env('PLUGIN_LRE_ACTION', LRE_ACTION),
    env('PLUGIN_LRE_DESCRIPTION', LRE_DESCRIPTION),
    env('PLUGIN_LRE_SERVER', LRE_SERVER),
    env('PLUGIN_LRE_HTTPS_PROTOCOL', LRE_HTTPS_PROTOCOL),
    env('PLUGIN_LRE_USERNAME', LRE_USERNAME),
    env('PLUGIN_LRE_PASSWORD', LRE_PASSWORD),
    env(
      'PLUGIN_LRE_AUTHENTICATE_WITH_TOKEN',
      LRE_AUTHENTICATE_WITH_TOKEN
    ),
    env('PLUGIN_LRE_DOMAIN', LRE_DOMAIN),
    env('PLUGIN_LRE_PROJECT', LRE_PROJECT),
    env('PLUGIN_LRE_TEST', LRE_TEST),
    env('PLUGIN_LRE_TEST_INSTANCE', LRE_TEST_INSTANCE),
    env(
      'PLUGIN_LRE_TIMESLOT_DURATION_HOURS',
      LRE_TIMESLOT_DURATION_HOURS
    ),
    env(
      'PLUGIN_LRE_TIMESLOT_DURATION_MINUTES',
      LRE_TIMESLOT_DURATION_MINUTES
    ),
    env('PLUGIN_LRE_POST_RUN_ACTION', LRE_POST_RUN_ACTION),
    env('PLUGIN_LRE_VUDS_MODE', LRE_VUDS_MODE),
    env('PLUGIN_LRE_TREND_REPORT', LRE_TREND_REPORT),
    env('PLUGIN_LRE_SEARCH_TIMESLOT', LRE_SEARCH_TIMESLOT),
    env('PLUGIN_LRE_STATUS_BY_SLA', LRE_STATUS_BY_SLA),
    env('PLUGIN_LRE_PROXY_OUT_URL', LRE_PROXY_OUT_URL),
    env('PLUGIN_LRE_USERNAME_PROXY', LRE_USERNAME_PROXY),
    env('PLUGIN_LRE_PASSWORD_PROXY', LRE_PASSWORD_PROXY),
    env('PLUGIN_LRE_ENABLE_STACKTRACE', LRE_ENABLE_STACKTRACE),
    env('PLUGIN_LRE_WORKSPACE_DIR', resolvedWorkspaceDir),
    env('PLUGIN_LRE_OUTPUT_DIR', resolvedOutputDir)
  ]
    .filter(Boolean)
    .join(' ');

  const cmd = `docker run --rm -v ${resolvedWorkspaceDir}:${resolvedWorkspaceDir} ${envArgs} ${IMAGE}`;

  // IMPORTANT: Do NOT log the full command (secrets!)
  execSync(cmd, { stdio: 'inherit' });

  console.log(
    JSON.stringify({
      status: 'SUCCESS',
      message:
        'OpenText Enterprise Performance Engineering test completed successfully.'
    })
  );
  process.exit(0);
} catch (err) {
  console.error('OpenText test execution failed:', err.message);

  console.log(
    JSON.stringify({
      status: 'FAILURE',
      message: err.message
    })
  );
  process.exit(1);
}

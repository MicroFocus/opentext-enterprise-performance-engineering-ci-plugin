# Model Convergence - Migration Guide

## Overview

The `LreWorkspaceSyncModel` has been updated to converge with `LreTestRunModel` for consistency and maintainability. This document explains the changes and how to migrate your existing configurations.

## What Changed?

### 1. Server Configuration Simplified

**Before:**
```json
{
  "lre_server": "lre.example.com",
  "lre_port": "443",
  "lre_tenant": "mytenant"
}
```

**After:**
```json
{
  "lre_server": "lre.example.com:443/LoadTest/mytenant"
}
```

**Rationale:** Both models now use the same `lre_server` format, which includes the port and tenant path. This matches how `LreTestRunModel` works.

### 2. Workspace Path Parameter Renamed

**Before:**
```json
{
  "workspace_path": "C:/workspace/scripts"
}
```

**After:**
```json
{
  "lre_workspace_path": "C:/workspace/scripts"
}
```

**Rationale:** All LRE-related parameters now consistently use the `lre_` prefix.

### 3. Token Authentication Added

**Before:** Not available

**After:**
```json
{
  "lre_authenticate_with_token": false
}
```

**Rationale:** Both models now support token-based authentication for enhanced security.

### 4. Model Field Names Aligned

The internal model field names have been updated to match `LreTestRunModel`:

| Old Name | New Name |
|----------|----------|
| `serverAndPort` | `lreServerAndPort` |
| `proxyUrl` | `proxyOutURL` |
| `proxyUsername` | `usernameProxy` |
| `proxyPassword` | `passwordProxy` |

### 5. Method Names Aligned

| Old Method | New Method |
|------------|------------|
| `getServerAndPortWithTenant()` | `getLreServerAndPort()` |
| `getProxyUrl()` | `getProxyOutURL()` |
| `getProxyUsername()` | `getUsernameProxy()` |
| `getProxyPassword()` | `getPasswordProxy()` |
| N/A | `isAuthenticateWithToken()` |

## Migration Steps

### Step 1: Update Configuration Files

If you have a `workspace-sync-config.json`, update it as follows:

**Old Configuration:**
```json
{
  "lre_action": "WorkspaceSync",
  "lre_server": "lre.example.com",
  "lre_port": "443",
  "lre_tenant": "mytenant",
  "lre_https_protocol": true,
  "lre_domain": "DEFAULT",
  "lre_project": "MyProject",
  "lre_proxy_url": "",
  "workspace_path": "C:/workspace/scripts",
  "lre_runtime_only": false
}
```

**New Configuration:**
```json
{
  "lre_action": "WorkspaceSync",
  "lre_server": "lre.example.com:443/LoadTest/mytenant",
  "lre_https_protocol": true,
  "lre_authenticate_with_token": false,
  "lre_domain": "DEFAULT",
  "lre_project": "MyProject",
  "lre_proxy_out_url": "",
  "lre_workspace_path": "C:/workspace/scripts",
  "lre_runtime_only": false
}
```

### Step 2: Update Environment Variables (if used)

If you're using environment variables, update as follows:

**Old:**
```bash
export PLUGIN_LRE_SERVER=lre.example.com
export PLUGIN_LRE_PORT=443
export PLUGIN_LRE_TENANT=mytenant
export PLUGIN_WORKSPACE_PATH=/workspace/scripts
```

**New:**
```bash
export PLUGIN_LRE_SERVER=lre.example.com:443/LoadTest/mytenant
export PLUGIN_LRE_WORKSPACE_PATH=/workspace/scripts
export PLUGIN_LRE_AUTHENTICATE_WITH_TOKEN=false
```

### Step 3: Update CI/CD Pipeline Configurations

#### Harness CI/CD Example

**Old:**
```yaml
settings:
  lre_action: WorkspaceSync
  lre_server: lre.example.com
  lre_port: "443"
  lre_tenant: mytenant
  workspace_path: /workspace/scripts
```

**New:**
```yaml
settings:
  lre_action: WorkspaceSync
  lre_server: lre.example.com:443/LoadTest/mytenant
  lre_authenticate_with_token: "false"
  lre_workspace_path: /workspace/scripts
```

## Server URL Format Examples

The `lre_server` parameter now includes the full path. Here are common patterns:

### With Port
```
lre.example.com:443/LoadTest/mytenant
```

### Without Port (uses default)
```
lre.example.com/LoadTest/mytenant
```

### With Query Parameters
```
lre.example.com:443/LoadTest/mytenant/?tenant=123
```

### Already Includes LoadTest Path
```
lre.example.com:443/LoadTest/mytenant
```
(No change needed - the model handles this automatically)

## Benefits of Convergence

### 1. Consistency
Both `LreTestRunModel` and `LreWorkspaceSyncModel` now use the same naming conventions and structure.

### 2. Simplified Configuration
- Fewer parameters to configure (no separate port and tenant)
- Matches the format used in test execution
- Easier to understand for users familiar with either model

### 3. Enhanced Security
- Token-based authentication now available for workspace sync
- Consistent authentication handling across both models

### 4. Easier Maintenance
- Developers only need to understand one configuration pattern
- Code duplication reduced
- Future enhancements benefit both models

### 5. Better Testing
- Same configuration validation logic
- Consistent error messages
- Unified documentation

## Backward Compatibility Notes

⚠️ **Breaking Changes:**
- The old parameter names (`lre_port`, `lre_tenant`, `workspace_path`) are **no longer supported**
- The old method names (`getServerAndPortWithTenant()`, etc.) have been removed
- You **must** update your configurations to use the new format

## Common Migration Issues

### Issue 1: Missing Tenant in Server URL
**Error:** Authentication fails or wrong tenant accessed

**Solution:** Ensure your `lre_server` includes the `/LoadTest/tenant` path:
```json
{
  "lre_server": "server.com:443/LoadTest/mytenant"
}
```

### Issue 2: Wrong Parameter Name
**Error:** `Missing required config value: lre_workspace_path`

**Solution:** Update `workspace_path` to `lre_workspace_path`:
```json
{
  "lre_workspace_path": "C:/workspace/scripts"
}
```

### Issue 3: Proxy Parameters Not Working
**Error:** Proxy authentication fails

**Solution:** Update proxy parameter names:
```json
{
  "lre_proxy_out_url": "http://proxy:8080",
  "lre_username_proxy": "proxyuser",
  "lre_password_proxy": "proxypass"
}
```

## Testing Your Migration

After updating your configuration, test it:

```bash
# Set credentials
export PLUGIN_LRE_USERNAME=myuser
export PLUGIN_LRE_PASSWORD=mypass

# Run with new config
java -jar lre-actions-1.1-SNAPSHOT-jar-with-dependencies.jar workspace-sync-config.json
```

Check the log output for:
- ✅ Successful authentication
- ✅ Correct server URL in logs
- ✅ Scripts uploaded to correct location

## Need Help?

If you encounter issues during migration:

1. Review the sample configuration: `workspace-sync-config-sample.json`
2. Check the README.md for detailed parameter descriptions
3. Review the ARCHITECTURE.md for design details
4. Check logs for specific error messages

## Summary

The model convergence brings consistency and maintainability improvements. While it requires configuration updates, the changes are straightforward and the new format is more intuitive and aligns with the test execution model.


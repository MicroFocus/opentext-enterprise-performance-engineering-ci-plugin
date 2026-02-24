# main_full_jar Configuration Guide

## Current Configuration

The `main_full_jar` IntelliJ run configuration has been updated with the correct logging parameters.

### VM Parameters (Complete)
```
-Djava.util.logging.manager=org.apache.logging.log4j.jul.LogManager -Dlog4j.configurationFile=jar:file:/$PROJECT_DIR$/target/lre-actions-1.1-SNAPSHOT-jar-with-dependencies.jar!/log4j2.xml
```

### Program Parameters
```
C:\Git\plugin\github.com\opentext-enterprise-performance-engineering-ci-plugin2\java-app\run-config.json
```

### Environment Variables
```
PLUGIN_LRE_USERNAME=daniel
PLUGIN_LRE_PASSWORD=W3lcome1
```

---

## Breakdown of VM Parameters

### Parameter 1: Java Util Logging Manager
```
-Djava.util.logging.manager=org.apache.logging.log4j.jul.LogManager
```
**Purpose**: Routes all java.util.logging calls to log4j2
**Result**: HTTP client warnings go through log4j2 instead of console
**Dependency**: log4j2-jul in pom.xml (already present)

### Parameter 2: Log4j Configuration File
```
-Dlog4j.configurationFile=jar:file:/$PROJECT_DIR$/target/lre-actions-1.1-SNAPSHOT-jar-with-dependencies.jar!/log4j2.xml
```
**Purpose**: Tells log4j2 where to find its configuration
**Format**: `jar:file:/path/to/jar!/resource/inside/jar`
**Resolution**: `$PROJECT_DIR$` is replaced with your project root
**Result**: log4j2 loads configuration from bundled log4j2.xml

---

## How to Set This Up in IntelliJ

### Method 1: Copy From XML (Already Done)
The configuration has been updated in `.idea/workspace.xml`. Simply reload the IDE if needed.

### Method 2: Manual Configuration in UI

1. **Click**: Run → Edit Configurations
2. **Select**: `main_full_jar` configuration
3. **In "Application" panel, set**:
   - **Main class**: `com.opentext.lre.actions.Main`
   - **Program arguments**: Your path to `run-config.json`
   - **VM options**: (See below)

4. **Set VM options (copy-paste entire line)**:
   ```
   -Djava.util.logging.manager=org.apache.logging.log4j.jul.LogManager -Dlog4j.configurationFile=jar:file:/$PROJECT_DIR$/target/lre-actions-1.1-SNAPSHOT-jar-with-dependencies.jar!/log4j2.xml
   ```

5. **Set Environment variables**:
   - Click **Environment variables** button
   - Add:
     - `PLUGIN_LRE_USERNAME=daniel`
     - `PLUGIN_LRE_PASSWORD=W3lcome1`
     - (Or use your own credentials)

6. **Click**: OK

---

## Complete XML Configuration

If you need to manually edit `.idea/workspace.xml`:

```xml
<configuration name="main_full_jar" type="Application" factoryName="Application">
  <envs>
    <env name="PLUGIN_LRE_USERNAME" value="daniel" />
    <env name="PLUGIN_LRE_PASSWORD" value="W3lcome1" />
  </envs>
  <option name="MAIN_CLASS_NAME" value="com.opentext.lre.actions.Main" />
  <module name="lre-actions" />
  <option name="PROGRAM_PARAMETERS" value="C:\Git\plugin\github.com\opentext-enterprise-performance-engineering-ci-plugin2\java-app\run-config.json" />
  <option name="VM_PARAMETERS" value="-Djava.util.logging.manager=org.apache.logging.log4j.jul.LogManager -Dlog4j.configurationFile=jar:file:/$PROJECT_DIR$/target/lre-actions-1.1-SNAPSHOT-jar-with-dependencies.jar!/log4j2.xml" />
  <method v="2">
    <option name="Make" enabled="true" />
  </method>
</configuration>
```

---

## Prerequisites

Before running `main_full_jar`:

### 1. Build the JAR
```bash
cd java-app
mvn clean package
```

This creates:
- `target/lre-actions-1.1-SNAPSHOT.jar`
- `target/lre-actions-1.1-SNAPSHOT-jar-with-dependencies.jar` ← Used by main_full_jar

### 2. Verify log4j2.xml is Bundled
```bash
jar tf target/lre-actions-1.1-SNAPSHOT-jar-with-dependencies.jar | grep log4j2.xml
```

Should output:
```
log4j2.xml
```

### 3. Verify run-config.json Exists
```bash
ls -l run-config.json
```

Should show the file exists.

---

## Running main_full_jar

### In IntelliJ UI

1. **Select configuration**: Dropdown → `main_full_jar`
2. **Click**: Green play button (Run)
3. **Or press**: Shift + F10

### Expected Output

```
Connected to the target VM, address: '127.0.0.1:62892', transport: 'socket'
2026-02-19 13:34:52 INFO  - Log file path set to: C:\...\lre_run_test_...log
2026-02-19 13:34:52 INFO  - Beginning LRE Test Execution
... execution logs ...
2026-02-19 13:35:15 INFO  - Build successful
```

### Verify Success

- [ ] No "Reconfiguration failed" errors
- [ ] No "Invalid cookie header" warnings
- [ ] Process exits with code 0
- [ ] Log file created in output directory (from run-config.json)
- [ ] Log file contains timestamped entries

---

## Troubleshooting

### Issue: "Cannot find log4j2.xml"
**Cause**: JAR not built or log4j2.xml not bundled
**Fix**: 
```bash
mvn clean package
jar tf target/lre-actions-1.1-SNAPSHOT-jar-with-dependencies.jar | grep log4j2
```

### Issue: "Reconfiguration failed"
**Cause**: Missing or incorrect VM parameter
**Fix**: Verify entire VM_PARAMETERS line is present:
```
-Djava.util.logging.manager=org.apache.logging.log4j.jul.LogManager -Dlog4j.configurationFile=jar:file:/$PROJECT_DIR$/target/lre-actions-1.1-SNAPSHOT-jar-with-dependencies.jar!/log4j2.xml
```

### Issue: Log file still empty
**Cause**: log4j2 configuration not loaded properly
**Fix**: 
1. Verify `log4j2.xml` exists in JAR
2. Verify `immediateFlush="true"` in log4j2.xml
3. Rebuild JAR and clear IDE cache

### Issue: Cookie warnings still appearing
**Cause**: java.util.logging manager not set
**Fix**: Verify this is in VM_PARAMETERS:
```
-Djava.util.logging.manager=org.apache.logging.log4j.jul.LogManager
```

---

## Key Points to Remember

1. **VM Parameters must include BOTH**:
   - java.util.logging manager bridge
   - log4j.configurationFile reference

2. **JAR must be built FIRST**:
   - VM parameters point to target/ directory
   - Need fresh build with `mvn clean package`

3. **log4j2.xml is BUNDLED inside the JAR**:
   - Use jar: protocol to access it
   - Format: `jar:file:/...jar!/log4j2.xml`

4. **PATH RESOLUTION**:
   - `$PROJECT_DIR$` is IntelliJ macro (automatic)
   - No need to manually replace it

5. **INITIALIZATION ORDER**:
   - java.util.logging bridge set first (VM parameter)
   - Java process starts with these settings
   - Main.java further configures logging
   - log4j2 initializes and creates file appender

---

## Quick Copy-Paste Template

If you need to create a new configuration:

**VM Parameters**:
```
-Djava.util.logging.manager=org.apache.logging.log4j.jul.LogManager -Dlog4j.configurationFile=jar:file:/$PROJECT_DIR$/target/lre-actions-1.1-SNAPSHOT-jar-with-dependencies.jar!/log4j2.xml
```

**Program Parameters**:
```
<path-to>/run-config.json
```

**Environment Variables**:
```
PLUGIN_LRE_USERNAME=<your-username>
PLUGIN_LRE_PASSWORD=<your-password>
```

---

## Comparison with Other Configurations

| Configuration | VM Parameters | Use Case |
|---------------|---------------|----------|
| **main_full_jar** | Both parameters | Testing with full JAR |
| **executetest** | Same as main_full_jar | Alternative test config |
| **Main** | Missing parameters | IDE debugging (limited logging) |
| **sync** | Same as main_full_jar | Workspace sync testing |

---

## Summary

The `main_full_jar` configuration is now correctly set up to:
1. ✅ Bridge java.util.logging to log4j2
2. ✅ Load log4j2.xml from inside the JAR
3. ✅ Create file appender with configured output path
4. ✅ Write logs to both console and file
5. ✅ Suppress cookie warnings

**Status**: Ready to use ✅

Just run it and verify logs appear in both console and log file!


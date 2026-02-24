# main_full_jar Configuration - Quick Setup Guide

## ✅ Configuration Updated

Your `main_full_jar` run configuration in IntelliJ has been successfully updated with the correct logging parameters.

---

## What's Configured

### VM Parameters (2 parameters in 1 line)
```
-Djava.util.logging.manager=org.apache.logging.log4j.jul.LogManager -Dlog4j.configurationFile=jar:file:/$PROJECT_DIR$/target/lre-actions-1.1-SNAPSHOT-jar-with-dependencies.jar!/log4j2.xml
```

**What each does:**
1. **java.util.logging.manager** - Routes HTTP client warnings to log4j2
2. **log4j.configurationFile** - Tells log4j2 where to find log4j2.xml inside the JAR

### Program Parameters
```
C:\Git\plugin\github.com\opentext-enterprise-performance-engineering-ci-plugin2\java-app\run-config.json
```

### Environment Variables
- `PLUGIN_LRE_USERNAME=daniel`
- `PLUGIN_LRE_PASSWORD=W3lcome1`

---

## How to Use

### 1. Build the JAR (if not already built)
```bash
cd java-app
mvn clean package
```

### 2. Run in IntelliJ

**Option A - Using Dropdown**:
1. Click the run configuration dropdown (top right)
2. Select: `main_full_jar`
3. Click green play button

**Option B - Keyboard Shortcut**:
- Select `main_full_jar` from dropdown, then press Shift + F10

### 3. Verify It Works
✅ Console shows: `2026-02-19 13:34:52 INFO  - Log file path set to: ...`
✅ No "Reconfiguration failed" errors
✅ No cookie warnings
✅ Log file created in output directory

---

## If You Need to Edit Manually

### In IntelliJ UI
1. Run → Edit Configurations
2. Find: `main_full_jar`
3. Scroll to: **VM options**
4. Paste:
```
-Djava.util.logging.manager=org.apache.logging.log4j.jul.LogManager -Dlog4j.configurationFile=jar:file:/$PROJECT_DIR$/target/lre-actions-1.1-SNAPSHOT-jar-with-dependencies.jar!/log4j2.xml
```
5. Click OK

---

## Expected Results

```
✓ Build succeeds: mvn clean package
✓ JAR created: target/lre-actions-1.1-SNAPSHOT-jar-with-dependencies.jar
✓ Run configuration works: main_full_jar
✓ Logging works:
  - Console: Clean output with timestamps
  - File: Log file created and populated
  - Errors: None
  - Warnings: None (cookie warnings suppressed)
```

---

## Troubleshooting

| Problem | Solution |
|---------|----------|
| "Cannot find log4j2.xml" | Run `mvn clean package` |
| "Reconfiguration failed" | Check VM_PARAMETERS line is complete (copy from above) |
| Log file empty | Verify `immediateFlush="true"` in log4j2.xml |
| Cookie warnings | Verify java.util.logging.manager parameter is set |

---

## Key Points

1. **Two parameters** in the VM_PARAMETERS field:
   - `java.util.logging.manager` (for warnings)
   - `log4j.configurationFile` (for config location)

2. **They must be space-separated** on one line

3. **`$PROJECT_DIR$` is automatically resolved** by IntelliJ

4. **log4j2.xml is inside the JAR**, accessed via `jar:file:/...jar!/log4j2.xml`

5. **Must build JAR first** - configuration points to `target/` directory

---

## Configuration String (Copy-Paste Ready)

If you need to add this to a different configuration:

```
-Djava.util.logging.manager=org.apache.logging.log4j.jul.LogManager -Dlog4j.configurationFile=jar:file:/$PROJECT_DIR$/target/lre-actions-1.1-SNAPSHOT-jar-with-dependencies.jar!/log4j2.xml
```

---

**Status**: ✅ Ready to use!

Just run `main_full_jar` and you should see logs in both console and file.


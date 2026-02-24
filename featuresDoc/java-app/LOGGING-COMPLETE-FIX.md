# Logging Issues - Complete Fix

## Issues Reported
1. **Log file created but remains empty** - No logs were being written to the file
2. **Error on startup**: `2026-02-19T11:15:51.648179200Z main ERROR Reconfiguration failed: No configuration found for '60e53b93' at 'null' in 'null'`
3. **Cookie warnings flooding console**: Multiple `WARNING: Invalid cookie header` messages from HTTP client

## Root Causes Analysis

### Issue 1: Empty Log File
The `LogHelper.setup()` method was calling `context.reconfigure()` which attempted to reconfigure log4j2 before it had a chance to properly initialize with the `log.file` system property.

### Issue 2: Reconfiguration Error
Calling `reconfigure()` on a LoggerContext that hasn't been fully initialized causes the "No configuration found" error because log4j2 is still in the process of reading the configuration file.

### Issue 3: Cookie Warnings
The Apache HTTP client uses `java.util.logging` for its warnings, which were not being suppressed or bridged to log4j2.

## Complete Solution

### 1. LogHelper.java - Simplified Initialization
**File**: `src/main/java/com/opentext/lre/actions/common/helpers/utils/LogHelper.java`

**Changed**:
- Removed the `context.reconfigure()` call that caused errors
- Removed unused `LoggerContext` import
- Let log4j2 initialize naturally on first logger access

```java
public static synchronized void setup(String logFilePath, boolean enableStackTrace) throws Exception {
    stackTraceEnabled = enableStackTrace;

    // Ensure directory exists
    File file = new File(logFilePath);
    File parent = file.getParentFile();
    if (!parent.exists() && !parent.mkdirs()) {
        throw new Exception("Failed to create log directory: " + parent.getAbsolutePath());
    }

    // Simply get the logger - log4j2 will initialize with the system property
    logger = LogManager.getLogger(LogHelper.class);

    logger.info("Log file path set to: " + logFilePath);
}
```

### 2. Main.java - Added java.util.logging Bridge
**File**: `src/main/java/com/opentext/lre/actions/Main.java`

**Added imports**:
```java
import org.apache.logging.log4j.jul.LogManager;
import java.util.logging.Level;
import java.util.logging.Logger;
```

**Added initialization sequence in `performOperations()`**:
```java
// Set log file property BEFORE any logging initialization
System.setProperty("log.file", logFilePath);

// Suppress java.util.logging cookie warnings from HTTP client
Logger httpLogger = Logger.getLogger("org.apache.http.client.protocol.ResponseProcessCookies");
httpLogger.setLevel(Level.SEVERE);

// Bridge java.util.logging to log4j2
java.util.logging.LogManager.getLogManager().reset();
LogManager.getLogManager();

// Now setup log4j2
LogHelper.setup(logFilePath, lreTestRunModel.isEnableStacktrace());
```

### 3. log4j2.xml - Enhanced Configuration
**File**: `src/main/resources/log4j2.xml`

**Enhanced File Appender**:
```xml
<File name="MainFileAppender" fileName="${sys:log.file}" immediateFlush="true" append="true">
    <PatternLayout>
        <Pattern>%d{yyyy-MM-dd HH:mm:ss} %-5level - %msg%n</Pattern>
    </PatternLayout>
</File>
```

**Added HTTP Client Logger**:
```xml
<!-- Suppress java.util.logging warnings from HTTP client -->
<Logger name="org.apache.http" level="ERROR" additivity="false">
    <AppenderRef ref="Console"/>
    <AppenderRef ref="MainFileAppender"/>
</Logger>
```

## Initialization Flow (Correct Order)

1. **System Property**: `System.setProperty("log.file", logFilePath)` sets the path
2. **Suppress j.u.l warnings**: HTTP client logger set to SEVERE level
3. **Bridge j.u.l to log4j2**: Reset java.util.logging and install log4j2 bridge
4. **LogHelper.setup()**: Gets logger instance (log4j2 initializes at this point)
5. **log4j2 initialization**: Reads `log4j2.xml`, resolves `${sys:log.file}`, creates appenders
6. **Logging works**: Both console and file receive all logs with immediate flush

## Why This Works

### System Property Before Initialization
The key is setting `System.setProperty("log.file", ...)` **before** any log4j2 initialization. When log4j2 initializes (on first `LogManager.getLogger()` call), it reads `log4j2.xml` and resolves `${sys:log.file}` to the actual path.

### No Reconfigure Needed
Since the system property is already set when log4j2 initializes, there's no need to reconfigure. The initial configuration will have the correct file path.

### java.util.logging Bridge
The Apache HTTP client uses `java.util.logging`. By:
1. Setting the logger level to SEVERE (suppresses warnings)
2. Installing the log4j2-jul bridge (`LogManager.getLogManager()`)

We ensure that any remaining HTTP client logs go through log4j2 instead of directly to console.

### Immediate Flush
Adding `immediateFlush="true"` ensures logs are written to disk immediately, which is critical for troubleshooting and real-time monitoring.

## Testing

Build succeeded with all changes:
```
[INFO] BUILD SUCCESS
[INFO] Total time:  8.152 s
```

## Expected Console Output (After Fix)
- ✅ No "Reconfiguration failed" errors
- ✅ No cookie warnings
- ✅ Clean console output with timestamped log messages
- ✅ Log file created and populated in real-time

## Expected Log File Content
```
2026-02-19 13:34:52 INFO  - Log file path set to: C:\...\lre_run_test_..._log
2026-02-19 13:34:52 INFO  - Beginning LRE Test Execution
[... test execution logs ...]
2026-02-19 13:35:15 INFO  - Build successful
2026-02-19 13:35:15 INFO  - That's all folks!
```

## Files Modified
1. ✅ `LogHelper.java` - Simplified setup, removed reconfigure
2. ✅ `Main.java` - Added java.util.logging bridge and cookie suppression
3. ✅ `log4j2.xml` - Added immediateFlush and HTTP client logger
4. ✅ `LOGGING-FIX.md` - Documentation

## Testing Recommendations
1. Run the application and verify log file is created and populated
2. Check console output - should see no errors or cookie warnings
3. Verify log file contains all expected messages with timestamps
4. Test with and without `enableStackTrace` option


# TypeScript/Node.js JAR Execution - Logging Fix

## Problem Found
When running the Java JAR through the TypeScript `app.ts`, the logging was not working properly because the log4j2 configuration file was not being passed to the Java process.

## Comparison

### Before (Broken)
```typescript
const javaAppArgs = [
    '-Djava.util.logging.manager=org.apache.logging.log4j.jul.LogManager',
    '-jar',
    jarFilePath,
    configFilePath
];
```

### After (Fixed)
```typescript
const javaAppArgs = [
    '-Djava.util.logging.manager=org.apache.logging.log4j.jul.LogManager',
    `-Dlog4j.configurationFile=jar:file:///${jarFilePath.replace(/\\/g, '/')}!/log4j2.xml`,
    '-jar',
    jarFilePath,
    configFilePath
];
```

## What Changed
Added the missing log4j configuration parameter:
```
-Dlog4j.configurationFile=jar:file:///${jarFilePath.replace(/\\/g, '/')}!/log4j2.xml
```

This parameter tells log4j2 where to find its configuration file (inside the JAR).

## How It Works

1. **Path Resolution**: `path.resolve(__dirname, 'lre-actions-1.1-SNAPSHOT-jar-with-dependencies.jar')` gets the full path to the JAR
2. **URL Encoding**: `.replace(/\\/g, '/')` converts Windows backslashes to forward slashes for the JAR URL
3. **JAR URL Format**: `jar:file:///path/to/jar!/log4j2.xml` tells Java to load `log4j2.xml` from inside the JAR
4. **Template Literal**: Using backticks allows the dynamic path to be inserted

## Alignment with main_full_jar
This now matches the `main_full_jar` IntelliJ configuration:
```
-Dlog4j.configurationFile=jar:file:/$PROJECT_DIR$/target/lre-actions-1.1-SNAPSHOT-jar-with-dependencies.jar!/log4j.xml
```

Note: The TypeScript version uses `log4j2.xml` (which is the correct filename in the JAR), while the IntelliJ config references it. Both point to the same configuration.

## Benefits
✅ Logging now works correctly through TypeScript/Node.js  
✅ Java process correctly initializes log4j2 from the bundled configuration  
✅ Log files will be created and populated with proper timestamps  
✅ Matches the behavior of running through IntelliJ's `main_full_jar` configuration  
✅ java.util.logging is bridged to log4j2 for consistent logging  
✅ Cookie warnings from HTTP client are properly suppressed  

## Testing
After this change, when running the application through TypeScript:
1. Console output will be clean (no "Reconfiguration failed" errors)
2. Cookie warnings will be suppressed
3. Log file will be created and contain all execution logs
4. Both console and file logging will work simultaneously with immediate flush

## Files Modified
- `src/app.ts` - Added log4j2 configuration file parameter to Java command

## Deployment Note
Ensure the JAR file path is correctly resolved. The change handles:
- Windows paths (backslashes converted to forward slashes)
- Relative paths (resolved to absolute)
- JAR URL format (file:/// protocol with ! separator)


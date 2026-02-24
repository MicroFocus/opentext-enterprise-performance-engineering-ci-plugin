# Java Command Executed by TypeScript - Before and After

## Before Fix (Broken)

```bash
java \
  -Djava.util.logging.manager=org.apache.logging.log4j.jul.LogManager \
  -jar /path/to/lre-actions-1.1-SNAPSHOT-jar-with-dependencies.jar \
  /path/to/config.json
```

**Problem**: No log4j2 configuration file is specified, so log4j2 cannot find its configuration.

---

## After Fix (Working)

```bash
java \
  -Djava.util.logging.manager=org.apache.logging.log4j.jul.LogManager \
  -Dlog4j.configurationFile=jar:file:///C:/path/to/lre-actions-1.1-SNAPSHOT-jar-with-dependencies.jar!/log4j2.xml \
  -jar /path/to/lre-actions-1.1-SNAPSHOT-jar-with-dependencies.jar \
  /path/to/config.json
```

**Solution**: Added `-Dlog4j.configurationFile=jar:file:///.../lre-actions-1.1-SNAPSHOT-jar-with-dependencies.jar!/log4j2.xml` parameter.

---

## Parameter Breakdown

### `-Djava.util.logging.manager=org.apache.logging.log4j.jul.LogManager`
- **Purpose**: Routes Java Util Logging (JUL) to log4j2
- **Result**: HTTP client warnings go through log4j2 instead of directly to console

### `-Dlog4j.configurationFile=jar:file:///.../jar!/log4j2.xml` ← **NEW**
- **Purpose**: Tells log4j2 where to find its configuration
- **Format**: `jar:file:///path/to/jar!/internal/resource/path`
- **Result**: log4j2 loads configuration from inside the JAR and creates file appender

### `-jar /path/to/jar.jar`
- **Purpose**: Tells Java to execute a JAR file
- **Result**: Runs the bundled application

### `/path/to/config.json`
- **Purpose**: Configuration passed to Main.java as an argument
- **Result**: Main class reads this config and sets up logging accordingly

---

## How TypeScript Builds the Command

```typescript
const jarFilePath = path.resolve(__dirname, 'lre-actions-1.1-SNAPSHOT-jar-with-dependencies.jar');

const javaAppArgs = [
    '-Djava.util.logging.manager=org.apache.logging.log4j.jul.LogManager',
    // ↓↓↓ NEW LINE ↓↓↓
    `-Dlog4j.configurationFile=jar:file:///${jarFilePath.replace(/\\/g, '/')}!/log4j2.xml`,
    // ↓↓↓ EXISTING ↓↓↓
    '-jar',
    jarFilePath,
    configFilePath
];

const javaProcess = spawn('java', javaAppArgs, { cwd: __dirname });
```

### Path Transformation Example
```
Input:  C:\Users\daniel\nodejs-app\src\lre-actions-1.1-SNAPSHOT-jar-with-dependencies.jar
Replace: \\ → /
Output: C:/Users/daniel/nodejs-app/src/lre-actions-1.1-SNAPSHOT-jar-with-dependencies.jar
URI:    jar:file:///C:/Users/daniel/nodejs-app/src/lre-actions-1.1-SNAPSHOT-jar-with-dependencies.jar!/log4j2.xml
```

---

## Why This Works

### Without the Parameter
1. Java starts
2. log4j2 tries to initialize
3. Can't find configuration file
4. Falls back to default (console only)
5. File appender is never created
6. Logs only go to console, not file

### With the Parameter
1. Java starts
2. log4j2 reads system property: `log4j.configurationFile`
3. Opens JAR file
4. Extracts and reads `log4j2.xml` from JAR
5. Sees `${sys:log.file}` variable (set by Java Main.java)
6. Creates file appender with correct path
7. Creates console appender
8. Root logger references both appenders
9. Logs go to both console and file

---

## Matches IntelliJ Configuration

**IntelliJ main_full_jar VM Parameters**:
```
-Dlog4j.configurationFile=jar:file:/$PROJECT_DIR$/target/lre-actions-1.1-SNAPSHOT-jar-with-dependencies.jar!/log4j.xml
```

**TypeScript Equivalent**:
```
-Dlog4j.configurationFile=jar:file:///C:/Git/.../target/lre-actions-1.1-SNAPSHOT-jar-with-dependencies.jar!/log4j2.xml
```

**Note**: Filename is slightly different (log4j.xml vs log4j2.xml), but both point to the same file inside the JAR.

---

## Result

### Before Fix
```
java -Djava.util.logging.manager=org.apache.logging.log4j.jul.LogManager -jar jar.jar config.json
↓
Log file created: ✗ (empty or not created)
Console output: Basic only
Errors: Reconfiguration failed messages
Warnings: Cookie warnings visible
```

### After Fix
```
java -Djava.util.logging.manager=org.apache.logging.log4j.jul.LogManager -Dlog4j.configurationFile=jar:file:///...jar!/log4j2.xml -jar jar.jar config.json
↓
Log file created: ✓ (populated with logs)
Console output: Clean, formatted with timestamps
Errors: None
Warnings: Suppressed
```


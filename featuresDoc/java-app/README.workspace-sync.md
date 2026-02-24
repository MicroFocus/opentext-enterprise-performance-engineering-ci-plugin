# Workspace Sync Task

This task authenticates to an LRE server, scans a workspace for script folders, zips each script folder, and uploads the zip to the same relative path in the LRE project.

## What counts as a script folder
A script folder is a folder that directly contains at least one file with one of these extensions:
- `.usr`
- `.jmx`

When a script folder is found, the scan does not continue into its subfolders.

## Config
Create a JSON file with the following keys:

```json
{
  "lre_server": "lre.example.com",
  "lre_port": "443",
  "lre_tenant": "tenant-id",
  "lre_https": true,
  "lre_username": "user",
  "lre_password": "pass",
  "lre_domain": "DOMAIN",
  "lre_project": "PROJECT",
  "lre_proxy_url": "http://proxy.example.com:8080",
  "lre_proxy_username": "proxy-user",
  "lre_proxy_password": "proxy-pass",
  "workspace_path": "C:/work/my-repo"
}
```

Notes:
- `lre_port` is optional. If omitted, `lre_server` is used as-is.
- `lre_tenant` is appended to the server as `/LoadTest/<tenant>` if not already present.

## Run

```powershell
mvn -q -DskipTests package
java -cp target/lre-actions-1.1-SNAPSHOT-jar-with-dependencies.jar com.opentext.lre.actions.workspacesync.LreWorkspaceSyncRunner path\to\config.json
```

## Behavior details
- Each script folder is zipped into `<folderName>.zip` alongside the folder.
- The zip file is uploaded to `Subject\<relative\path>` in the LRE project.
- The zip is deleted locally after each upload attempt.


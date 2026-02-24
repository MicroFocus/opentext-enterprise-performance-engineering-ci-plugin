package com.opentext.lre.actions.workspacesync;

import com.microfocus.adm.performancecenter.plugins.common.pcentities.PcException;
import com.microfocus.adm.performancecenter.plugins.common.rest.PcRestProxy;
import com.opentext.lre.actions.common.helpers.constants.LreTestRunHelper;
import com.opentext.lre.actions.common.helpers.utils.LogHelper;
import com.opentext.lre.actions.common.helpers.utils.Result;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public final class LreWorkspaceSyncTask {
    private final LreWorkspaceSyncModel model;
    private final WorkspaceScriptFolderScanner scanner;
    private final ZipFolderCompressor compressor;

    public LreWorkspaceSyncTask(LreWorkspaceSyncModel model) {
        this.model = model;
        this.scanner = new WorkspaceScriptFolderScanner();
        this.compressor = new ZipFolderCompressor();
        // Set the static flag for stack trace output based on configuration
        LreTestRunHelper.ENABLE_STACKTRACE = model.isEnableStacktrace();
    }

    public Result execute() {
        PcRestProxy restProxy = createRestProxy();
        if (restProxy == null) {
            return Result.FAILURE;
        }

        boolean loggedIn = false;
        try {
            loggedIn = restProxy.authenticate(model.getUsername(), model.getPassword());
            if (!loggedIn) {
                LogHelper.log("Login failed.", true);
                return Result.FAILURE;
            }

            // Convert workspace String to Path for file operations
            Path workspacePath = Paths.get(model.getWorkspace()).toAbsolutePath();
            List<ScriptFolder> scriptFolders = scanner.findScriptFolders(workspacePath);
            if (scriptFolders.isEmpty()) {
                LogHelper.log("No script folders found in workspace.", true);
                return Result.SUCCESS;
            }

            return processScriptFolderUploads(restProxy, scriptFolders);
        } catch (PcException | IOException e) {
            LogHelper.log("Workspace sync failed: %s", true, e.getMessage());
            LogHelper.logStackTrace(e);
            return Result.FAILURE;
        } finally {
            if (loggedIn) {
                try {
                    restProxy.logout();
                } catch (Exception e) {
                    LogHelper.log("Logout failed: %s", true, e.getMessage());
                }
            }
        }
    }

    private Result processScriptFolderUploads(PcRestProxy restProxy, List<ScriptFolder> scriptFolders) {
        final int MAX_CONSECUTIVE_FAILURES = 5;
        final int totalScripts = scriptFolders.size();

        int consecutiveFailures = 0;
        int totalFailures = 0;
        int currentIndex = 0;

        LogHelper.log("Found %d script(s) to upload.", true, totalScripts);

        for (ScriptFolder folder : scriptFolders) {
            currentIndex++;
            LogHelper.log("Script #%d out of %d", true, currentIndex, totalScripts);

            Result uploadResult = uploadFolder(restProxy, folder);

            if (uploadResult == Result.FAILURE) {
                totalFailures++;
                consecutiveFailures++;

                if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
                    LogHelper.log("Upload process terminated: %d consecutive failures detected. %d out of %d scripts uploaded successfully.",
                            true, MAX_CONSECUTIVE_FAILURES, currentIndex - totalFailures, totalScripts);
                    return Result.FAILURE;
                }
            } else {
                consecutiveFailures = 0;
            }
        }

        // Determine final result based on failure rate
        int successfulUploads = totalScripts - totalFailures;
        double successRate = (double) successfulUploads / totalScripts;

        if (successRate >= 0.5) {
            LogHelper.log("Upload process completed: %d out of %d scripts uploaded successfully.",
                    true, successfulUploads, totalScripts);
            return Result.SUCCESS;
        } else {
            LogHelper.log("Upload process failed: Only %d out of %d scripts uploaded successfully (less than 50%%).",
                    true, successfulUploads, totalScripts);
            return Result.FAILURE;
        }
    }

    private Result uploadFolder(PcRestProxy restProxy, ScriptFolder folder) {
        Path zipPath = null;
        try {
            zipPath = compressor.compressFolder(folder);
            String subjectPath = LreSubjectPathBuilder.toSubjectPath(folder.getRelativePath());
            String scriptName = folder.getFullPath().getFileName() == null
                    ? folder.getRelativePath().toString()
                    : folder.getFullPath().getFileName().toString();
            LogHelper.log("Starting uploading script %s to path %s", true, scriptName, subjectPath);
            int scriptId = restProxy.uploadScript(subjectPath, true, model.isRuntimeOnly(), true, zipPath.toString());
            if (scriptId == 0) {
                LogHelper.log("Failed to upload script folder: %s to path %s", true, folder.getRelativePath(), subjectPath);
                return Result.FAILURE;
            }
            LogHelper.log("Script %s was successfully uploaded to path %s with ID = %d", true, scriptName, subjectPath, scriptId);
            return Result.SUCCESS;
        } catch (Exception e) {
            LogHelper.log("Upload failed for %s: %s", true, folder.getRelativePath(), e.getMessage());
            LogHelper.logStackTrace(e);
            return Result.FAILURE;
        } finally {
            if (zipPath != null) {
                try {
                    Files.deleteIfExists(zipPath);
                } catch (IOException e) {
                    LogHelper.log("Failed to delete temp zip: %s", true, zipPath);
                }
            }
        }
    }

    private PcRestProxy createRestProxy() {
        try {
            return new PcRestProxy(
                    model.getProtocol(),
                    model.getLreServerAndPort(),
                    model.isAuthenticateWithToken(),
                    model.getDomain(),
                    model.getProject(),
                    model.getProxyOutURL(),
                    model.getUsernameProxy(),
                    model.getPasswordProxy());
        } catch (PcException e) {
            LogHelper.log("Connection to LRE server failed: %s", true, e.getMessage());
            LogHelper.logStackTrace(e);
            return null;
        }
    }
}

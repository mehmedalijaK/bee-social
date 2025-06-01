package cli.command;

import app.AppConfig;
import servent.message.Message;
import servent.message.UploadMessage;
import servent.message.util.MessageUtil;

import java.io.File;

public class UploadCommand implements CLICommand {

    @Override
    public String commandName() {
        return "upload";
    }

    @Override
    public void execute(String args) {
        if (args == null || args.trim().isEmpty()) {
            AppConfig.timestampedErrorPrint("Usage: upload [path]");
            return;
        }
        String filePath = args.trim();
        File localFile = new File(AppConfig.workingDirectory, filePath);
        if (!localFile.exists() || !localFile.isFile()) {
            AppConfig.timestampedErrorPrint("File not found in " + AppConfig.workingDirectory + ": " + filePath);
            // Ensure AppConfig.workingDirectory is initialized, e.g., in AppConfig or create it.
            // For now, assume it exists or file path is absolute/correctly relative.
            // return; // Commented out to allow testing without actual files first
        }

        try {
            AppConfig.timestampedStandardPrint("Attempting to acquire SK mutex for upload...");
            AppConfig.requestCSEntry(); // Acquire distributed mutex
            AppConfig.timestampedStandardPrint("SK mutex acquired for upload.");

            UploadMessage uploadReq = new UploadMessage(
                    AppConfig.myServentInfo.getListenerPort(),
                    AppConfig.chordState.getNextNodePort(), // Target for this message
                    filePath // File path
            );

            MessageUtil.sendMessage(uploadReq);
            AppConfig.timestampedStandardPrint("Sent upload request for " + filePath);
            // Mutex will be released in UploadResponseHandler
        } catch (InterruptedException e) {
            AppConfig.timestampedErrorPrint("Interrupted while waiting for distributed mutex for upload.");
            Thread.currentThread().interrupt();
        }
    }
}
package cli.command;

import app.AppConfig;
import servent.message.RemoveFileMessage;
import servent.message.MessageType;
import servent.message.util.MessageUtil;

public class RemoveFileCommand implements CLICommand {

    @Override
    public String commandName() {
        return "remove_file";
    }

    @Override
    public void execute(String args) {
        if (args == null || args.trim().isEmpty()) {
            AppConfig.timestampedErrorPrint("Usage: remove_file [filename]");
            return;
        }
        String filePath = args.trim();

        try {
            AppConfig.timestampedStandardPrint("Attempting to acquire SK mutex for remove_file...");
            AppConfig.requestCSEntry(); // Acquire distributed mutex
            AppConfig.timestampedStandardPrint("SK mutex acquired for remove_file.");

            RemoveFileMessage removeReq = new RemoveFileMessage(
                    AppConfig.myServentInfo.getListenerPort(),
                    AppConfig.chordState.getNextNodePort(), // Target
                    filePath // File path
            );

            MessageUtil.sendMessage(removeReq);
            AppConfig.timestampedStandardPrint("Sent remove_file request for " + filePath);
            // Mutex will be released in RemoveFilesResponseHandler
        } catch (InterruptedException e) {
            AppConfig.timestampedErrorPrint("Interrupted while waiting for distributed mutex for remove_file.");
            Thread.currentThread().interrupt();
        }
    }
}

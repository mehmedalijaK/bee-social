package cli.command;

import app.AppConfig;
import servent.message.ListFilesMessage;
import servent.message.MessageType;
import servent.message.util.MessageUtil;

public class ListFilesCommand implements CLICommand {

    @Override
    public String commandName() {
        return "list_files";
    }

    @Override
    public void execute(String args) {
        if (args == null || args.trim().isEmpty()) {
            AppConfig.timestampedErrorPrint("Usage: list_files [address:port]");
            return;
        }
        String addrPort = args.trim();
        String[] parts = addrPort.split(":");
        if (parts.length != 2) {
            AppConfig.timestampedErrorPrint("Invalid format. Usage: list_files [address:port]");
            return;
        }
        // String ip = parts[0]; // ip not used if assuming localhost for target port
        int port;
        try {
            port = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            AppConfig.timestampedErrorPrint("Invalid port: " + parts[1]);
            return;
        }

        try {
            AppConfig.timestampedStandardPrint("Attempting to acquire SK mutex for list_files...");
            AppConfig.requestCSEntry(); // Acquire distributed mutex
            AppConfig.timestampedStandardPrint("SK mutex acquired for list_files.");

            String myAddrPort = AppConfig.myServentInfo.getIpAddress() + ":"
                    + AppConfig.myServentInfo.getListenerPort();
            ListFilesMessage listFilesReq = new ListFilesMessage(
                    MessageType.LIST_FILES,
                    AppConfig.myServentInfo.getListenerPort(),
                    port,
                    myAddrPort
            );
            MessageUtil.sendMessage(listFilesReq);
            AppConfig.timestampedStandardPrint("Sent list_files request to " + addrPort);
            // Mutex will be released in ListFilesResponseHandler
        } catch (InterruptedException e) {
            AppConfig.timestampedErrorPrint("Interrupted while waiting for distributed mutex for list_files.");
            Thread.currentThread().interrupt();
        }
    }
}
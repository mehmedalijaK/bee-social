package servent.handler;

import app.AppConfig;
import servent.message.Message;
import servent.message.MessageType;

public class ListFilesResponseHandler implements MessageHandler{
    private Message clientMessage;

    public ListFilesResponseHandler(Message clientMessage) {
        this.clientMessage = clientMessage;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() != MessageType.LIST_FILES_RESPONSE) {
            AppConfig.timestampedErrorPrint("LIST_FILES_RESPONSE handler got wrong message type.");
            return;
        }

        String filesString = clientMessage.getMessageText();
        if (filesString == null || filesString.isEmpty()) {
            AppConfig.timestampedStandardPrint("Nema dostupnih fajlova ili je lista prazna.");
        } else {
            String[] fileNames = filesString.split(",");
            AppConfig.timestampedStandardPrint("Primljena lista fajlova:");
            for (String fname : fileNames) {
                AppConfig.timestampedStandardPrint("  - " + fname);
            }
        }
        // Release distributed mutex
        AppConfig.timestampedStandardPrint("Releasing SK mutex after list_files response.");
        AppConfig.releaseCSEntry();
    }
}
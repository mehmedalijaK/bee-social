package servent.handler;

import app.AppConfig;
import servent.message.Message;
import servent.message.MessageType;

public class UploadResponseHandler implements MessageHandler {

    private Message clientMessage;

    public UploadResponseHandler(Message clientMessage) {
        this.clientMessage = clientMessage;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() != MessageType.UPLOAD_RESPONSE) {
            AppConfig.timestampedErrorPrint("UPLOAD_RESPONSE handler got something that is not UPLOAD_RESPONSE.");
            return;
        }

        String responseText = clientMessage.getMessageText();
        AppConfig.timestampedStandardPrint("Stigao UPLOAD_RESPONSE: " + responseText);

        // Release distributed mutex
        AppConfig.timestampedStandardPrint("Releasing SK mutex after upload response.");
        AppConfig.releaseCSEntry();
    }
}
package servent.handler;

import app.AppConfig;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.UploadResponseMessage;
import servent.message.util.MessageUtil;

public class UploadResponseHandler implements MessageHandler {

    private Message clientMessage;

    public UploadResponseHandler(Message clientMessage) {
        this.clientMessage = clientMessage;
    }

    @Override
    public void run() {
        // 1) Proverimo da li je stvarno UPLOAD_RESPONSE
        if (clientMessage.getMessageType() != MessageType.UPLOAD_RESPONSE) {
            AppConfig.timestampedErrorPrint("UPLOAD_RESPONSE handler got something that is not UPLOAD_RESPONSE.");
            return;
        }

        // 2) U payload-u (messageText) očekujemo npr. "OK:relative/path.jpg"
        String responseText = clientMessage.getMessageText();
        AppConfig.timestampedStandardPrint("Stigao UPLOAD_RESPONSE: " + responseText);

        // 3) Otključavamo distribuisani mutex, jer je upload završio
        AppConfig.mutex.unlock();
    }
}
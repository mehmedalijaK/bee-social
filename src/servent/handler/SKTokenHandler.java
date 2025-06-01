package servent.handler;

import app.AppConfig;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.SKTokenMessage;

import java.util.Arrays;
import java.util.LinkedList;

public class SKTokenHandler implements MessageHandler {

    private final Message clientMessage;

    public SKTokenHandler(Message clientMessage) {
        this.clientMessage = clientMessage;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() == MessageType.SK_TOKEN) {
            SKTokenMessage tokenMsg = (SKTokenMessage) clientMessage;

            AppConfig.skMutex.lock();
            try {
                AppConfig.hasToken = true;
                // It's crucial to copy the arrays/collections from the message
                // to avoid aliasing if the message object holds references.
                // SKTokenMessage constructor already makes copies if needed.
                AppConfig.LN_token = Arrays.copyOf(tokenMsg.getLN(), tokenMsg.getLN().length);
                AppConfig.Q_token = new LinkedList<>(tokenMsg.getQ()); // Assuming LinkedList or similar for the queue

                AppConfig.timestampedStandardPrint("SK: Servent " + AppConfig.myServentId + " received TOKEN. " +
                        "LN_token: " + Arrays.toString(AppConfig.LN_token) +
                        ", Q_token: " + AppConfig.Q_token.toString());

                if (AppConfig.waitingForToken) {
                    AppConfig.timestampedStandardPrint("SK: Servent " + AppConfig.myServentId + " was waiting. Signaling token arrival.");
                    AppConfig.skTokenArrivedCondition.signal();
                }
            } finally {
                AppConfig.skMutex.unlock();
            }
        } else {
            AppConfig.timestampedErrorPrint("SKTokenHandler got a message that is not SK_TOKEN");
        }
    }
}

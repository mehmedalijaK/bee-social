package servent.handler;

import app.AppConfig;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.SKTokenRequestMessage;

import java.util.Arrays;

public class SKTokenRequestHandler implements MessageHandler {

    private final Message clientMessage;

    public SKTokenRequestHandler(Message clientMessage) {
        this.clientMessage = clientMessage;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() == MessageType.SK_TOKEN_REQUEST) {
            SKTokenRequestMessage requestMsg = (SKTokenRequestMessage) clientMessage;
            int requestingNodeId = requestMsg.getRequesterId();
            int sequenceNum = requestMsg.getSequenceNumber();

            AppConfig.skMutex.lock();
            try {
                AppConfig.timestampedStandardPrint("SK: Servent " + AppConfig.myServentId +
                        " received TOKEN_REQUEST from servent " + requestingNodeId +
                        " with SN=" + sequenceNum + ". Current RN: " + Arrays.toString(AppConfig.RN));

                AppConfig.RN[requestingNodeId] = Math.max(AppConfig.RN[requestingNodeId], sequenceNum);
                AppConfig.timestampedStandardPrint("SK: Updated RN[" + requestingNodeId + "] to " + AppConfig.RN[requestingNodeId]);

                if (AppConfig.hasToken && !AppConfig.inCriticalSection &&
                        AppConfig.RN[requestingNodeId] == AppConfig.LN_token[requestingNodeId] + 1) {

                    AppConfig.timestampedStandardPrint("SK: Servent " + AppConfig.myServentId +
                            " has token, not in CS, and request is next. Sending token to " + requestingNodeId);
                    AppConfig.sendToken(requestingNodeId);
                } else {
                    AppConfig.timestampedStandardPrint("SK: Servent " + AppConfig.myServentId +
                            " will not send token now. hasToken=" + AppConfig.hasToken +
                            ", inCS=" + AppConfig.inCriticalSection +
                            ", RN[" + requestingNodeId + "]=" + AppConfig.RN[requestingNodeId] +
                            ", LN_token[" + requestingNodeId + "]=" + (AppConfig.LN_token != null ? AppConfig.LN_token[requestingNodeId] : "N/A"));
                }
            } finally {
                AppConfig.skMutex.unlock();
            }
        } else {
            AppConfig.timestampedErrorPrint("SKTokenRequestHandler got a message that is not SK_TOKEN_REQUEST");
        }
    }
}
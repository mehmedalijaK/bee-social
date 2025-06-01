package servent.handler;

import app.AppConfig;
import app.ServentInfo;
import servent.message.Message;
import servent.message.MessageType;

public class FollowHandler implements MessageHandler {

    private Message clientMessage;

    public FollowHandler(Message clientMessage) {
        this.clientMessage = clientMessage;
    }

    // Dobijam poruku i procesuiram follow req (treba cvor da me prati)
    @Override
    public void run() {
        if (clientMessage.getMessageType() != MessageType.FOLLOW) {
            AppConfig.timestampedErrorPrint(
                    "FOLLOW handler got unexpected message type: " + clientMessage.getMessageType()
            );
            return;
        }

        int senderPort = clientMessage.getSenderPort();

        ServentInfo requester = new ServentInfo("localhost", senderPort);

        AppConfig.chordState.addPendingFollower(requester);

        AppConfig.timestampedStandardPrint(
                "Added pending follow request from: " + requester.getIpAddress() + ":" + requester.getListenerPort()
        );
    }
}
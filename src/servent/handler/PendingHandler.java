package servent.handler;

import app.AppConfig;
import app.ServentInfo;
import servent.message.Message;
import servent.message.MessageType;

import java.util.List;
import java.util.Map;

/**
 * PendingHandler is invoked kada korisnik ukuca "pending" komandu.
 * Prikazuje sve čvorove koji su poslali FOLLOW zahtev, ali još nisu prihvaćeni.
 */
public class PendingHandler implements MessageHandler {

    private Message clientMessage;

    public PendingHandler(Message clientMessage) {
        this.clientMessage = clientMessage;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() == MessageType.PENDING) {
            List<ServentInfo> pending = AppConfig.chordState.getPendingFollowers();
            if (pending.isEmpty()) {
                AppConfig.timestampedStandardPrint("No pending follow requests.");
            } else {
                AppConfig.timestampedStandardPrint("Pending follow requests from:");
                for (ServentInfo requester : pending) {
                    AppConfig.timestampedStandardPrint(
                            " - " + requester.getIpAddress() + ":" + requester.getListenerPort()
                    );
                }
            }
        } else {
            AppConfig.timestampedErrorPrint(
                    "PENDING handler got unexpected message type: " + clientMessage.getMessageType());
        }
    }
}
package servent.handler;

import app.AppConfig;
import app.ServentInfo;
import servent.message.AcceptMessage;
import servent.message.FollowMessage;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.util.MessageUtil;

import java.util.List;

/**
 * AcceptHandler procesuira lokalnu ACCEPT komandu.
 * Premesta zahtev iz pendingFollowers u followers i salje potvrdnu poruku.
 */
public class AcceptHandler implements MessageHandler {

    private Message clientMessage;

    public AcceptHandler(Message clientMessage) {
        this.clientMessage = clientMessage;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() == MessageType.ACCEPT) {
            // ReceiverPort u AcceptMessage je port serventa čiji FOLLOW zahtev prihvatamo
            int requesterPort = clientMessage.getReceiverPort();

            // 2) Pronađemo odgovarajući ServentInfo u ChordState.getPendingFollowers()
            List<ServentInfo> pendingList = AppConfig.chordState.getPendingFollowers();
            ServentInfo foundInfo = null;
            for (ServentInfo si : pendingList) {
                if (si.getListenerPort() == requesterPort) {
                    foundInfo = si;
                    break;
                }
            }

            if (foundInfo == null) {
                // Nema takvog pending follow zahteva
                AppConfig.timestampedErrorPrint(
                        "No pending follow request from port " + requesterPort
                );
                return;
            }

            // 3) Premestimo ga iz pending u confirmed followers
            AppConfig.chordState.acceptFollower(foundInfo);
            AppConfig.timestampedStandardPrint(
                    "Accepted follow request from: " + foundInfo.getIpAddress() + ":"
                            + foundInfo.getListenerPort()
            );

            // 4) Pošaljemo ACCEPT poruku nazad originalnom pošiljaocu (requesterPort)
            Message response = new AcceptMessage(
                    MessageType.ACCEPT,
                    AppConfig.myServentInfo.getListenerPort(), // sender = ovaj nod
                    requesterPort,                             // receiver = onaj ko je tražio FOLLOW
                    ""  // dodaj neku finu poruku
            );
            MessageUtil.sendMessage(response);
        }
    }
}
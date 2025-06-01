package servent.handler;

import app.AppConfig;
import servent.message.LeaveMessage;
import servent.message.Message;
import servent.message.MessageType;

import app.ServentInfo;
import servent.message.util.MessageUtil;


public class StopHandler implements MessageHandler {

    private Message clientMessage;

    public StopHandler(Message clientMessage) {
        this.clientMessage = clientMessage;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() != MessageType.STOP) {
            AppConfig.timestampedErrorPrint("STOP handler got something that is not STOP.");
            return;
        }

        AppConfig.timestampedStandardPrint("Received STOP. Initiating graceful leave...");

        ServentInfo myInfo = AppConfig.myServentInfo;
        ServentInfo predecessor = AppConfig.chordState.getPredecessor();

        ServentInfo successor = null;
        ServentInfo[] succTable = AppConfig.chordState.getSuccessorTable();
        if (succTable.length > 0 && succTable[0] != null) {
            successor = succTable[0];
            // proveravamo da li successor zapravo nismo mi sami
            if (successor.getChordId() == myInfo.getChordId()
                    && successor.getListenerPort() == myInfo.getListenerPort()
                    && successor.getIpAddress().equals(myInfo.getIpAddress())) {
                successor = null;
            }
        }

        if (successor != null) {
            Message leaveToSucc = new LeaveMessage(
                    MessageType.LEAVE,
                    myInfo.getListenerPort(),
                    successor.getListenerPort(),
                    predecessor != null
                            ? predecessor.getIpAddress() + ":" + predecessor.getListenerPort()
                            : "null"
            );
            MessageUtil.sendMessage(leaveToSucc);
            AppConfig.timestampedStandardPrint(
                    "Sent LEAVE → successor ("
                            + successor.getIpAddress() + ":" + successor.getListenerPort()
                            + ") with new predecessor="
                            + (predecessor != null
                            ? predecessor.getIpAddress() + ":" + predecessor.getListenerPort()
                            : "null")
            );
        }

        if (predecessor != null) {
            LeaveMessage leaveToPred = new LeaveMessage(
                    MessageType.LEAVE,
                    myInfo.getListenerPort(),
                    predecessor.getListenerPort(),
                    successor != null
                            ? successor.getIpAddress() + ":" + successor.getListenerPort()
                            : "null"
            );
            MessageUtil.sendMessage(leaveToPred);
            AppConfig.timestampedStandardPrint(
                    "Sent LEAVE → predecessor ("
                            + predecessor.getIpAddress() + ":" + predecessor.getListenerPort()
                            + ") with new successor="
                            + (successor != null
                            ? successor.getIpAddress() + ":" + successor.getListenerPort()
                            : "null")
            );
        }

        AppConfig.shutdown();
    }
}

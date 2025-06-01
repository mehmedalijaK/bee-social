package servent.message;

public class LeaveMessage extends BasicMessage {
    public LeaveMessage(MessageType type, int senderPort, int receiverPort, String messageText) {
        super(type, senderPort, receiverPort, messageText);
    }
}
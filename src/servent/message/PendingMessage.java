package servent.message;

public class PendingMessage extends BasicMessage{
    public PendingMessage(MessageType type, int senderPort, int receiverPort, String messageText) {
        super(type, senderPort, receiverPort, messageText);
    }
}

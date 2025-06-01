package servent.message;

public class VisibilityMessage extends BasicMessage{
    public VisibilityMessage(MessageType type, int senderPort, int receiverPort, String messageText) {
        super(type, senderPort, receiverPort, messageText);
    }
}
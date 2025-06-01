package servent.message;

public class AcceptMessage extends BasicMessage{
    public AcceptMessage(MessageType type, int senderPort, int receiverPort, String messageText) {
        super(type, senderPort, receiverPort, messageText);
    }
}

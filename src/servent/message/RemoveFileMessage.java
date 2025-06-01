package servent.message;

public class RemoveFileMessage extends BasicMessage{
    public RemoveFileMessage(MessageType type, int senderPort, int receiverPort, String messageText) {
        super(type, senderPort, receiverPort, messageText);
    }
}

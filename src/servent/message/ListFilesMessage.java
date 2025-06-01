package servent.message;

public class ListFilesMessage extends BasicMessage{
    public ListFilesMessage(MessageType type, int senderPort, int receiverPort, String messageText) {
        super(type, senderPort, receiverPort, messageText);
    }
}
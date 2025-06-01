package servent.message;

public class ListFilesResponseMessage extends BasicMessage {

    public ListFilesResponseMessage(MessageType type, int senderPort, int receiverPort, String messageText) {
        super(type, senderPort, receiverPort, messageText);
    }
}
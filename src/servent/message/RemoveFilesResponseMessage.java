package servent.message;

public class RemoveFilesResponseMessage extends BasicMessage {

    public RemoveFilesResponseMessage(MessageType type, int senderPort, int receiverPort, String messageText) {
        super(type, senderPort, receiverPort, messageText);
    }

}
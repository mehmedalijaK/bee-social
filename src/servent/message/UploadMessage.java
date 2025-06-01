package servent.message;

public class UploadMessage extends BasicMessage {

    // Convenience constructor: defaults the message type to UPLOAD
    public UploadMessage(int senderPort, int receiverPort, String messageText) {
        super(MessageType.UPLOAD, senderPort, receiverPort, messageText);
    }

    // Full‐arg constructor (in case you ever need a different MessageType)
    public UploadMessage(MessageType type, int senderPort, int receiverPort, String messageText) {
        super(type, senderPort, receiverPort, messageText);
    }
}
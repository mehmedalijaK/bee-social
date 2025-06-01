package servent.message;

public class UploadResponseMessage extends BasicMessage {

    // Convenience constructor: defaults the message type to UPLOAD_RESPONSE
    public UploadResponseMessage(int senderPort, int receiverPort, String messageText) {
        super(MessageType.UPLOAD_RESPONSE, senderPort, receiverPort, messageText);
    }

    // Full‐arg constructor (if you ever need to specify a different MessageType)
    public UploadResponseMessage(MessageType type, int senderPort, int receiverPort, String messageText) {
        super(type, senderPort, receiverPort, messageText);
    }
}

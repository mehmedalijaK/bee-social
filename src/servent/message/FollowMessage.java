package servent.message;

public class FollowMessage extends BasicMessage{
    public FollowMessage(MessageType type, int senderPort, int receiverPort, String messageText) {
        super(type, senderPort, receiverPort, messageText);
    }
}

package servent.message;

public class SKTokenRequestMessage extends BasicMessage {
    private static final long serialVersionUID = 1L; // Example version UID
    private final int senderId;
    private final int sequenceNumber;

    public SKTokenRequestMessage(int senderPort, int receiverPort, int senderId, int sequenceNumber) {
        super(MessageType.SK_TOKEN_REQUEST, senderPort, receiverPort,
                "SK_REQ: id=" + senderId + ", sn=" + sequenceNumber);
        this.senderId = senderId;
        this.sequenceNumber = sequenceNumber;
    }

    public int getRequesterId() {
        return senderId;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }
}
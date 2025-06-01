package servent.message;

public class UploadMessage extends BasicMessage {
    private static final long serialVersionUID = 3L;
    private final int originalRequesterPort;
    private final String filePath;

    // Used by CLI command
    public UploadMessage(int senderPort, int receiverPort, String filePath) {
        super(MessageType.UPLOAD, senderPort, receiverPort, ""); // Message text not used for path now
        this.originalRequesterPort = senderPort; // Initial sender is the original requester
        this.filePath = filePath;
    }

    // Used by handler when forwarding
    public UploadMessage(int senderPort, int receiverPort, int originalRequesterPort, String filePath) {
        super(MessageType.UPLOAD, senderPort, receiverPort, "");
        this.originalRequesterPort = originalRequesterPort;
        this.filePath = filePath;
    }

    public int getOriginalRequesterPort() { return originalRequesterPort; }
    public String getFilePath() { return filePath; }

    @Override
    public String getMessageText() {
        // For consistent logging, or if BasicMessage's text is still used somewhere
        return "UPLOAD_REQ: origPort=" + originalRequesterPort + ", path=" + filePath;
    }
}

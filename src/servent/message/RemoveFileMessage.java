package servent.message;

public class RemoveFileMessage extends BasicMessage {
    private static final long serialVersionUID = 4L;
    private final int originalRequesterPort;
    private final String filePath;

    // Used by CLI command
    public RemoveFileMessage(int senderPort, int receiverPort, String filePath) {
        super(MessageType.REMOVE_FILES, senderPort, receiverPort, "");
        this.originalRequesterPort = senderPort;
        this.filePath = filePath;
    }

    // Used by handler when forwarding
    public RemoveFileMessage(int senderPort, int receiverPort, int originalRequesterPort, String filePath) {
        super(MessageType.REMOVE_FILES, senderPort, receiverPort, "");
        this.originalRequesterPort = originalRequesterPort;
        this.filePath = filePath;
    }

    public int getOriginalRequesterPort() { return originalRequesterPort; }
    public String getFilePath() { return filePath; }

    @Override
    public String getMessageText() {
        return "REMOVE_REQ: origPort=" + originalRequesterPort + ", path=" + filePath;
    }
}
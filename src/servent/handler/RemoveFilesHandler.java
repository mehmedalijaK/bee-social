package servent.handler;

import app.AppConfig;
import app.ChordState;
import app.ServentInfo;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.RemoveFileMessage;
import servent.message.RemoveFilesResponseMessage;
import servent.message.util.MessageUtil;

import java.io.File;

public class RemoveFilesHandler implements MessageHandler {

    private Message clientMessage;

    public RemoveFilesHandler(Message clientMessage) {
        this.clientMessage = clientMessage;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() != MessageType.REMOVE_FILES) {
            AppConfig.timestampedErrorPrint("REMOVE_FILES handler got something that is not REMOVE_FILES.");
            return;
        }

        // Cast to the correct message type to access specific fields
        RemoveFileMessage currentRemoveMsg = (RemoveFileMessage) clientMessage;
        int originalRequesterActualPort = currentRemoveMsg.getOriginalRequesterPort();
        String relativeFilePath = currentRemoveMsg.getFilePath();

        // Local mutex is removed. Distributed Suzuki-Kasami mutex is handled by the original requester.

        try {
            // Calculate Chord key based on the relative file path
            int key = ChordState.chordHash(relativeFilePath.hashCode());
            boolean iStoreIt = AppConfig.chordState.isKeyMine(key);

            if (iStoreIt) {
                // This node is responsible for the file
                AppConfig.timestampedStandardPrint(
                        "Servent " + AppConfig.myServentInfo.getChordId() + " is responsible for file \"" +
                                relativeFilePath + "\" (key=" + key + "). Attempting to remove."
                );

                // Simulate actual file removal from servent-specific directory
                File serventDir = new File(AppConfig.workingDirectory);
                File targetFile = new File(serventDir, relativeFilePath); // Look inside servent-specific dir

                boolean deleted = false;
                String deleteStatusMessage;

                if (targetFile.exists()) {
                    if (targetFile.delete()) {
                        deleted = true;
                        deleteStatusMessage = "Successfully removed file: " + targetFile.getAbsolutePath();
                    } else {
                        deleteStatusMessage = "Failed to remove file (delete operation failed): " + targetFile.getAbsolutePath();
                    }
                } else {
                    deleteStatusMessage = "File not found for removal: " + targetFile.getAbsolutePath();
                    // Depending on requirements, not finding a file might still be an "OK" for removal
                    // or it could be a specific type of failure/warning.
                    // For now, we'll consider it "not found or failed" for the response.
                }
                AppConfig.timestampedStandardPrint(deleteStatusMessage);


                if (deleted) {
                    // Simulate triggering removal of replicas from successor and predecessor
                    // TODO: Implement actual replication removal message sending
                    ServentInfo successor = AppConfig.chordState.getSuccessorTable()[0];
                    ServentInfo predecessor = AppConfig.chordState.getPredecessor();

                    if (successor != null && successor.getChordId() != AppConfig.myServentInfo.getChordId()) {
                        AppConfig.timestampedStandardPrint("Simulating trigger removal of replica " + relativeFilePath + " from successor: " + successor);
                    }
                    if (predecessor != null && predecessor.getChordId() != AppConfig.myServentInfo.getChordId()) {
                        AppConfig.timestampedStandardPrint("Simulating trigger removal of replica " + relativeFilePath + " from predecessor: " + predecessor);
                    }
                }

                // Send response back to the original requester
                String responsePayload = (deleted ? "OK:" : "FAIL:") + relativeFilePath;
                RemoveFilesResponseMessage response = new RemoveFilesResponseMessage(
                        MessageType.REMOVE_FILE_RESPONSE,
                        AppConfig.myServentInfo.getListenerPort(),
                        originalRequesterActualPort, // Send response to the original requester
                        responsePayload
                );
                MessageUtil.sendMessage(response);

            } else {
                // This node is NOT responsible, forward the request
                ServentInfo nextNode = AppConfig.chordState.getNextNodeForKey(key);
                AppConfig.timestampedStandardPrint(
                        "Servent " + AppConfig.myServentInfo.getChordId() + " is NOT responsible for file \"" +
                                relativeFilePath + "\" (key=" + key + "). Forwarding to " + nextNode
                );

                // Use the correct constructor for RemoveFileMessage to propagate original requester info
                RemoveFileMessage forwardMsg = new RemoveFileMessage(
                        AppConfig.myServentInfo.getListenerPort(),   // Current node is sender of this forwarded message
                        nextNode.getListenerPort(),                  // Target is the next node
                        originalRequesterActualPort,                 // Propagate original requester's port
                        relativeFilePath                             // Propagate the file path
                );
                MessageUtil.sendMessage(forwardMsg);
            }
        } catch (Exception e) {
            // Catch any unexpected errors during processing
            AppConfig.timestampedErrorPrint("Unexpected error in RemoveFilesHandler for file " + relativeFilePath + ": " + e.getMessage());
            e.printStackTrace();
            // Attempt to send a generic failure message back to the original requester if possible
            RemoveFilesResponseMessage response = new RemoveFilesResponseMessage(
                    MessageType.REMOVE_FILE_RESPONSE,
                    AppConfig.myServentInfo.getListenerPort(),
                    originalRequesterActualPort,
                    "FAIL:Unexpected error during removal of " + relativeFilePath
            );
            MessageUtil.sendMessage(response);
        }
    }
}
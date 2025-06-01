package servent.handler;

import app.AppConfig;
import app.ChordState;
import app.ServentInfo;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.UploadMessage;
import servent.message.UploadResponseMessage;
import servent.message.util.MessageUtil;

import java.io.File;
import java.io.IOException;

public class UploadHandler implements MessageHandler {

    private Message clientMessage;

    public UploadHandler(Message clientMessage) {
        this.clientMessage = clientMessage;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() != MessageType.UPLOAD) {
            AppConfig.timestampedErrorPrint("UPLOAD handler got something that is not UPLOAD.");
            return;
        }

        // Cast to the correct message type to access specific fields
        UploadMessage currentUploadMsg = (UploadMessage) clientMessage;
        int originalRequesterActualPort = currentUploadMsg.getOriginalRequesterPort();
        String relativeFilePath = currentUploadMsg.getFilePath();

        // Local mutex is removed. Distributed Suzuki-Kasami mutex is handled by the original requester.

        try {
            // Calculate Chord key based on the relative file path
            // Note: Using String.hashCode() for Chord keys is simplistic and can lead to poor distribution.
            // A more robust hashing function (like SHA-1, truncated) would be better in a real system.
            int key = ChordState.chordHash(relativeFilePath.hashCode());
            boolean iStoreIt = AppConfig.chordState.isKeyMine(key);

            if (iStoreIt) {
                // This node is responsible for the file
                AppConfig.timestampedStandardPrint(
                        "Servent " + AppConfig.myServentInfo.getChordId() + " is responsible for file \"" +
                                relativeFilePath + "\" (key=" + key + "). Attempting to store."
                );

                // Simulate actual file storage
                // Create a unique directory for this servent if it doesn't exist
                File serventDir = new File(AppConfig.workingDirectory);
                if (!serventDir.exists()) {
                    if (serventDir.mkdirs()) {
                        AppConfig.timestampedStandardPrint("Created directory for servent " + AppConfig.myServentInfo.getChordId() + ": " + serventDir.getAbsolutePath());
                    } else {
                        AppConfig.timestampedErrorPrint("Failed to create directory for servent " + AppConfig.myServentInfo.getChordId() + ": " + serventDir.getAbsolutePath());
                        // Send a failure response if directory creation fails, as storage will fail
                        UploadResponseMessage response = new UploadResponseMessage(
                                AppConfig.myServentInfo.getListenerPort(),
                                originalRequesterActualPort, // Send response to the original requester
                                "FAIL:Could not create storage directory for " + relativeFilePath
                        );
                        MessageUtil.sendMessage(response);
                        return;
                    }
                }

                File localFile = new File(serventDir, relativeFilePath);
                boolean success = false;
                try {
                    // "Touch" the file to simulate creation/storage
                    if (localFile.getParentFile() != null && !localFile.getParentFile().exists()) {
                        localFile.getParentFile().mkdirs(); // Create parent subdirectories if specified in relativeFilePath
                    }
                    success = localFile.createNewFile(); // Creates the file if it doesn't exist
                    if (success) {
                        AppConfig.timestampedStandardPrint("Successfully stored/created file: " + localFile.getAbsolutePath());
                    } else {
                        // This might mean the file already exists. Overwriting could be an option.
                        // For now, if createNewFile returns false, assume it's okay if it exists.
                        if (localFile.exists()) {
                            AppConfig.timestampedStandardPrint("File already exists, considered stored: " + localFile.getAbsolutePath());
                            success = true; // Treat existing as success for this simulation
                        } else {
                            AppConfig.timestampedErrorPrint("Failed to create file (and it doesn't exist): " + localFile.getAbsolutePath());
                        }
                    }
                } catch (IOException e) {
                    AppConfig.timestampedErrorPrint("IOException during file storage for " + relativeFilePath + ": " + e.getMessage());
                    success = false;
                }

                if (success) {
                    // Simulate replication to successor and predecessor
                    // TODO: Implement actual replication message sending (e.g., ReplicaUploadMessage)
                    ServentInfo successor = AppConfig.chordState.getSuccessorTable()[0];
                    ServentInfo predecessor = AppConfig.chordState.getPredecessor();

                    if (successor != null && successor.getChordId() != AppConfig.myServentInfo.getChordId()) {
                        AppConfig.timestampedStandardPrint("Simulating replication of " + relativeFilePath + " to successor: " + successor);
                        // Example: Message replicaMsgToSucc = new ReplicaUploadMessage(..., successor.getPort(), filePath, fileBytes);
                        // MessageUtil.sendMessage(replicaMsgToSucc);
                    }
                    if (predecessor != null && predecessor.getChordId() != AppConfig.myServentInfo.getChordId()) {
                        AppConfig.timestampedStandardPrint("Simulating replication of " + relativeFilePath + " to predecessor: " + predecessor);
                        // Example: Message replicaMsgToPred = new ReplicaUploadMessage(..., predecessor.getPort(), filePath, fileBytes);
                        // MessageUtil.sendMessage(replicaMsgToPred);
                    }
                }

                // Send response back to the original requester
                String responsePayload = (success ? "OK:" : "FAIL:") + relativeFilePath;
                UploadResponseMessage response = new UploadResponseMessage(
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

                UploadMessage forwardMsg = new UploadMessage(
                        AppConfig.myServentInfo.getListenerPort(),   // Current node is sender of this forwarded message
                        nextNode.getListenerPort(),                  // Target is the next node
                        originalRequesterActualPort,                 // Propagate original requester's port
                        relativeFilePath                             // Propagate the file path
                );
                MessageUtil.sendMessage(forwardMsg);
            }
        } catch (Exception e) {
            // Catch any unexpected errors during processing
            AppConfig.timestampedErrorPrint("Unexpected error in UploadHandler for file " + relativeFilePath + ": " + e.getMessage());
            e.printStackTrace();
            // Attempt to send a generic failure message back to the original requester if possible
            UploadResponseMessage response = new UploadResponseMessage(
                    AppConfig.myServentInfo.getListenerPort(),
                    originalRequesterActualPort,
                    "FAIL:Unexpected error during upload of " + relativeFilePath
            );
            MessageUtil.sendMessage(response);
        }
    }
}
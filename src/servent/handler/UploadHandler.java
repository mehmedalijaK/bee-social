package servent.handler;

import app.AppConfig;
import app.ChordState;
import app.ServentInfo;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.UploadMessage;
import servent.message.UploadResponseMessage;
import servent.message.util.MessageUtil;

import java.util.concurrent.locks.Lock;

public class UploadHandler implements MessageHandler {

    private Message clientMessage;

    public UploadHandler(Message clientMessage) {
        this.clientMessage = clientMessage;
    }

    @Override
    public void run() {
        // 1) Check that we got an UPLOAD message
        if (clientMessage.getMessageType() != MessageType.UPLOAD) {
            AppConfig.timestampedErrorPrint("UPLOAD handler got something that is not UPLOAD.");
            return;
        }

        // 2) Extract the requester’s port
        int requesterPort = clientMessage.getSenderPort();

        // NOTE: Message interface does NOT have getSenderIpAddress().
        //       We typically assume "localhost" for all internal Chord messages.
        ServentInfo requesterInfo = new ServentInfo("localhost", requesterPort);

        // 3) Acquire the distributed mutex
        //    We stored a ReentrantLock in AppConfig, so .lock() does NOT throw InterruptedException.
        Lock mutex = AppConfig.mutex;
        mutex.lock();  // no try/catch needed here

        try {
            // 4) The messageText carries the relative path of the file to upload
            String relativeFilePath = clientMessage.getMessageText().trim();

            // 5) Compute the Chord key for this file
            int key = ChordState.chordHash(relativeFilePath.hashCode());

            // 6) Check if THIS node is responsible for storing 'key'
            boolean iStoreIt = AppConfig.chordState.isKeyMine(key);

            if (iStoreIt) {
                // ─── We are the node responsible ───

                // TODO: actually read the bytes from the UploadMessage (if you embed file‐bytes in it)
                AppConfig.timestampedStandardPrint(
                        "Storing file \"" + relativeFilePath + "\" locally (key=" + key + ")."
                );

                // TODO: replicate to successor and predecessor
                // Example pseudocode:
                // ServentInfo succ = AppConfig.chordState.getSuccessorTable()[0];
                // byte[] fileBytes = ((UploadMessage) clientMessage).getFileBytes();
                // ReplicaUploadMessage succReplica =
                //     new ReplicaUploadMessage(
                //         MessageType.UPLOAD_REPLICA,
                //         AppConfig.myServentInfo.getListenerPort(),
                //         succ.getListenerPort(),
                //         relativeFilePath,
                //         fileBytes
                //     );
                // MessageUtil.sendMessage(succReplica);
                //
                // Likewise for predecessor…

                // 6c) Send back an ACK to the original requester
                UploadResponseMessage response = new UploadResponseMessage(
                        MessageType.UPLOAD_RESPONSE,
                        AppConfig.myServentInfo.getListenerPort(),  // sender = this node
                        requesterPort,                              // receiver = original requester
                        "OK:" + relativeFilePath                     // payload
                );
                MessageUtil.sendMessage(response);
            } else {
                // ─── We are NOT responsible. Forward to the next hop ───

                // 7a) Find the next node in the Chord ring
                ServentInfo nextNode = AppConfig.chordState.getNextNodeForKey(key);

                // 7b) Forward an UPLOAD message to that node:
                UploadMessage forwardMsg = new UploadMessage(
                        MessageType.UPLOAD,                          // message type
                        AppConfig.myServentInfo.getListenerPort(),   // now I am the sender
                        nextNode.getListenerPort(),                  // forward to nextNode
                        relativeFilePath                             // same payload
                        // If UploadMessage carried file‐bytes, include them here as well
                );
                MessageUtil.sendMessage(forwardMsg);

                // 7c) Do NOT unlock here—only unlock once the responsible node replies with UPLOAD_RESPONSE.
                return;
            }
        } finally {
            // 6d) Release the mutex if we actually stored the file ourselves.
            //     In the forwarding case, we returned above and never come here.
            if (AppConfig.chordState.isKeyMine(
                    ChordState.chordHash(clientMessage.getMessageText().trim().hashCode())
            )) {
                mutex.unlock();
            }
        }
    }
}

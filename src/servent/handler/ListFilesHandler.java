package servent.handler;

import app.AppConfig;
import app.ServentInfo;
import servent.message.ListFilesMessage;
import servent.message.ListFilesResponseMessage;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.util.MessageUtil;

import java.io.File;

public class ListFilesHandler implements MessageHandler {

    private Message clientMessage;

    public ListFilesHandler(Message clientMessage) {
        this.clientMessage = clientMessage;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() != MessageType.LIST_FILES) {
            AppConfig.timestampedErrorPrint("LIST_FILES handler got something that is not LIST_FILES.");
            return;
        }

        int requesterPort = clientMessage.getSenderPort();
        String requesterIp = "localhost";
        ServentInfo requesterInfo = new ServentInfo(requesterIp, requesterPort);

        boolean isPublic = AppConfig.chordState.isPublic();
        boolean isFollower = AppConfig.chordState.isFollower(requesterInfo);
        if (!isPublic && !isFollower) {
            AppConfig.timestampedStandardPrint("Profil je privatan.");
            return;
        }

        // Uzmi sve fajlove
        File workingDir = new File(AppConfig.workingDirectory);
        File[] files = workingDir.listFiles();
        StringBuilder fileListBuilder = new StringBuilder();

        if (files != null) {
            for (File f : files) {
                if (f.isFile()) {
                    fileListBuilder.append(f.getName()).append(",");
                }
            }
        }

        // 5) Sklanjamo zarez za jedan faj
        String filesString = "";
        if (fileListBuilder.length() > 0) {
            filesString = fileListBuilder.substring(0, fileListBuilder.length() - 1);
        }

        // 6) Saljemo fajlove
        Message response = new ListFilesResponseMessage(
                MessageType.LIST_FILES_RESPONSE,
                AppConfig.myServentInfo.getListenerPort(),
                requesterPort,
                filesString
        );
        MessageUtil.sendMessage(response);
    }
}
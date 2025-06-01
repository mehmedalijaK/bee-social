package servent.handler;

import app.AppConfig;
import servent.message.Message;
import servent.message.MessageType;

public class ListFilesResponseHandler implements MessageHandler{
    private Message clientMessage;

    public ListFilesResponseHandler(Message clientMessage) {
        this.clientMessage = clientMessage;
    }

    @Override
    public void run() {
        // 1) Provjera tipa
        if (clientMessage.getMessageType() != MessageType.LIST_FILES_RESPONSE) {
            AppConfig.timestampedErrorPrint("LIST_FILES_RESPONSE handler got wrong message type.");
            return;
        }

        // 2) Izvuci tekst poruke – comma‐separated nazivi datoteka
        String filesString = clientMessage.getMessageText(); // npr. "file1.txt,file2.png,notes.docx"
        if (filesString == null || filesString.isEmpty()) {
            AppConfig.timestampedStandardPrint("Nema dostupnih fajlova ili je lista prazna.");
            return;
        }

        // 3) Raspakiranje i ispis (ili druga obrada)
        String[] fileNames = filesString.split(",");
        AppConfig.timestampedStandardPrint("Primljena lista fajlova:");
        for (String fname : fileNames) {
            AppConfig.timestampedStandardPrint("  - " + fname);
        }
    }
}

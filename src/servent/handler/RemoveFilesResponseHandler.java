package servent.handler;

import app.AppConfig;
import servent.message.Message;
import servent.message.MessageType;

/**
 * RemoveFilesResponseHandler procesuira odgovor na REMOVE_FILES poruku.
 * Kada originalni podnosilac zahtjeva (koji je poslao REMOVE_FILES) dobije
 * poruku s rezultatom brisanja, on tu poruku koristi za uklanjanje lokok
 * mutex-a ili za ispis poruke korisniku.
 */
public class RemoveFilesResponseHandler implements MessageHandler {

    private Message clientMessage;

    public RemoveFilesResponseHandler(Message clientMessage) {
        this.clientMessage = clientMessage;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() != MessageType.REMOVE_FILE_RESPONSE) {
            AppConfig.timestampedErrorPrint("REMOVE_FILE_RESPONSE handler got something that is not REMOVE_FILE_RESPONSE.");
            return;
        }

        String responseText = clientMessage.getMessageText().trim();
        AppConfig.timestampedStandardPrint("Stigao REMOVE_FILE_RESPONSE: " + responseText);

        // Otključavamo distribuisani mutex jer je uklanjanje završeno
        AppConfig.mutex.unlock();
    }
}
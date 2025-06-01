package servent.handler;

import app.AppConfig;
import servent.message.Message;
import servent.message.MessageType;

public class VisibilityHandler implements MessageHandler {

    private Message clientMessage;

    public VisibilityHandler(Message clientMessage) {
        this.clientMessage = clientMessage;
    }

    @Override
    public void run() {
        // 1) Proverimo da li je tip poruke VISIBILITY
        if (clientMessage.getMessageType() != MessageType.VISIBILITY) {
            AppConfig.timestampedErrorPrint("VISIBILITY handler got something that is not VISIBILITY.");
            return;
        }

        // 2) Izvučemo payload – očekujemo "public" ili "private" u messageText
        String text = clientMessage.getMessageText().trim().toLowerCase();

        // 3) Postavimo lokalni vidljivost‐flaga na true/false
        if (text.equals("public")) {
            AppConfig.chordState.setPublicMode(true);
            AppConfig.timestampedStandardPrint("Profil je sada PUBLIC.");
        } else if (text.equals("private")) {
            AppConfig.chordState.setPublicMode(false);
            AppConfig.timestampedStandardPrint("Profil je sada PRIVATE.");
        } else {
            AppConfig.timestampedErrorPrint("Nevažeći parametar za visibility: " + text
                    + ". Očekivano 'public' ili 'private'.");
        }
        // (Napomena: za ovu komandu ne šaljemo nikakav poseban odgovor nazad;
        // samo menjamo lokalno stanje i ispisujemo potvrdu.)
    }
}

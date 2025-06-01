package servent.handler;

import app.AppConfig;
import app.ServentInfo;
import servent.message.Message;
import servent.message.MessageType;

/**
 * LeaveHandler procesuira LEAVE poruke.
 * Kada jedan čvor odlazi iz Chorda, on obaveštava svog successor-a i predecessor-a
 * o novoj topologiji (predecessor/successor linkovima).
 *
 * U messageText (payload) očekujemo string oblika "ip:port", koji označava
 * novu vrednost predecessor-a (ako šaljemo successor-u) ili successor-a (ako šaljemo predecessor-u).
 */
public class LeaveHandler implements MessageHandler {

    private Message clientMessage;

    public LeaveHandler(Message clientMessage) {
        this.clientMessage = clientMessage;
    }

    @Override
    public void run() {
        // 1) Provera tipa poruke
        if (clientMessage.getMessageType() != MessageType.LEAVE) {
            AppConfig.timestampedErrorPrint("LEAVE handler got something that is not LEAVE.");
            return;
        }

        // 2) Parsiramo payload: očekujemo "ip:port" ili "null"
        String payload = clientMessage.getMessageText().trim();
        ServentInfo newNeighbor = null;
        if (!payload.equals("null")) {
            String[] parts = payload.split(":");
            String ip = parts[0];
            int port = Integer.parseInt(parts[1]);
            newNeighbor = new ServentInfo(ip, port);
        }

        // 3) Utvrdimo odakle je stigao LEAVE
        int senderPort = clientMessage.getSenderPort();
        ServentInfo senderInfo = new ServentInfo("localhost", senderPort);
        AppConfig.timestampedStandardPrint(
                "Prijem LEAVE od " + senderInfo.getIpAddress() + ":" + senderInfo.getListenerPort() +
                        " (payload = " + payload + ")"
        );

        // 4) Ako je ova servisna instanca successor onog koji šalje:
        //    znači da senderInfo-ja više nema, i njegov successor (mi) treba da postavi
        //    novog predecessor-a na osnovu payload-a.
        ServentInfo myInfo = AppConfig.myServentInfo;
        ServentInfo[] succTable = AppConfig.chordState.getSuccessorTable();
        ServentInfo myPredecessor = AppConfig.chordState.getPredecessor();

        // Provera da li je sender moj predecessor ili successor:
        //   - Ako mi je sender port jedini successor, menjamo successor
        //   - Ako mi je sender port jedini predecessor, menjamo predecessor
        boolean wasPredecessor = false, wasSuccessor = false;

        if (myPredecessor != null
                && myPredecessor.getListenerPort() == senderPort) {
            wasPredecessor = true;
        }
        if (succTable.length > 0
                && succTable[0] != null
                && succTable[0].getListenerPort() == senderPort) {
            wasSuccessor = true;
        }

        // 5) Ako je sender bio moj predecessor, postavimo novog predecessor-a:
        if (wasPredecessor) {
            AppConfig.chordState.setPredecessor(newNeighbor);
            AppConfig.timestampedStandardPrint(
                    "Updated predecessor to " +
                            (newNeighbor != null
                                    ? newNeighbor.getIpAddress() + ":" + newNeighbor.getListenerPort()
                                    : "null")
            );
        }

        // 6) Ako je sender bio moj successor, postavimo novog successor-a:
        if (wasSuccessor) {
            if (newNeighbor != null) {
                // Menjamo successorTable[0] i, po potrebi, osvežimo celu tabelu preskoka
                ServentInfo[] table = AppConfig.chordState.getSuccessorTable();
                table[0] = newNeighbor;
                AppConfig.timestampedStandardPrint(
                        "Updated successor to " +
                                newNeighbor.getIpAddress() + ":" + newNeighbor.getListenerPort()
                );
                // (Opcionalno: nakon promene successorTable[0], možete pozvati metodu
                // za ponovno izračunavanje svih skokova ako želite tačan finger table.)
            } else {
                // Ako payload == "null", znači odlazi poslednji čvor u mreži.
                // Tada mi nemamo successor-a više.
                ServentInfo[] table = AppConfig.chordState.getSuccessorTable();
                table[0] = null;
                AppConfig.timestampedStandardPrint("Successor is now null (network is empty).");
            }
        }

        // 7) (Opcionalno) Možete distribuirati pridržavanje DHT vrednosti:
        //    ako je odlazeći čvor čuvao neke ključeve koji su sada u opsegu vaše odgovornosti,
        //    treba da ih preuzmete. Međutim, u ovoj jednostavnoj implementaciji
        //    to nije obavezno.

        // 8) Kod LEAVE ne šaljemo direktan ACK. Svaki čvor menja svoje pokazivače
        //    i nastavlja normalno.
    }
}

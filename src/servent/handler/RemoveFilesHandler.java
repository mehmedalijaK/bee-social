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
        // 1) Proverimo da li je tip poruke REMOVE_FILES
        if (clientMessage.getMessageType() != MessageType.REMOVE_FILES) {
            AppConfig.timestampedErrorPrint("REMOVE_FILES handler got something that is not REMOVE_FILES.");
            return;
        }

        // 2) Izvucemo port onoga ko je poslao zahtev
        int requesterPort = clientMessage.getSenderPort();

        // Napomena: pretpostavljamo "localhost" za interne poruke
        ServentInfo requesterInfo = new ServentInfo("localhost", requesterPort);

        // 3) Poruka nosi relativnu putanju fajla za brisanje (npr. "photos/vacation.jpg")
        String relativeFilePath = clientMessage.getMessageText().trim();

        // 4) Izracunamo Chord ključ za ovu putanju fajla
        int key = ChordState.chordHash(relativeFilePath.hashCode());

        // 5) Proverimo da li je OVAJ cvor odgovoran za brisanje fajla s tim ključem
        boolean iStoreIt = AppConfig.chordState.isKeyMine(key);

        if (iStoreIt) {
            // ─── Mi smo nod odgovoran za taj ključ ───

            // 6) Zakljucavamo lokalni mutex pre ulaska u kriticnu sekciju
            AppConfig.mutex.lock();
            try {
                // 6a) Pokusamo da izbrisemo fajl iz radnog direktorijuma
                File targetFile = new File(AppConfig.workingDirectory + File.separator + relativeFilePath);
                boolean deleted = false;
                if (targetFile.exists()) {
                    deleted = targetFile.delete();
                }
                AppConfig.timestampedStandardPrint(
                        "Removing file \"" + relativeFilePath + "\" locally (key=" + key + "): " +
                                (deleted ? "SUCCESS" : "NOT FOUND")
                );

                // 6b) (Opcionalno) Slanje poruka nasljedniku i prethodniku da se obrise replika fajla
                //     U punoj implementaciji, ovde biste kreirali ReplicaRemoveMessage za successor i predecessor
                //     i poslali ih putem MessageUtil.sendMessage(...).

                // 6c) Kada je brisanje (i eventualne replike) obavljeno, saljemo odgovor originalnom zahtevacu
                String payload = (deleted ? "OK:" : "FAIL:") + relativeFilePath;
                Message response = new RemoveFilesResponseMessage(
                        MessageType.REMOVE_FILE_RESPONSE,
                        AppConfig.myServentInfo.getListenerPort(), // sender = ovaj nod
                        requesterPort,                             // receiver = originalni zahtevac
                        payload
                );
                MessageUtil.sendMessage(response);

            } finally {
                // 6d) Otkljucavamo mutex nakon zavrsetka obrade
                AppConfig.mutex.unlock();
            }
        } else {
            // ─── Nismo odgovorni za taj ključ: prosledjujemo zahtev dalje ───

            // 7a) Nalazimo sledeci nod u Chord prstenu koji je blizi kljucu
            ServentInfo nextNode = AppConfig.chordState.getNextNodeForKey(key);

            // 7b) Prosledjujemo novi RemoveFileMessage na sledeci nod
            Message forwardMsg = new RemoveFileMessage(
                    MessageType.REMOVE_FILES,                   // tip poruke
                    AppConfig.myServentInfo.getListenerPort(),  // sada ja stajem u ulogu posiljaoca
                    nextNode.getListenerPort(),                 // prosledjujem ka sledecem nodu
                    relativeFilePath                            // isti payload (putanja fajla)
            );
            MessageUtil.sendMessage(forwardMsg);

            // 7c) Ne otkljucavamo ovde mutex, jer nismo niti zakljucali — samo forwardujemo
            return;
        }
    }
}
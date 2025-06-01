package cli.command;

import app.AppConfig;
import servent.message.RemoveFileMessage;
import servent.message.MessageType;
import servent.message.util.MessageUtil;

public class RemoveFileCommand implements CLICommand {

    @Override
    public String commandName() {
        return "remove_file";
    }

    @Override
    public void execute(String args) {
        // 1) Provera argumenata: komanda očekuje tačno jedan argument – naziv (relativna putanja) datoteke
        if (args == null || args.trim().isEmpty()) {
            AppConfig.timestampedErrorPrint("Usage: remove_file [filename]");
            return;
        }
        String filePath = args.trim(); // naziv datoteke – relativno u odnosu na radni koren :contentReference[oaicite:0]{index=0}

        // 2) Akvizicija globalnog mutex-a (Suzuki–Kasami) pre slanja zahteva za brisanje :contentReference[oaicite:1]{index=1}
        AppConfig.mutex.lock();
        try {
            // 3) Određujemo port čvora koji je sledeći u Chord ringu
            int nextNodePort = AppConfig.chordState.getNextNodePort();

            // 4) Kreiramo poruku tipa REMOVE_FILE_REQUEST; messageText sadrži samo naziv datoteke
            RemoveFileMessage removeReq = new RemoveFileMessage(
                    MessageType.REMOVE_FILES,
                    AppConfig.myServentInfo.getListenerPort(),
                    nextNodePort,
                    filePath
            );

            // 5) Šaljemo poruku u mrežu
            MessageUtil.sendMessage(removeReq);
            AppConfig.timestampedStandardPrint("Sent remove_file request for " + filePath);

            // Napomena: kada ciljni čvor zaprimi ovu poruku, on će:
            //  • generisati ključ iz imena fajla i proveriti da li je odgovoran
            //  • ako nije, proslediti dalje sledećem čvoru u prstenu
            //  • ako jeste, obaviti lokalno brisanje i slanje ACK natrag originalnom zahtevocu,
            //    potom obrisati replike na predhodniku i nasledniku
            // Nakon primanja ACK, mutex će biti otpušten u odgovarajućem handler-u poruke. :contentReference[oaicite:2]{index=2}

        } finally {
            AppConfig.mutex.unlock();
        }
    }
}
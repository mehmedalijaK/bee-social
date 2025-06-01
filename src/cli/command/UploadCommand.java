package cli.command;

import app.AppConfig;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.UploadMessage;
import servent.message.util.MessageUtil;

import java.io.File;

public class UploadCommand implements CLICommand {

    @Override
    public String commandName() {
        return "upload";
    }

    @Override
    public void execute(String args) {
        // 1) Provera argumenata: komanda očekuje tačno jedan argument – relativnu putanju fajla
        if (args == null || args.trim().isEmpty()) {
            AppConfig.timestampedErrorPrint("Usage: upload [path]");
            return;
        }
        String filePath = args.trim(); // putanja relativna u odnosu na radni koren :contentReference[oaicite:0]{index=0}

        // 2) Provera da li fajl postoji u radnom direktorijumu
        File localFile = new File(AppConfig.workingDirectory, filePath);
        if (!localFile.exists() || !localFile.isFile()) {
            AppConfig.timestampedErrorPrint("File not found: " + filePath);
            return;
        }

        // 3) Akvizicija globalnog mutex-a (Suzuki–Kasami) pre slanja zahteva za upload :contentReference[oaicite:1]{index=1}
        AppConfig.mutex.lock();
        try {
            // 4) Određujemo port čvora koji je sledeći u Chord prstenu
            int nextNodePort = AppConfig.chordState.getNextNodePort();

            // 5) Kreiramo poruku tipa UPLOAD_FILE_REQUEST; messageText sadrži samo relativnu putanju fajla
            Message uploadReq = new UploadMessage(
                    MessageType.UPLOAD,
                    AppConfig.myServentInfo.getListenerPort(),
                    nextNodePort,
                    filePath
            );

            // 6) Šaljemo poruku u mrežu
            MessageUtil.sendMessage(uploadReq);
            AppConfig.timestampedStandardPrint("Sent upload request for " + filePath);

            // Napomena: Kada poruka stigne na odgovoran čvor, taj čvor će pročitati fajl sa radnog direktorijuma
            // (koristeći primljenu relativnu putanju), sačuvati ga lokalno i zatim kreirati replike
            // na predhodniku i nasledniku. Nakon uspešnog čuvanja, odgovoran čvor šalje ACK,
            // a originalni zahtevalac oslobađa mutex. :contentReference[oaicite:2]{index=2}

        } finally {
            AppConfig.mutex.unlock();
        }
    }
}
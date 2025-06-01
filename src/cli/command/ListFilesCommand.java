package cli.command;

import app.AppConfig;
import servent.message.ListFilesMessage;
import servent.message.MessageType;
import servent.message.util.MessageUtil;

public class ListFilesCommand implements CLICommand {

    @Override
    public String commandName() {
        return "list_files";
    }

    @Override
    public void execute(String args) {
        // 1) Provera argumenata
        if (args == null || args.trim().isEmpty()) {
            AppConfig.timestampedErrorPrint("Usage: list_files [address:port]");
            return;
        }

        String addrPort = args.trim();
        String[] parts = addrPort.split(":");
        if (parts.length != 2) {
            AppConfig.timestampedErrorPrint("Invalid format. Usage: list_files [address:port]");
            return;
        }

        String ip = parts[0];
        int port;
        try {
            port = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            AppConfig.timestampedErrorPrint("Invalid port: " + parts[1]);
            return;
        }

        // 2) Sastavimo LIST_FILES zahtev
        //    Po specifikaciji, node prvo mora da akvizira mutex (Suzuki‐Kasami)
        AppConfig.mutex.lock();
        try {
            // Kreiramo poruku tipa REQUEST za listanje fajlova
            String myAddrPort = AppConfig.myServentInfo.getIpAddress() + ":"
                    + AppConfig.myServentInfo.getListenerPort();
            ListFilesMessage listFilesReq = new ListFilesMessage(
                    MessageType.LIST_FILES,
                    AppConfig.myServentInfo.getListenerPort(),
                    port,
                    myAddrPort
            );

            // 3) Šaljemo zahtev u mrežu
            MessageUtil.sendMessage(listFilesReq);
            AppConfig.timestampedStandardPrint("Sent list_files request to " + addrPort);

            // Nakon slanja, čvor će čekati LIST_FILES_RESPONSE kako bi ispisao fajlove.
            // Oslobađanje mutex-a će se obaviti onda kada se obradi response.
        } finally {
            AppConfig.mutex.unlock();
        }
    }
}
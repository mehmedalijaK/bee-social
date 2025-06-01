package cli.command;

import app.AppConfig;
import app.ServentInfo;
import servent.message.FollowMessage;
import servent.message.MessageType;
import servent.message.util.MessageUtil;

/**
 * 'accept [address:port]' – prihvata (approve) jedan pending follow‐zahtev.
 *
 * Ako čvor [address:port] postoji u AppConfig.pendingFollowers,
 * uklanja ga iz pending i stavlja u AppConfig.followers,
 * zatim šalje nazad FollowMessage tipa ACCEPT.
 */
public class AcceptCommand implements CLICommand {

    @Override
    public String commandName() {
        return "accept";
    }

    @Override
    public void execute(String args) {
        // 1) Provera argumenata
        if (args == null || args.trim().isEmpty()) {
            AppConfig.timestampedErrorPrint("Usage: accept [address:port]");
            return;
        }

        String addrPort = args.trim();
        String[] parts = addrPort.split(":");
        if (parts.length != 2) {
            AppConfig.timestampedErrorPrint("Invalid format. Usage: accept [address:port]");
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

        // 2) Pronađemo pending follower-a u chordState
        ServentInfo toAccept = null;
        synchronized (AppConfig.chordState) {
            for (ServentInfo si : AppConfig.chordState.getPendingFollowers()) {
                if (si.getIpAddress().equals(ip) && si.getListenerPort() == port) {
                    toAccept = si;
                    break;
                }
            }

            if (toAccept == null) {
                AppConfig.timestampedErrorPrint("No pending follow request from: " + addrPort);
                return;
            }

            // 3) Premestimo ga iz pending u followers
            AppConfig.chordState.acceptFollower(toAccept);
        }

        AppConfig.timestampedStandardPrint("Accepted follow request from " + addrPort);

        // 4) Pošaljemo nazad FollowMessage tipa ACCEPT da obavestimo originalnog requestera
        //    U messageText možemo staviti adresu:port onog koji je prihvatio (ovo je po konvenciji)
        String myAddrPort = AppConfig.myServentInfo.getIpAddress() + ":"
                + AppConfig.myServentInfo.getListenerPort();
        FollowMessage acceptResponse = new FollowMessage(
                MessageType.ACCEPT,
                AppConfig.myServentInfo.getListenerPort(),
                port,
                myAddrPort
        );
        MessageUtil.sendMessage(acceptResponse);
    }
}
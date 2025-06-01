package cli.command;

import app.AppConfig;
import servent.message.FollowMessage;
import servent.message.MessageType;
import servent.message.util.MessageUtil;

public class FollowCommand implements CLICommand {

    @Override
    public String commandName() {
        return "follow";
    }

    @Override
    public void execute(String args) {
        // 1) Provera argumenata
        if (args == null || args.trim().isEmpty()) {
            AppConfig.timestampedErrorPrint("Usage: follow [address:port]");
            return;
        }

        String addrPort = args.trim();
        String[] parts = addrPort.split(":");
        if (parts.length != 2) {
            AppConfig.timestampedErrorPrint("Invalid format. Usage: follow [address:port]");
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

        // 2) Kreiramo i šaljemo FollowMessage tipa REQUEST
        String myAddrPort = AppConfig.myServentInfo.getIpAddress() + ":"
                + AppConfig.myServentInfo.getListenerPort();

        FollowMessage followRequest = new FollowMessage(
                MessageType.FOLLOW,
                AppConfig.myServentInfo.getListenerPort(),
                port,
                myAddrPort
        );

        MessageUtil.sendMessage(followRequest);
        AppConfig.timestampedStandardPrint("Sent follow request to " + addrPort);
    }
}
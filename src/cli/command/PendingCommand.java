package cli.command;

import app.AppConfig;
import app.ServentInfo;

import java.util.List;

public class PendingCommand implements CLICommand {

    @Override
    public String commandName() {
        return "pending";
    }

    @Override
    public void execute(String args) {
        // 1) 'pending' ne prima argumente
        if (args != null && !args.trim().isEmpty()) {
            AppConfig.timestampedErrorPrint("Usage: pending");
            return;
        }

        // 2) Dohvatamo listu pending follow zahteva iz ChordState
        List<ServentInfo> pendingList;
        synchronized (AppConfig.chordState) {
            pendingList = AppConfig.chordState.getPendingFollowers();
        }

        // 3) Ispisujemo rezultate
        if (pendingList.isEmpty()) {
            AppConfig.timestampedStandardPrint("No pending follow requests");
        } else {
            AppConfig.timestampedStandardPrint("Pending follow requests:");
            for (ServentInfo si : pendingList) {
                String addrPort = si.getIpAddress() + ":" + si.getListenerPort();
                AppConfig.timestampedStandardPrint("  - " + addrPort);
            }
        }
    }
}
package cli.command;

import app.AppConfig;

public class VisibilityCommand implements CLICommand {

    @Override
    public String commandName() {
        return "visibility";
    }

    @Override
    public void execute(String args) {
        // 1) Provera argumenata: komanda očekuje "public" ili "private"
        if (args == null || args.trim().isEmpty()) {
            AppConfig.timestampedErrorPrint("Usage: visibility [public|private]");
            return;
        }

        String mode = args.trim().toLowerCase();
        boolean newVisibility;
        if (mode.equals("public")) {
            newVisibility = true;
        } else if (mode.equals("private")) {
            newVisibility = false;
        } else {
            AppConfig.timestampedErrorPrint("Invalid option. Usage: visibility [public|private]");
            return;
        }

        // 2) Postavljamo novu vidljivost u ChordState
        synchronized (AppConfig.chordState) {
            AppConfig.chordState.setPublicMode(newVisibility);
        }

        AppConfig.timestampedStandardPrint("Visibility set to " + mode);
    }
}
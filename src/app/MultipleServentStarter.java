package app;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * This class implements the logic for starting multiple servent instances.
 *
 * To use it, invoke startServentTest with a directory name as parameter.
 * This directory should include:
 * <ul>
 * <li>A <code>servent_list.properties</code> file (explained in {@link AppConfig} class</li>
 * <li>A directory called <code>output</code> </li>
 * <li>A directory called <code>error</code> </li>
 * <li>A directory called <code>input</code> with text files called
 * <code> servent0_in.txt </code>, <code>servent1_in.txt</code>, ... and so on for each servent.
 * These files should contain the commands for each servent, as they would be entered in console.</li>
 * </ul>
 *
 * @author bmilojkovic
 */
public class MultipleServentStarter {

	/**
	 * We will wait for user stop in a separate thread.
	 * The main thread is waiting for processes to end naturally.
	 */
	private static class ServentCLI implements Runnable {

		private List<Process> serventProcesses;
		private Process bsProcess;

		public ServentCLI(List<Process> serventProcesses, Process bsProcess) {
			this.serventProcesses = serventProcesses;
			this.bsProcess = bsProcess;
		}

		@Override
		public void run() {
			Scanner sc = new Scanner(System.in);

			while(true) {
				String line = sc.nextLine();

				if (line.equals("stop")) {
					AppConfig.timestampedStandardPrint("Stop command received. Terminating processes...");
					// Destroy servent processes first
					for (Process process : serventProcesses) {
						if (process.isAlive()) {
							AppConfig.timestampedStandardPrint("Destroying servent process: " + process.pid());
							process.destroy();
						}
					}
					// Then destroy bootstrap process
					if (bsProcess != null && bsProcess.isAlive()) {
						AppConfig.timestampedStandardPrint("Destroying bootstrap process: " + bsProcess.pid());
						bsProcess.destroy();
					}
					break;
				}
			}
			sc.close();
			AppConfig.timestampedStandardPrint("CLI thread for stop command finished.");
		}
	}

	/**
	 * The parameter for this function should be the name of a directory that
	 * contains a servent_list.properties file which will describe our distributed system.
	 */
	private static void startServentTest(String testName) {
		List<Process> serventProcesses = new ArrayList<>();

		// Read config once to get SERVENT_COUNT and BOOTSTRAP_PORT for the BootstrapServer process
		// AppConfig.myServentId will be 0 here, but it's mainly for BS_PORT and SERVENT_COUNT at this stage.
		AppConfig.readConfig(testName + "/servent_list.properties", 0);

		AppConfig.timestampedStandardPrint("Starting multiple servent runner. "
				+ "If servents do not finish on their own, type \"stop\" to finish them");

		// Define the common classpath. Adjust "bee-social" if your module name is different.
		// This assumes your IDE (like IntelliJ) compiles to out/production/<module_name>
		// For Eclipse, it might be just "bin" or a similar configured output folder.
		// Ensure this path is correct for your build environment.
		String commonClasspath = "out" + File.separator + "production" + File.separator + "bee-social";
		// A simpler, more common manual setup might just be "out" if you compile with `javac -d out ...`
		// Example: String commonClasspath = "out";

		Process bsProcess = null;
		ProcessBuilder bsBuilder = new ProcessBuilder("java", "-cp", commonClasspath,
				"app.BootstrapServer", String.valueOf(AppConfig.BOOTSTRAP_PORT));

		// Redirect BootstrapServer's output and error streams
		File bsOutFile = new File(testName + "/output/bootstrap_out.txt");
		File bsErrFile = new File(testName + "/error/bootstrap_err.txt");
		try {
			bsOutFile.getParentFile().mkdirs(); // Ensure directories exist
			bsErrFile.getParentFile().mkdirs();
			bsBuilder.redirectOutput(bsOutFile);
			bsBuilder.redirectError(bsErrFile);
			AppConfig.timestampedStandardPrint("Attempting to start BootstrapServer...");
			bsProcess = bsBuilder.start();
			AppConfig.timestampedStandardPrint("BootstrapServer process started (PID: " + (bsProcess != null ? bsProcess.pid() : "unknown") + "). Output: " + bsOutFile.getAbsolutePath() + ", Error: " + bsErrFile.getAbsolutePath());
		} catch (IOException e1) {
			AppConfig.timestampedErrorPrint("Failed to start BootstrapServer: " + e1.getMessage());
			e1.printStackTrace();
			// If bootstrap fails to start, there's no point in continuing with servents.
			return;
		}

		// Wait for bootstrap to start and open its socket. Increased delay for robustness.
		AppConfig.timestampedStandardPrint("Waiting for BootstrapServer to initialize...");
		try {
			Thread.sleep(5000); // Increased to 5 seconds
		} catch (InterruptedException e1) {
			e1.printStackTrace();
			Thread.currentThread().interrupt(); // Restore interrupted status
		}
		AppConfig.timestampedStandardPrint("Finished waiting for BootstrapServer.");


		int serventCount = AppConfig.SERVENT_COUNT;

		// Corrected: Single loop to start servents
		for(int i = 0; i < serventCount; i++) {
			try {
				AppConfig.timestampedStandardPrint("Starting servent " + i + "...");
				ProcessBuilder builder = new ProcessBuilder("java", "-cp", commonClasspath, "app.ServentMain",
						testName + "/servent_list.properties", String.valueOf(i));

				// We use files to read and write.
				// System.out, System.err and System.in will point to these files.
				File serventOutFile = new File(testName + "/output/servent" + i + "_out.txt");
				File serventErrFile = new File(testName + "/error/servent" + i + "_err.txt");
				File serventInFile = new File(testName + "/input/servent" + i + "_in.txt");

				serventOutFile.getParentFile().mkdirs(); // Ensure directories exist
				serventErrFile.getParentFile().mkdirs();
				serventInFile.getParentFile().mkdirs();

				builder.redirectOutput(serventOutFile);
				builder.redirectError(serventErrFile);
				builder.redirectInput(serventInFile);

				// Starts the servent as a completely separate process.
				Process p = builder.start();
				serventProcesses.add(p);
				AppConfig.timestampedStandardPrint("Servent " + i + " process started (PID: " + p.pid() + "). Output: " + serventOutFile.getAbsolutePath());

			} catch (IOException e) {
				AppConfig.timestampedErrorPrint("Failed to start servent " + i + ": " + e.getMessage());
				e.printStackTrace();
			}
			// Delay between starting subsequent servents
			if (i < serventCount - 1) { // No need to sleep after the last one
				try {
					AppConfig.timestampedStandardPrint("Pausing before starting next servent...");
					Thread.sleep(3000); // Adjusted delay, original was 10s, can be tuned.
				} catch (InterruptedException e) {
					e.printStackTrace();
					Thread.currentThread().interrupt(); // Restore interrupted status
				}
			}
		}

		Thread t = new Thread(new ServentCLI(serventProcesses, bsProcess));
		t.setName("ServentCLI-Stopper");
		t.start(); // CLI thread waiting for user to type "stop".

		AppConfig.timestampedStandardPrint("All servent processes launched. Waiting for them to finish or for 'stop' command.");

		// Wait for servent processes to finish
		for (Process process : serventProcesses) {
			try {
				if (process.isAlive()) {
					// AppConfig.timestampedStandardPrint("Waiting for servent process " + process.pid() + " to finish...");
					process.waitFor(); // Wait for graceful process finish.
					AppConfig.timestampedStandardPrint("Servent process " + process.pid() + " finished with exit code: " + process.exitValue());
				} else {
					AppConfig.timestampedStandardPrint("Servent process " + process.pid() + " already finished with exit code: " + process.exitValue());
				}
			} catch (InterruptedException e) {
				AppConfig.timestampedErrorPrint("Interrupted while waiting for servent process " + process.pid());
				e.printStackTrace();
				Thread.currentThread().interrupt(); // Restore interrupted status
			}
		}

		AppConfig.timestampedStandardPrint("All servent processes have finished.");
		// If servents finish, we might want to stop bootstrap or wait for "stop" command for it.
		// The current logic waits for "stop" command or for bootstrap to terminate itself.
		// If bootstrap is still alive after all servents finished, the CLI "stop" is needed to kill it.
		if (bsProcess != null && bsProcess.isAlive()) {
			AppConfig.timestampedStandardPrint("Bootstrap process is still alive. Type \"stop\" to halt bootstrap.");
			try {
				bsProcess.waitFor(); // Wait for bootstrap if it hasn't been stopped by CLI
				AppConfig.timestampedStandardPrint("Bootstrap process finished with exit code: " + bsProcess.exitValue());
			} catch (InterruptedException e) {
				AppConfig.timestampedErrorPrint("Interrupted while waiting for bootstrap process.");
				e.printStackTrace();
				Thread.currentThread().interrupt();
			}
		} else if (bsProcess != null) {
			AppConfig.timestampedStandardPrint("Bootstrap process already finished with exit code: " + bsProcess.exitValue());
		}
	}

	public static void main(String[] args) {
		startServentTest("chord");
	}
}
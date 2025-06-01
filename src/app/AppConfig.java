package app;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This class contains all the global application configuration stuff.
 */
public class AppConfig {

	/** Convenience access for this servent's information */
	public static ServentInfo myServentInfo;
	public static String workingDirectory;

	/** Suzuki–Kasami mutex */
	public static final Lock mutex = new ReentrantLock();

	public static boolean INITIALIZED = false;
	public static int BOOTSTRAP_PORT;
	public static int SERVENT_COUNT;

	public static ChordState chordState;

	public static Thread listenerThread;
	public static ServerSocket serverSocket;
	public static List<Thread> workerThreads = new ArrayList<>();

	// ================================================

	/**
	 * Print a message to stdout with a timestamp
	 *
	 * @param message message to print
	 */
	public static void timestampedStandardPrint(String message) {
		DateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
		Date now = new Date();
		System.out.println(timeFormat.format(now) + " - " + message);
	}

	/**
	 * Print a message to stderr with a timestamp
	 *
	 * @param message message to print
	 */
	public static void timestampedErrorPrint(String message) {
		DateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
		Date now = new Date();
		System.err.println(timeFormat.format(now) + " - " + message);
	}

	/**
	 * Reads a config file. Should be called once at start of app.
	 * The config file should be of the following format:
	 * <code>
	 *    servent_count=3
	 *    chord_size=64
	 *    bs.port=2000
	 *    servent0.port=1100
	 *    servent1.port=1200
	 *    servent2.port=1300
	 * </code>
	 *
	 * @param configName name of configuration file
	 * @param serventId  id of the servent, as used in the configuration file
	 */
	public static void readConfig(String configName, int serventId) {
		Properties properties = new Properties();
		try {
			properties.load(new FileInputStream(new File(configName)));
		} catch (IOException e) {
			timestampedErrorPrint("Couldn't open properties file. Exiting...");
			System.exit(0);
		}

		try {
			BOOTSTRAP_PORT = Integer.parseInt(properties.getProperty("bs.port"));
		} catch (NumberFormatException e) {
			timestampedErrorPrint("Problem reading bs.port. Exiting...");
			System.exit(0);
		}

		try {
			SERVENT_COUNT = Integer.parseInt(properties.getProperty("servent_count"));
		} catch (NumberFormatException e) {
			timestampedErrorPrint("Problem reading servent_count. Exiting...");
			System.exit(0);
		}

		try {
			int chordSize = Integer.parseInt(properties.getProperty("chord_size"));
			ChordState.CHORD_SIZE = chordSize;
			chordState = new ChordState();
		} catch (NumberFormatException e) {
			timestampedErrorPrint("Problem reading chord_size. Must be a power of 2. Exiting...");
			System.exit(0);
		}

		String portProperty = "servent" + serventId + ".port";
		int serventPort = -1;
		try {
			serventPort = Integer.parseInt(properties.getProperty(portProperty));
		} catch (NumberFormatException e) {
			timestampedErrorPrint("Problem reading " + portProperty + ". Exiting...");
			System.exit(0);
		}
		myServentInfo = new ServentInfo("localhost", serventPort);
	}

	/**
	 * Graceful shutdown/cleanup before exit.
	 */
	public static void shutdown() {
		timestampedStandardPrint("Cleaning up before exit…");

		// 1) Prekid listener niti
		if (listenerThread != null && listenerThread.isAlive()) {
			try {
				listenerThread.interrupt();
				timestampedStandardPrint("Listener thread interrupted.");
			} catch (Exception e) {
				timestampedErrorPrint("Greška pri gašenju listener niti: " + e.getMessage());
			}
		}

		// 2) Zatvaranje ServerSocket-a (ako postoji)
		if (serverSocket != null && !serverSocket.isClosed()) {
			try {
				serverSocket.close();
				timestampedStandardPrint("ServerSocket closed.");
			} catch (IOException e) {
				timestampedErrorPrint("Greška pri zatvaranju server socket‐a: " + e.getMessage());
			}
		}

		// 3) Čekanje radnih niti (worker threads)
		for (Thread t : workerThreads) {
			try {
				if (t.isAlive()) {
					timestampedStandardPrint("Waiting for worker thread " + t.getName() + " to finish …");
					t.join(2000);  // čekaj do 2 sekunde po niti
				}
			} catch (InterruptedException ignored) { }
		}

		// 4) (Optional) Zatvaranje drugih resursa (npr. fajlovi, DB konekcije…)
		//    …

		// 5) KONAČNI IZLAZ IZ PROGRAMA
		timestampedStandardPrint("Exiting now.");
		System.exit(0);
	}
}

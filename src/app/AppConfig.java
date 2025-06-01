package app;

import servent.message.SKTokenMessage;
import servent.message.SKTokenRequestMessage;
import servent.message.util.MessageUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This class contains all the global application configuration stuff.
 */
public class AppConfig {

	/** Convenience access for this servent's information */
	public static ServentInfo myServentInfo;
	public static String workingDirectory = "storage/"; // Define a working directory for file storage

	/** Suzuki–Kasami mutex - OLD LOCAL MUTEX, to be replaced for distributed operations */
	// public static final Lock mutex = new ReentrantLock(); // Will be replaced by SK logic calls

	public static boolean INITIALIZED = false;
	public static int BOOTSTRAP_PORT;
	public static int SERVENT_COUNT;

	public static ChordState chordState;

	public static Thread listenerThread;
	public static ServerSocket serverSocket;
	public static List<Thread> workerThreads = new ArrayList<>();

	// Suzuki-Kasami State
	public static Lock skMutex = new ReentrantLock(); // To protect SK algorithm's internal state
	public static Condition skTokenArrivedCondition;  // For threads waiting for the token

	public static int myServentId; // 0 to SERVENT_COUNT-1, corresponds to serventN index
	public static ServentInfo[] allServents; // Array of all servents, index is serventId

	public static volatile boolean hasToken;
	public static int[] RN; // Request Number array for this servent
	public static volatile boolean inCriticalSection;
	public static volatile boolean waitingForToken;

	// Token's data (only valid if hasToken is true)
	public static int[] LN_token;         // Last Granted Number array
	public static Queue<Integer> Q_token; // Queue of serventIds waiting for the token


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

	private static void initSuzukiKasami(int currentServentId, Properties properties) {
		myServentId = currentServentId;
		skTokenArrivedCondition = skMutex.newCondition();

		RN = new int[SERVENT_COUNT]; // Initialized to 0 by default
		allServents = new ServentInfo[SERVENT_COUNT];
		for (int i = 0; i < SERVENT_COUNT; i++) {
			String portProperty = "servent" + i + ".port";
			try {
				int serventPort = Integer.parseInt(properties.getProperty(portProperty));
				allServents[i] = new ServentInfo("localhost", serventPort);
			} catch (NumberFormatException e) {
				timestampedErrorPrint("Problem reading " + portProperty + " for SK allServents. Exiting...");
				System.exit(0);
			}
		}

		if (myServentId == 0) { // Servent 0 starts with the token
			hasToken = true;
			LN_token = new int[SERVENT_COUNT]; // Initialized to 0
			Q_token = new LinkedBlockingQueue<>(); // Using LinkedBlockingQueue as it's a common Queue impl
			timestampedStandardPrint("Servent " + myServentId + " initialized WITH Suzuki-Kasami token.");
		} else {
			hasToken = false;
			LN_token = new int[SERVENT_COUNT];
			Q_token = new LinkedBlockingQueue<>();
			timestampedStandardPrint("Servent " + myServentId + " initialized WITHOUT Suzuki-Kasami token.");
		}
		inCriticalSection = false;
		waitingForToken = false;
	}

	public static void requestCSEntry() throws InterruptedException {
		skMutex.lock();
		try {
			if (hasToken) {
				inCriticalSection = true;
				timestampedStandardPrint("SK: Already have token. Entering CS for servent " + myServentId);
				return;
			}

			RN[myServentId]++;
			waitingForToken = true;
			timestampedStandardPrint("SK: Servent " + myServentId + " requesting token with RN[" + myServentId + "]=" + RN[myServentId]);
			broadcastTokenRequest(RN[myServentId]);

			while (!hasToken) {
				timestampedStandardPrint("SK: Servent " + myServentId + " waiting for token...");
				skTokenArrivedCondition.await();
			}
			// Token has arrived
			waitingForToken = false;
			inCriticalSection = true;
			timestampedStandardPrint("SK: Servent " + myServentId + " received token. Entering CS.");

		} finally {
			skMutex.unlock();
		}
	}

	public static void releaseCSEntry() {
		skMutex.lock();
		try {
			if (!hasToken) {
				timestampedErrorPrint("SK: ERROR! Servent " + myServentId + " tried to release CS without token.");
				// This case should ideally not happen if logic is correct
				inCriticalSection = false; // Still mark as not in CS
				return;
			}
			if (!inCriticalSection) {
				// This might happen if releaseCSEntry is called multiple times or erroneously
				timestampedErrorPrint("SK: Servent " + myServentId + " tried to release CS but was not marked as in CS.");
				// Proceed with token logic anyway, assuming it was a valid release intention
			}

			inCriticalSection = false;
			LN_token[myServentId] = RN[myServentId];
			timestampedStandardPrint("SK: Servent " + myServentId + " released CS. LN_token[" + myServentId + "]=" + LN_token[myServentId]);


			for (int j = 0; j < SERVENT_COUNT; j++) {
				if (j != myServentId && RN[j] == LN_token[j] + 1 && !Q_token.contains(j)) {
					Q_token.add(j);
					timestampedStandardPrint("SK: Added servent " + j + " to token queue. Q_token: " + Q_token);
				}
			}

			if (!Q_token.isEmpty()) {
				int targetServentId = Q_token.poll();
				timestampedStandardPrint("SK: Servent " + myServentId + " sending token to servent " + targetServentId + ". Q_token after poll: " + Q_token);
				sendToken(targetServentId); // sendToken will set hasToken = false
			} else {
				timestampedStandardPrint("SK: Servent " + myServentId + " keeps token, queue is empty.");
			}
		} finally {
			skMutex.unlock();
		}
	}

	private static void broadcastTokenRequest(int sequenceNumber) {
		// Assumes skMutex is held by the caller (requestCSEntry)
		timestampedStandardPrint("SK: Servent " + myServentId + " broadcasting TOKEN_REQUEST with seq_num " + sequenceNumber);
		for (int i = 0; i < SERVENT_COUNT; i++) {
			if (i != myServentId) {
				ServentInfo targetServent = allServents[i];
				SKTokenRequestMessage specificRequestMessage = new SKTokenRequestMessage(
						myServentInfo.getListenerPort(),
						targetServent.getListenerPort(),
						myServentId, // The ID of the servent making the request
						sequenceNumber
				);
				MessageUtil.sendMessage(specificRequestMessage);
			}
		}
	}

	public static void sendToken(int targetServentId) {
		// Assumes skMutex is held by the caller (releaseCSEntry, SKTokenRequestHandler, passTokenOnShutdown)
		if (!hasToken) {
			timestampedErrorPrint("SK: ERROR! Servent " + myServentId + " tried to send token but doesn't have it.");
			return;
		}
		ServentInfo targetInfo = allServents[targetServentId];
		// Create a new queue instance for the message to avoid concurrent modification issues if Q_token is used directly
		SKTokenMessage tokenMessage = new SKTokenMessage(
				myServentInfo.getListenerPort(),
				targetInfo.getListenerPort(),
				Arrays.copyOf(LN_token, LN_token.length), // Send a copy of LN
				new LinkedBlockingQueue<>(Q_token)        // Send a copy of Q
		);
		MessageUtil.sendMessage(tokenMessage);
		timestampedStandardPrint("SK: Sent TOKEN from servent " + myServentId + " to servent " + targetServentId +
				". LN: " + Arrays.toString(LN_token) + ", Q: " + Q_token);
		hasToken = false;
		// LN_token and Q_token are now conceptually part of the sent token,
		// local copies will be overwritten when this node receives a token again.
	}


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

		// Initialize Suzuki-Kasami specific configurations
		initSuzukiKasami(serventId, properties);
		INITIALIZED = true;
	}


	private static void passTokenOnShutdown() {
		skMutex.lock();
		try {
			if (hasToken) {
				if (Q_token != null && !Q_token.isEmpty()) {
					int nextHolderId = Q_token.poll();
					timestampedStandardPrint("SK: Passing SK token to servent " + nextHolderId + " on shutdown.");
					sendToken(nextHolderId);
				} else {
					timestampedStandardPrint("SK: Holding SK token on shutdown, but queue is empty or null. Token remains with servent " + myServentId);
				}
			}
		} finally {
			skMutex.unlock();
		}
	}

	/**
	 * Graceful shutdown/cleanup before exit.
	 */
	public static void shutdown() {
		timestampedStandardPrint("SK: Initiating Suzuki-Kasami token passing on shutdown...");
		passTokenOnShutdown();

		timestampedStandardPrint("Cleaning up before exit…");

		if (listenerThread != null && listenerThread.isAlive()) {
			try {
				listenerThread.interrupt();
				timestampedStandardPrint("Listener thread interrupted.");
			} catch (Exception e) {
				timestampedErrorPrint("Greška pri gašenju listener niti: " + e.getMessage());
			}
		}

		if (serverSocket != null && !serverSocket.isClosed()) {
			try {
				serverSocket.close();
				timestampedStandardPrint("ServerSocket closed.");
			} catch (IOException e) {
				timestampedErrorPrint("Greška pri zatvaranju server socket‐a: " + e.getMessage());
			}
		}

		for (Thread t : workerThreads) {
			try {
				if (t.isAlive()) {
					timestampedStandardPrint("Waiting for worker thread " + t.getName() + " to finish …");
					t.join(2000);
				}
			} catch (InterruptedException ignored) { }
		}
		timestampedStandardPrint("Exiting now.");
		System.exit(0);
	}
}
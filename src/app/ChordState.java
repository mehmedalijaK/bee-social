package app;

import servent.message.AskGetMessage;
import servent.message.PutMessage;
import servent.message.WelcomeMessage;
import servent.message.util.MessageUtil;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Ova klasa implementira sav Chord state (successor table, predecessor, DHT vrednosti…)
 * i dodatno drži informaciju o tome da li je čvor public ili private, kao i koje servente pratimo.
 */
public class ChordState {

	public static int CHORD_SIZE;
	public static int chordHash(int value) {
		return 61 * value % CHORD_SIZE;
	}

	private int chordLevel; // log_2(CHORD_SIZE)

	private ServentInfo[] successorTable;
	private ServentInfo predecessorInfo;

	// Spisak svih poznatih čvorova (za izgradnju tabele preskoka)
	private List<ServentInfo> allNodeInfo;

	// DHT vrednosti koje ovaj čvor drži
	private Map<Integer, Integer> valueMap;

	// *** Nova polja za public/private vidljivost i listu pratilaca ***
	/** Ako je true, profil je public i svako može tražiti listu fajlova. Ako je false, samo pratitelji. */
	private boolean publicMode = true;
	/** Lista serventa koja su dobila „follow“ odobrenje */
	private List<ServentInfo> followers = new CopyOnWriteArrayList<>();
	/** Lista serventa koja su trenutno pending (poslali su follow zahtev, ali još nisu prihvaćeni) */
	private List<ServentInfo> pendingFollowers = new CopyOnWriteArrayList<>();
	// *******************************************************************

	public ChordState() {
		this.chordLevel = 1;
		int tmp = CHORD_SIZE;
		while (tmp != 2) {
			if (tmp % 2 != 0) { // nije stepen od 2
				throw new NumberFormatException();
			}
			tmp /= 2;
			this.chordLevel++;
		}

		successorTable = new ServentInfo[chordLevel];
		for (int i = 0; i < chordLevel; i++) {
			successorTable[i] = null;
		}

		predecessorInfo = null;
		valueMap = new HashMap<>();
		allNodeInfo = new ArrayList<>();
	}

	/**
	 * Poziva se jednom kada stigne WELCOME poruka od bootstrap servera.
	 */
	public void init(WelcomeMessage welcomeMsg) {
		// setujemo prvog successor-a
		successorTable[0] = new ServentInfo("localhost", welcomeMsg.getSenderPort());
		this.valueMap = welcomeMsg.getValues();

		// javimo bootstrap-u da nije bilo kolizije
		try {
			Socket bsSocket = new Socket("localhost", AppConfig.BOOTSTRAP_PORT);
			PrintWriter bsWriter = new PrintWriter(bsSocket.getOutputStream());
			bsWriter.write("New\n" + AppConfig.myServentInfo.getListenerPort() + "\n");
			bsWriter.flush();
			bsSocket.close();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Da li je ovaj čvor public?
	 */
	public boolean isPublic() {
		return publicMode;
	}

	/**
	 * Postavi da li je čvor public ili private.
	 */
	public void setPublicMode(boolean publicMode) {
		this.publicMode = publicMode;
	}

	/**
	 * Proverava da li je zadati servent u listi pratilaca.
	 */
	public boolean isFollower(ServentInfo si) {
		for (ServentInfo f : followers) {
			if (f.getChordId() == si.getChordId()
					&& f.getListenerPort() == si.getListenerPort()
					&& f.getIpAddress().equals(si.getIpAddress())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Dodaje novog pratioca (kada stigne FollowAccept).
	 */
	public void addFollower(ServentInfo si) {
		if (!isFollower(si)) {
			followers.add(si);
		}
	}

	/**
	 * Uklanja pratioca (ako zatreba unsubscribe).
	 */
	public void removeFollower(ServentInfo si) {
		followers.removeIf(f ->
				f.getChordId() == si.getChordId()
						&& f.getListenerPort() == si.getListenerPort()
						&& f.getIpAddress().equals(si.getIpAddress())
		);
	}

	/**
	 * Dodaje novog pending follower-a (kada stigne FollowRequest).
	 */
	public void addPendingFollower(ServentInfo si) {
		// ne dupliramo u pending, niti ako je već u followers
		if (!pendingFollowers.contains(si) && !followers.contains(si)) {
			pendingFollowers.add(si);
		}
	}

	/**
	 * Proverava da li postoji pending zahtev od prosleđenog serventa.
	 */
	public boolean hasPendingFollower(ServentInfo si) {
		return pendingFollowers.contains(si);
	}

	/**
	 * Prihvata pending follower-a i prebacuje ga u followers.
	 */
	public void acceptFollower(ServentInfo si) {
		pendingFollowers.remove(si);
		if (!followers.contains(si)) {
			followers.add(si);
		}
	}

	/**
	 * Vraća nepromenjivu listu pending follower-a.
	 */
	public List<ServentInfo> getPendingFollowers() {
		return Collections.unmodifiableList(pendingFollowers);
	}

	/**
	 * Vraća nepromenjivu listu confirmed follower-a.
	 */
	public List<ServentInfo> getFollowers() {
		return Collections.unmodifiableList(followers);
	}
	// *******************************************************************

	public int getChordLevel() {
		return chordLevel;
	}

	public List<ServentInfo> getAllNodeInfos() {
		return Collections.unmodifiableList(allNodeInfo);
	}

	public ServentInfo[] getSuccessorTable() {
		return successorTable;
	}

	public int getNextNodePort() {
		return successorTable[0].getListenerPort();
	}

	public ServentInfo getPredecessor() {
		return predecessorInfo;
	}

	public void setPredecessor(ServentInfo newNodeInfo) {
		this.predecessorInfo = newNodeInfo;
	}

	public Map<Integer, Integer> getValueMap() {
		return valueMap;
	}

	public void setValueMap(Map<Integer, Integer> valueMap) {
		this.valueMap = valueMap;
	}

	public boolean isCollision(int chordId) {
		if (chordId == AppConfig.myServentInfo.getChordId()) {
			return true;
		}
		for (ServentInfo serventInfo : allNodeInfo) {
			if (serventInfo.getChordId() == chordId) {
				return true;
			}
		}
		return false;
	}

	public boolean isKeyMine(int key) {
		if (predecessorInfo == null) {
			return true;
		}

		int predecessorChordId = predecessorInfo.getChordId();
		int myChordId = AppConfig.myServentInfo.getChordId();

		if (predecessorChordId < myChordId) { // bez overflow-a
			return (key <= myChordId && key > predecessorChordId);
		} else { // overflow
			return (key <= myChordId || key > predecessorChordId);
		}
	}

	public ServentInfo getNextNodeForKey(int key) {
		if (isKeyMine(key)) {
			return AppConfig.myServentInfo;
		}

		int startInd = 0;
		if (key < AppConfig.myServentInfo.getChordId()) {
			int skip = 1;
			while (successorTable[skip].getChordId() > successorTable[startInd].getChordId()) {
				startInd++;
				skip++;
			}
		}

		int previousId = successorTable[startInd].getChordId();

		for (int i = startInd + 1; i < successorTable.length; i++) {
			if (successorTable[i] == null) {
				AppConfig.timestampedErrorPrint("Couldn't find successor for " + key);
				break;
			}

			int successorId = successorTable[i].getChordId();

			if (successorId >= key) {
				return successorTable[i - 1];
			}
			if (key > previousId && successorId < previousId) { // overflow
				return successorTable[i - 1];
			}
			previousId = successorId;
		}
		return successorTable[0];
	}

	private void updateSuccessorTable() {
		int currentNodeIndex = 0;
		ServentInfo currentNode = allNodeInfo.get(currentNodeIndex);
		successorTable[0] = currentNode;

		int currentIncrement = 2;
		ServentInfo previousNode = AppConfig.myServentInfo;

		for (int i = 1; i < chordLevel; i++, currentIncrement *= 2) {
			int currentValue = (AppConfig.myServentInfo.getChordId() + currentIncrement) % CHORD_SIZE;

			int currentId = currentNode.getChordId();
			int previousId = previousNode.getChordId();

			while (true) {
				if (currentValue > currentId) {
					if (currentId > previousId || currentValue < previousId) {
						previousId = currentId;
						currentNodeIndex = (currentNodeIndex + 1) % allNodeInfo.size();
						currentNode = allNodeInfo.get(currentNodeIndex);
						currentId = currentNode.getChordId();
					} else {
						successorTable[i] = currentNode;
						break;
					}
				} else {
					ServentInfo nextNode = allNodeInfo.get((currentNodeIndex + 1) % allNodeInfo.size());
					int nextNodeId = nextNode.getChordId();
					if (nextNodeId < currentId && currentValue <= nextNodeId) {
						previousId = currentId;
						currentNodeIndex = (currentNodeIndex + 1) % allNodeInfo.size();
						currentNode = allNodeInfo.get(currentNodeIndex);
						currentId = currentNode.getChordId();
					} else {
						successorTable[i] = currentNode;
						break;
					}
				}
			}
		}
	}

	public void addNodes(List<ServentInfo> newNodes) {
		allNodeInfo.addAll(newNodes);

		allNodeInfo.sort(Comparator.comparingInt(ServentInfo::getChordId));

		List<ServentInfo> newList = new ArrayList<>();
		List<ServentInfo> newList2 = new ArrayList<>();

		int myId = AppConfig.myServentInfo.getChordId();
		for (ServentInfo serventInfo : allNodeInfo) {
			if (serventInfo.getChordId() < myId) {
				newList2.add(serventInfo);
			} else {
				newList.add(serventInfo);
			}
		}

		allNodeInfo.clear();
		allNodeInfo.addAll(newList);
		allNodeInfo.addAll(newList2);
		if (newList2.size() > 0) {
			predecessorInfo = newList2.get(newList2.size() - 1);
		} else {
			predecessorInfo = newList.get(newList.size() - 1);
		}

		updateSuccessorTable();
	}

	public void putValue(int key, int value) {
		if (isKeyMine(key)) {
			valueMap.put(key, value);
		} else {
			ServentInfo nextNode = getNextNodeForKey(key);
			PutMessage pm = new PutMessage(
					AppConfig.myServentInfo.getListenerPort(),
					nextNode.getListenerPort(),
					key, value
			);
			MessageUtil.sendMessage(pm);
		}
	}

	public int getValue(int key) {
		if (isKeyMine(key)) {
			return valueMap.getOrDefault(key, -1);
		}

		ServentInfo nextNode = getNextNodeForKey(key);
		AskGetMessage agm = new AskGetMessage(
				AppConfig.myServentInfo.getListenerPort(),
				nextNode.getListenerPort(),
				String.valueOf(key)
		);
		MessageUtil.sendMessage(agm);
		return -2;
	}
}

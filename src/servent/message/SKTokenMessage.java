package servent.message;

import java.util.Arrays;
import java.util.Queue;
import java.util.LinkedList;

public class SKTokenMessage extends BasicMessage {
    private static final long serialVersionUID = 2L; // Example version UID
    private final int[] lnArray;
    private final Queue<Integer> queue;

    public SKTokenMessage(int senderPort, int receiverPort, int[] lnArray, Queue<Integer> queue) {
        super(MessageType.SK_TOKEN, senderPort, receiverPort,
                "SK_TOKEN: LN=" + Arrays.toString(lnArray) + ", Q=" + queue.toString());
        this.lnArray = lnArray;
        // Ensure a serializable queue implementation is used, LinkedList is fine.
        this.queue = new LinkedList<>(queue); // Make a copy to ensure serializability and immutability of message
    }

    public int[] getLN() {
        return lnArray;
    }

    public Queue<Integer> getQ() {
        return queue;
    }
}
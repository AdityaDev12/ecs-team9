package Protocol;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Carries messages inside one program, with no network.
 * Used with FakeDoor so the harness can run before the real GUI exists.
 */
public final class LocalMessenger implements Messenger {

    private final AtomicInteger nextId = new AtomicInteger(1);
    private final Map<Integer, List<byte[]>> subs = new ConcurrentHashMap<>();
    private final Map<Integer, ConcurrentLinkedQueue<Message>> inboxes = new ConcurrentHashMap<>();

    @Override
    public int register() {
        int id = nextId.getAndIncrement();
        subs.put(id, new ArrayList<>());
        inboxes.put(id, new ConcurrentLinkedQueue<>());
        return id;
    }

    @Override
    public void subscribe(int id, byte topic, byte sub) {
        List<byte[]> list = subs.get(id);
        if (list == null) {
            throw new IllegalStateException("id " + id + " never registered");
        }
        synchronized (list) {
            list.add(new byte[] { topic, sub });
        }
    }

    @Override
    public void publish(int id, Message message) {
        // Pack and unpack even though we never leave the program, so that
        // anything wrong with the 8-byte format shows up here and not on demo day.
        Message m = Message.fromBytes(message.toBytes());

        for (Map.Entry<Integer, List<byte[]>> e : subs.entrySet()) {
            int other = e.getKey();
            if (other == id) {
                continue;                       // don't send it back to the sender
            }
            boolean wanted = false;
            List<byte[]> list = e.getValue();
            synchronized (list) {
                for (byte[] s : list) {
                    if (matches(s[0], s[1], m)) {
                        wanted = true;
                        break;
                    }
                }
            }
            if (wanted) {
                inboxes.get(other).add(m);
            }
        }
    }

    @Override
    public Message getMsg(int id, byte topic, byte sub) {
        ConcurrentLinkedQueue<Message> inbox = inboxes.get(id);
        if (inbox == null) {
            throw new IllegalStateException("id " + id + " never registered");
        }
        if (topic == Codes.ANY && sub == Codes.ANY) {
            return inbox.poll();
        }
        for (Message m : inbox) {
            if (matches(topic, sub, m) && inbox.remove(m)) {
                return m;
            }
        }
        return null;
    }

    private static boolean matches(byte topic, byte sub, Message m) {
        boolean t = (topic == Codes.ANY) || (topic == m.topic());
        boolean s = (sub == Codes.ANY) || (sub == m.sub());
        return t && s;
    }
}
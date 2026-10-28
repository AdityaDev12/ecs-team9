package Protocol;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class NetworkMessenger implements Messenger {

    private final Socket socket;
    private final DataInputStream in;
    private final OutputStream out;
    private final ConcurrentLinkedQueue<Message> inbox = new ConcurrentLinkedQueue<>();
    private final List<byte[]> filters = new ArrayList<>();
    private volatile boolean running = true;

    public NetworkMessenger(String host, int port) throws IOException {
        socket = new Socket(host, port);
        socket.setTcpNoDelay(true);
        in = new DataInputStream(socket.getInputStream());
        out = socket.getOutputStream();

        Thread reader = new Thread(this::readLoop, "messenger-reader");
        reader.setDaemon(true);
        reader.start();
    }

    private void readLoop() {
        byte[] buf = new byte[Message.SIZE];
        while (running) {
            try {
                in.readFully(buf);
                Message m = Message.fromBytes(buf.clone());
                if (wanted(m)) {
                    inbox.add(m);
                }
            } catch (IOException e) {
                if (running) {
                    System.err.println("[net] connection lost: " + e.getMessage());
                }
                return;
            } catch (IllegalArgumentException bad) {
                System.err.println("[net] bad message dropped: " + bad.getMessage());
            }
        }
    }

    private boolean wanted(Message m) {
        synchronized (filters) {
            if (filters.isEmpty()) {
                return true;
            }
            for (byte[] s : filters) {
                boolean t = (s[0] == Codes.ANY) || (s[0] == m.topic());
                boolean sub = (s[1] == Codes.ANY) || (s[1] == m.sub());
                if (t && sub) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public int register() {
        return 1;   // TODO ask Michelle whether the server assigns this
    }

    @Override
    public void subscribe(int id, byte topic, byte sub) {
        synchronized (filters) {
            filters.add(new byte[] { topic, sub });
        }
    }

    @Override
    public void publish(int id, Message message) {
        try {
            out.write(message.toBytes());
            out.flush();
        } catch (IOException e) {
            throw new IllegalStateException("could not send: " + e.getMessage(), e);
        }
    }

    @Override
    public Message getMsg(int id, byte topic, byte sub) {
        if (topic == Codes.ANY && sub == Codes.ANY) {
            return inbox.poll();
        }
        for (Message m : inbox) {
            boolean t = (topic == Codes.ANY) || (topic == m.topic());
            boolean s = (sub == Codes.ANY) || (sub == m.sub());
            if (t && s && inbox.remove(m)) {
                return m;
            }
        }
        return null;
    }

    public void close() {
        running = false;
        try {
            socket.close();
        } catch (IOException ignored) {
            // shutting down anyway
        }
    }
}
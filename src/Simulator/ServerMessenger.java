package Simulator;

import Protocol.Codes;
import Protocol.Message;
import Protocol.Messenger;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;


public final class ServerMessenger implements Messenger {

    private final int port;
    private final ConcurrentLinkedQueue<Message> inbox = new ConcurrentLinkedQueue<>();
    private final List<byte[]> filters = new ArrayList<>();
    private volatile OutputStream out;
    private volatile boolean running = true;
    private ServerSocket server;
    private int nextId = 1;

    public ServerMessenger(int port) throws IOException {
        this.port = port;
        server = new ServerSocket(port);
        Thread t = new Thread(this::acceptLoop, "door-server");
        t.setDaemon(true);
        t.start();
        System.out.println("[server] waiting for harness on port " + port);
    }

    private void acceptLoop() {
        while (running) {
            try (Socket socket = server.accept()) {
                socket.setTcpNoDelay(true);
                System.out.println("[server] harness connected");
                out = socket.getOutputStream();
                DataInputStream in = new DataInputStream(socket.getInputStream());
                byte[] buf = new byte[Message.SIZE];
                while (running) {
                    in.readFully(buf);
                    try {
                        Message m = Message.fromBytes(buf.clone());
                        if (wanted(m)) {
                            inbox.add(m);
                        }
                    } catch (IllegalArgumentException bad) {
                        System.err.println("[server] bad message dropped: " + bad.getMessage());
                    }
                }
            } catch (IOException e) {
                if (running) {
                    System.out.println("[server] harness disconnected, waiting again on port " + port);
                }
            } finally {
                out = null;
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
    public synchronized int register() {
        return nextId++;
    }

    @Override
    public void subscribe(int id, byte topic, byte sub) {
        synchronized (filters) {
            filters.add(new byte[] { topic, sub });
        }
    }

    @Override
    public void publish(int id, Message message) {
        OutputStream o = out;
        if (o == null) {
            return;   // no harness connected yet; the GUI still works on its own
        }
        try {
            synchronized (this) {
                o.write(message.toBytes());
                o.flush();
            }
        } catch (IOException e) {
            System.err.println("[server] could not send: " + e.getMessage());
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
            server.close();
        } catch (IOException ignored) {
        }
    }
}
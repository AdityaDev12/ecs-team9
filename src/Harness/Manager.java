package Harness;

import Protocol.Codes;
import Protocol.Message;
import Simulator.DoorSimulatorMain;
import Protocol.ServerMessenger;
import javafx.application.Application;
import java.io.IOException;

/**
 * coordinates commands from the test harness with the elevator door system.
 * doesn't implement door movement or safety rules
 */
public class Manager {
    private final int port;
    private final byte elevator;
    private final EventLog log;
    private ServerMessenger messenger;
    private int id;
    private volatile boolean running = false;

    public Manager(int port, byte elevator, EventLog log) {
        this.port = port;
        this.elevator = elevator;
        this.log = log;
    }

    // starts server and log listener, then launches GUI
    public void start() {
        try {
            messenger = new ServerMessenger(port);
        } catch (IOException e) {
            log.error("Manager: Could not open server port " + port + ": " + e.getMessage());
            return;
        }
        id = messenger.register();
        subscribeToMessages();
        running = true;

        Thread listener = new Thread(this::listen, "manager-listener");
        listener.setDaemon(true);
        listener.start();

        log.note("Manager: Started on port " + port);
        log.note("Manager: id " + id + ", elevator " + elevator);
        log.note("Manager: Launching Door GUI...");

        try {
            // javafx runs on its own app thread
            Application.launch(DoorSimulatorMain.class,
                    new String[] {
                            "--host", "127.0.0.1",
                            "--port", Integer.toString(port),
                            "--elevator", Byte.toString(elevator)
                    });
        } catch (IllegalStateException e) {
            log.error("Manager: Could not launch JavaFX: " + e.getMessage());
        } finally {
            shutdown();
        }
    }

    private void subscribeToMessages() {
        // requests coming from the gui
        messenger.subscribe(id, Codes.CMD, elevator);
        messenger.subscribe(id, Codes.SPEED, elevator);
        messenger.subscribe(id, Codes.SENSOR, elevator);
        // reports coming from the gui
        messenger.subscribe(id, Codes.STATE, elevator);
        // system requests and responses
        messenger.subscribe(id, Codes.SYS, Codes.ANY);
    }

    private void listen() {
        while (running) {
            Message message = messenger.getMsg(id, Codes.ANY, Codes.ANY);
            if (message == null) {
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
                continue;
            }
            // log every message from the GUI
            log.received(message);
            // relay requests that the GUI should execute
            if (shouldRelay(message)) {
                log.sent(message);
                messenger.publish(id, message);
            }
        }
    }

    private boolean shouldRelay(Message message) {
        if (message.sub() != elevator && !(message.topic() == Codes.SYS && message.sub() == Codes.ANY)) {
            return false;
        }
        if (message.topic() == Codes.CMD || message.topic() == Codes.SPEED || message.topic() == Codes.SENSOR) {
            return true;
        }
        // relay a ping
        return message.topic() == Codes.SYS && message.body() == Codes.PING;
    }

    // information about participant
    public int getId() {
        return id;
    }
    public byte getElevator() {
        return elevator;
    }

    private void shutdown() {
        running = false;
        if (messenger != null) {
            messenger.close();
        }
        log.note("Manager: Stopped.");
    }
}

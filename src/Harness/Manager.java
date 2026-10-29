package Harness;

import Protocol.Codes;
import Protocol.Message;
import Simulator.DoorSimulatorMain;
import Protocol.ServerMessenger;
import javafx.application.Application;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * coordinates commands from the test harness with the elevator door system.
 * doesn't implement door movement or safety rules
 */
public class Manager {
    private final int port;
    private final byte elevator;
    private final EventLog log;
    private final String scriptPath;
    private ServerMessenger messenger;
    private int id;
    private volatile boolean running = false;

    public Manager(int port, byte elevator, EventLog log, String scriptPath) {
        this.port = port;
        this.elevator = elevator;
        this.log = log;
        this.scriptPath = scriptPath;
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

        // start script separately
        if (scriptPath != null) {
            Thread scriptThread = new Thread(this::waitForGuiAndRunScript, "demo-script");
            scriptThread.setDaemon(true);
            scriptThread.start();
        }

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

    // script startup
    private void waitForGuiAndRunScript() {
        log.note("Manager: Waiting for GUI to connect before running " + scriptPath);
        long deadline = System.currentTimeMillis() + 30_000;
        while (running && !messenger.isConnected() && System.currentTimeMillis() < deadline) {
            sleep(100);
        }
        if (!running) {
            return;
        }
        if (!messenger.isConnected()) {
            log.error("Manager: GUI did not connect; script was not started.");
            return;
        }
        log.note("Manager: GUI connected. Starting demo script.");
        runScript(scriptPath);
        if (running) {
            log.note("Demo script finished.");
        }
    }

    // script file reader
    private void runScript(String filename) {
        Path path = Path.of(filename);
        log.note("Running script: " + path);
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String line;
            while (running && (line = reader.readLine()) != null) {
                String commandLine = line.trim();
                if (commandLine.isEmpty() || commandLine.startsWith("#")) {
                    continue;
                }
                runCommand(commandLine, path.getParent());
            }
        } catch (IOException e) {
            log.error("Manager: Cannot read script " + path + ": " + e.getMessage());
        }
    }

    private void runCommand(String line, Path scriptDirectory) {
        String[] parts = line.split("\\s+");
        String command = parts[0].toLowerCase();
        try {
            switch (command) {
                case "open" ->
                    send(Codes.CMD, Codes.OPEN, 0);
                case "close" ->
                    send(Codes.CMD, Codes.CLOSE, 0);
                case "stop" ->
                        send(Codes.CMD, Codes.STOP, 0);
                case "speed" -> {
                    requireArguments(parts, 2, command);
                    int speed = Integer.parseInt(parts[1]);
                    send(Codes.SPEED, Codes.SET_SPEED, speed);
                }
                case "ping" ->
                    send(Codes.SYS, Codes.PING, 0);
                case "press", "release" -> {
                    requireArguments(parts, 2, command);
                    setSensor(parts[1], command.equals("press"));
                }
                case "wait" -> {
                    requireArguments(parts, 2, command);
                    long milliseconds = Long.parseLong(parts[1]);
                    if (milliseconds < 0) {
                        log.error("Manager: Wait duration cannot be negative.");
                    } else {
                        sleep(milliseconds);
                    }
                }
                case "echo" -> {
                    String text = line.length() > 4 ? line.substring(4).trim() : "";
                    log.note(text);
                }
                case "run" -> {
                    requireArguments(parts, 2, command);
                    Path child = Path.of(parts[1]);
                    // resolve nested paths
                    if (!child.isAbsolute() && scriptDirectory != null) {
                        child = scriptDirectory.resolve(child);
                    }
                    runScript(child.normalize().toString());
                }
                default ->
                    log.error("Manager: Unknown script command: " + command);
            }
        } catch (IllegalArgumentException e) {
            log.error("Manager: Invalid argument in command '" + line + "': " + e.getMessage());
        } catch (IndexOutOfBoundsException e) {
            log.error("Manager: Missing argument for command '" + command + "'.");
        }
    }

    private void setSensor(String which, boolean active) {
        byte body;
        if (which.equalsIgnoreCase("obstruction")) {
            body = active ? Codes.OBSTRUCTION_ON : Codes.OBSTRUCTION_OFF;
        } else if (which.equalsIgnoreCase("overload")) {
            body = active ? Codes.OVERLOAD_ON : Codes.OVERLOAD_OFF;
        } else {
            log.error("Manager: Unknown sensor '" + which + "' (use obstruction or overload).");
            return;
        }
        send(Codes.SENSOR, body, 0);
    }

    private void requireArguments(
            String[] parts, int expected, String command) {
        if (parts.length < expected) {
            throw new IndexOutOfBoundsException("Manager: Missing argument for " + command);
        }
    }

    private void send(byte topic, byte body, int value) {
        Message message = new Message(topic, elevator, body, value);
        log.sent(message);
        messenger.publish(id, message);
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

    private static void sleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch(InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

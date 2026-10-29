package Harness;

import Protocol.Codes;
import Protocol.LocalMessenger;
import Protocol.Message;
import Protocol.Messenger;
import Protocol.NetworkMessenger;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Starts the application and delegates control
 */
public final class Main {

    private Messenger messenger;
    private Manager manager;
    private byte elevator = 1;
    private EventLog log;

    private FakeDoor fake;             // null when the real GUI is connected
    private volatile boolean running = true;

    public static void main(String[] args) {
        new Main().start(args);
    }

    private void start(String[] args) {
        String host = null;
        int port = 0;
        String script = null;
        boolean hex = true;

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--net")) {
                host = args[++i];
                port = Integer.parseInt(args[++i]);
            } else if (args[i].equals("--script")) {
                script = args[++i];
            } else if (args[i].equals("--elevator")) {
                elevator = (byte) Integer.parseInt(args[++i]);
            } else if (args[i].equals("--no-hex")) {
                hex = false;
            } else {
                System.err.println("unknown option: " + args[i]);
                return;
            }
        }

        log = new EventLog(hex);

        if (host != null) {
            try {
                messenger = new NetworkMessenger(host, port);
                log.note("connected to " + host + ":" + port);
            } catch (IOException e) {
                log.error("could not connect: " + e.getMessage());
                log.error("using FakeDoor instead so the harness still runs");
                messenger = new LocalMessenger();
            }
        } else {
            messenger = new LocalMessenger();
        }

        manager = new Manager(messenger, elevator, log);
        log.note("Main: Harness Manager is id " + manager.getId() + ", driving elevator " + elevator);

        if (messenger instanceof LocalMessenger) {
            fake = new FakeDoor(messenger, elevator);
            Thread t = new Thread(fake, "fake-door");
            t.setDaemon(true);
            t.start();
            log.note("FakeDoor started as id " + fake.id());
        }

        Thread listener = new Thread(this::listen, "listener");
        listener.setDaemon(true);
        listener.start();

        if (script != null) {
            runScript(script);
            sleep(500);                 // let the last events arrive
            log.note("done");
        } else {
            log.note("type 'help' for commands");
            typeCommands();
        }
        shutdown();
    }

    /** asks the Manager for messages and prints whatever the GUI reports. */
    private void listen() {
        while (running) {
            Message message = manager.pollMessage();
            if (message == null) {
                sleep(20);
            } else {
                log.received(message);
            }
        }
    }

    private void typeCommands() {
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        try {
            String line;
            while (running && (line = in.readLine()) != null) {
                if (!run(line.trim())) {
                    return;
                }
            }
        } catch (IOException e) {
            log.error("console closed: " + e.getMessage());
        }
    }

    /** Runs the commands in a file, one per line. Ignores blanks and # comments. */
    private boolean runScript(String path) {
        log.note("running script: " + path);
        try (BufferedReader r = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = r.readLine()) != null) {
                String s = line.trim();
                if (s.isEmpty() || s.startsWith("#")) {
                    continue;
                }
                if (!run(s)) {
                    return false;
                }
            }
        } catch (IOException e) {
            log.error("cannot read " + path + ": " + e.getMessage());
        }
        log.note("script finished: " + path);
        return true;
    }

    /** Does one command. Returns false if it was 'quit'. */
    private boolean run(String line) {
        if (line.isEmpty()) {
            return true;
        }
        String[] parts = line.split("\\s+");
        String cmd = parts[0].toLowerCase();

        try {
            if (cmd.equals("open")) {
                manager.openDoor();
            } else if (cmd.equals("close")) {
                manager.closeDoor();
            } else if (cmd.equals("stop")) {
                manager.stopDoor();
            } else if (cmd.equals("speed")) {
                manager.setSpeed(Integer.parseInt(parts[1]));
            } else if (cmd.equals("ping")) {
                manager.ping();
            } else if (cmd.equals("press") || cmd.equals("release")) {
                sensor(parts[1], cmd.equals("press"));
            } else if (cmd.equals("status")) {
                log.note(fake != null ? fake.status() : "real GUI - look at the window");
            } else if (cmd.equals("wait")) {
                sleep(Long.parseLong(parts[1]));
            } else if (cmd.equals("echo")) {
                log.note(line.substring(4).trim());
            } else if (cmd.equals("run")) {
                return runScript(parts[1]);
            } else if (cmd.equals("help")) {
                help();
            } else if (cmd.equals("quit") || cmd.equals("exit")) {
                return false;
            } else {
                log.error("unknown command: " + cmd + " (try 'help')");
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            log.error("missing argument for '" + cmd + "'");
        } catch (NumberFormatException e) {
            log.error("that should be a number");
        }
        return true;
    }

    /**
     * Obstruction and overload are buttons a person clicks in the GUI. The
     * harness can only fake them against FakeDoor.
     */
    private void sensor(String which, boolean on) {
        if (fake == null) {
            log.error("press/release only works with FakeDoor - click the real button");
            return;
        }
        if (which.equalsIgnoreCase("obstruction")) {
            if (on) fake.obstructionOn(); else fake.obstructionOff();
        } else if (which.equalsIgnoreCase("overload")) {
            if (on) fake.overloadOn(); else fake.overloadOff();
        } else {
            log.error("unknown sensor: " + which + " (obstruction or overload)");
        }
    }

    private void shutdown() {
        running = false;
        if (fake != null) {
            fake.stop();
        }
        if (messenger instanceof NetworkMessenger) {
            ((NetworkMessenger) messenger).close();
        }
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void help() {
        System.out.println("  open / close / stop     move the doors");
        System.out.println("  speed <n>               set door speed");
        System.out.println("  press obstruction       fake the obstruction button");
        System.out.println("  press overload          fake the overload button");
        System.out.println("  release obstruction     clear it");
        System.out.println("  release overload        clear it");
        System.out.println("  ping                    check the GUI is alive");
        System.out.println("  status                  show FakeDoor's state");
        System.out.println("  wait <ms>               pause (for scripts)");
        System.out.println("  echo <text>             print a label");
        System.out.println("  run <file>              replay a script");
        System.out.println("  quit                    exit");
    }
}
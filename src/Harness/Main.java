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
 * Creates event log and starts the Manager
 */

public final class Main {
    public static void main(String[] args) {
        int port = 5001;
        byte elevator = 1;
        boolean hex = true;
        String script = null;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--port" -> {
                    if (i + 1 >= args.length) {
                        System.err.println("Main: Missing port number.");
                        return;
                    }
                    port = Integer.parseInt(args[++i]);
                }
                case "--elevator" -> {
                    if (i + 1 >= args.length) {
                        System.err.println("Main: Missing elevator number.");
                        return;
                    }
                    elevator = (byte) Integer.parseInt(args[++i]);
                }
                case "--script" -> {
                    if (i + 1 >= args.length) {
                        System.err.println("Main: Missing script filename.");
                        return;
                    }
                    script = args[++i];
                }
                case "--no-hex" -> hex = false;
                default -> {
                    System.err.println("Main: Unknown option: " + args[i]);
                    printUsage();
                    return;
                }
            }
        }
        if (port < 1 || port > 65535) {
            System.err.println("Main: Port must be between 1 and 65535.");
            return;
        }
        if (elevator < 1 || elevator > 4) {
            System.err.println("Main: Elevator must be between 1 and 4.");
            return;
        }
        EventLog log = new EventLog(hex);
        log.note("Main: Starting the Elevator Control System test harness.");
        if (script != null) {
            log.note("Main: Demo script: " + script);
        }
        Manager manager = new Manager (port, elevator, log, script);
        manager.start();
    }

    private static void printUsage() {
        System.out.println("Usage: java Harness.Main "
                + "[--port PORT] "
                + "[--elevator 1-4] "
                + "[--script FILE] "
                + "[--no-hex]");
    }
}
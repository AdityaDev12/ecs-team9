import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

import Harness.FakeDoor;
import Protocol.LocalMessenger;
import Protocol.Codes;
import Protocol.Message;
import Protocol.Messenger;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class DoorsGUITest extends Application {
    private static final byte ELEVATOR = 1;
    private Messenger messenger;
    private FakeDoor fakeDoor;
    private DoorsGUI doors;
    private Stage stage;

    private int consoleId;
    private volatile boolean running = true;

    @Override
    public void start(Stage stage) {

        // REVISED CODE, START DELETE FROM HERE TO REPAIR
        this.stage = stage;

        // one shared messenger for entire application
        messenger = new LocalMessenger();
        // terminal harness registers with the shared messenger
        consoleId = messenger.register();
        messenger.subscribe(consoleId, Codes.STATE, Codes.ANY);
        messenger.subscribe(consoleId, Codes.SENSOR, Codes.ANY);
        messenger.subscribe(consoleId, Codes.SYS, Codes.ANY);

        // one simulated door shared by terminal commands and GUI
        fakeDoor = new FakeDoor(messenger, ELEVATOR);
        Thread fakeDoorThread = new Thread(fakeDoor, "fake-door");
        fakeDoorThread.setDaemon(true);
        fakeDoorThread.start();

        // the GUI uses exact same messenger
        doors = new DoorsGUI(messenger, ELEVATOR);

        Scene scene = new Scene(doors, 800, 600);

        stage.setTitle("Elevator Door Assembly");
        stage.setScene(scene);
        stage.setResizable(false);

        stage.setOnCloseRequest(event -> shutdown());
        stage.show();

        // print incoming state/sensor/system messages
        Thread listener = new Thread(this::listenForMessages, "terminal-message-listener");
        listener.setDaemon(true);
        listener.start();

        // read terminal input separately so JavaFX stays responsive
        Thread console = new Thread(this::runTerminal, "terminal-command-loop");
        console.setDaemon(true);
        console.start();
        System.out.println("GUITest: Terminal commands ready. Type 'help for commands.");
    }

    private void listenForMessages() {
        while (running) {
            Message message = messenger.getMsg(consoleId, Codes.ANY, Codes.ANY);
            if (message == null) {
                sleep(20);
            } else {
                System.out.println("GUITest: Received: " + message);
            }
        }
    }

    private void runTerminal() {
        BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
        try {
            String line;
            while (running && (line = input.readLine()) != null) {
                if (!runCommand(line.trim())) {
                    break;
                }
            }
        } catch (IOException e) {
            if (running) {
                System.err.println("GUITest: Terminal input error: " + e.getMessage());
            }
        }
    }

    private boolean runCommand(String line) {
        if (line.isEmpty()) {
            return true;
        }
        String[] parts = line.split("\\s+");
        String command = parts[0].toLowerCase();
        try {
            switch (command) {
                case "open":
                    send(Codes.CMD, Codes.OPEN, 0);
                    break;
                case "close":
                    send(Codes.CMD, Codes.CLOSE, 0);
                    break;
                case "stop":
                    send(Codes.CMD, Codes.STOP, 0);
                    break;
                case "speed":
                    if (parts.length < 2) {
                        System.out.println("GUITest Usage: speed <number>");
                        break;
                    }
                    send(Codes.SPEED, Codes.SET_SPEED, Integer.parseInt(parts[1]));
                    break;
                case "ping":
                    send(Codes.SYS, Codes.PING, 0);
                    break;
                case "press":
                case "release":
                    if (parts.length < 2) {
                        System.out.println("GUITest Usage: press/release obstruction|overload");
                        break;
                    }
                    boolean turnOn = command.equals("press");
                    String sensor = parts[1].toLowerCase();
                    if (sensor.equals("obstruction")) {
                        if (turnOn) {
                            fakeDoor.obstructionOn();
                        } else {
                            fakeDoor.obstructionOff();
                        }
                    } else if (sensor.equals("overload")) {
                        if (turnOn) {
                            fakeDoor.overloadOn();;
                        } else {
                            fakeDoor.overloadOff();
                        }
                    } else {
                        System.out.println("GUITest: Unknown sensor: use obstruction or overload");
                    }
                    break;
                case "status":
                    System.out.println(fakeDoor.status());
                    break;
                case "help":
                    printHelp();
                    break;
                case "quit":
                case "exit":
                    Platform.runLater(stage::close);
                    return false;
                default:
                    System.out.println("GUITest: Unknown command: Type 'help'.");
            }
        } catch (NumberFormatException e) {
            System.out.println("GUITest: The speed must be a number.");
        }
        return true;
    }
    private void send(byte topic, byte body, int value) {
        Message message = new Message(topic, ELEVATOR, body, value);
        System.out.println("GUITest: SEND " + message);
        messenger.publish(consoleId,message);
    }

    private void printHelp() {
        System.out.println("Commands:");
        System.out.println("  open / close / stop");
        System.out.println("  speed <number>");
        System.out.println("  press obstruction");
        System.out.println("  release obstruction");
        System.out.println("  press overload");
        System.out.println("  release overload");
        System.out.println("  ping");
        System.out.println("  status");
        System.out.println("  help");
        System.out.println("  quit");
    }

    private void shutdown() {
        running = false;
        if (doors != null) {
            doors.close();
        }
        if (fakeDoor != null) {
            fakeDoor.stop();
        }
    }
    @Override
    public void stop() {
        shutdown();
    }

    private static void sleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}

        //ORIGINAL CODE STARTS HERE; UNCOMMENT TO REPAIR
//        DoorsGUI doors = new DoorsGUI();
//
//        Scene scene = new Scene(doors, 800, 600);
//
//        stage.setTitle("Elevator Door Assembly");
//        stage.setScene(scene);
//        stage.setResizable(false);
//
//        stage.setOnCloseRequest(event -> {
//            doors.close();
//            fakeDoor.stop();
//        });
//
//        stage.show();
//    }
//
//    public static void main(String[] args) {
//        launch(args);
//    }
//}
package Harness;

import SoftwareBus.SoftwareBus;
import SoftwareBus.Message;
import Protocol.DoorProtocol;
import Simulator.DoorController;
import Simulator.DoorGUIMain;
import Simulator.DoorSimulatorMain;
import javafx.application.Application;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import java.util.UUID;
import java.util.Scanner;


/**
 * Coordinates communication and operations for Door subsystem:
 *   Connects to the SoftwareBus, registers/subscribes to messages
 *   Forwards instructions to DoorController.
 *   Receives door status updates and publishes to the SoftwareBus
 */

public class Manager {
    private final SoftwareBus bus;
    private final int elevator;
    private UUID managerId;
    private DoorController controller;
    private Timeline busPoller; // checks SoftwareBus for incoming messages
    private volatile boolean overload = false; // local record of sensor condition
    private volatile boolean obstruction = false; // local record of sensor condition

//    // uncomment this constructor when testing on router
//    public Manager(String routerIp, int routerPort, int elevator) {
//
//        this.bus = new SoftwareBus(routerIp, routerPort);
//        this.elevator = elevator;
//    }

    // uncomment this constructor when testing locally
    public Manager(String routerIp, int routerPort, int elevator) {
        this.bus = new SoftwareBus();
        this.elevator = elevator;
    }

    // constructor uses shared bus supplied by Elevator Control Board
    public Manager(SoftwareBus sharedBus, int elevator) {
        this.bus = sharedBus;
        this.elevator = elevator;
    }

    // start Door subsystem
    public void start() {
        // register Manager with SoftwareBus
        managerId = bus.register();
        System.out.println(
                "[DoorManager] Manager connected to Software Bus.");
        System.out.println(
                "[DoorManager] Managing Elevator " + elevator);
        // subscribe to incoming door commands
        bus.subscribe(
                managerId,
                DoorProtocol.DOOR_COMMAND,
                elevator);

        // start testing console
        Thread testThread = new Thread(this::runTestConsole);
        testThread.setDaemon(true);
        testThread.start();
        // configure and launch Door Simulator
        DoorSimulatorMain.configure(elevator, gui -> {
            // create DoorController
            // sensor clicks (overload / obstruction) are published on the bus
            controller = new DoorController(
                    gui, elevator, this::publishDoorStatus, this::handleSensorCondition
            );
            controller.start();
            // start checking for incoming bus messages
            startBusPolling();
        });
        Application.launch(DoorSimulatorMain.class);
    }

    // check SoftwareBus for incoming messages
    private void startBusPolling() {
        busPoller = new Timeline(
                new KeyFrame(
                        Duration.millis(50),
                        event -> readMessages()
                )
        );
        busPoller.setCycleCount(Timeline.INDEFINITE);
        busPoller.play();
    }

    // receive and process incoming messages
    private void readMessages() {
        Message message;
        // process door commands
        while ((message = bus.getMsg(
                managerId,
                DoorProtocol.DOOR_COMMAND,
                elevator)) != null) {
            executeCommand(message.body());
        }
    }

    // tell DoorController what operation to perform
    private void executeCommand(int command) {
        switch (command) {
            case DoorProtocol.OPEN ->
                    controller.openDoor();
            case DoorProtocol.CLOSE ->
                    controller.closeDoor();
            case DoorProtocol.STOP ->
                    controller.stopDoor();
            default ->
                    System.err.println("[DoorManager] Unknown command: " + command);
        }
    }

    // publish door status updates through SoftwareBus.
    public void publishDoorStatus(int state) {
        Message message = new Message(
                DoorProtocol.DOOR_STATUS, elevator, state
        );
        bus.publish(managerId, message);
        System.out.println("[DoorManager] Published door status: " + message);
    }

    // publish message through SoftwareBus.
    public void publishMessage(int topic, int body) {
        Message message = new Message(topic, elevator, body);
        bus.publish(managerId, message);
        System.out.println("[DoorManager] Published: " + message);
    }

    // records the sensor condition locally and publishes it to the SoftwareBus.
    private void handleSensorCondition(int condition) {
        switch (condition) {
            case DoorProtocol.OVERLOAD_ACTIVE ->
                    overload = true;
            case DoorProtocol.OVERLOAD_CLEAR ->
                    overload = false;
            case DoorProtocol.OBSTRUCTION_ACTIVE ->
                    obstruction = true;
            case DoorProtocol.OBSTRUCTION_CLEAR ->
                    obstruction = false;
            default -> {
                System.err.println("[DoorManager] Unknown sensor condition: " + condition);
                return;
            }
        }
        // publish condition for other subsystems
        publishMessage(DoorProtocol.DOOR_CONDITION, condition);
    }

    // console for manually testing the Door subsystem.
    private void runTestConsole() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("""
                
                DOOR MANAGER TEST CONSOLE:
                open              - Open door
                close             - Close door
                stop              - Stop door
                getoverload       - Check overload status
                getobstruction    - Check obstruction status
                quit              - Stop test console
                """);

        while (true) {
            System.out.print("[DoorManager] Door command: ");
            String command = scanner.nextLine().trim().toLowerCase();
            switch (command) {
                case "open" ->
                        publishMessage(
                                DoorProtocol.DOOR_COMMAND,
                                DoorProtocol.OPEN);
                case "close" ->
                        publishMessage(
                                DoorProtocol.DOOR_COMMAND,
                                DoorProtocol.CLOSE);
                case "stop" ->
                        publishMessage(
                                DoorProtocol.DOOR_COMMAND,
                                DoorProtocol.STOP);
                case "getoverload" ->
                    System.out.println("[DoorManager] Overload: " + overload);
                case "getobstruction" ->
                        System.out.println("[DoorManager] Obstruction: " + obstruction);
                case "quit" -> {
                    System.out.println(
                            "[DoorManager] Test console closed.");
                    return;
                }
                default ->
                        System.out.println(
                                "[DoorManager] Unknown command. Try again.");
            }
        }
    }
}
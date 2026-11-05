package Harness;

import SoftwareBus.SoftwareBus;
import SoftwareBus.Message;
import Protocol.DoorProtocol;
import Simulator.DoorSimulatorMain;

import javafx.application.Application;

import java.util.Scanner;
import java.util.UUID;

public class Manager {
    private final SoftwareBus bus;
    private final int elevator;
    private UUID managerId;

    public Manager(String routerIp, int routerPort, int elevator) {
        this.bus = new SoftwareBus(routerIp, routerPort);
        this.elevator = elevator;
    }

    // Used later when the E-board provides a shared bus.
    public Manager(SoftwareBus sharedBus, int elevator) {
        this.bus = sharedBus;
        this.elevator = elevator;
    }

    public void start() {
        // register Manager as SoftwareBus client.
        managerId = bus.register();
        System.out.println("[DoorManager] Manager connected to Software Bus.");
        System.out.println("[DoorManager] Managing Elevator " + elevator);
        // start testing console
        Thread testThread = new Thread(this::runTestConsole);
        testThread.setDaemon(true);
        testThread.start();
        // configure and launch Door GUI
        DoorSimulatorMain.configure(bus, elevator);
        Application.launch(DoorSimulatorMain.class);
    }

    // publishes message through the SoftwareBus
    public void publishMessage(int topic, int body) {
        Message message = new Message(topic, elevator, body);
        bus.publish(managerId, message);
        System.out.println("Published: " + message);
    }

    // temporary console for manually testing Door subsystem
    private void runTestConsole() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("""

                DOOR MANAGER TEST CONSOLE:

                open              - Open door
                close             - Close door
                stop              - Stop door
                overload          - Activate overload
                clearoverload     - Clear overload
                obstruction       - Activate obstruction
                clearobstruction  - Clear obstruction
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
                case "overload" ->
                        publishMessage(
                                DoorProtocol.DOOR_CONDITION,
                                DoorProtocol.OVERLOAD_ACTIVE);
                case "clearoverload" ->
                        publishMessage(
                                DoorProtocol.DOOR_CONDITION,
                                DoorProtocol.OVERLOAD_CLEAR);
                case "obstruction" ->
                        publishMessage(
                                DoorProtocol.DOOR_CONDITION,
                                DoorProtocol.OBSTRUCTION_ACTIVE);
                case "clearobstruction" ->
                        publishMessage(
                                DoorProtocol.DOOR_CONDITION,
                                DoorProtocol.OBSTRUCTION_CLEAR);
                case "quit" -> {
                    System.out.println("[DoorManager] Test console closed.");
                    return;
                }
                default ->
                        System.out.println("[DoorManager] Unknown command. Try again.");
            }
        }
    }
}

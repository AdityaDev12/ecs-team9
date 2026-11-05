package Simulator;

import SoftwareBus.SoftwareBus;
import javafx.application.Application;
import javafx.stage.Stage;

public class DoorSimulatorMain extends Application {
    private static SoftwareBus bus;
    private static int elevator = 1;
    // receives the bus created by subsystem
    public static void configure(
            SoftwareBus softwareBus,
            int elevatorNumber) {
        bus = softwareBus;
        elevator = elevatorNumber;
    }
    @Override
    public void start(Stage stage) {
        if (bus == null) {
            throw new IllegalStateException(
                    "DoorSimulatorMain: SoftwareBus.SoftwareBus not configured.");
        }
        System.out.println(
                "Starting Door Control for Elevator " + elevator);
        // create visual representation
        DoorGUIMain gui = new DoorGUIMain(stage);
        gui.attachDoorPanels(new DoorsGUI());
        gui.makeGUI();
        // connect door controller directly to bus
        DoorController controller =
                new DoorController(bus, gui, elevator);
        controller.start();
        // stop the controller when GUI closes
        stage.setOnCloseRequest(event ->
                controller.stop());
    }
}
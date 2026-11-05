package Simulator;

import javafx.application.Application;
import javafx.stage.Stage;
import java.util.function.Consumer;

public class DoorSimulatorMain extends Application {

    private static int elevator = 1;

    private static Consumer<DoorGUIMain> onGuiReady;

    public static void configure(
            int elevatorNumber,
            Consumer<DoorGUIMain> guiCallback) {

        elevator = elevatorNumber;
        onGuiReady = guiCallback;
    }

    @Override
    public void start(Stage stage) {

        System.out.println(
                "[DoorSimMain] Starting Door Simulator for Elevator " + elevator);

        DoorGUIMain gui = new DoorGUIMain(stage);

        gui.attachDoorPanels(new DoorsGUI());
        gui.makeGUI();

        // give the GUI to the Manager to connect to the controller
        onGuiReady.accept(gui);
    }
}
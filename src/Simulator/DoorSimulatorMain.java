package Simulator;

import Protocol.LocalMessenger;
import Protocol.Messenger;
import Protocol.NetworkMessenger;

import javafx.application.Application;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

/**
 * creates the GUI and connects to the Manager using NetworkMessenger
 */

public class DoorSimulatorMain extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        int port = 5001;
        byte elevator = 1;
        String host = null; //ADDED
        boolean local = false;

        List<String> args = getParameters().getRaw();
        for (int i = 0; i < args.size(); i++) {
            switch (args.get(i)) {
                case "--host" -> host = args.get(++i); //ADDED
                case "--port" -> port = Integer.parseInt(args.get(++i));
                case "--elevator" -> elevator = (byte) Integer.parseInt(args.get(++i));
                case "--local" -> local = true;
                default -> System.err.println("DoorSimMain: unknown option: " + args.get(i));
            }
        }

        System.out.println("DoorSimMain: Starting Door GUI (elevator " + elevator + ")...");
        DoorGUIMain gui = new DoorGUIMain(stage);
        gui.addListener(DoorEventListener.console());
        gui.attachDoorPanels(new DoorsGUI());
        gui.makeGUI();

        Messenger messenger;
        //REVISED
        if (local || host == null) {
            // standalone mode, no manager involved
            messenger = new LocalMessenger();
            System.out.println("DoorSimMain: Running in standalone local mode.");
        } else {
            // manager mode, connect to server owned by Manager
            try {
                messenger = new NetworkMessenger(host, port);
                System.out.println("DoorSimMain: Connected to Manager at " + host + ":" + port);
            } catch (IOException e) {
                throw new IllegalArgumentException("DoorSimMain: Could not connect to Manager at " + host + ":" + port, e);
            }
        }
        //ORIGINAL
//        if (local) {
//            messenger = new LocalMessenger();
//        } else {
//            try {
//                messenger = new ServerMessenger(port);
//            } catch (IOException e) {
//                System.err.println("could not open port " + port + " (" + e.getMessage() + "), running local only");
//                messenger = new LocalMessenger();
//            }
//        }

        DoorController controller = new DoorController(gui, messenger, elevator);
        controller.start();

        stage.getScene().addEventHandler(KeyEvent.KEY_PRESSED, e -> {
            switch (e.getCode()) {
                case O -> controller.open();
                case C -> controller.close();
                case S -> controller.halt();
                case EQUALS, PLUS, ADD -> controller.setSpeed(controller.getSpeed() + 1);
                case MINUS, SUBTRACT -> controller.setSpeed(controller.getSpeed() - 1);
                default -> { }
            }
        });

        //ADDED
        stage.setOnCloseRequest(e -> {
            controller.stop();
            if (messenger instanceof NetworkMessenger network) {
                network.close();
            }
        });

        //ORIGINALLY INCLUDED
//        Messenger toClose = messenger;
//        stage.setOnCloseRequest(e -> {
//            controller.stop();
//            if (toClose instanceof ServerMessenger s) {
//                s.close();
//            }
//        });
    }
}
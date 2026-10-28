package Simulator;

import Protocol.LocalMessenger;
import Protocol.Messenger;
import javafx.application.Application;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

public class DoorSimulatorMain extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        int port = 5001;
        byte elevator = 1;
        boolean local = false;

        List<String> args = getParameters().getRaw();
        for (int i = 0; i < args.size(); i++) {
            switch (args.get(i)) {
                case "--port" -> port = Integer.parseInt(args.get(++i));
                case "--elevator" -> elevator = (byte) Integer.parseInt(args.get(++i));
                case "--local" -> local = true;
                default -> System.err.println("unknown option: " + args.get(i));
            }
        }

        System.out.println("Starting Door GUI (elevator " + elevator + ")...");
        DoorGUIMain gui = new DoorGUIMain(stage);
        gui.addListener(DoorEventListener.console());
        gui.makeGUI();

        Messenger messenger;
        if (local) {
            messenger = new LocalMessenger();
        } else {
            try {
                messenger = new ServerMessenger(port);
            } catch (IOException e) {
                System.err.println("could not open port " + port + " (" + e.getMessage() + "), running local only");
                messenger = new LocalMessenger();
            }
        }

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

        Messenger toClose = messenger;
        stage.setOnCloseRequest(e -> {
            controller.stop();
            if (toClose instanceof ServerMessenger s) {
                s.close();
            }
        });
    }
}
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import Harness.FakeDoor;
import Protocol.LocalMessenger;

public class DoorsGUITest extends Application {
    private FakeDoor fakeDoor;
    private DoorsGUI doors;

    @Override
    public void start(Stage stage) {
        LocalMessenger messenger = new LocalMessenger();
        byte elevator = 1;

        // start simulated door on shared message bus
        fakeDoor = new FakeDoor(messenger, elevator);
        Thread fakeDoorThread = new Thread(fakeDoor, "fake-door");
        fakeDoorThread.setDaemon(true);
        fakeDoorThread.start();
        // connect GUI to the message bus
        doors = new DoorsGUI(messenger, elevator);

        //DoorsGUI doors = new DoorsGUI();

        Scene scene = new Scene(doors, 800, 600);

        stage.setTitle("Elevator Door Assembly");
        stage.setScene(scene);
        stage.setResizable(false);

        stage.setOnCloseRequest(event -> {
            doors.close();
            fakeDoor.stop();
        });

        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import Protocol.Codes;
import Protocol.Message;
import Protocol.Messenger;

public class DoorsGUI extends Pane {

    //window dimensions
    private static final double WINDOW_WIDTH = 800;
    private static final double WINDOW_HEIGHT = 600;

    //door dimensions
    private static final double DOOR_WIDTH = WINDOW_WIDTH / 2;
    private static final double DOOR_HEIGHT = WINDOW_HEIGHT;
    private static final double ANIMATION_SECONDS = 2.0;

    //open/close door button
    private final Button doorButton;

    //doors
    private final Rectangle leftDoor;
    private final Rectangle rightDoor;

    private final Messenger messenger;
    private final int id;
    private final byte elevator;

    //door animations
    private Timeline animation;
    private volatile boolean listening = true;

    // desired state can change before door finishes moving
    private boolean desiredOpen = false;
    //tracks if door is open
    private boolean isOpen = false;

    public DoorsGUI(Messenger messenger, byte elevator) {
        // register GUI in messaging
        this.messenger = messenger;
        this.elevator = elevator;
        id = messenger.register();
        messenger.subscribe(id, Codes.STATE, elevator);

        setPrefSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        setMinSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        setMaxSize(WINDOW_WIDTH, WINDOW_HEIGHT);

        setStyle("-fx-background-color: black;");

        Pane doorPane = new Pane();
        doorPane.setPrefSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        doorPane.setMinSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        doorPane.setMaxSize(WINDOW_WIDTH, WINDOW_HEIGHT);

        //left door
        leftDoor = new Rectangle(0, 0, DOOR_WIDTH, DOOR_HEIGHT);
        leftDoor.setFill(Color.LIGHTGRAY);
        leftDoor.setStroke(Color.GRAY);
        leftDoor.setStrokeWidth(2);

        //right door
        rightDoor = new Rectangle(DOOR_WIDTH, 0, DOOR_WIDTH, DOOR_HEIGHT);
        rightDoor.setFill(Color.LIGHTGRAY);
        rightDoor.setStroke(Color.GRAY);
        rightDoor.setStrokeWidth(2);

        doorPane.getChildren().addAll(
                leftDoor,
                rightDoor
        );

        //button for door
        doorButton = new Button("Open Doors");
        doorButton.setStyle(
                "-fx-font-size: 16px;" +
                        "-fx-padding: 10px 20px;" +
                        "-fx-background-radius: 5px;"
        );

        // REVISION STARTS HERE; DELETE ALL TO REPAIR ORIGINAL CODE
        // button for door
        doorButton.setOnAction(event -> requestDoorMovement());
        getChildren().addAll(doorPane, doorButton);
        StackPane.setAlignment(doorButton, Pos.BOTTOM_CENTER);
        StackPane.setMargin(doorButton, new javafx.geometry.Insets(0, 0, 20, 0));

        // read incoming messages off JavaFX thread
        Thread listener = new Thread(this::listenForStates, "doors-gui-listener");
        listener.setDaemon(true);
        listener.start();
    }

    private void requestDoorMovement() {
        boolean open = !desiredOpen;
        desiredOpen = open;
        byte command = open? Codes.OPEN : Codes.CLOSE;
        messenger.publish(id, new Message(Codes.CMD, elevator, command, 0));
    }

    private void listenForStates() {
        while (listening) {
            Message message = messenger.getMsg(id, Codes.STATE, elevator);
            if (message == null) {
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                continue;
            }
            // JavaFX scene change runs on JavaFX thread
            Platform.runLater(() -> handleState(message));
        }
    }

    private void handleState(Message message) {
        switch (message.body()) {
            case Codes.OPENING:
                desiredOpen = true;
                doorButton.setText("Opening...");
                animateDoors(true);
                break;
            case Codes.CLOSING:
                desiredOpen = false;
                doorButton.setText("Closing...");
                animateDoors(false);
                break;
            case Codes.REVERSING:
            case Codes.HELD_OPEN:
            case Codes.FULLY_OPEN:
                desiredOpen = false;
                setDoorPosition(false);
                doorButton.setText("Open Doors");
                break;
            default:
                // ignore unhandled states
                break;
        }
    }
    private void animateDoors(boolean open) {

        //stop an existing animation before starting another
        if (animation != null) {
            animation.stop();
        }

        double distance = DOOR_WIDTH;

        //determine each panel's target position
        double leftTarget = open ? -distance : 0;
        double rightTarget = open ? distance : 0;

        //animate both panels together
        animation = new Timeline(
                new KeyFrame(
                        Duration.seconds(ANIMATION_SECONDS),

                        new KeyValue(
                                leftDoor.translateXProperty(),
                                leftTarget,
                                Interpolator.EASE_BOTH
                        ),

                        new KeyValue(
                                rightDoor.translateXProperty(),
                                rightTarget,
                                Interpolator.EASE_BOTH
                        )
                )
        );

        //update the state and button after the animation finishes
        animation.setOnFinished(event -> {
            isOpen = open;
            doorButton.setText(isOpen ? "Close Doors" : "Open Doors");
        });

        animation.play();
    }

    private void setDoorPosition(boolean open) {
        if (animation != null) {
            animation.stop();
        }
        leftDoor.setTranslateX(open ? -DOOR_WIDTH : 0);
        rightDoor.setTranslateX(open ? DOOR_WIDTH : 0);
        isOpen = open;
    }

    public void close() {
        listening = false;
        if (animation != null) {
            animation.stop();
        }
    }
}



//        //button for door
//        doorButton = new Button("Open Doors");
//        doorButton.setStyle(
//                "-fx-font-size: 16px;" +
//                "-fx-padding: 10px 20px;" +
//                "-fx-background-radius: 5px;"
//        );
//
//        //open/close doors
//        doorButton.setOnAction(event -> {
//            if (isOpen) {
//                closeDoor();
//            }
//
//            else {
//                openDoor();
//            }
//        });
//
//        getChildren().addAll(doorPane, doorButton);
//
//        StackPane.setAlignment(doorButton, Pos.BOTTOM_CENTER);
//
//        StackPane.setMargin(doorButton, new javafx.geometry.Insets(0, 0, 20, 0));
//    }
//
//    //open the doors
//    public void openDoor() {
//        animateDoors(true);
//    }
//
//    //close the doors
//    public void closeDoor() {
//        animateDoors(false);
//    }
//
//    //animate the doors
//    private void animateDoors(boolean open) {
//
//        //stop an existing animation before starting another
//        if (animation != null) {
//            animation.stop();
//        }
//
//        double distance = DOOR_WIDTH;
//
//        //determine each panel's target position
//        double leftTarget = open ? -distance : 0;
//        double rightTarget = open ? distance : 0;
//
//        //animate both panels together
//        animation = new Timeline(
//                new KeyFrame(
//                        Duration.seconds(ANIMATION_SECONDS),
//
//                        new KeyValue(
//                                leftDoor.translateXProperty(),
//                                leftTarget,
//                                Interpolator.EASE_BOTH
//                        ),
//
//                        new KeyValue(
//                                rightDoor.translateXProperty(),
//                                rightTarget,
//                                Interpolator.EASE_BOTH
//                        )
//                )
//        );
//
//        //update the state and button after the animation finishes
//        animation.setOnFinished(event -> {
//            isOpen = open;
//            doorButton.setText(isOpen ? "Close Doors" : "Open Doors");
//        });
//
//        animation.play();
//    }
//}

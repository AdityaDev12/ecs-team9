package Simulator;

import SoftwareBus.SoftwareBus;
import SoftwareBus.Message;
import Protocol.DoorProtocol;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;


public class DoorController {

    // door movement duration
    private static final int TICK_MS = 50;
    private static final double FULL_TRAVEL_TIME_MS = 3000.0;
    private static final double SPEED_PER_MS =
            100.0 / FULL_TRAVEL_TIME_MS;

    private enum Motion {
        OPENING,
        CLOSING,
        STOPPED
    }

    private final SoftwareBus bus;
    private final UUID doorId;
    private final int elevator;

    private final DoorGUIMain gui;
    private final Timeline ticker;

    // publish messages sequentially without blocking GUI animation
    private final ExecutorService publisher =
            Executors.newSingleThreadExecutor();

    private double percentOpen = 0.0;

    private Motion motion = Motion.STOPPED;

    private boolean overload = false;
    private boolean obstruction = false;

    private int lastPublishedState = -1;

    public DoorController(
            SoftwareBus bus,
            DoorGUIMain gui,
            int elevator) {
        this.bus = bus;
        this.gui = gui;
        this.elevator = elevator;

        // Register with SoftwareBus.SoftwareBus
        doorId = bus.register();

        // Subscribe to messages for this elevator only
        bus.subscribe(
                doorId,
                DoorProtocol.DOOR_COMMAND,
                elevator);
        bus.subscribe(
                doorId,
                DoorProtocol.DOOR_CONDITION,
                elevator);

        ticker = new Timeline(
                new KeyFrame(
                        Duration.millis(TICK_MS),
                        event -> tick()
                )
        );
        ticker.setCycleCount(Timeline.INDEFINITE);
    }
    // begin automatic door operation
    public void start() {
        gui.onDoorPositionChanged(0.0);
        updateStatus();
        ticker.play();
    }
    // stop animation and background publisher
    public void stop() {
        ticker.stop();
        publisher.shutdown();
    }

    // door response methods
    public void openDoor() {
        if (percentOpen >= 100.0) {
            motion = Motion.STOPPED;
            updateState();
            return;
        }
        motion = Motion.OPENING;
        publishState(DoorProtocol.OPENING);
        updateStatus();
    }

    public void closeDoor() {
        // never close while either safety condition is active.
        if (overload || obstruction) {
            if (percentOpen < 100.0) {
                motion = Motion.OPENING;
                publishState(DoorProtocol.OPENING);
            } else {
                motion = Motion.STOPPED;
                publishState(DoorProtocol.HELD_OPEN);
            }
            updateStatus();
            return;
        }
        if (percentOpen <= 0.0) {
            motion = Motion.STOPPED;
            updateState();
            return;
        }
        motion = Motion.CLOSING;
        publishState(DoorProtocol.CLOSING);
        updateStatus();
    }

    public void stopDoor() {
        motion = Motion.STOPPED;
        publishState(DoorProtocol.STOPPED);
        updateStatus();
    }

    // received safety conditions
    public void setOverload(boolean active) {
        overload = active;
        if (active) {
            // overload requires doors remain open
            if (motion == Motion.CLOSING) {
                motion = Motion.OPENING;
                publishState(DoorProtocol.REVERSING);
            } else if (percentOpen < 100.0) {
                motion = Motion.OPENING;
                publishState(DoorProtocol.OPENING);
            } else {
                motion = Motion.STOPPED;
                publishState(DoorProtocol.HELD_OPEN);
            }
        } else {
            // clearing overload doesn't automatically close door
            updateState();
        }
        updateStatus();
    }

    public void setObstruction(boolean active) {
        obstruction = active;
        if (active && motion == Motion.CLOSING) {
            // reverse when obstruction is detected during closing
            motion = Motion.OPENING;
            publishState(DoorProtocol.REVERSING);
        } else {
            // clearing obstruction doesn't automatically close door
            updateState();
        }
        updateStatus();
    }

    // message processing
    private void tick() {
        readMessages();
        moveDoor();
    }

    private void readMessages() {
        Message message;
        // process all pending door commands
        while ((message = bus.getMsg(
                doorId,
                DoorProtocol.DOOR_COMMAND,
                elevator)) != null) {
            executeCommand(message.body());
        }
        // process all pending safety conditions
        while ((message = bus.getMsg(
                doorId,
                DoorProtocol.DOOR_CONDITION,
                elevator)) != null) {
            executeCondition(message.body());
        }
    }

    private void executeCommand(int command) {
        switch (command) {
            case DoorProtocol.OPEN ->
                    openDoor();
            case DoorProtocol.CLOSE ->
                    closeDoor();
            case DoorProtocol.STOP ->
                    stopDoor();
            default ->
                    System.err.println(
                            "DoorController: Unknown command "
                                    + command);
        }
    }

    private void executeCondition(int condition) {
        switch (condition) {
            case DoorProtocol.OVERLOAD_ACTIVE ->
                    setOverload(true);
            case DoorProtocol.OVERLOAD_CLEAR ->
                    setOverload(false);
            case DoorProtocol.OBSTRUCTION_ACTIVE ->
                    setObstruction(true);
            case DoorProtocol.OBSTRUCTION_CLEAR ->
                    setObstruction(false);
            default ->
                    System.err.println(
                            "DoorController: Unknown condition "
                                    + condition);
        }
    }

    // automatic door movement
    private void moveDoor() {
        if (motion == Motion.STOPPED) {
            return;
        }
        double movement =
                SPEED_PER_MS * TICK_MS;
        if (motion == Motion.OPENING) {
            percentOpen = Math.min(
                    100.0,
                    percentOpen + movement);
            if (percentOpen >= 100.0) {
                percentOpen = 100.0;
                motion = Motion.STOPPED;
                updateState();
            }
        } else if (motion == Motion.CLOSING) {
            percentOpen = Math.max(
                    0.0,
                    percentOpen - movement);
            if (percentOpen <= 0.0) {
                percentOpen = 0.0;
                motion = Motion.STOPPED;
                updateState();
            }
        }
        gui.onDoorPositionChanged(
                percentOpen / 100.0);
        updateStatus();
    }

    // door state reporting
    private void updateState() {
        if (motion == Motion.OPENING) {
            publishState(DoorProtocol.OPENING);
        } else if (motion == Motion.CLOSING) {
            publishState(DoorProtocol.CLOSING);
        } else if (percentOpen >= 100.0) {
            if (overload || obstruction) {
                publishState(DoorProtocol.HELD_OPEN);
            } else {
                publishState(DoorProtocol.FULLY_OPEN);
            }
        } else if (percentOpen <= 0.0) {
            publishState(DoorProtocol.FULLY_CLOSED);
        } else {
            publishState(DoorProtocol.STOPPED);
        }
    }

    private void publishState(int state) {
        // avoid repeated publishing of unchanged state
        if (lastPublishedState == state) {
            return;
        }
        lastPublishedState = state;
        Message message = new Message(
                DoorProtocol.DOOR_STATUS,
                elevator,
                state);
        publisher.execute(() ->
                bus.publish(doorId, message));
    }

    private void updateStatus() {
        String state;
        if (motion == Motion.OPENING) {
            state = "OPENING";
        } else if (motion == Motion.CLOSING) {
            state = "CLOSING";
        } else if (percentOpen >= 100.0) {
            state = (overload || obstruction)
                    ? "HELD OPEN" : "FULLY OPEN";
        } else if (percentOpen <= 0.0) {
            state = "FULLY CLOSED";
        } else {
            state = "STOPPED";
        }
        gui.setStatus(
                "Elevator " + elevator + ": " + state);
    }

    public double getPercentOpen() {
        return percentOpen;
    }

    public boolean isOverloaded() {
        return overload;
    }

    public boolean isObstructed() {
        return obstruction;
    }
}

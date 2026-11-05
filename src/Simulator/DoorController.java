package Simulator;

import Archive.DoorSensor;
import Protocol.DoorProtocol;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import java.util.function.IntConsumer;

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

    private final int elevator;
    private final DoorGUIMain gui;
    private final Timeline ticker;

    // callback to report door states to Manager
    private final IntConsumer statusReporter;
    // callback to report sensor readings (overload / obstruction) to Manager
    private final IntConsumer conditionReporter;
    private int lastPublishedState = -1;

    private double percentOpen = 0.0;
    private Motion motion = Motion.STOPPED;

    // constructor
    public DoorController(
            DoorGUIMain gui, int elevator,
            IntConsumer statusReporter, IntConsumer conditionReporter) {
        this.gui = gui;
        this.elevator = elevator;
        this.statusReporter = statusReporter;
        this.conditionReporter = conditionReporter;

        // sensors only report: clicking overload or obstruction sends active / clear
        gui.addListener((sensor, active, fromUser) -> {
            if (sensor == DoorSensor.OVERLOAD) {
                conditionReporter.accept(active
                        ? DoorProtocol.OVERLOAD_ACTIVE
                        : DoorProtocol.OVERLOAD_CLEAR);
            } else if (sensor == DoorSensor.OBSTRUCTION) {
                conditionReporter.accept(active
                        ? DoorProtocol.OBSTRUCTION_ACTIVE
                        : DoorProtocol.OBSTRUCTION_CLEAR);
            }
        });

        ticker = new Timeline(
                new KeyFrame(
                        Duration.millis(TICK_MS),
                        event -> tick()
                )
        );
        ticker.setCycleCount(Timeline.INDEFINITE);
    }


    // starts automatic door operation
    public void start() {
        gui.onDoorPositionChanged(0.0);
        updateStatus();
        ticker.play();
    }


    // stop animation
    public void stop() {
        ticker.stop();
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

    // automatic door movement
    private void tick() {
        moveDoor();
    }

    private void moveDoor() {
        if (motion == Motion.STOPPED) {
            return;
        }
        double movement = SPEED_PER_MS * TICK_MS;
        if (motion == Motion.OPENING) {
            percentOpen = Math.min(
                    100.0,
                    percentOpen + movement
            );
            if (percentOpen >= 100.0) {
                percentOpen = 100.0;
                motion = Motion.STOPPED;
                updateState();
            }
        } else if (motion == Motion.CLOSING) {
            percentOpen = Math.max(
                    0.0,
                    percentOpen - movement
            );
            if (percentOpen <= 0.0) {
                percentOpen = 0.0;
                motion = Motion.STOPPED;
                updateState();
            }
        }
        gui.onDoorPositionChanged(percentOpen / 100.0);
        updateStatus();
    }

    // door state reporting
    private void updateState() {
        if (motion == Motion.OPENING) {
            publishState(DoorProtocol.OPENING);
        } else if (motion == Motion.CLOSING) {
            publishState(DoorProtocol.CLOSING);
        } else if (percentOpen >= 100.0) {
            publishState(DoorProtocol.FULLY_OPEN);
        } else if (percentOpen <= 0.0) {
            publishState(DoorProtocol.FULLY_CLOSED);
        } else {
            publishState(DoorProtocol.STOPPED);
        }
    }

    // notify Manager when door state changes
    private void publishState(int state) {
        // avoid reporting unchanged state
        if (lastPublishedState == state) {
            return;
        }
        lastPublishedState = state;
        statusReporter.accept(state);
    }


    // update simulator's displayed status
    private void updateStatus() {
        String state;
        if (motion == Motion.OPENING) {
            state = "OPENING";
        } else if (motion == Motion.CLOSING) {
            state = "CLOSING";
        } else if (percentOpen >= 100.0) {
            state = "FULLY OPEN";
        } else if (percentOpen <= 0.0) {
            state = "FULLY CLOSED";
        } else {
            state = "STOPPED";
        }
        gui.setStatus(
                "Elevator " + elevator + ": " + state
        );
    }

    // accessors
    public double getPercentOpen() {
        return percentOpen;
    }
}
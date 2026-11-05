package Simulator;

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
    private int lastPublishedState = -1;

    private double percentOpen = 0.0;
    private Motion motion = Motion.STOPPED;
    private boolean overload = false;
    private boolean obstruction = false;

    // constructor
    public DoorController(
            DoorGUIMain gui, int elevator, IntConsumer statusReporter) {
        this.gui = gui;
        this.elevator = elevator;
        this.statusReporter = statusReporter;

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
        // Never close while either safety condition is active.
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

    // safety conditions
    public void setOverload(boolean active) {
        overload = active;
        if (active) {
            // overload requires doors remain open.
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
            // reverse when obstruction detected during closing
            motion = Motion.OPENING;
            publishState(DoorProtocol.REVERSING);
        } else {
            // clearing obstruction doesn't automatically close door
            updateState();
        }
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
            state = (overload || obstruction)
                    ? "HELD OPEN" : "FULLY OPEN";
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

    public boolean isOverloaded() {
        return overload;
    }

    public boolean isObstructed() {
        return obstruction;
    }
}


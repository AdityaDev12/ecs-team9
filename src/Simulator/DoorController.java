package Simulator;

import Protocol.Codes;
import Protocol.LocalMessenger;
import Protocol.Message;
import Protocol.Messenger;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

/**
 * simulates the door, sends requests to the Manager,
 * executes commands relayed back
 */
public class DoorController {

    public static final int TICK_MS = 50;
    public static final int MIN_SPEED = 1;     // percent per tick
    public static final int MAX_SPEED = 50;
    public static final int DEFAULT_SPEED = 3; // about 1.7 s from closed to open

    private enum Motion { OPENING, CLOSING, STOPPED }

    private final DoorGUIMain gui;
    private final Messenger messenger;
    private final byte elevator;
    private final int id;
    private final Timeline ticker;

    private int percentOpen = 0;
    private int speed = DEFAULT_SPEED;
    private Motion motion = Motion.STOPPED;
    private boolean obstruction = false;
    private boolean overload = false;

    public DoorController(DoorGUIMain gui, Messenger messenger, byte elevator) {
        this.gui = gui;
        this.messenger = messenger;
        this.elevator = elevator;
        this.id = messenger.register();
        messenger.subscribe(id, Codes.CMD, elevator);
        messenger.subscribe(id, Codes.SPEED, elevator);
        messenger.subscribe(id, Codes.SENSOR, elevator);
        messenger.subscribe(id, Codes.SYS, Codes.ANY);

        // REVISED: GUI interactions request changes, do not execute them directly
        gui.addListener((sensor, active, fromUser) -> {
            if (!fromUser) {
                return;
            }
            if (sensor == DoorSensor.OBSTRUCTION) {
                requestObstruction(active);
            } else if (sensor == DoorSensor.OVERLOAD) {
                requestOverload(active);
            }
        });

//        // ORIGINAL: user clicks on the obstruction / overload buttons
//        gui.addListener((sensor, active, fromUser) -> {
//            if (sensor == DoorSensor.OBSTRUCTION) {
//                setObstruction(active);
//            } else if (sensor == DoorSensor.OVERLOAD) {
//                setOverload(active);
//            }
//        });

        ticker = new Timeline(new KeyFrame(Duration.millis(TICK_MS), e -> tick()));
        ticker.setCycleCount(Timeline.INDEFINITE);
    }

    // REVISED
    public void start() {
        gui.onDoorPositionChanged(percentOpen /100.0);
        updateStatus();
        ticker.play();
    }

    // ORIGINAL
//    public void start() {
//        gui.onDoorPositionChanged(0.0);
//        updateStatus();
//        ticker.play();
//    }

    public void stop() {
        ticker.stop();
    }

    //REVISED
    public void open() {
        requestCommand(Codes.OPEN);
    }
    public void close() {
        requestCommand(Codes.CLOSE);
    }
    public void halt() {
        requestCommand(Codes.STOP);
    }

    //ORIGINAL
    // local controls (keyboard, no harness needed)
//    public void open()  { doCommand(Codes.OPEN); }
//    public void close() { doCommand(Codes.CLOSE); }
//    public void halt()  { doCommand(Codes.STOP); }


    //ORIGINAL
//    public void setSpeed(int percentPerTick) {
//        speed = Math.max(MIN_SPEED, Math.min(MAX_SPEED, percentPerTick));
//        updateStatus();
//    }

    // ADDED
    private void requestCommand(byte command) {
        if (messenger instanceof LocalMessenger) {
            executeCommand(command);
        } else {
            send(Codes.CMD, command, 0);
        }
    }
    public void setSpeed(int percentPerTick) {
        int requestedSpeed = clampSpeed(percentPerTick);
        if (messenger instanceof LocalMessenger) {
            applySpeed(requestedSpeed);
        } else {
            send(Codes.SPEED, Codes.SET_SPEED, requestedSpeed);
        }
    }
    private void requestObstruction(boolean active) {
        if (messenger instanceof LocalMessenger) {
            applyObstruction(active);
        } else {
            send(Codes.SENSOR, active ? Codes.OBSTRUCTION_ON : Codes.OBSTRUCTION_OFF, 0);
        }
    }
    private void requestOverload(boolean active) {
        if (messenger instanceof LocalMessenger) {
            applyOverload(active);
        } else {
            send(Codes.SENSOR, active ? Codes.OVERLOAD_ON : Codes.OVERLOAD_OFF, 0);
        }
    }

    public int getSpeed() { return speed; }
    public int getPercentOpen() { return percentOpen; }

    // one tick
    private void tick() {
        readMessages();
        move();
    }

    //REVISED
    private void readMessages() {
        Message message;
        while ((message = messenger.getMsg(id, Codes.ANY, Codes.ANY)) != null) {
            if (message.sub() != elevator && !(message.topic() == Codes.SYS && message.sub() == Codes.ANY)) {
                continue;
            }
            if (message.topic() == Codes.CMD && message.sub() == elevator) {
                executeCommand(message.body());
            } else if (message.topic() == Codes.SPEED && message.sub() == elevator && message.body() == Codes.SET_SPEED) {
                applySpeed(message.value());
            } else if (message.topic() == Codes.SENSOR && message.sub() == elevator) {
                applySensorMessage(message.body());
            } else if (message.topic() == Codes.SYS && message.body() == Codes.PING) {
                send(Codes.SYS, Codes.PONG, 0);
            }
        }
    }

    //ORIGINAL
//    private void readMessages() {
//        Message m;
//        while ((m = messenger.getMsg(id, Codes.ANY, Codes.ANY)) != null) {
//            if (m.topic() == Codes.CMD && m.sub() == elevator) {
//                doCommand(m.body());
//            } else if (m.topic() == Codes.SPEED && m.sub() == elevator && m.body() == Codes.SET_SPEED) {
//                setSpeed(m.value());
//            } else if (m.topic() == Codes.SENSOR && m.sub() == elevator) {
//                pressFromHarness(m.body());
//            } else if (m.topic() == Codes.SYS && m.body() == Codes.PING) {
//                send(Codes.SYS, Codes.PONG, 0);
//            }
//        }
//    }

    //ADDED
    private void executeCommand(byte command) {
        if (command == Codes.OPEN) {
            if (percentOpen < 100) {
                motion = Motion.OPENING;
                send(Codes.STATE, Codes.OPENING, percentOpen);
            } else {
                motion = Motion.STOPPED;
                send(Codes.STATE, Codes.FULLY_OPEN, 100);
            }
        } else if (command == Codes.CLOSE) {
            if (obstruction || overload) {
                motion = Motion.STOPPED;
                send(Codes.STATE, Codes.HELD_OPEN,  percentOpen);
                updateStatus();
                return;
            }
            if (percentOpen > 0) {
                motion = Motion.CLOSING;
                send(Codes.STATE, Codes.CLOSING, percentOpen);
            } else {
                motion = Motion.STOPPED;
                send(Codes.STATE, Codes.FULLY_CLOSED, 0);
            }
        } else if (command == Codes.STOP) {
            motion = Motion.STOPPED;
            send(Codes.STATE, Codes.STOPPED, percentOpen);
        }
        updateStatus();;
    }

    // ORIGINALLY INCLUDED
//    private void doCommand(byte cmd) {
//        if (cmd == Codes.OPEN) {
//            if (percentOpen < 100) {
//                motion = Motion.OPENING;
//                send(Codes.STATE, Codes.OPENING, percentOpen);
//            } else {
//                send(Codes.STATE, Codes.FULLY_OPEN, 100);
//            }
//        } else if (cmd == Codes.CLOSE) {
//            if (obstruction || overload) {
//                send(Codes.STATE, Codes.HELD_OPEN, percentOpen);  // refuse to close
//                return;
//            }
//            if (percentOpen > 0) {
//                motion = Motion.CLOSING;
//                send(Codes.STATE, Codes.CLOSING, percentOpen);
//            } else {
//                send(Codes.STATE, Codes.FULLY_CLOSED, 0);
//            }
//        } else if (cmd == Codes.STOP) {
//            motion = Motion.STOPPED;
//            send(Codes.STATE, Codes.STOPPED, percentOpen);
//        }
//        updateStatus();
//    }

    //ADDED
    private void applySpeed(int requestedSpeed) {
        speed = clampSpeed(requestedSpeed);
        updateStatus();
    }
    private int clampSpeed(int requestedSpeed) {
        return Math.max(MIN_SPEED, Math.min(MAX_SPEED, requestedSpeed));
    }

    private void move() {
        if (motion == Motion.OPENING) {
            percentOpen = Math.min(100, percentOpen + speed);
            if (percentOpen == 100) {
                motion = Motion.STOPPED;
                send(Codes.STATE, Codes.FULLY_OPEN, 100);
            }
        } else if (motion == Motion.CLOSING) {
            percentOpen = Math.max(0, percentOpen - speed);
            if (percentOpen == 0) {
                motion = Motion.STOPPED;
                send(Codes.STATE, Codes.FULLY_CLOSED, 0);
            }
        } else {
            return;
        }
        gui.onDoorPositionChanged(percentOpen / 100.0);
        updateStatus();
    }

    //ADDED
    private void applySensorMessage(byte body) {
        if (body == Codes.OBSTRUCTION_ON) {
            applyObstruction(true);
            gui.setSensor(DoorSensor.OBSTRUCTION, true);
        } else if (body == Codes.OBSTRUCTION_OFF) {
            applyObstruction(false);
            gui.setSensor(DoorSensor.OBSTRUCTION, false);
        } else if (body == Codes.OVERLOAD_ON) {
            applyOverload(true);
            gui.setSensor(DoorSensor.OVERLOAD, true);
        } else if (body == Codes.OVERLOAD_OFF) {
            applyOverload(false);
            gui.setSensor(DoorSensor.OVERLOAD, false);
        }
    }

    // ADDED
    private void applyObstruction(boolean active) {
        obstruction = active;
        if (active && motion == Motion.CLOSING) {
            motion = Motion.OPENING;
            send(Codes.STATE, Codes.REVERSING, percentOpen);
        }
        updateStatus();;
    }
    private void applyOverload(boolean active) {
        overload = active;
        if (active) {
            if (percentOpen < 100) {
                motion = Motion.OPENING;
            }
            send(Codes.STATE, Codes.HELD_OPEN, percentOpen);
        }
        updateStatus();
    }

    // ORIGINALLY INCLUDED
//    // sensors
//    /** Harness "presses" a button: flip it in the GUI, which then runs the normal click logic. */
//    private void pressFromHarness(byte body) {
//        if (body == Codes.OBSTRUCTION_ON)  gui.setSensor(DoorSensor.OBSTRUCTION, true);
//        if (body == Codes.OBSTRUCTION_OFF) gui.setSensor(DoorSensor.OBSTRUCTION, false);
//        if (body == Codes.OVERLOAD_ON)     gui.setSensor(DoorSensor.OVERLOAD, true);
//        if (body == Codes.OVERLOAD_OFF)    gui.setSensor(DoorSensor.OVERLOAD, false);
//    }
//
//    private void setObstruction(boolean on) {
//        obstruction = on;
//        send(Codes.SENSOR, on ? Codes.OBSTRUCTION_ON : Codes.OBSTRUCTION_OFF, 0);
//        if (on && motion == Motion.CLOSING) {
//            motion = Motion.OPENING;                          // doors reverse
//            send(Codes.STATE, Codes.REVERSING, percentOpen);
//        }
//        updateStatus();
//    }
//
//    private void setOverload(boolean on) {
//        overload = on;
//        send(Codes.SENSOR, on ? Codes.OVERLOAD_ON : Codes.OVERLOAD_OFF, 0);
//        if (on) {
//            if (percentOpen < 100) {
//                motion = Motion.OPENING;                      // overload holds doors open
//            }
//            send(Codes.STATE, Codes.HELD_OPEN, percentOpen);
//        }
//        updateStatus();
//    }

    // helpers
    private void send(byte topic, byte body, int value) {
        messenger.publish(id, new Message(topic, elevator, body, value));
    }

    private void updateStatus() {
        String state;
        if (motion == Motion.OPENING) state = "OPENING";
        else if (motion == Motion.CLOSING) state = "CLOSING";
        else if (percentOpen == 100) state = "FULLY OPEN";
        else if (percentOpen == 0) state = "FULLY CLOSED";
        else state = "STOPPED";
        gui.setStatus("Elevator " + elevator + ":  " + state + "   speed " + speed);
    }
}
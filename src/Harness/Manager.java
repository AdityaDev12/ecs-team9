package Harness;

import Protocol.Codes;
import Protocol.Message;
import Protocol.Messenger;

/**
 * coordinates commands from the test harness with the elevator door system.
 * doesn't implement door movement or safety rules
 */
public class Manager {
    private final Messenger messenger;
    private final EventLog log;
    private final int id;
    private final byte elevator;

    public Manager(Messenger messenger, byte elevator, EventLog log) {
        this.messenger = messenger;
        this.elevator = elevator;
        this.log = log;

        // register this manager as messaging participant.
        this.id = messenger.register();

        // receive reports for this elevator
        messenger.subscribe(id, Codes.STATE, elevator);
        messenger.subscribe(id, Codes.SENSOR, elevator);

        // receive system responses
        messenger.subscribe(id, Codes.SYS, Codes.ANY);
    }

    // door commands
    public void openDoor() {
        send(Codes.CMD, Codes.OPEN, 0);
    }
    public void closeDoor() {
        send(Codes.CMD, Codes.CLOSE, 0);
    }
    public void stopDoor() {
        send(Codes.CMD, Codes.STOP, 0);
    }

    // speed control
    public void setSpeed(int percentPerTick) {
        send(Codes.SPEED, Codes.SET_SPEED, percentPerTick);
    }

    // sensor controls, send protocol messages
    public void setObstruction(boolean active) {
        send(Codes.SENSOR, active ? Codes.OBSTRUCTION_ON : Codes.OBSTRUCTION_OFF, 0);
    }
    public void setOverload(boolean active) {
        send(Codes.SENSOR, active ? Codes.OVERLOAD_ON : Codes.OVERLOAD_OFF, 0);
    }

    // system communication
    public void ping() {
        send(Codes.SYS, Codes.PING, 0);
    }

    // receiving reports, returns next queued message or null if none
    public Message pollMessage() {
        return messenger.getMsg(id, Codes.ANY, Codes.ANY);
    }

    // information about participant
    public int getId() {
        return id;
    }
    public byte getElevator() {
        return elevator;
    }

    // helper
    private void send(byte topic, byte body, int value) {
        messenger.publish(id, new Message(topic, elevator, body, value));
    }
}

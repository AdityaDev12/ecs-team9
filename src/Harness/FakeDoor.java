package Harness;

import Protocol.Codes;
import Protocol.Message;
import Protocol.Messenger;

/**
 * A pretend door with no window. It obeys CMD and SPEED and reports STATE and
 */
public final class FakeDoor implements Runnable {

    private volatile double percentOpen = 0.0;      // 0 = closed, 100 = open
    private volatile double speed = 2.5;            // percent per tick
    private volatile char moving = 'X';        // O = opening, C = closing, X = stopped
    private volatile boolean obstruction = false;
    private volatile boolean overload = false;
    private volatile boolean running = true;

    private final Messenger messenger;
    private final int id;
    private final byte elevator;

    public FakeDoor(Messenger messenger, byte elevator) {
        this.messenger = messenger;
        this.elevator = elevator;
        this.id = messenger.register();
        messenger.subscribe(id, Codes.CMD, elevator);
        messenger.subscribe(id, Codes.SPEED, elevator);
        messenger.subscribe(id, Codes.SYS, Codes.ANY);
    }

    public int id() {
        return id;
    }

    public void stop() {
        running = false;
    }

    public String status() {
        return String.format(
                "%.1f%% open, moving=%c, speed=%.1f, obstruction=%s, overload=%s",
                percentOpen, moving, speed, obstruction, overload);
    }

    public void obstructionOn() {
        obstruction = true;
        send(Codes.SENSOR, Codes.OBSTRUCTION_ON, 0);
        if (moving == 'C') {
            moving = 'O';                                  // doors reverse
            send(Codes.STATE, Codes.REVERSING, (int) Math.round(percentOpen));
        }
    }

    public void obstructionOff() {
        obstruction = false;
        send(Codes.SENSOR, Codes.OBSTRUCTION_OFF, 0);
    }

    public void overloadOn() {
        overload = true;
        send(Codes.SENSOR, Codes.OVERLOAD_ON, 0);
        moving = 'O';                                      // overload holds doors open
        send(Codes.STATE, Codes.HELD_OPEN, (int) Math.round(percentOpen));
    }

    public void overloadOff() {
        overload = false;
        send(Codes.SENSOR, Codes.OVERLOAD_OFF, 0);
    }

    @Override
    public void run() {
        while (running) {
            readCommands();
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            // advance door only once per 50 ms tick
            moveDoors();
        }
    }

    private void readCommands() {
        Message m;
        while ((m = messenger.getMsg(id, Codes.ANY, Codes.ANY)) != null) {
            if (m.topic() == Codes.CMD) {
                doCommand(m.body());
            } else if (m.topic() == Codes.SPEED) {
                speed = Math.max(1, Math.min(50, m.value()));
            } else if (m.topic() == Codes.SYS && m.body() == Codes.PING) {
                send(Codes.SYS, Codes.PONG, 0);
            }
        }
    }

    private void doCommand(byte cmd) {
        if (cmd == Codes.OPEN) {
            moving = 'O';
            send(Codes.STATE, Codes.OPENING, (int) Math.round(percentOpen));
        } else if (cmd == Codes.CLOSE) {
            if (obstruction || overload) {
                send(Codes.STATE, Codes.HELD_OPEN, (int) Math.round(percentOpen));   // refuse to close
                return;
            }
            moving = 'C';
            send(Codes.STATE, Codes.CLOSING, (int) Math.round(percentOpen));
        } else if (cmd == Codes.STOP) {
            moving = 'X';
            send(Codes.STATE, Codes.STOPPED, (int) Math.round(percentOpen));
        }
    }

    private void moveDoors() {
        if (moving == 'O') {
            percentOpen = Math.min(100.0, percentOpen + speed);
            if (percentOpen >= 100) {
                percentOpen = 100.0;
                moving = 'X';
                send(Codes.STATE, Codes.FULLY_OPEN, 100);
            }
        } else if (moving == 'C') {
            percentOpen = Math.max(0.0, percentOpen - speed);
            if (percentOpen <= 0.0) {
                percentOpen = 0.0;
                moving = 'X';
                send(Codes.STATE, Codes.FULLY_CLOSED, 0);
            }
        }
    }

    private void send(byte topic, byte body, int value) {
        messenger.publish(id, new Message(topic, elevator, body, value));
    }
}
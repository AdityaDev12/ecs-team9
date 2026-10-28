package Protocol;

/** All the numbers the GUI and the harness have to agree on. */
public final class Codes {

    private Codes() { }

    public static final byte ANY = 0;

    // Topics
    public static final byte CMD    = 1;
    public static final byte SPEED  = 2;
    public static final byte STATE  = 3;
    public static final byte SENSOR = 4;
    public static final byte SYS    = 5;

    // Bodies for CMD
    public static final byte OPEN  = 1;
    public static final byte CLOSE = 2;
    public static final byte STOP  = 3;

    // Body for SPEED
    public static final byte SET_SPEED = 1;

    // Bodies for STATE
    public static final byte FULLY_OPEN   = 1;
    public static final byte FULLY_CLOSED = 2;
    public static final byte OPENING      = 3;
    public static final byte CLOSING      = 4;
    public static final byte REVERSING    = 5;
    public static final byte HELD_OPEN    = 6;
    public static final byte STOPPED      = 7;

    // Bodies for SENSOR
    public static final byte OBSTRUCTION_ON  = 1;
    public static final byte OBSTRUCTION_OFF = 2;
    public static final byte OVERLOAD_ON     = 3;
    public static final byte OVERLOAD_OFF    = 4;

    // Bodies for SYS
    public static final byte PING = 1;
    public static final byte PONG = 2;

    public static String topicName(byte t) {
        switch (t) {
            case ANY:    return "ANY";
            case CMD:    return "CMD";
            case SPEED:  return "SPEED";
            case STATE:  return "STATE";
            case SENSOR: return "SENSOR";
            case SYS:    return "SYS";
            default:     return "TOPIC_" + (t & 0xFF);
        }
    }

    /** Turns a body byte back into a word, for the log. */
    public static String bodyName(byte topic, byte b) {
        switch (topic) {
            case CMD:
                if (b == OPEN)  return "OPEN";
                if (b == CLOSE) return "CLOSE";
                if (b == STOP)  return "STOP";
                break;
            case SPEED:
                if (b == SET_SPEED) return "SET_SPEED";
                break;
            case STATE:
                if (b == FULLY_OPEN)   return "FULLY_OPEN";
                if (b == FULLY_CLOSED) return "FULLY_CLOSED";
                if (b == OPENING)      return "OPENING";
                if (b == CLOSING)      return "CLOSING";
                if (b == REVERSING)    return "REVERSING";
                if (b == HELD_OPEN)    return "HELD_OPEN";
                if (b == STOPPED)      return "STOPPED";
                break;
            case SENSOR:
                if (b == OBSTRUCTION_ON)  return "OBSTRUCTION_ON";
                if (b == OBSTRUCTION_OFF) return "OBSTRUCTION_OFF";
                if (b == OVERLOAD_ON)     return "OVERLOAD_ON";
                if (b == OVERLOAD_OFF)    return "OVERLOAD_OFF";
                break;
            case SYS:
                if (b == PING) return "PING";
                if (b == PONG) return "PONG";
                break;
            default:
                break;
        }
        return "BODY_" + (b & 0xFF);
    }
}
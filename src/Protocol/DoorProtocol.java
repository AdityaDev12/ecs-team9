package Protocol;

public final class DoorProtocol {

    private DoorProtocol() {}

    // message topics
    public static final int DOOR_COMMAND = 1;
    public static final int DOOR_CONDITION = 2;
    public static final int DOOR_STATUS = 3;

    // command messages
    public static final int OPEN = 1;
    public static final int CLOSE = 2;
    public static final int STOP = 3;

    // condition messages
    public static final int OVERLOAD_ACTIVE = 1;
    public static final int OVERLOAD_CLEAR = 2;
    public static final int OBSTRUCTION_ACTIVE = 3;
    public static final int OBSTRUCTION_CLEAR = 4;

    // door status messages
    public static final int OPENING = 1;
    public static final int CLOSING = 2;
    public static final int FULLY_OPEN = 3;
    public static final int FULLY_CLOSED = 4;
    public static final int STOPPED = 5;
    public static final int HELD_OPEN = 6;
    public static final int REVERSING = 7;
}
package Simulator;

@FunctionalInterface
public interface DoorEventListener {

    void onSensorChanged(DoorSensor sensor, boolean active, boolean fromUser);

    /** Default listener: prints every event to the terminal. */
    static DoorEventListener console() {
        return (sensor, active, fromUser) ->
                System.out.println("[DoorGUI] " + sensor + " = " + (active ? "ON" : "OFF")
                        + (fromUser ? "  (user)" : ""));
    }
}
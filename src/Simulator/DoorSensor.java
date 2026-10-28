package Simulator;

public enum DoorSensor {
    FULLY_OPEN_LEFT,
    FULLY_CLOSED,
    FULLY_OPEN_RIGHT,
    OBSTRUCTION,   // pressable by the user
    OVERLOAD       // pressable by the user (yes / no)
}
package Simulator;

public final class DoorGrid {

    private DoorGrid() {}

  // scale
    public static final double CELL_PX = 24;          // pixels per cell
    public static final double FEET_PER_CELL = 0.5;   // 1 cell = 6 inches

    //  whole window
    public static final int COLS = 22;
    public static final int ROWS = 30;
    public static final double WIDTH_PX = COLS * CELL_PX;
    public static final double HEIGHT_PX = ROWS * CELL_PX;

    // door opening (8 ft tall x 4 ft wide)
    public static final int DOOR_HEIGHT_CELLS = 16;   // 8 ft
    public static final int DOOR_WIDTH_CELLS = 8;     // 4 ft
    public static final int PANEL_WIDTH_CELLS = DOOR_WIDTH_CELLS / 2; // 2 ft each

    public static final int OPENING_LEFT_COL = 7;
    public static final int OPENING_RIGHT_COL = OPENING_LEFT_COL + DOOR_WIDTH_CELLS;  // 15
    public static final int OPENING_TOP_ROW = 6;
    public static final int OPENING_BOTTOM_ROW = OPENING_TOP_ROW + DOOR_HEIGHT_CELLS; // 22 (floor line)
    public static final int CENTER_COL = OPENING_LEFT_COL + PANEL_WIDTH_CELLS;        // 11

    // pockets the panels slide into when fully open
    public static final int LEFT_POCKET_COL = OPENING_LEFT_COL - PANEL_WIDTH_CELLS;    // 3
    public static final int RIGHT_POCKET_END_COL = OPENING_RIGHT_COL + PANEL_WIDTH_CELLS; // 19

    // wall / header around the door
    public static final int WALL_LEFT_COL = 1;
    public static final int WALL_RIGHT_COL = COLS - 1;                // 21
    public static final int HEADER_TOP_ROW = 4;

    // sensors
    public static final int SENSOR_ROW = 3;                           // triangles above the track
    public static final int OBSTRUCTION_ROW = OPENING_TOP_ROW + DOOR_HEIGHT_CELLS / 2; // 14, mid door

    // overload bar under the floor
    public static final int OVERLOAD_TOP_ROW = 24;
    public static final int OVERLOAD_ROWS = 2;
    public static final int OVERLOAD_LEFT_COL = 5;
    public static final int OVERLOAD_RIGHT_COL = 17;

    /** Grid column (can be fractional) to pixel x. */
    public static double x(double col) {
        return col * CELL_PX;
    }

    /** Grid row (can be fractional) to pixel y. */
    public static double y(double row) {
        return row * CELL_PX;
    }

    /** Number of cells to pixels. */
    public static double px(double cells) {
        return cells * CELL_PX;
    }

    /** Feet to pixels. */
    public static double feet(double ft) {
        return ft / FEET_PER_CELL * CELL_PX;
    }
}
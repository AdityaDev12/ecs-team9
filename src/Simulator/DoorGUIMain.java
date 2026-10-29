package Simulator;

import javafx.application.Platform;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static Simulator.DoorGrid.*;


public class DoorGUIMain {

    private static final Color BG = Color.web("#f6f6f4");
    private static final Color GRID = Color.web("#e7e7e3");
    private static final Color WALL = Color.web("#dcd9d2");
    private static final Color DARK = Color.web("#2e3338");
    private static final Color OFF = Color.web("#c4c4c4");
    private static final Color ON = Color.web("#2e9e4f");
    private static final Color RED = Color.web("#d32f2f");
    private static final Color ORANGE = Color.web("#f29e0c");

    private final Stage stage;
    private final Pane root = new Pane();
    private final Group grid = new Group();
    private final Pane doorLayer = new Pane();

    private final Map<DoorSensor, Boolean> state = new EnumMap<>(DoorSensor.class);
    private final List<DoorEventListener> listeners = new ArrayList<>();

    private DoorPanels panels = new PlaceholderPanels();
    private Polygon openLeft, closed, openRight;
    private Circle obstruction;
    private Rectangle overload;
    private Text overloadText, status;

    public DoorGUIMain(Stage stage) {
        this.stage = stage;
        for (DoorSensor s : DoorSensor.values()) {
            state.put(s, false);
        }
    }

    /** Builds and shows the window. */
    public void makeGUI() {
        root.setStyle("-fx-background-color: #f6f6f4;");

        // grid
        for (int c = 0; c <= COLS; c++) grid.getChildren().add(line(x(c), 0, x(c), HEIGHT_PX, GRID, 1));
        for (int r = 0; r <= ROWS; r++) grid.getChildren().add(line(0, y(r), WIDTH_PX, y(r), GRID, 1));

        // wall + doorway + floor
        Rectangle wall = rect(LEFT_POCKET_COL, HEADER_TOP_ROW, RIGHT_POCKET_END_COL - LEFT_POCKET_COL,
                OPENING_BOTTOM_ROW - HEADER_TOP_ROW, WALL);
        Rectangle doorway = rect(OPENING_LEFT_COL, OPENING_TOP_ROW, DOOR_WIDTH_CELLS, DOOR_HEIGHT_CELLS, DARK);
        Line floor = line(x(1), y(OPENING_BOTTOM_ROW), x(COLS - 1), y(OPENING_BOTTOM_ROW), DARK, 3);

        doorLayer.getChildren().setAll(panels.getNode());

        // fully open / fully closed markers
        openLeft = marker(LEFT_POCKET_COL);
        closed = marker(CENTER_COL);
        openRight = marker(RIGHT_POCKET_END_COL);

        // obstruction button (circle with an X)
        double cx = x(CENTER_COL), cy = y(OBSTRUCTION_ROW), r = px(1.2);
        obstruction = new Circle(cx, cy, r);
        obstruction.setStroke(DARK);
        obstruction.setStrokeWidth(2);
        Group obstructionButton = new Group(obstruction,
                line(cx - r * 0.55, cy - r * 0.55, cx + r * 0.55, cy + r * 0.55, DARK, 2),
                line(cx - r * 0.55, cy + r * 0.55, cx + r * 0.55, cy - r * 0.55, DARK, 2));
        clickable(obstructionButton, DoorSensor.OBSTRUCTION);

        // overload button (bar under the floor)
        overload = rect(OVERLOAD_LEFT_COL, OVERLOAD_TOP_ROW, OVERLOAD_RIGHT_COL - OVERLOAD_LEFT_COL, OVERLOAD_ROWS, Color.WHITE);
        overload.setStroke(DARK);
        overload.setArcWidth(14);
        overload.setArcHeight(14);
        overloadText = text("Overload", CENTER_COL - 1.6, OVERLOAD_TOP_ROW + 1.25, 14);
        overloadText.setMouseTransparent(true);
        clickable(overload, DoorSensor.OVERLOAD);

        // labels and status
        Text help = text("O open   C close   S stop   +/- speed   G grid", 1, ROWS - 1, 12);
        help.setFill(Color.GRAY);
        status = text("", 1, ROWS - 2.2, 14);

        root.getChildren().addAll(grid, wall, doorway, floor, doorLayer,
                openLeft, closed, openRight,
                text("open", LEFT_POCKET_COL - 0.9, SENSOR_ROW - 0.6, 12),
                text("closed", CENTER_COL - 1.2, SENSOR_ROW - 0.6, 12),
                text("open", RIGHT_POCKET_END_COL - 0.9, SENSOR_ROW - 0.6, 12),
                obstructionButton, overload, overloadText, status, help);

        Scene scene = new Scene(root, WIDTH_PX, HEIGHT_PX);
        scene.setOnKeyPressed(e -> {
            if (e.getText().equalsIgnoreCase("g")) grid.setVisible(!grid.isVisible());
        });
        stage.setTitle("ECS - Elevator Door");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();

        onDoorPositionChanged(0.0);
        for (DoorSensor s : DoorSensor.values()) refresh(s);
    }

    // public API

    public void addListener(DoorEventListener listener) { listeners.add(listener); }

    public boolean getSensor(DoorSensor sensor) { return state.get(sensor); }

    /** Turns a sensor on or off from code (harness, controller). */
    public void setSensor(DoorSensor sensor, boolean on) { fx(() -> change(sensor, on, false)); }

    /** 0.0 = closed, 1.0 = open. Moves the panels and updates the markers. */
    public void onDoorPositionChanged(double open) {
        fx(() -> {
            panels.setOpenAmount(open);
            change(DoorSensor.FULLY_CLOSED, open <= 0.001, false);
            change(DoorSensor.FULLY_OPEN_LEFT, open >= 0.999, false);
            change(DoorSensor.FULLY_OPEN_RIGHT, open >= 0.999, false);
        });
    }

    /** Replaces the placeholder panels with real ones. */
    public void attachDoorPanels(DoorPanels newPanels) {
        fx(() -> {
            panels = newPanels;
            doorLayer.getChildren().setAll(panels.getNode());
        });
    }

    /** Status line under the door. */
    public void setStatus(String text) { fx(() -> { if (status != null) status.setText(text); }); }

    public void setGridVisible(boolean visible) { fx(() -> grid.setVisible(visible)); }

    public Pane getRoot() { return root; }

    // state

    private void change(DoorSensor sensor, boolean on, boolean fromUser) {
        if (state.get(sensor) == on) return;
        state.put(sensor, on);
        refresh(sensor);
        for (DoorEventListener l : listeners) l.onSensorChanged(sensor, on, fromUser);
    }

    private void refresh(DoorSensor sensor) {
        if (openLeft == null) return;   // window not built yet
        boolean on = state.get(sensor);
        switch (sensor) {
            case FULLY_OPEN_LEFT -> openLeft.setFill(on ? ON : OFF);
            case FULLY_CLOSED -> closed.setFill(on ? ON : OFF);
            case FULLY_OPEN_RIGHT -> openRight.setFill(on ? ON : OFF);
            case OBSTRUCTION -> obstruction.setFill(on ? RED : Color.WHITE);
            case OVERLOAD -> {
                overload.setFill(on ? ORANGE : Color.WHITE);
                overloadText.setText(on ? "OVERLOADED" : "Overload");
                overloadText.setX(x(CENTER_COL) - overloadText.getLayoutBounds().getWidth() / 2);
            }
        }
    }

    // helpers

    private void clickable(Node node, DoorSensor sensor) {
        node.setCursor(Cursor.HAND);
        node.setOnMouseClicked(e -> change(sensor, !state.get(sensor), true));
    }

    private static Polygon marker(double col) {
        double cx = x(col), y0 = y(SENSOR_ROW), s = px(0.5);
        Polygon p = new Polygon(cx - s, y0, cx + s, y0, cx, y0 + px(0.9));
        p.setStroke(DARK);
        return p;
    }

    private static Rectangle rect(double col, double row, double w, double h, Color fill) {
        Rectangle r = new Rectangle(x(col), y(row), px(w), px(h));
        r.setFill(fill);
        return r;
    }

    private static Line line(double x1, double y1, double x2, double y2, Color color, double width) {
        Line l = new Line(x1, y1, x2, y2);
        l.setStroke(color);
        l.setStrokeWidth(width);
        return l;
    }

    private static Text text(String s, double col, double row, double size) {
        Text t = new Text(x(col), y(row), s);
        t.setFont(Font.font("System", size));
        t.setFill(DARK);
        return t;
    }

    private static void fx(Runnable r) {
        if (Platform.isFxApplicationThread()) r.run();
        else Platform.runLater(r);
    }
}
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
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;

import static Simulator.DoorGrid.*;

public class DoorGUIMain {

    // colors
    private static final Color BG = Color.web("#fafafa");
    private static final Color GRID_MINOR = Color.web("#e6e6e6");
    private static final Color GRID_MAJOR = Color.web("#cfcfcf");
    private static final Color WALL = Color.web("#d9d6d0");
    private static final Color WALL_EDGE = Color.web("#5f5b55");
    private static final Color HEADER = Color.web("#9a958d");
    private static final Color INK = Color.web("#1f1f1f");
    private static final Color MUTED = Color.web("#6b6b6b");
    private static final Color SENSOR_OFF = Color.web("#bdbdbd");
    private static final Color SENSOR_ON = Color.web("#2e9e4f");
    private static final Color ALERT = Color.web("#d32f2f");
    private static final Color OVERLOAD_ON = Color.web("#f29e0c");

    private final Stage stage;
    private final Pane root = new Pane();
    private final Group gridLayer = new Group();
    private final Group frameLayer = new Group();
    private final Pane doorLayer = new Pane();
    private final Group sensorLayer = new Group();

    private final Map<DoorSensor, Boolean> state = new EnumMap<>(DoorSensor.class);
    private final List<DoorEventListener> listeners = new CopyOnWriteArrayList<>();

    // sensor visuals
    private Polygon openLeftMarker, closedMarker, openRightMarker;
    private Circle obstructionCircle;
    private Line obstructionX1, obstructionX2;
    private Rectangle overloadBar;
    private Text overloadText, obstructionText;

    // moving panels (Sang's part); placeholder until the real ones are attached
    private DoorPanels panels = new PlaceholderPanels();
    private Text statusText;

    public DoorGUIMain(Stage stage) {
        this.stage = stage;
        for (DoorSensor s : DoorSensor.values()) {
            state.put(s, false);
        }
    }

    /** Builds the window. Call once on the JavaFX thread. */
    public void makeGUI() {
        root.setPrefSize(WIDTH_PX, HEIGHT_PX);
        root.setStyle("-fx-background-color: " + toHex(BG) + ";");

        doorLayer.setPickOnBounds(false);
        drawGrid();
        drawFrame();
        doorLayer.getChildren().add(panels.getNode());
        drawSensors();

        root.getChildren().addAll(gridLayer, frameLayer, doorLayer, sensorLayer);

        Scene scene = new Scene(root, WIDTH_PX, HEIGHT_PX);
        scene.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case G -> setGridVisible(!gridLayer.isVisible());
                default -> { }
            }
        });

        stage.setTitle("ECS - Elevator Door");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();

        onDoorPositionChanged(0.0); // doors start closed
    }

    // Public API
    /** Registers a listener for every sensor change. */
    public void addListener(DoorEventListener listener) {
        listeners.add(listener);
    }

    public void removeListener(DoorEventListener listener) {
        listeners.remove(listener);
    }

    /** Current state of a sensor. */
    public boolean getSensor(DoorSensor sensor) {
        return state.get(sensor);
    }

    /** Turns a sensor on or off from code (harness, network, panels). */
    public void setSensor(DoorSensor sensor, boolean active) {
        fx(() -> changeSensor(sensor, active, false));
    }

    public void onDoorPositionChanged(double leftOpen, double rightOpen) {
        fx(() -> {
            changeSensor(DoorSensor.FULLY_OPEN_LEFT, leftOpen >= 0.999, false);
            changeSensor(DoorSensor.FULLY_OPEN_RIGHT, rightOpen >= 0.999, false);
            changeSensor(DoorSensor.FULLY_CLOSED, leftOpen <= 0.001 && rightOpen <= 0.001, false);
            panels.setOpenAmount(clamp((leftOpen + rightOpen) / 2));
        });
    }

    /** Same as above when both panels move together */
    public void onDoorPositionChanged(double open) {
        onDoorPositionChanged(open, open);
    }


    public void attachDoorPanels(DoorPanels newPanels) {
        fx(() -> {
            doorLayer.getChildren().clear();
            panels = newPanels;
            doorLayer.getChildren().add(panels.getNode());
        });
    }

    /** One line of status text under the door, e.g. "CLOSING  42% open  speed 3". */
    public void setStatus(String text) {
        fx(() -> {
            if (statusText != null) {
                statusText.setText(text);
            }
        });
    }

    /** Shows or hides the grid lines (also toggled with the G key). */
    public void setGridVisible(boolean visible) {
        fx(() -> gridLayer.setVisible(visible));
    }

    public Pane getRoot() {
        return root;
    }

    // Drawing
    private void drawGrid() {
        for (int c = 0; c <= COLS; c++) {
            Line l = new Line(x(c), 0, x(c), HEIGHT_PX);
            l.setStroke(c % 2 == 0 ? GRID_MAJOR : GRID_MINOR);
            gridLayer.getChildren().add(l);
        }
        for (int r = 0; r <= ROWS; r++) {
            Line l = new Line(0, y(r), WIDTH_PX, y(r));
            l.setStroke(r % 2 == 0 ? GRID_MAJOR : GRID_MINOR);
            gridLayer.getChildren().add(l);
        }
    }

    private void drawFrame() {
        // title
        Text title = text("Elevator Door", 1, 1.2, 18, FontWeight.BOLD, INK);

        // wall around the opening (left and right of the pockets, plus header)
        Rectangle wall = rect(WALL_LEFT_COL, HEADER_TOP_ROW,
                WALL_RIGHT_COL - WALL_LEFT_COL, OPENING_BOTTOM_ROW - HEADER_TOP_ROW, WALL, WALL_EDGE, 1.5);

        // header box holding the door motor and track
        Rectangle header = rect(LEFT_POCKET_COL, HEADER_TOP_ROW,
                RIGHT_POCKET_END_COL - LEFT_POCKET_COL, OPENING_TOP_ROW - HEADER_TOP_ROW, HEADER, WALL_EDGE, 1.5);
        Text headerLabel = centered("door motor / track", CENTER_COL, HEADER_TOP_ROW + 1.25, 11, Color.WHITE);

        // pockets where the panels slide when open
        Rectangle leftPocket = rect(LEFT_POCKET_COL, OPENING_TOP_ROW, PANEL_WIDTH_CELLS, DOOR_HEIGHT_CELLS,
                WALL.deriveColor(0, 1, 0.94, 1), WALL_EDGE, 1);
        leftPocket.getStrokeDashArray().addAll(6.0, 5.0);
        Rectangle rightPocket = rect(OPENING_RIGHT_COL, OPENING_TOP_ROW, PANEL_WIDTH_CELLS, DOOR_HEIGHT_CELLS,
                WALL.deriveColor(0, 1, 0.94, 1), WALL_EDGE, 1);
        rightPocket.getStrokeDashArray().addAll(6.0, 5.0);

        // door opening (seen as the dark cabin behind the doors)
        Rectangle opening = rect(OPENING_LEFT_COL, OPENING_TOP_ROW, DOOR_WIDTH_CELLS, DOOR_HEIGHT_CELLS,
                Color.web("#3a3f45"), INK, 2);

        // track line along the top of the opening and pockets
        Line track = new Line(x(LEFT_POCKET_COL), y(OPENING_TOP_ROW), x(RIGHT_POCKET_END_COL), y(OPENING_TOP_ROW));
        track.setStrokeWidth(3);
        track.setStroke(INK);

        // floor / sill
        Line floor = new Line(x(0.5), y(OPENING_BOTTOM_ROW), x(COLS - 0.5), y(OPENING_BOTTOM_ROW));
        floor.setStrokeWidth(3);
        floor.setStroke(INK);

        // 8 ft dimension on the left
        double dx = x(0.5);
        Line dim = new Line(dx, y(OPENING_TOP_ROW), dx, y(OPENING_BOTTOM_ROW));
        dim.setStroke(MUTED);
        Line dimTop = new Line(dx - 5, y(OPENING_TOP_ROW), dx + 5, y(OPENING_TOP_ROW));
        Line dimBot = new Line(dx - 5, y(OPENING_BOTTOM_ROW), dx + 5, y(OPENING_BOTTOM_ROW));
        dimTop.setStroke(MUTED);
        dimBot.setStroke(MUTED);
        Text dimText = centered("8 ft", 0.5, OBSTRUCTION_ROW, 11, MUTED);
        dimText.setRotate(-90);
        Rectangle dimGap = new Rectangle(dx - 8, y(OBSTRUCTION_ROW) - 20, 16, 28);
        dimGap.setFill(BG);

        frameLayer.getChildren().addAll(title, wall, header, headerLabel, leftPocket, rightPocket,
                opening, track, floor, dim, dimTop, dimBot, dimGap, dimText);
    }

    private void drawSensors() {
        // fully open / fully closed markers: triangles pointing down at the track
        openLeftMarker = marker(LEFT_POCKET_COL);
        closedMarker = marker(CENTER_COL);
        openRightMarker = marker(RIGHT_POCKET_END_COL);
        Text openLeftLabel = centered("fully open", LEFT_POCKET_COL, SENSOR_ROW - 0.9, 11, MUTED);
        Text closedLabel = centered("fully closed", CENTER_COL, SENSOR_ROW - 0.9, 11, MUTED);
        Text openRightLabel = centered("fully open", RIGHT_POCKET_END_COL, SENSOR_ROW - 0.9, 11, MUTED);

        // obstruction button: circle with an X in the middle of the doorway
        double cx = x(CENTER_COL), cy = y(OBSTRUCTION_ROW), r = px(1.25);
        obstructionCircle = new Circle(cx, cy, r);
        obstructionCircle.setStrokeWidth(2.5);
        double d = r * 0.6;
        obstructionX1 = new Line(cx - d, cy - d, cx + d, cy + d);
        obstructionX2 = new Line(cx - d, cy + d, cx + d, cy - d);
        obstructionX1.setStrokeWidth(2.5);
        obstructionX2.setStrokeWidth(2.5);
        obstructionText = centered("obstruction", CENTER_COL, OBSTRUCTION_ROW - 1.75, 12, INK);
        double tw = obstructionText.getLayoutBounds().getWidth() + 14;
        Rectangle obstructionTag = new Rectangle(cx - tw / 2, y(OBSTRUCTION_ROW - 2.45), tw, px(0.95));
        obstructionTag.setArcWidth(10);
        obstructionTag.setArcHeight(10);
        obstructionTag.setFill(Color.WHITE);
        obstructionTag.setStroke(INK);
        obstructionTag.setMouseTransparent(true);
        Group obstruction = new Group(obstructionCircle, obstructionX1, obstructionX2);
        makeButton(obstruction, DoorSensor.OBSTRUCTION);

        // overload bar under the floor
        overloadBar = rect(OVERLOAD_LEFT_COL, OVERLOAD_TOP_ROW,
                OVERLOAD_RIGHT_COL - OVERLOAD_LEFT_COL, OVERLOAD_ROWS, Color.WHITE, INK, 2);
        overloadBar.setArcWidth(12);
        overloadBar.setArcHeight(12);
        overloadText = centered("OVERLOAD SENSOR  (click to toggle)", CENTER_COL,
                OVERLOAD_TOP_ROW + OVERLOAD_ROWS / 2.0 + 0.2, 13, INK);
        overloadText.setMouseTransparent(true);
        makeButton(overloadBar, DoorSensor.OVERLOAD);

        // legend
        statusText = text("", 1, ROWS - 3.1, 13, FontWeight.BOLD, INK);
        Text legend = text("Click obstruction or overload to turn them on / off.",
                1, ROWS - 2.2, 11, FontWeight.NORMAL, MUTED);
        Text legend2 = text("Keys:  O = open   C = close   S = stop   + / - = speed   G = grid",
                1, ROWS - 1.3, 11, FontWeight.NORMAL, MUTED);

        sensorLayer.getChildren().addAll(openLeftMarker, closedMarker, openRightMarker,
                openLeftLabel, closedLabel, openRightLabel,
                obstructionTag, obstructionText, obstruction, overloadBar, overloadText, statusText, legend, legend2);

        for (DoorSensor s : DoorSensor.values()) {
            refresh(s);
        }
    }

    private Polygon marker(double col) {
        double cx = x(col), tipY = y(SENSOR_ROW + 0.9), half = px(0.55);
        Polygon p = new Polygon(cx - half, tipY - px(0.9), cx + half, tipY - px(0.9), cx, tipY);
        p.setStroke(INK);
        p.setStrokeWidth(1.5);
        return p;
    }

    private void makeButton(Node node, DoorSensor sensor) {
        node.setCursor(Cursor.HAND);
        node.setOnMouseClicked(e -> changeSensor(sensor, !state.get(sensor), true));
        node.setOnMouseEntered(e -> node.setOpacity(0.85));
        node.setOnMouseExited(e -> node.setOpacity(1.0));
    }
    // State

    /** Must run on the JavaFX thread. */
    private void changeSensor(DoorSensor sensor, boolean active, boolean fromUser) {
        if (state.get(sensor) == active) {
            return;
        }
        state.put(sensor, active);
        refresh(sensor);
        for (DoorEventListener l : listeners) {
            l.onSensorChanged(sensor, active, fromUser);
        }
    }

    private void refresh(DoorSensor sensor) {
        boolean on = state.get(sensor);
        switch (sensor) {
            case FULLY_OPEN_LEFT -> openLeftMarker.setFill(on ? SENSOR_ON : SENSOR_OFF);
            case FULLY_CLOSED -> closedMarker.setFill(on ? SENSOR_ON : SENSOR_OFF);
            case FULLY_OPEN_RIGHT -> openRightMarker.setFill(on ? SENSOR_ON : SENSOR_OFF);
            case OBSTRUCTION -> {
                obstructionCircle.setFill(on ? ALERT : Color.WHITE);
                obstructionCircle.setStroke(on ? ALERT.darker() : INK);
                Color x = on ? Color.WHITE : INK;
                obstructionX1.setStroke(x);
                obstructionX2.setStroke(x);
            }
            case OVERLOAD -> {
                overloadBar.setFill(on ? OVERLOAD_ON : Color.WHITE);
                overloadText.setText(on ? "OVERLOADED" : "OVERLOAD SENSOR  (click to toggle)");
                overloadText.setFont(Font.font("System", on ? FontWeight.BOLD : FontWeight.NORMAL, 13));
                overloadText.setX(x(CENTER_COL) - overloadText.getLayoutBounds().getWidth() / 2);
            }
        }
    }

    // Helpers
    private static Rectangle rect(double col, double row, double wCells, double hCells, Color fill, Color stroke, double sw) {
        Rectangle r = new Rectangle(x(col), y(row), px(wCells), px(hCells));
        r.setFill(fill);
        r.setStroke(stroke);
        r.setStrokeWidth(sw);
        return r;
    }

    private static Text text(String s, double col, double row, double size, FontWeight weight, Color color) {
        Text t = new Text(x(col), y(row), s);
        t.setFont(Font.font("System", weight, size));
        t.setFill(color);
        return t;
    }

    private static Text centered(String s, double col, double row, double size, Color color) {
        Text t = new Text(s);
        t.setFont(Font.font("System", size));
        t.setFill(color);
        t.setTextAlignment(TextAlignment.CENTER);
        t.setX(x(col) - t.getLayoutBounds().getWidth() / 2);
        t.setY(y(row));
        t.setMouseTransparent(true);
        return t;
    }

    private static double clamp(double v) {
        return Math.max(0, Math.min(1, v));
    }

    private static void fx(Runnable r) {
        if (Platform.isFxApplicationThread()) {
            r.run();
        } else {
            Platform.runLater(r);
        }
    }

    private static String toHex(Color c) {
        return String.format("#%02x%02x%02x",
                (int) (c.getRed() * 255), (int) (c.getGreen() * 255), (int) (c.getBlue() * 255));
    }
}
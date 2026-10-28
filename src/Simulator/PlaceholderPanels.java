package Simulator;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import static Simulator.DoorGrid.*;

public class PlaceholderPanels implements DoorPanels {

    private final Rectangle left;
    private final Rectangle right;
    private final Group group;

    public PlaceholderPanels() {
        left = panel(OPENING_LEFT_COL);
        right = panel(CENTER_COL);
        group = new Group(left, right);
    }

    private static Rectangle panel(int col) {
        Rectangle r = new Rectangle(x(col), y(OPENING_TOP_ROW), px(PANEL_WIDTH_CELLS), px(DOOR_HEIGHT_CELLS));
        r.setFill(Color.web("#b9c2cc"));
        r.setStroke(Color.web("#5d6670"));
        r.setStrokeWidth(1.5);
        return r;
    }

    @Override
    public Node getNode() {
        return group;
    }

    @Override
    public void setOpenAmount(double open) {
        double shift = px(PANEL_WIDTH_CELLS) * Math.max(0, Math.min(1, open));
        left.setTranslateX(-shift);
        right.setTranslateX(shift);
    }
}
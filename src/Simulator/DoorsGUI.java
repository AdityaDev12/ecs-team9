package Simulator;

import javafx.scene.Group;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;

import static Simulator.DoorGrid.*;

public class DoorsGUI extends Pane implements DoorPanels {

    private final Group doorGroup = new Group();

    private final Rectangle leftDoor;
    private final Rectangle rightDoor;

    private final Line leftSeam;
    private final Line rightSeam;

    public DoorsGUI() {
        setStyle("-fx-background-color: transparent;");

        double width = px(PANEL_WIDTH_CELLS);
        double height = px(DOOR_HEIGHT_CELLS);
        double top = y(OPENING_TOP_ROW);

        leftDoor = createPanel(OPENING_LEFT_COL);


        rightDoor = createPanel(CENTER_COL);

        //vertical details on each panel
        leftSeam = new Line(
                x(OPENING_LEFT_COL) + width - 3, top,
                x(OPENING_LEFT_COL) + width - 3, top + height
        );

        rightSeam = new Line(
                x(CENTER_COL) + 3, top,
                x(CENTER_COL) + 3, top + height
        );

        leftSeam.setStroke(Color.web("#89939d"));
        rightSeam.setStroke(Color.web("#89939d"));

        doorGroup.getChildren().addAll(
                leftDoor,
                rightDoor,
                leftSeam,
                rightSeam
        );

        getChildren().add(doorGroup);
    }

    //creates door panel
    private Rectangle createPanel(int col) {
        Rectangle panel = new Rectangle(
                x(col),
                y(OPENING_TOP_ROW),
                px(PANEL_WIDTH_CELLS),
                px(DOOR_HEIGHT_CELLS)
        );

        panel.setFill(Color.web("#c7cdd3"));
        panel.setStroke(Color.web("#59636d"));
        panel.setStrokeWidth(2);

        return panel;
    }

    @Override
    public Pane getNode() {
        return this;
    }

    @Override
    public void setOpenAmount(double amount) {
        double clamped = Math.max(0.0, Math.min(1.0, amount));
        double shift = px(PANEL_WIDTH_CELLS) * clamped;

        leftDoor.setTranslateX(-shift);
        leftSeam.setTranslateX(-shift);

        rightDoor.setTranslateX(shift);
        rightSeam.setTranslateX(shift);
    }
}
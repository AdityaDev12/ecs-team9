package Simulator;

import javafx.scene.Node;


public interface DoorPanels {

    /** The JavaFX node holding both panels, drawn in window pixels. */
    Node getNode();

    /** 0.0 = fully closed, 1.0 = fully open. */
    void setOpenAmount(double open);
}
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class DoorsGUITest extends Application {

    @Override
    public void start(Stage stage) {

        DoorsGUI doors = new DoorsGUI();

        Scene scene = new Scene(doors, 800, 600);

        stage.setTitle("Elevator Door Assembly");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
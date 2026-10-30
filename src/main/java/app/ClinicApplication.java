package app;

import controller.MainController;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ClinicApplication extends Application {
    private static final double WINDOW_WIDTH = 1200;
    private static final double WINDOW_HEIGHT = 800;
    private static final double MIN_WIDTH = 900;
    private static final double MIN_HEIGHT = 640;

    @Override
    public void start(Stage stage) {
        MainController controller = new MainController(stage);
        Scene scene = new Scene(controller.createView(), WINDOW_WIDTH, WINDOW_HEIGHT);

        stage.setTitle("Электронная очередь поликлиники");
        stage.setMinWidth(MIN_WIDTH);
        stage.setMinHeight(MIN_HEIGHT);
        stage.setScene(scene);
        stage.show();
    }

    public static void launchApplication(String[] args) {
        launch(args);
    }
}

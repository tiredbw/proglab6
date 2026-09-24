package app;

import controller.MainController;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ClinicApplication extends Application {
    @Override
    public void start(Stage stage) {
        MainController controller = new MainController(stage);
        Scene scene = new Scene(controller.createView(), 980, 560);

        stage.setTitle("Электронная очередь поликлиники");
        stage.setMinWidth(760);
        stage.setMinHeight(420);
        stage.setScene(scene);
        stage.show();
    }

    public static void launchApplication(String[] args) {
        launch(args);
    }
}

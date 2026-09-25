package controller;

import javafx.scene.control.Alert;
import javafx.stage.Stage;

public final class Dialogs {
    private Dialogs() {
    }

    public static void error(Stage stage, String message) {
        show(stage, Alert.AlertType.ERROR, "Ошибка", message);
    }

    public static void warning(Stage stage, String message) {
        show(stage, Alert.AlertType.WARNING, "Предупреждение", message);
    }

    private static void show(Stage stage, Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.initOwner(stage);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}


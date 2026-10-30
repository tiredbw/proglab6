package controller;

import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Stage;

import java.util.Optional;
import java.util.OptionalInt;

public final class Dialogs {
    private static final double TEXT_WIDTH = 620;
    private static final double TEXT_HEIGHT = 300;

    private Dialogs() {
    }

    public static void error(Stage stage, String message) {
        show(stage, Alert.AlertType.ERROR, "Ошибка", message);
    }

    public static void warning(Stage stage, String message) {
        show(stage, Alert.AlertType.WARNING, "Предупреждение", message);
    }

    // Длинный результат показываем в области с прокруткой
    public static void text(Stage stage, String title, String content) {
        TextArea area = new TextArea(content);
        area.setEditable(false);
        area.setWrapText(true);
        area.setPrefSize(TEXT_WIDTH, TEXT_HEIGHT);

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.initOwner(stage);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.getDialogPane().setContent(area);
        alert.showAndWait();
    }

    public static Optional<String> askText(Stage stage, String title, String prompt, String defaultValue) {
        TextInputDialog dialog = new TextInputDialog(defaultValue);
        dialog.initOwner(stage);
        dialog.setTitle(title);
        dialog.setHeaderText(null);
        dialog.setContentText(prompt);
        return dialog.showAndWait();
    }

    public static OptionalInt askPositiveInt(Stage stage, String title, String prompt, int defaultValue, int max) {
        Optional<String> text = askText(stage, title, prompt, String.valueOf(defaultValue));
        if (text.isEmpty()) {
            return OptionalInt.empty();
        }

        String rangeMessage = "Введите целое число от 1 до " + max;
        try {
            int value = Integer.parseInt(text.get().trim());
            if (value < 1 || value > max) {
                error(stage, rangeMessage);
                return OptionalInt.empty();
            }
            return OptionalInt.of(value);
        } catch (NumberFormatException e) {
            error(stage, rangeMessage);
            return OptionalInt.empty();
        }
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

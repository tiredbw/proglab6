package controller;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.util.function.Consumer;

// Строка состояния: показывает прогресс фоновой операции и запускает её в отдельном потоке.
// Одновременно выполняется только одна операция: на это время busy блокирует кнопки.
public class TaskPanel extends HBox {
    private static final double SPACING = 10;
    private static final double BAR_WIDTH = 220;

    private final Stage stage;
    private final ProgressBar bar = new ProgressBar(0);
    private final Label status = new Label();
    private final Button cancelButton = new Button("Отмена");
    private final BooleanProperty busy = new SimpleBooleanProperty(false);
    private Task<?> current;

    public TaskPanel(Stage stage) {
        super(SPACING);
        this.stage = stage;
        bar.setPrefWidth(BAR_WIDTH);
        cancelButton.setDisable(true);
        cancelButton.setOnAction(event -> current.cancel());
        setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(status, Priority.ALWAYS);
        getChildren().addAll(bar, cancelButton, status);
    }

    public ReadOnlyBooleanProperty busyProperty() {
        return busy;
    }

    public void setStatus(String text) {
        status.setText(text);
    }

    public <T> void run(Task<T> task, Consumer<T> onSuccess) {
        start(task, onSuccess, false);
    }

    // Только для операций, которые не пишут в файл: cancel() прерывает поток и оставил бы файл обрезанным
    public <T> void runCancellable(Task<T> task, Consumer<T> onSuccess) {
        start(task, onSuccess, true);
    }

    private <T> void start(Task<T> task, Consumer<T> onSuccess, boolean cancellable) {
        current = task;
        busy.set(true);
        cancelButton.setDisable(!cancellable);
        bar.progressProperty().bind(task.progressProperty());
        status.textProperty().bind(task.messageProperty());

        task.setOnSucceeded(event -> {
            finish("Готово");
            onSuccess.accept(task.getValue());
        });
        task.setOnFailed(event -> {
            finish("Операция завершилась ошибкой");
            Dialogs.error(stage, describe(task.getException()));
        });
        task.setOnCancelled(event -> finish("Операция отменена"));

        Thread thread = new Thread(task, "queue-task");
        thread.setDaemon(true);
        thread.start();
    }

    private void finish(String text) {
        bar.progressProperty().unbind();
        bar.setProgress(0);
        status.textProperty().unbind();
        status.setText(text);
        cancelButton.setDisable(true);
        busy.set(false);
    }

    private String describe(Throwable error) {
        if (error instanceof CharacterCodingException) {
            return "Файл должен быть в кодировке UTF-8";
        }
        if (error instanceof IOException) {
            return "Ошибка работы с файлом: " + error;
        }
        return error.getMessage() == null ? error.toString() : error.getMessage();
    }
}

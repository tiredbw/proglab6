package controller;

import csv.CsvSaver;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import model.Ticket;
import serialization.Serializer;
import serialization.Serializers;
import structure.TicketHeap;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

// Операции с файлами. Чтение и запись идут в фоне, куча подменяется уже в FX-потоке после завершения задачи.
public class FileActions {
    private static final int MAX_SHOWN_ERRORS = 20;

    private final Stage stage;
    private final TicketHeap<Ticket> heap;
    private final TaskPanel tasks;
    private final Runnable onQueueChanged;

    public FileActions(Stage stage, TicketHeap<Ticket> heap, TaskPanel tasks, Runnable onQueueChanged) {
        this.stage = stage;
        this.heap = heap;
        this.tasks = tasks;
        this.onQueueChanged = onQueueChanged;
    }

    public void loadCsv() {
        File file = csvChooser("Загрузить CSV").showOpenDialog(stage);
        if (file != null) {
            tasks.runCancellable(QueueTasks.loadCsv(file.toPath(), List.of()), this::applyLoaded);
        }
    }

    public void mergeCsv() {
        File file = csvChooser("Слить с CSV-файлом").showOpenDialog(stage);
        if (file != null) {
            tasks.runCancellable(QueueTasks.loadCsv(file.toPath(), heap.toList()), this::applyLoaded);
        }
    }

    public void saveCsv() {
        File file = csvChooser("Сохранить CSV").showSaveDialog(stage);
        if (file == null) {
            return;
        }
        Path path = file.toPath();
        List<Ticket> snapshot = heap.toList();
        tasks.run(QueueTasks.background("Сохранение файла " + path.getFileName(), () -> {
            new CsvSaver().save(path, snapshot);
            return null;
        }), ignored -> tasks.setStatus("Сохранено талонов: " + snapshot.size()));
    }

    public void exportQueue() {
        File file = formatChooser("Экспорт очереди").showSaveDialog(stage);
        if (file == null) {
            return;
        }
        Path path = file.toPath();
        Optional<Serializer> serializer = serializerFor(path);
        if (serializer.isEmpty()) {
            return;
        }
        List<Ticket> snapshot = heap.toList();
        tasks.run(QueueTasks.background("Экспорт в файл " + path.getFileName(), () -> {
            serializer.get().write(path, snapshot);
            return null;
        }), ignored -> tasks.setStatus("Экспортировано талонов: " + snapshot.size()));
    }

    public void importQueue() {
        File file = formatChooser("Импорт очереди").showOpenDialog(stage);
        if (file == null) {
            return;
        }
        Path path = file.toPath();
        Optional<Serializer> serializer = serializerFor(path);
        if (serializer.isEmpty()) {
            return;
        }
        tasks.run(QueueTasks.background("Импорт из файла " + path.getFileName(),
                () -> QueueTasks.heapOf(serializer.get().read(path))), imported -> {
            heap.replaceWith(imported);
            onQueueChanged.run();
            tasks.setStatus("Импортировано талонов: " + heap.size());
        });
    }

    private Optional<Serializer> serializerFor(Path path) {
        Optional<Serializer> serializer = Serializers.forFile(path);
        if (serializer.isEmpty()) {
            Dialogs.error(stage, "Формат выбирается по расширению файла, поддерживаются .bin и .json");
        }
        return serializer;
    }

    private void applyLoaded(QueueTasks.LoadedQueue loaded) {
        heap.replaceWith(loaded.heap());
        onQueueChanged.run();
        tasks.setStatus("Прочитано талонов: " + loaded.loadedCount() + ", всего в очереди: " + heap.size());
        warnAboutBrokenLines(loaded.errors());
    }

    private void warnAboutBrokenLines(List<String> errors) {
        if (errors.isEmpty()) {
            return;
        }
        List<String> shown = errors.subList(0, Math.min(errors.size(), MAX_SHOWN_ERRORS));
        String more = errors.size() > shown.size() ? "\nи ещё " + (errors.size() - shown.size()) : "";
        Dialogs.warning(stage, "Пропущены битые строки:\n" + String.join("\n", shown) + more);
    }

    private FileChooser formatChooser(String title) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        for (Serializer serializer : Serializers.all()) {
            String extension = serializer.extension();
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                    extension.toUpperCase(Locale.ROOT) + " (*." + extension + ")", "*." + extension));
        }
        return chooser;
    }

    private FileChooser csvChooser(String title) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV (*.csv)", "*.csv"));
        return chooser;
    }
}

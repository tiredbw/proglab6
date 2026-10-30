package controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import model.Editable;
import model.RegularTicket;
import model.Ticket;
import service.RaceDemo;
import structure.TicketHeap;

import java.util.List;
import java.util.function.Consumer;

public class MainController {
    private static final double BUTTON_MIN_WIDTH = 135;
    private static final double BUTTON_GAP = 10;
    private static final double SECTION_GAP = 12;
    private static final double ROOT_PADDING = 16;
    private static final double TABLE_SHARE = 0.42;
    private static final int DEFAULT_GENERATED = 1000;
    private static final int MAX_GENERATED = 5_000_000;
    private static final int DEFAULT_TOP_K = 5;
    private static final int MAX_TOP_K = 10_000;

    private final Stage stage;
    private final TicketHeap<Ticket> heap = new TicketHeap<>(Ticket.PRIORITY);
    // Копия содержимого кучи для показа в таблице, данные хранятся только в heap
    private final ObservableList<Ticket> tableItems = FXCollections.observableArrayList();
    private final TableView<Ticket> table = new TableView<>(tableItems);
    private final HeapCanvas heapCanvas = new HeapCanvas(heap);
    private final TaskPanel tasks;
    private final FileActions files;
    private final Button editButton = new Button("Изменить");
    private final Button priorityButton = new Button("Изменить приоритет");
    private final Button deleteButton = new Button("Удалить");

    public MainController(Stage stage) {
        this.stage = stage;
        this.tasks = new TaskPanel(stage);
        this.files = new FileActions(stage, heap, tasks, this::refresh);
    }

    public Parent createView() {
        TicketTableFactory.configure(table);

        editButton.setDisable(true);
        priorityButton.setDisable(true);
        deleteButton.setDisable(true);
        table.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, selectedTicket) -> {
            editButton.setDisable(!(selectedTicket instanceof Editable));
            priorityButton.setDisable(!(selectedTicket instanceof Editable));
            deleteButton.setDisable(selectedTicket == null);
        });

        FlowPane fileButtons = buttonRow(
                button("Добавить", this::addTicket),
                prepared(editButton, this::editTicket),
                button("Загрузить CSV", files::loadCsv),
                button("Сохранить CSV", files::saveCsv),
                button("Слить с CSV-файлом", files::mergeCsv));
        FlowPane queueButtons = buttonRow(
                button("Извлечь минимум", this::extractMin),
                button("Найти", this::findTicket),
                prepared(deleteButton, this::deleteTicket),
                button("Top-K", this::showTopK),
                prepared(priorityButton, this::changePriority),
                button("Сгенерировать", this::generate),
                button("Демонстрация гонки", this::showRace));

        VBox toolbar = new VBox(BUTTON_GAP, fileButtons, queueButtons);
        toolbar.disableProperty().bind(tasks.busyProperty());

        SplitPane content = new SplitPane(table, heapCanvas);
        content.setOrientation(Orientation.VERTICAL);
        content.setDividerPositions(TABLE_SHARE);

        Label title = new Label("Электронная очередь поликлиники");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        VBox root = new VBox(SECTION_GAP, title, content, toolbar, tasks);
        root.setPadding(new Insets(ROOT_PADDING));
        VBox.setVgrow(content, Priority.ALWAYS);
        return root;
    }

    private Button button(String text, Runnable action) {
        return prepared(new Button(text), action);
    }

    private Button prepared(Button button, Runnable action) {
        button.setMinWidth(BUTTON_MIN_WIDTH);
        button.setOnAction(event -> action.run());
        return button;
    }

    private FlowPane buttonRow(Button... buttons) {
        return new FlowPane(BUTTON_GAP, BUTTON_GAP, buttons);
    }

    private void refresh() {
        tableItems.setAll(heap.toList());
        table.sort();
        heapCanvas.redraw();
    }

    private Ticket selectedTicket() {
        return table.getSelectionModel().getSelectedItem();
    }

    private int indexOf(Ticket ticket) {
        return heap.find(item -> item == ticket);
    }

    private void addTicket() {
        TicketDialog.showForCreate(stage).ifPresent(ticket -> {
            heap.insert(ticket);
            refresh();
            table.getSelectionModel().select(ticket);
        });
    }

    private void editTicket() {
        if (selectedTicket() instanceof RegularTicket ticket) {
            TicketDialog.showForEdit(stage, ticket).ifPresent(updated ->
                    updateInHeap(ticket, item -> item.copyFrom(updated)));
        }
    }

    private void changePriority() {
        if (selectedTicket() instanceof RegularTicket ticket) {
            TicketDialog.showForPriority(stage, ticket).ifPresent(priority ->
                    updateInHeap(ticket, item -> {
                        item.setUrgency(priority.urgency());
                        item.setTakenAt(priority.takenAt());
                    }));
        }
    }

    // Единственное место, где меняется талон из очереди: прямой вызов сеттера сломал бы инвариант кучи
    private void updateInHeap(RegularTicket ticket, Consumer<RegularTicket> change) {
        heap.changePriority(indexOf(ticket), item -> change.accept(ticket));
        refresh();
        table.getSelectionModel().select(ticket);
    }

    private void deleteTicket() {
        Ticket ticket = selectedTicket();
        if (ticket != null) {
            heap.delete(indexOf(ticket));
            refresh();
            tasks.setStatus("Удалён: " + ticket);
        }
    }

    private void extractMin() {
        if (heap.isEmpty()) {
            tasks.setStatus("Очередь пуста");
            return;
        }
        Ticket ticket = heap.extractMin();
        refresh();
        tasks.setStatus("Извлечён: " + ticket);
    }

    private void findTicket() {
        Dialogs.askText(stage, "Найти талон", "Номер карты:", "").ifPresent(text -> {
            String card = text.trim();
            int index = heap.find(ticket -> ticket.getCardNumber().equals(card));
            heapCanvas.redraw();
            if (index < 0) {
                tasks.setStatus("Карта " + card + " не найдена");
                return;
            }
            Ticket found = heap.get(index);
            table.getSelectionModel().select(found);
            table.scrollTo(found);
            tasks.setStatus("Найден в ячейке " + index + ": " + found);
        });
    }

    private void showTopK() {
        Dialogs.askPositiveInt(stage, "Top-K", "Сколько самых срочных талонов показать:",
                DEFAULT_TOP_K, MAX_TOP_K).ifPresent(k -> {
            List<Ticket> top = heap.topK(k);
            heapCanvas.redraw();
            StringBuilder text = new StringBuilder();
            for (int i = 0; i < top.size(); i++) {
                text.append(i + 1).append(". ").append(top.get(i)).append('\n');
            }
            Dialogs.text(stage, "Самые срочные талоны: " + top.size(), text.toString());
        });
    }

    private void generate() {
        Dialogs.askPositiveInt(stage, "Сгенерировать", "Сколько случайных талонов добавить в очередь:",
                DEFAULT_GENERATED, MAX_GENERATED).ifPresent(count ->
                tasks.runCancellable(QueueTasks.generate(heap.toList(), count), generated -> {
                    heap.replaceWith(generated);
                    refresh();
                    tasks.setStatus("Добавлено талонов: " + count + ", всего: " + heap.size());
                }));
    }

    private void showRace() {
        tasks.run(QueueTasks.background("Потоки вставляют в кучи", RaceDemo::run),
                report -> Dialogs.text(stage, "Демонстрация гонки", RaceDemo.describe(report)));
    }
}

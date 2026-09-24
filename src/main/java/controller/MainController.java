package controller;

import csv.CsvLoader;
import csv.CsvSaver;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import model.Editable;
import model.HomeVisitTicket;
import model.Ticket;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Optional;

public class MainController {
    private final Stage stage;
    private final ArrayList<Ticket> tickets = new ArrayList<>();
    private final ObservableList<Ticket> tableItems = FXCollections.observableArrayList();
    private final TableView<Ticket> table = new TableView<>(tableItems);
    private final Button editButton = new Button("Изменить");

    public MainController(Stage stage) {
        this.stage = stage;
    }

    public Parent createView() {
        configureTable();

        Button addButton = new Button("Добавить");
        Button loadButton = new Button("Загрузить CSV");
        Button saveButton = new Button("Сохранить CSV");

        Button[] buttons = {addButton, editButton, loadButton, saveButton};
        for (Button button : buttons) {
            button.setMinWidth(135);
        }

        editButton.setDisable(true);
        table.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, selectedTicket) ->
                editButton.setDisable(!(selectedTicket instanceof Editable)));

        addButton.setOnAction(event -> addTicket());
        editButton.setOnAction(event -> editTicket());
        loadButton.setOnAction(event -> loadCsv());
        saveButton.setOnAction(event -> saveCsv());

        Label title = new Label("Электронная очередь поликлиники");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        HBox buttonsBox = new HBox(10, addButton, editButton, loadButton, saveButton);
        buttonsBox.setAlignment(Pos.CENTER_LEFT);

        VBox root = new VBox(14, title, table, buttonsBox);
        root.setPadding(new Insets(18));
        VBox.setVgrow(table, Priority.ALWAYS);
        return root;
    }

    private void configureTable() {
        TableColumn<Ticket, String> cardColumn = new TableColumn<>("№ карты");
        cardColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getCardNumber()));
        cardColumn.setPrefWidth(105);

        TableColumn<Ticket, String> nameColumn = new TableColumn<>("ФИО");
        nameColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getFullName()));
        nameColumn.setPrefWidth(215);

        TableColumn<Ticket, String> roomColumn = new TableColumn<>("Кабинет");
        roomColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getRoom()));
        roomColumn.setPrefWidth(90);

        TableColumn<Ticket, Integer> urgencyColumn = new TableColumn<>("Срочность");
        urgencyColumn.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getUrgency()));
        urgencyColumn.setPrefWidth(90);

        TableColumn<Ticket, String> timeColumn = new TableColumn<>("Время");
        timeColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getTakenAt().toString()));
        timeColumn.setPrefWidth(155);

        TableColumn<Ticket, String> addressColumn = new TableColumn<>("Адрес");
        addressColumn.setCellValueFactory(data -> {
            if (data.getValue() instanceof HomeVisitTicket homeTicket) {
                return new ReadOnlyStringWrapper(homeTicket.getAddress());
            }
            return new ReadOnlyStringWrapper("");
        });
        addressColumn.setPrefWidth(230);

        table.getColumns().addAll(cardColumn, nameColumn, roomColumn, urgencyColumn, timeColumn, addressColumn);
        table.setPlaceholder(new Label("Загрузите CSV или добавьте талон на дом"));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    }

    private void addTicket() {
        TicketFields fields = showTicketDialog("Добавить талон", null);
        if (fields == null) {
            return;
        }

        HomeVisitTicket ticket = new HomeVisitTicket(fields.cardNumber, fields.fullName, fields.room,
                fields.urgency, fields.takenAt, fields.address);
        tickets.add(ticket);
        tableItems.add(ticket);
    }

    private void editTicket() {
        Ticket selectedTicket = table.getSelectionModel().getSelectedItem();
        if (!(selectedTicket instanceof HomeVisitTicket homeTicket)) {
            return;
        }

        TicketFields fields = showTicketDialog("Изменить талон", homeTicket);
        if (fields == null) {
            return;
        }

        homeTicket.setCardNumber(fields.cardNumber);
        homeTicket.setFullName(fields.fullName);
        homeTicket.setRoom(fields.room);
        homeTicket.setUrgency(fields.urgency);
        homeTicket.setTakenAt(fields.takenAt);
        homeTicket.setAddress(fields.address);
        table.refresh();
    }

    private TicketFields showTicketDialog(String title, HomeVisitTicket ticket) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.initOwner(stage);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField cardField = new TextField(ticket == null ? "" : ticket.getCardNumber());
        TextField nameField = new TextField(ticket == null ? "" : ticket.getFullName());
        TextField roomField = new TextField(ticket == null ? "" : ticket.getRoom());
        TextField urgencyField = new TextField(ticket == null ? "" : String.valueOf(ticket.getUrgency()));
        TextField timeField = new TextField(ticket == null ? LocalDateTime.now().withSecond(0).withNano(0).toString()
                : ticket.getTakenAt().toString());
        TextField addressField = new TextField(ticket == null ? "" : ticket.getAddress());

        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(10);
        form.setPadding(new Insets(12));
        addField(form, 0, "Номер карты:", cardField);
        addField(form, 1, "ФИО:", nameField);
        addField(form, 2, "Кабинет:", roomField);
        addField(form, 3, "Срочность (0–3):", urgencyField);
        addField(form, 4, "Время:", timeField);
        addField(form, 5, "Адрес:", addressField);
        dialog.getDialogPane().setContent(form);

        while (true) {
            Optional<ButtonType> result = dialog.showAndWait();
            if (result.isEmpty() || result.get() != ButtonType.OK) {
                return null;
            }

            TicketFields fields = validateFields(cardField.getText(), nameField.getText(), roomField.getText(),
                    urgencyField.getText(), timeField.getText(), addressField.getText());
            if (fields != null) {
                return fields;
            }
        }
    }

    private void addField(GridPane form, int row, String label, TextField field) {
        field.setPrefWidth(300);
        form.add(new Label(label), 0, row);
        form.add(field, 1, row);
    }

    private TicketFields validateFields(String cardNumber, String fullName, String room, String urgencyText,
                                        String timeText, String address) {
        cardNumber = cardNumber.trim();
        fullName = fullName.trim();
        room = room.trim();
        address = address.trim();

        if (cardNumber.isBlank() || fullName.isBlank() || room.isBlank() || address.isBlank()) {
            showError("Заполните номер карты, ФИО, кабинет и адрес");
            return null;
        }
        if (containsSemicolon(cardNumber, fullName, room, address)) {
            showError("Поля не должны содержать точку с запятой");
            return null;
        }

        int urgency;
        try {
            urgency = Integer.parseInt(urgencyText.trim());
        } catch (NumberFormatException e) {
            showError("Срочность должна быть целым числом от 0 до 3");
            return null;
        }
        if (urgency < 0 || urgency > 3) {
            showError("Срочность должна быть от 0 до 3");
            return null;
        }

        LocalDateTime takenAt;
        try {
            takenAt = LocalDateTime.parse(timeText.trim());
        } catch (DateTimeParseException e) {
            showError("Введите время в формате 2026-09-24T10:30");
            return null;
        }

        return new TicketFields(cardNumber, fullName, room, urgency, takenAt, address);
    }

    private boolean containsSemicolon(String... values) {
        for (String value : values) {
            if (value.contains(";")) {
                return true;
            }
        }
        return false;
    }

    private void loadCsv() {
        FileChooser chooser = createCsvChooser("Загрузить CSV");
        File file = chooser.showOpenDialog(stage);
        if (file == null) {
            return;
        }

        CsvLoader loader = new CsvLoader();
        try {
            ArrayList<Ticket> loadedTickets = loader.load(file.toPath());
            tickets.addAll(loadedTickets);
            tableItems.addAll(loadedTickets);

            if (!loader.getErrors().isEmpty()) {
                showWarning("Часть строк пропущена:\n" + String.join("\n", loader.getErrors()));
            }
        } catch (IOException e) {
            showError("Не удалось прочитать CSV: " + e.getMessage());
        }
    }

    private void saveCsv() {
        FileChooser chooser = createCsvChooser("Сохранить CSV");
        File file = chooser.showSaveDialog(stage);
        if (file == null) {
            return;
        }

        try {
            new CsvSaver().save(file.toPath(), tickets);
        } catch (IOException | IllegalArgumentException e) {
            showError("Не удалось сохранить CSV: " + e.getMessage());
        }
    }

    private FileChooser createCsvChooser(String title) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV (*.csv)", "*.csv"));
        return chooser;
    }

    private void showError(String message) {
        showAlert(Alert.AlertType.ERROR, "Ошибка", message);
    }

    private void showWarning(String message) {
        showAlert(Alert.AlertType.WARNING, "Предупреждение", message);
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.initOwner(stage);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private static class TicketFields {
        private final String cardNumber;
        private final String fullName;
        private final String room;
        private final int urgency;
        private final LocalDateTime takenAt;
        private final String address;

        private TicketFields(String cardNumber, String fullName, String room, int urgency,
                             LocalDateTime takenAt, String address) {
            this.cardNumber = cardNumber;
            this.fullName = fullName;
            this.room = room;
            this.urgency = urgency;
            this.takenAt = takenAt;
            this.address = address;
        }
    }
}

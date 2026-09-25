package controller;

import csv.CsvLoader;
import csv.CsvSaver;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import model.ClosedTicket;
import model.Editable;
import model.HomeVisitTicket;
import model.Ticket;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class MainController {

    private final Stage stage;
    private final ObservableList<Ticket> tableItems = FXCollections.observableArrayList();
    private final TableView<Ticket> table = new TableView<>(tableItems);
    private final Button editButton = new Button("Изменить");

    public MainController(Stage stage) {
        this.stage = stage;
    }

    public Parent createView() {
        TicketTableFactory.configure(table);

        Button addButton = new Button("Добавить");
        Button loadButton = new Button("Загрузить CSV");
        Button saveButton = new Button("Сохранить CSV");

        Button[] buttons = {addButton, editButton, loadButton, saveButton};
        for (Button button : buttons) {
            button.setMinWidth(135);
        }

        editButton.setDisable(true);
        table.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, selectedTicket) ->
                editButton.setDisable(selectedTicket == null
                        || !(selectedTicket instanceof Editable)
                        || selectedTicket instanceof ClosedTicket));

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

    private void addTicket() {
        TicketDialog.showForCreate(stage).ifPresent(tableItems::add);
    }

    private void editTicket() {
        Ticket selectedTicket = table.getSelectionModel().getSelectedItem();
        if (selectedTicket == null || !(selectedTicket instanceof Editable) || selectedTicket instanceof ClosedTicket) {
            return;
        }

        TicketDialog.showForEdit(stage, selectedTicket).ifPresent(updated -> {
            selectedTicket.setCardNumber(updated.getCardNumber());
            selectedTicket.setFullName(updated.getFullName());
            selectedTicket.setRoom(updated.getRoom());
            selectedTicket.setUrgency(updated.getUrgency());
            selectedTicket.setTakenAt(updated.getTakenAt());

            if (selectedTicket instanceof HomeVisitTicket homeTicket && updated instanceof HomeVisitTicket updatedHome) {
                homeTicket.setAddress(updatedHome.getAddress());
            }

            table.refresh();
        });
    }

    private void loadCsv() {
        FileChooser chooser = createCsvChooser("Загрузить CSV");
        File file = chooser.showOpenDialog(stage);
        if (file == null) {
            return;
        }

        CsvLoader loader = new CsvLoader();
        try {
            CsvLoader.LoadResult result = loader.loadWithReport(file.toPath());
            ArrayList<Ticket> loadedTickets = result.tickets();
            tableItems.setAll(loadedTickets);

            if (!result.errors().isEmpty()) {
                Dialogs.warning(stage, "Пропущены битые строки:\n" + String.join("\n", result.errors()));
            }
        } catch (IOException e) {
            Dialogs.error(stage, "Ошибка чтения CSV: " + e.getMessage());
        }
    }

    private void saveCsv() {
        FileChooser chooser = createCsvChooser("Сохранить CSV");
        File file = chooser.showSaveDialog(stage);
        if (file == null) {
            return;
        }

        try {
            new CsvSaver().save(file.toPath(), tableItems);
        } catch (IOException | IllegalArgumentException e) {
            Dialogs.error(stage, "Ошибка сохранения CSV: " + e.getMessage());
        }
    }

    private FileChooser createCsvChooser(String title) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV (*.csv)", "*.csv"));
        return chooser;
    }
}
package controller;

import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import model.HomeVisitTicket;
import model.Ticket;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

public final class TicketDialog {

    private TicketDialog() {
    }

    public static Optional<Ticket> showForCreate(Stage stage) {
        return show(stage, "Добавить талон", null);
    }

    public static Optional<Ticket> showForEdit(Stage stage, Ticket ticket) {
        return show(stage, "Редактировать талон", ticket);
    }

    private static Optional<Ticket> show(Stage stage, String title, Ticket source) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.initOwner(stage);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        ComboBox<String> typeBox = new ComboBox<>();
        typeBox.getItems().addAll("Обычный талон", "Вызов на дом");
        if (source instanceof HomeVisitTicket) {
            typeBox.setValue("Вызов на дом");
        } else {
            typeBox.setValue("Обычный талон");
        }
        if (source != null) {
            typeBox.setDisable(true); // При редактировании тип не меняем
        }

        TextField cardField = new TextField(source == null ? "" : source.getCardNumber());
        TextField nameField = new TextField(source == null ? "" : source.getFullName());
        TextField roomField = new TextField(source == null ? "" : source.getRoom());
        TextField urgencyField = new TextField(source == null ? "0" : String.valueOf(source.getUrgency()));
        TextField timeField = new TextField(source == null
                ? LocalDateTime.now().withSecond(0).withNano(0).toString()
                : source.getTakenAt().toString());
        TextField addressField = new TextField(source instanceof HomeVisitTicket h ? h.getAddress() : "");

        Label addressLabel = new Label("Адрес:");
        addressLabel.setVisible(typeBox.getValue().equals("Вызов на дом"));
        addressField.setVisible(typeBox.getValue().equals("Вызов на дом"));

        typeBox.setOnAction(e -> {
            boolean isHome = "Вызов на дом".equals(typeBox.getValue());
            addressLabel.setVisible(isHome);
            addressField.setVisible(isHome);
        });

        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(10);
        form.setPadding(new Insets(12));

        form.add(new Label("Тип талона:"), 0, 0);
        form.add(typeBox, 1, 0);
        addField(form, 1, "Номер карты:", cardField);
        addField(form, 2, "ФИО пациента:", nameField);
        addField(form, 3, "Кабинет:", roomField);
        addField(form, 4, "Срочность (0-3):", urgencyField);
        addField(form, 5, "Время взятия:", timeField);
        form.add(addressLabel, 0, 6);
        form.add(addressField, 1, 6);

        dialog.getDialogPane().setContent(form);

        while (true) {
            Optional<ButtonType> result = dialog.showAndWait();
            if (result.isEmpty() || result.get() != ButtonType.OK) {
                return Optional.empty();
            }

            Ticket ticket = parseTicket(stage, typeBox.getValue(), cardField, nameField, roomField, urgencyField, timeField, addressField);
            if (ticket == null) {
                continue;
            }

            List<String> validationErrors = ticket.validate();
            if (!validationErrors.isEmpty()) {
                Dialogs.error(stage, String.join("\n", validationErrors));
                continue;
            }

            return Optional.of(ticket);
        }
    }

    private static Ticket parseTicket(
            Stage stage,
            String selectedType,
            TextField cardField,
            TextField nameField,
            TextField roomField,
            TextField urgencyField,
            TextField timeField,
            TextField addressField
    ) {
        int urgency;
        try {
            urgency = Integer.parseInt(urgencyField.getText().trim());
        } catch (NumberFormatException e) {
            Dialogs.error(stage, "Срочность должна быть числом от 0 до 3");
            return null;
        }

        LocalDateTime takenAt;
        try {
            takenAt = LocalDateTime.parse(timeField.getText().trim());
        } catch (DateTimeParseException e) {
            Dialogs.error(stage, "Неверный формат времени, пример: 2026-09-24T10:30");
            return null;
        }

        if ("Вызов на дом".equals(selectedType)) {
            return new HomeVisitTicket(
                    cardField.getText().trim(),
                    nameField.getText().trim(),
                    roomField.getText().trim(),
                    urgency,
                    takenAt,
                    addressField.getText().trim()
            );
        } else {
            return new Ticket(
                    cardField.getText().trim(),
                    nameField.getText().trim(),
                    roomField.getText().trim(),
                    urgency,
                    takenAt
            );
        }
    }

    private static void addField(GridPane form, int row, String label, TextField field) {
        field.setPrefWidth(300);
        form.add(new Label(label), 0, row);
        form.add(field, 1, row);
    }
}
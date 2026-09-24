package controller;

import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import model.HomeVisitTicket;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

public final class TicketDialog {
    private TicketDialog() {
    }

    public static Optional<HomeVisitTicket> showForCreate(Stage stage) {
        return show(stage, "Добавить талон", null);
    }

    public static Optional<HomeVisitTicket> showForEdit(Stage stage, HomeVisitTicket ticket) {
        return show(stage, "Изменить талон", ticket);
    }

    private static Optional<HomeVisitTicket> show(Stage stage, String title, HomeVisitTicket source) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.initOwner(stage);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField cardField = new TextField(source == null ? "" : source.getCardNumber());
        TextField nameField = new TextField(source == null ? "" : source.getFullName());
        TextField roomField = new TextField(source == null ? "" : source.getRoom());
        TextField urgencyField = new TextField(source == null ? "" : String.valueOf(source.getUrgency()));
        TextField timeField = new TextField(source == null
                ? LocalDateTime.now().withSecond(0).withNano(0).toString()
                : source.getTakenAt().toString());
        TextField addressField = new TextField(source == null ? "" : source.getAddress());

        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(10);
        form.setPadding(new Insets(12));
        addField(form, 0, "Номер карты:", cardField);
        addField(form, 1, "ФИО:", nameField);
        addField(form, 2, "Кабинет:", roomField);
        addField(form, 3, "Срочность (0-3):", urgencyField);
        addField(form, 4, "Время:", timeField);
        addField(form, 5, "Адрес:", addressField);
        dialog.getDialogPane().setContent(form);

        while (true) {
            Optional<ButtonType> result = dialog.showAndWait();
            if (result.isEmpty() || result.get() != ButtonType.OK) {
                return Optional.empty();
            }

            HomeVisitTicket ticket = parseTicket(
                    stage,
                    cardField,
                    nameField,
                    roomField,
                    urgencyField,
                    timeField,
                    addressField);
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

    private static HomeVisitTicket parseTicket(
            Stage stage,
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
            Dialogs.error(stage, "Срочность должна быть целым числом от 0 до 3");
            return null;
        }

        LocalDateTime takenAt;
        try {
            takenAt = LocalDateTime.parse(timeField.getText().trim());
        } catch (DateTimeParseException e) {
            Dialogs.error(stage, "Введите время в формате 2026-09-24T10:30");
            return null;
        }

        return new HomeVisitTicket(
                cardField.getText().trim(),
                nameField.getText().trim(),
                roomField.getText().trim(),
                urgency,
                takenAt,
                addressField.getText().trim());
    }

    private static void addField(GridPane form, int row, String label, TextField field) {
        field.setPrefWidth(300);
        form.add(new Label(label), 0, row);
        form.add(field, 1, row);
    }
}



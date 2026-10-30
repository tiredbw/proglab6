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
import model.RegularTicket;
import model.Ticket;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

public final class TicketDialog {

    private static final String REGULAR_TYPE = "Обычный талон";
    private static final String HOME_TYPE = "Вызов на дом";
    private static final double FIELD_WIDTH = 300;
    private static final double FORM_HGAP = 12;
    private static final double FORM_VGAP = 10;
    private static final double FORM_PADDING = 12;

    // Значения полей формы, введённые пользователем
    private record TicketForm(String type, String card, String name, String room,
                              String urgency, String time, String address) {
    }

    private TicketDialog() {
    }

    public static Optional<RegularTicket> showForCreate(Stage stage) {
        return show(stage, "Добавить талон", null);
    }

    public static Optional<RegularTicket> showForEdit(Stage stage, RegularTicket ticket) {
        return show(stage, "Редактировать талон", ticket);
    }

    private static Optional<RegularTicket> show(Stage stage, String title, RegularTicket source) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.initOwner(stage);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        ComboBox<String> typeBox = new ComboBox<>();
        typeBox.getItems().addAll(REGULAR_TYPE, HOME_TYPE);
        typeBox.setValue(source instanceof HomeVisitTicket ? HOME_TYPE : REGULAR_TYPE);
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
        TextField addressField = new TextField(source == null ? "" : source.getAddress());

        Label addressLabel = new Label("Адрес:");
        addressLabel.setVisible(HOME_TYPE.equals(typeBox.getValue()));
        addressField.setVisible(HOME_TYPE.equals(typeBox.getValue()));

        typeBox.setOnAction(e -> {
            boolean isHome = HOME_TYPE.equals(typeBox.getValue());
            addressLabel.setVisible(isHome);
            addressField.setVisible(isHome);
        });

        GridPane form = new GridPane();
        form.setHgap(FORM_HGAP);
        form.setVgap(FORM_VGAP);
        form.setPadding(new Insets(FORM_PADDING));

        form.add(new Label("Тип талона:"), 0, 0);
        form.add(typeBox, 1, 0);
        addField(form, 1, "Номер карты:", cardField);
        addField(form, 2, "ФИО пациента:", nameField);
        addField(form, 3, "Кабинет:", roomField);
        addField(form, 4, "Срочность (" + Ticket.MIN_URGENCY + "-" + Ticket.MAX_URGENCY + "):", urgencyField);
        addField(form, 5, "Время взятия:", timeField);
        form.add(addressLabel, 0, 6);
        form.add(addressField, 1, 6);

        dialog.getDialogPane().setContent(form);

        while (true) {
            Optional<ButtonType> result = dialog.showAndWait();
            if (result.isEmpty() || result.get() != ButtonType.OK) {
                return Optional.empty();
            }

            TicketForm values = new TicketForm(typeBox.getValue(), cardField.getText(), nameField.getText(),
                    roomField.getText(), urgencyField.getText(), timeField.getText(), addressField.getText());
            RegularTicket ticket = parseTicket(stage, values);
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

    private static RegularTicket parseTicket(Stage stage, TicketForm form) {
        int urgency;
        try {
            urgency = Integer.parseInt(form.urgency().trim());
        } catch (NumberFormatException e) {
            Dialogs.error(stage, "Срочность должна быть числом от "
                    + Ticket.MIN_URGENCY + " до " + Ticket.MAX_URGENCY);
            return null;
        }

        LocalDateTime takenAt;
        try {
            takenAt = LocalDateTime.parse(form.time().trim());
        } catch (DateTimeParseException e) {
            Dialogs.error(stage, "Неверный формат времени, пример: 2026-09-24T10:30");
            return null;
        }

        if (HOME_TYPE.equals(form.type())) {
            return new HomeVisitTicket(form.card().trim(), form.name().trim(), form.room().trim(),
                    urgency, takenAt, form.address().trim());
        }
        return new RegularTicket(form.card().trim(), form.name().trim(), form.room().trim(), urgency, takenAt);
    }

    private static void addField(GridPane form, int row, String label, TextField field) {
        field.setPrefWidth(FIELD_WIDTH);
        form.add(new Label(label), 0, row);
        form.add(field, 1, row);
    }
}

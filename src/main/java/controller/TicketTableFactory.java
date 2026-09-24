package controller;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import model.HomeVisitTicket;
import model.Ticket;

public final class TicketTableFactory {
    private TicketTableFactory() {
    }

    public static void configure(TableView<Ticket> table) {
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
}


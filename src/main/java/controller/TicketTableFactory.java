package controller;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import model.Ticket;

import java.util.List;
import java.util.function.Function;

public final class TicketTableFactory {
    private TicketTableFactory() {
    }

    public static void configure(TableView<Ticket> table) {
        table.getColumns().addAll(List.of(
                column("№ карты", Ticket::getCardNumber, 105),
                column("ФИО", Ticket::getFullName, 215),
                column("Кабинет", Ticket::getRoom, 90),
                column("Срочность", Ticket::getUrgency, 90),
                column("Время", ticket -> ticket.getTakenAt().toString(), 155),
                column("Адрес", Ticket::getAddress, 230)));
        table.setPlaceholder(new Label("Загрузите CSV или добавьте талон"));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    }

    private static <V> TableColumn<Ticket, V> column(String title, Function<Ticket, V> getter, double width) {
        TableColumn<Ticket, V> column = new TableColumn<>(title);
        column.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(getter.apply(data.getValue())));
        column.setPrefWidth(width);
        // Строки идут в порядке массива кучи. Сортировка 10^5 строк заняла бы FX-поток на полсекунды
        column.setSortable(false);
        return column;
    }
}

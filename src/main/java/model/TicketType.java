package model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public enum TicketType {
    TICKET(1),
    HOME(2),
    CLOSED(3);

    // Код записывается в бинарный файл, поэтому не зависит от порядка констант
    private final int code;

    TicketType(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    public static Optional<TicketType> parse(String name) {
        for (TicketType type : values()) {
            if (type.name().equals(name)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }

    public static Optional<TicketType> fromCode(int code) {
        for (TicketType type : values()) {
            if (type.code == code) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }

    // Единая точка создания талона при чтении файлов любого формата.
    // Некорректные данные дают IllegalArgumentException, вызывающий код переводит его в свою ошибку
    public Ticket create(String cardNumber, String fullName, String room, int urgency,
                         LocalDateTime takenAt, String address) {
        Ticket ticket = switch (this) {
            case TICKET -> new RegularTicket(cardNumber, fullName, room, urgency, takenAt);
            case HOME -> new HomeVisitTicket(cardNumber, fullName, room, urgency, takenAt, address);
            case CLOSED -> new ClosedTicket(cardNumber, fullName, room, urgency, takenAt);
        };

        List<String> errors = ticket instanceof Editable editable
                ? editable.validate()
                : ticket.validateCommon();
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join("; ", errors));
        }
        return ticket;
    }
}

package serialization;

import model.Ticket;
import model.TicketType;

import java.io.Serial;
import java.io.Serializable;

import java.time.LocalDateTime;

// Файловое представление талона не зависит от иерархии модели.
// urgency имеет тип Integer, иначе пропущенное поле молча стало бы нулём, то есть самой высокой срочностью.
record TicketDto(String type, String cardNumber, String fullName, String room,
                 Integer urgency, LocalDateTime takenAt, String address) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    static TicketDto from(Ticket ticket) {
        return new TicketDto(ticket.type().name(), ticket.getCardNumber(), ticket.getFullName(),
                ticket.getRoom(), ticket.getUrgency(), ticket.getTakenAt(), ticket.getAddress());
    }

    Ticket toTicket(int number) throws SerializationException {
        TicketType ticketType = TicketType.parse(type).orElseThrow(() ->
                new SerializationException(SerializationErrorCode.UNKNOWN_TYPE,
                        "Запись " + number + ": неизвестный тип " + type));
        if (urgency == null) {
            throw invalidRecord(number, "нет срочности");
        }
        try {
            return ticketType.create(cardNumber, fullName, room, urgency, takenAt, address);
        } catch (IllegalArgumentException e) {
            throw invalidRecord(number, e.getMessage());
        }
    }

    static SerializationException invalidRecord(int number, String details) {
        return new SerializationException(SerializationErrorCode.CORRUPTED_DATA,
                "Запись " + number + ": " + details);
    }
}

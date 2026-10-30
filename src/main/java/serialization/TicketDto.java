package serialization;

import model.Ticket;

import java.time.LocalDateTime;

// Запись для JSON. Держит Jackson вне модели: ClosedTicket остаётся без сеттеров и конструктора по умолчанию.
// urgency имеет тип Integer, иначе пропущенное поле молча стало бы нулём, то есть самой высокой срочностью.
record TicketDto(String type, String cardNumber, String fullName, String room,
                 Integer urgency, LocalDateTime takenAt, String address) {

    static TicketDto from(Ticket ticket) {
        return new TicketDto(ticket.type().name(), ticket.getCardNumber(), ticket.getFullName(),
                ticket.getRoom(), ticket.getUrgency(), ticket.getTakenAt(), ticket.getAddress());
    }
}

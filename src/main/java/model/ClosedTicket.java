package model;

import java.time.LocalDateTime;

public final class ClosedTicket extends Ticket {

    public ClosedTicket(String cardNumber, String fullName, String room, int urgency, LocalDateTime takenAt) {
        super(cardNumber, fullName, room, urgency, takenAt);
    }

    @Override
    public TicketType type() {
        return TicketType.CLOSED;
    }
}

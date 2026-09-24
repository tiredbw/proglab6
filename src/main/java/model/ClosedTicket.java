package model;

import java.time.LocalDateTime;

public class ClosedTicket extends Ticket {
    public ClosedTicket(String cardNumber, String fullName, String room, int urgency, LocalDateTime takenAt) {
        super(cardNumber, fullName, room, urgency, takenAt);
    }
}

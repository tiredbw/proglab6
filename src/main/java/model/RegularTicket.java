package model;

import java.time.LocalDateTime;
import java.util.List;

public class RegularTicket extends Ticket implements Editable {

    public RegularTicket(String cardNumber, String fullName, String room, int urgency, LocalDateTime takenAt) {
        super(cardNumber, fullName, room, urgency, takenAt);
    }

    @Override
    public TicketType type() {
        return TicketType.TICKET;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public void setRoom(String room) {
        this.room = room;
    }

    public void setUrgency(int urgency) {
        this.urgency = urgency;
    }

    public void setTakenAt(LocalDateTime takenAt) {
        this.takenAt = takenAt;
    }

    public void copyFrom(RegularTicket source) {
        cardNumber = source.cardNumber;
        fullName = source.fullName;
        room = source.room;
        urgency = source.urgency;
        takenAt = source.takenAt;
    }

    @Override
    public List<String> validate() {
        return validateCommon();
    }
}

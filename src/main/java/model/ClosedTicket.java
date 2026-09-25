package model;

import java.time.LocalDateTime;

public final class ClosedTicket extends Ticket {
    public static final String CSV_TYPE = "CLOSED";
    public ClosedTicket(String cardNumber, String fullName, String room, int urgency, LocalDateTime takenAt) {
        super(cardNumber, fullName, room, urgency, takenAt);
    }
    @Override public void setCardNumber(String cardNumber) { throw readOnly(); }
    @Override public void setFullName(String fullName) { throw readOnly(); }
    @Override public void setRoom(String room) { throw readOnly(); }
    @Override public void setUrgency(int urgency) { throw readOnly(); }
    @Override public void setTakenAt(LocalDateTime takenAt) { throw readOnly(); }

    private UnsupportedOperationException readOnly() {
        return new UnsupportedOperationException("Закрытый талон read-only: редактирование запрещено");
    }
}

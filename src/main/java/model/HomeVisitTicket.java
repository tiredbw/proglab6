package model;

import java.time.LocalDateTime;

public class HomeVisitTicket extends Ticket implements Editable {
    private String address;

    public HomeVisitTicket(String cardNumber, String fullName, String room, int urgency,
                           LocalDateTime takenAt, String address) {
        super(cardNumber, fullName, room, urgency, takenAt);
        this.address = address;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    @Override
    public String toString() {
        return super.toString() + ", адрес: " + address;
    }
}

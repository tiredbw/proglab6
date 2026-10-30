package model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class HomeVisitTicket extends RegularTicket {

    private String address;

    public HomeVisitTicket(String cardNumber, String fullName, String room, int urgency,
                           LocalDateTime takenAt, String address) {
        super(cardNumber, fullName, room, urgency, takenAt);
        this.address = address;
    }

    @Override
    public TicketType type() {
        return TicketType.HOME;
    }

    @Override
    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    @Override
    public void copyFrom(RegularTicket source) {
        super.copyFrom(source);
        if (source instanceof HomeVisitTicket home) {
            address = home.address;
        }
    }

    @Override
    public List<String> validate() {
        List<String> errors = new ArrayList<>(validateCommon());
        if (address == null || address.isBlank()) {
            errors.add("Не указан адрес");
        } else if (address.contains(";")) {
            errors.add("Поля не должны содержать символ ';'");
        }
        return errors;
    }

    @Override
    public String toString() {
        return super.toString() + ", адрес: " + address;
    }
}

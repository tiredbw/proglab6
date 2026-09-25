package model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class HomeVisitTicket extends Ticket implements Editable {
    public static final String CSV_TYPE = "HOME";

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

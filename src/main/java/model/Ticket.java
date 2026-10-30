package model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public abstract class Ticket {
    public static final int MIN_URGENCY = 0;
    public static final int MAX_URGENCY = 3;

    // Порядок в очереди: сначала меньшая срочность, при равенстве раньше взятый талон
    public static final Comparator<Ticket> PRIORITY =
            Comparator.comparingInt(Ticket::getUrgency).thenComparing(Ticket::getTakenAt);

    protected String cardNumber;
    protected String fullName;
    protected String room;
    protected int urgency;
    protected LocalDateTime takenAt;

    protected Ticket(String cardNumber, String fullName, String room, int urgency, LocalDateTime takenAt) {
        this.cardNumber = cardNumber;
        this.fullName = fullName;
        this.room = room;
        this.urgency = urgency;
        this.takenAt = takenAt;
    }

    public abstract TicketType type();

    public String getCardNumber() {
        return cardNumber;
    }

    public String getFullName() {
        return fullName;
    }

    public String getRoom() {
        return room;
    }

    public int getUrgency() {
        return urgency;
    }

    public LocalDateTime getTakenAt() {
        return takenAt;
    }

    public String getAddress() {
        return "";
    }

    public List<String> validateCommon() {
        List<String> errors = new ArrayList<>();
        validateRequired("Номер карты", cardNumber, errors);
        validateRequired("ФИО", fullName, errors);
        validateRequired("Кабинет", room, errors);
        if (urgency < MIN_URGENCY || urgency > MAX_URGENCY) {
            errors.add("Срочность должна быть от " + MIN_URGENCY + " до " + MAX_URGENCY);
        }
        if (takenAt == null) {
            errors.add("Время взятия не указано");
        }
        return errors;
    }

    private void validateRequired(String field, String value, List<String> errors) {
        if (value == null || value.isBlank()) {
            errors.add(field + " не может быть пустым");
            return;
        }
        if (value.contains(";")) {
            errors.add(field + " не может содержать ';'");
        }
    }

    @Override
    public String toString() {
        return cardNumber + ", " + fullName + ", каб. " + room
                + ", срочность: " + urgency + ", " + takenAt;
    }
}

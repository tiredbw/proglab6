package model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Ticket {
    private String cardNumber;
    private String fullName;
    private String room;
    private int urgency;
    private LocalDateTime takenAt;

    public Ticket(String cardNumber, String fullName, String room, int urgency, LocalDateTime takenAt) {
        this.cardNumber = cardNumber;
        this.fullName = fullName;
        this.room = room;
        this.urgency = urgency;
        this.takenAt = takenAt;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }

    public int getUrgency() {
        return urgency;
    }

    public void setUrgency(int urgency) {
        this.urgency = urgency;
    }

    public LocalDateTime getTakenAt() {
        return takenAt;
    }

    public void setTakenAt(LocalDateTime takenAt) {
        this.takenAt = takenAt;
    }

    public List<String> validateCommon() {
        List<String> errors = new ArrayList<>();
        validateRequired("Не указан номер карты", cardNumber, errors);
        validateRequired("Не указано ФИО", fullName, errors);
        validateRequired("Не указан кабинет", room, errors);

        if (urgency < 0 || urgency > 3) {
            errors.add("Срочность должна быть от 0 до 3");
        }
        if (takenAt == null) {
            errors.add("Не указано время взятия");
        }
        return errors;
    }

    private void validateRequired(String message, String value, List<String> errors) {
        if (value == null || value.isBlank()) {
            errors.add(message);
            return;
        }
        if (value.contains(";")) {
            errors.add("Поля не должны содержать символ ';'");
        }
    }

    @Override
    public String toString() {
        return cardNumber + ", " + fullName + ", кабинет " + room
                + ", срочность " + urgency + ", " + takenAt;
    }
}

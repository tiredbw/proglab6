package model;

import java.time.LocalDateTime;

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

    @Override
    public String toString() {
        return cardNumber + ", " + fullName + ", кабинет " + room
                + ", срочность " + urgency + ", " + takenAt;
    }
}

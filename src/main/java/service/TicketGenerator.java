package service;

import model.ClosedTicket;
import model.HomeVisitTicket;
import model.RegularTicket;
import model.Ticket;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TicketGenerator {
    private static final String[] LAST_NAMES = {
            "Иванов", "Петров", "Сидоров", "Козлов", "Морозов", "Волков", "Лебедев", "Соколов"};
    private static final String[] FIRST_NAMES = {
            "Иван", "Пётр", "Алексей", "Олег", "Сергей", "Андрей", "Илья", "Роман"};
    private static final String[] STREETS = {
            "ул. Ленина", "ул. Гоголя", "Красный проспект", "ул. Мира", "ул. Советская"};

    private static final int MIN_ROOM = 100;
    private static final int ROOM_COUNT = 300;
    private static final int MIN_HOUSE = 1;
    private static final int HOUSE_COUNT = 120;
    private static final int MINUTES_IN_WEEK = 7 * 24 * 60;
    private static final int URGENCY_COUNT = Ticket.MAX_URGENCY - Ticket.MIN_URGENCY + 1;
    private static final int HOME_PERCENT = 20;
    private static final int CLOSED_PERCENT = 10;
    private static final int PERCENT_TOTAL = 100;

    private static final LocalDateTime BASE_TIME = LocalDateTime.of(2026, 1, 1, 8, 0);

    private final Random random;
    private int nextCard = 1;

    public TicketGenerator() {
        this(new Random());
    }

    public TicketGenerator(long seed) {
        this(new Random(seed));
    }

    private TicketGenerator(Random random) {
        this.random = random;
    }

    public Ticket next() {
        String card = "K-" + nextCard;
        nextCard++;
        String name = pick(LAST_NAMES) + " " + pick(FIRST_NAMES);
        String room = String.valueOf(MIN_ROOM + random.nextInt(ROOM_COUNT));
        int urgency = Ticket.MIN_URGENCY + random.nextInt(URGENCY_COUNT);
        LocalDateTime takenAt = BASE_TIME.plusMinutes(random.nextInt(MINUTES_IN_WEEK));

        int kind = random.nextInt(PERCENT_TOTAL);
        if (kind < HOME_PERCENT) {
            String address = pick(STREETS) + ", " + (MIN_HOUSE + random.nextInt(HOUSE_COUNT));
            return new HomeVisitTicket(card, name, room, urgency, takenAt, address);
        }
        if (kind < HOME_PERCENT + CLOSED_PERCENT) {
            return new ClosedTicket(card, name, room, urgency, takenAt);
        }
        return new RegularTicket(card, name, room, urgency, takenAt);
    }

    public List<Ticket> generate(int count) {
        List<Ticket> tickets = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            tickets.add(next());
        }
        return tickets;
    }

    private String pick(String[] values) {
        return values[random.nextInt(values.length)];
    }
}

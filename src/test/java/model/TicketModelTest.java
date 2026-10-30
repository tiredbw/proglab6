package model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TicketModelTest {

    private static final LocalDateTime TIME = LocalDateTime.of(2026, 9, 24, 10, 30);

    @Test
    void closedTicketIsNotEditable() {
        Ticket closed = new ClosedTicket("K-1", "Иванов", "101", 1, TIME);

        assertFalse(closed instanceof Editable);
    }

    @Test
    void regularAndHomeTicketsAreEditable() {
        Ticket regular = new RegularTicket("K-1", "Иванов", "101", 1, TIME);
        Ticket home = new HomeVisitTicket("K-2", "Петрова", "На дому", 1, TIME, "ул. Ленина, 1");

        assertTrue(regular instanceof Editable && home instanceof Editable);
    }

    @Test
    void homeTicketRequiresAddress() {
        HomeVisitTicket ticket = new HomeVisitTicket("K-1", "Иванов", "101", 1, TIME, " ");

        assertEquals(1, ticket.validate().size());
    }

    @Test
    void copyFromCopiesAddressEvenThroughBaseType() {
        RegularTicket target = new HomeVisitTicket("K-1", "Иванов", "101", 1, TIME, "старый адрес");
        RegularTicket source = new HomeVisitTicket("K-2", "Петрова", "102", 2, TIME, "новый адрес");

        target.copyFrom(source);

        assertEquals("новый адрес", target.getAddress());
    }

    @Test
    void priorityComparesUrgencyBeforeTime() {
        Ticket urgentButLate = new RegularTicket("K-1", "Иванов", "101", 0, TIME.plusHours(1));
        Ticket calmButEarly = new RegularTicket("K-2", "Петрова", "102", 3, TIME);

        assertTrue(Ticket.PRIORITY.compare(urgentButLate, calmButEarly) < 0);
    }

    @Test
    void priorityUsesTimeWhenUrgencyIsEqual() {
        Ticket early = new RegularTicket("K-1", "Иванов", "101", 1, TIME);
        Ticket late = new RegularTicket("K-2", "Петрова", "102", 1, TIME.plusMinutes(1));

        assertTrue(Ticket.PRIORITY.compare(early, late) < 0);
    }

    @Test
    void parseRejectsUnknownType() {
        assertEquals(Optional.empty(), TicketType.parse("PATIENT"));
    }

    @Test
    void createRejectsInvalidUrgency() {
        assertThrows(IllegalArgumentException.class,
                () -> TicketType.TICKET.create("K-1", "Иванов", "101", 9, TIME, ""));
    }

    @Test
    void createClosedTicketValidatesCommonFields() {
        assertThrows(IllegalArgumentException.class,
                () -> TicketType.CLOSED.create(" ", "Иванов", "101", 1, TIME, ""));
    }

    @Test
    void semicolonMessageNamesTheField() {
        RegularTicket ticket = new RegularTicket("K;1", "Иванов", "101", 1, TIME);

        assertEquals("Номер карты не может содержать ';'", ticket.validate().getFirst());
    }
}

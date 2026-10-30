package controller;

import model.RegularTicket;
import model.Ticket;
import org.junit.jupiter.api.Test;
import structure.TicketHeap;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QueueTasksTest {

    private static final LocalDateTime TIME = LocalDateTime.of(2026, 9, 24, 10, 30);

    private Ticket ticket(String card, int urgency) {
        return new RegularTicket(card, "Иванов", "101", urgency, TIME);
    }

    @Test
    void mergedKeepsTicketsOfBothQueues() {
        TicketHeap<Ticket> queue = QueueTasks.merged(List.of(ticket("K-1", 2)), List.of(ticket("K-2", 3)));

        assertEquals(2, queue.size());
    }

    @Test
    void mergedPutsMostUrgentTicketIntoRoot() {
        TicketHeap<Ticket> queue = QueueTasks.merged(List.of(ticket("K-1", 2)), List.of(ticket("K-2", 0)));

        assertEquals("K-2", queue.peek().getCardNumber());
    }

    @Test
    void mergedBuildsValidHeap() {
        TicketHeap<Ticket> queue = QueueTasks.merged(
                List.of(ticket("K-1", 3), ticket("K-2", 2)),
                List.of(ticket("K-3", 1), ticket("K-4", 0)));

        assertTrue(queue.isValidHeap());
    }
}

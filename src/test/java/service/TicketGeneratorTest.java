package service;

import model.Editable;
import model.Ticket;
import model.TicketType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TicketGeneratorTest {

    private static final long SEED = 7;
    private static final int COUNT = 1000;

    @Test
    void sameSeedGivesSameTickets() {
        String first = new TicketGenerator(SEED).generate(COUNT).toString();
        String second = new TicketGenerator(SEED).generate(COUNT).toString();

        assertEquals(first, second);
    }

    @Test
    void generatesRequestedCount() {
        assertEquals(COUNT, new TicketGenerator(SEED).generate(COUNT).size());
    }

    @Test
    void allGeneratedTicketsPassValidation() {
        List<Ticket> tickets = new TicketGenerator(SEED).generate(COUNT);

        boolean allValid = tickets.stream().allMatch(ticket -> (ticket instanceof Editable editable
                ? editable.validate()
                : ticket.validateCommon()).isEmpty());

        assertTrue(allValid);
    }

    @Test
    void cardNumbersContinueBetweenBatches() {
        TicketGenerator generator = new TicketGenerator(SEED);
        generator.generate(COUNT);

        assertEquals("K-" + (COUNT + 1), generator.next().getCardNumber());
    }

    @Test
    void onlyEditableTypesAreGenerated() {
        Set<TicketType> types = new TicketGenerator(SEED).generate(COUNT).stream()
                .map(Ticket::type).collect(Collectors.toSet());

        assertEquals(Set.of(TicketType.TICKET, TicketType.HOME), types);
    }

    @Test
    void cardNumbersAreUnique() {
        long unique = new TicketGenerator(SEED).generate(COUNT).stream()
                .map(Ticket::getCardNumber).distinct().count();

        assertEquals(COUNT, unique);
    }
}

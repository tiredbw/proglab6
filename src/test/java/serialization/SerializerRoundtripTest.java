package serialization;

import model.ClosedTicket;
import model.HomeVisitTicket;
import model.RegularTicket;
import model.Ticket;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import service.TicketGenerator;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Главный контракт сериализации: read(write(x)) возвращает те же данные. Тест работает только через интерфейс
class SerializerRoundtripTest {

    private static final LocalDateTime TIME = LocalDateTime.of(2026, 9, 24, 10, 30);
    private static final long SEED = 11;
    private static final int LARGE_COUNT = 10_000;

    static Stream<Serializer> formats() {
        return Serializers.all().stream();
    }

    // У Ticket нет equals, поэтому сравниваем записи: record сравнивает все поля, включая тип
    private static List<TicketDto> dtos(List<Ticket> tickets) {
        return tickets.stream().map(TicketDto::from).toList();
    }

    private static List<TicketDto> roundtrip(Serializer serializer, Path dir, List<Ticket> tickets) throws Exception {
        Path file = dir.resolve("queue." + serializer.extension());
        serializer.write(file, tickets);
        return dtos(serializer.read(file));
    }

    @ParameterizedTest
    @MethodSource("formats")
    void allTicketTypesSurviveRoundtrip(Serializer serializer, @TempDir Path dir) throws Exception {
        List<Ticket> tickets = List.of(
                new RegularTicket("K-1", "Иванов Иван", "101", 2, TIME),
                new HomeVisitTicket("K-2", "Петрова Анна", "На дому", 0, TIME.plusHours(1), "ул. Ленина, 12"),
                new ClosedTicket("K-3", "Сидоров Пётр", "204", 3, TIME.minusDays(1)));

        assertEquals(dtos(tickets), roundtrip(serializer, dir, tickets));
    }

    @ParameterizedTest
    @MethodSource("formats")
    void emptyQueueSurvivesRoundtrip(Serializer serializer, @TempDir Path dir) throws Exception {
        assertEquals(List.of(), roundtrip(serializer, dir, List.of()));
    }

    @ParameterizedTest
    @MethodSource("formats")
    void nanosecondsOfTimeAreKept(Serializer serializer, @TempDir Path dir) throws Exception {
        List<Ticket> tickets = List.of(new RegularTicket("K-1", "Иванов", "101", 1, TIME.plusNanos(123_456_789)));

        assertEquals(dtos(tickets), roundtrip(serializer, dir, tickets));
    }

    @ParameterizedTest
    @MethodSource("formats")
    void largeQueueSurvivesRoundtrip(Serializer serializer, @TempDir Path dir) throws Exception {
        List<Ticket> tickets = new TicketGenerator(SEED).generate(LARGE_COUNT);

        assertEquals(dtos(tickets), roundtrip(serializer, dir, tickets));
    }
}

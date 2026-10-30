package serialization;

import model.Ticket;
import model.TicketType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonSerializerTest {

    private final Serializer serializer = new JsonSerializer();

    private Path resource(String name) throws URISyntaxException {
        return Path.of(getClass().getResource("/" + name).toURI());
    }

    @Test
    void referenceFileKeepsTicketTypes() throws Exception {
        List<TicketType> types = serializer.read(resource("queue_valid.json")).stream().map(Ticket::type).toList();

        assertEquals(List.of(TicketType.TICKET, TicketType.HOME, TicketType.CLOSED), types);
    }

    @Test
    void writtenFileStoresFormatVersion(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("queue.json");

        serializer.write(file, List.of());

        assertTrue(Files.readString(file).contains("\"formatVersion\" : 1"));
    }

    @Test
    void timeIsWrittenAsReadableString(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("queue.json");

        serializer.write(file, serializer.read(resource("queue_valid.json")));

        assertTrue(Files.readString(file).contains("\"2026-09-24T10:30:00\""));
    }

    @ParameterizedTest
    @CsvSource({
            "queue_bad_syntax.json,     CORRUPTED_DATA",
            "queue_empty.json,          CORRUPTED_DATA",
            "queue_wrong_version.json,  UNSUPPORTED_VERSION",
            "queue_no_version.json,     CORRUPTED_DATA",
            "queue_no_tickets.json,     CORRUPTED_DATA",
            "queue_unknown_type.json,   UNKNOWN_TYPE",
            "queue_no_urgency.json,     CORRUPTED_DATA",
            "queue_bad_urgency.json,    CORRUPTED_DATA",
            "queue_bad_date.json,       CORRUPTED_DATA",
            "queue_null_record.json,    CORRUPTED_DATA",
            "queue_float_urgency.json,  CORRUPTED_DATA",
            "queue_trailing_garbage.json, CORRUPTED_DATA"
    })
    void brokenFileGivesErrorCode(String file, SerializationErrorCode expected) throws Exception {
        Path path = resource(file);

        SerializationException error = assertThrows(SerializationException.class, () -> serializer.read(path));

        assertEquals(expected, error.getCode());
    }
}

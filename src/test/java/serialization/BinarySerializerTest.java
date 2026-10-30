package serialization;

import model.Ticket;
import model.TicketType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BinarySerializerTest {

    private final Serializer serializer = new BinarySerializer();

    private Path resource(String name) throws URISyntaxException {
        return Path.of(getClass().getResource("/" + name).toURI());
    }

    @Test
    void referenceFileKeepsTicketTypes() throws Exception {
        List<TicketType> types = serializer.read(resource("queue_valid.bin")).stream().map(Ticket::type).toList();

        assertEquals(List.of(TicketType.TICKET, TicketType.HOME, TicketType.CLOSED), types);
    }

    @Test
    void referenceFileKeepsAddress() throws Exception {
        assertEquals("ул. Ленина, 12", serializer.read(resource("queue_valid.bin")).get(1).getAddress());
    }

    @Test
    void referenceFileKeepsTime() throws Exception {
        LocalDateTime expected = LocalDateTime.of(2026, 9, 24, 9, 15);

        assertEquals(expected, serializer.read(resource("queue_valid.bin")).get(1).getTakenAt());
    }

    @ParameterizedTest
    @CsvSource({
            "queue_bad_magic.bin,        BAD_MAGIC",
            "queue_wrong_version.bin,    UNSUPPORTED_VERSION",
            "queue_truncated_header.bin, CORRUPTED_DATA",
            "queue_truncated.bin,        CORRUPTED_DATA",
            "queue_unknown_type.bin,     UNKNOWN_TYPE",
            "queue_huge_count.bin,       CORRUPTED_DATA",
            "queue_negative_count.bin,   CORRUPTED_DATA",
            "queue_trailing_bytes.bin,   CORRUPTED_DATA",
            "queue_bad_urgency.bin,      CORRUPTED_DATA",
            "queue_bad_utf.bin,          CORRUPTED_DATA"
    })
    void brokenFileGivesErrorCode(String file, SerializationErrorCode expected) throws Exception {
        Path path = resource(file);

        SerializationException error = assertThrows(SerializationException.class, () -> serializer.read(path));

        assertEquals(expected, error.getCode());
    }
}

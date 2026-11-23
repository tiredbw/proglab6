package serialization;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ObjectStreamBinaryTest {
    @TempDir
    Path temporary;

    private final Serializer serializer = new BinarySerializer();

    @Test
    void exportUsesJavaObjectStreamHeader() throws Exception {
        Path file = temporary.resolve("queue.bin");
        serializer.write(file, List.of());

        byte[] bytes = Files.readAllBytes(file);
        assertEquals("aced0005", java.util.HexFormat.of().formatHex(bytes, 0, 4));
    }

    @Test
    void rejectsUnsupportedVersion() throws Exception {
        assertError(SerializationErrorCode.UNSUPPORTED_VERSION,
                writeObjects(new BinarySerializer.QueueFile(BinarySerializer.VERSION + 1, new TicketDto[0])));
    }

    @Test
    void rejectsUnexpectedClass() throws Exception {
        assertError(SerializationErrorCode.CORRUPTED_DATA, writeObjects(new Date(0)));
    }

    @Test
    void rejectsTruncatedSnapshot() throws Exception {
        Path file = writeObjects(new BinarySerializer.QueueFile(BinarySerializer.VERSION, new TicketDto[0]));
        byte[] original = Files.readAllBytes(file);
        Files.write(file, Arrays.copyOf(original, original.length - 1));

        assertError(SerializationErrorCode.CORRUPTED_DATA, file);
    }

    @Test
    void rejectsTrailingObject() throws Exception {
        assertError(SerializationErrorCode.CORRUPTED_DATA,
                writeObjects(new BinarySerializer.QueueFile(BinarySerializer.VERSION, new TicketDto[0]), "лишнее"));
    }

    @Test
    void validatesTicketFieldsAfterDeserialization() throws Exception {
        TicketDto badTicket = new TicketDto("TICKET", "100", "Иванов Иван", "101", 9,
                LocalDateTime.of(2026, 1, 1, 8, 0), "");

        assertError(SerializationErrorCode.CORRUPTED_DATA,
                writeObjects(new BinarySerializer.QueueFile(BinarySerializer.VERSION, new TicketDto[]{badTicket})));
    }

    private Path writeObjects(Object... values) throws Exception {
        Path file = temporary.resolve("queue.bin");
        try (var out = new ObjectOutputStream(Files.newOutputStream(file))) {
            for (Object value : values) {
                out.writeObject(value);
            }
        }
        return file;
    }

    private void assertError(SerializationErrorCode expected, Path file) {
        SerializationException error = assertThrows(SerializationException.class, () -> serializer.read(file));
        assertEquals(expected, error.getCode());
    }
}

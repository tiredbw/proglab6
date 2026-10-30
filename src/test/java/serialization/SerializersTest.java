package serialization;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SerializersTest {

    @ParameterizedTest
    @CsvSource({"queue.bin, bin", "queue.json, json", "QUEUE.JSON, json", "архив.очередь.bin, bin"})
    void serializerIsChosenByExtension(String file, String expected) {
        assertEquals(expected, Serializers.forFile(Path.of(file)).orElseThrow().extension());
    }

    @Test
    void unknownExtensionGivesNothing() {
        assertEquals(Optional.empty(), Serializers.forFile(Path.of("queue.csv")));
    }
}

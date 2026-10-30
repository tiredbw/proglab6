package serialization;

import model.Ticket;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

// Общий контракт форматов. GUI и тесты работают с ним и не знают, что внутри файла.
// IOException означает сбой диска, SerializationException означает плохое содержимое файла.
public interface Serializer {
    void write(Path file, List<Ticket> tickets) throws IOException;

    List<Ticket> read(Path file) throws IOException, SerializationException;

    String extension();
}

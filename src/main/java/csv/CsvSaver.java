package csv;

import model.Ticket;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class CsvSaver {
    public void save(Path file, List<Ticket> tickets) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            writer.write(CsvFormat.HEADER);
            writer.newLine();
            for (Ticket ticket : tickets) {
                writer.write(toLine(ticket));
                writer.newLine();
            }
        }
    }

    private String toLine(Ticket ticket) {
        return String.join(CsvFormat.DELIMITER,
                ticket.type().name(),
                ticket.getCardNumber(),
                ticket.getFullName(),
                ticket.getRoom(),
                String.valueOf(ticket.getUrgency()),
                String.valueOf(ticket.getTakenAt()),
                ticket.getAddress());
    }
}

package csv;

import model.ClosedTicket;
import model.HomeVisitTicket;
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
        String type;
        String address = "";

        if (ticket instanceof HomeVisitTicket homeTicket) {
            type = HomeVisitTicket.CSV_TYPE;
            address = homeTicket.getAddress();
        } else if (ticket instanceof ClosedTicket) {
            type = ClosedTicket.CSV_TYPE;
        } else {
            throw new IllegalArgumentException("Неподдерживаемый тип талона");
        }

        return String.join(CsvFormat.DELIMITER,
                type,
                ticket.getCardNumber(),
                ticket.getFullName(),
                ticket.getRoom(),
                String.valueOf(ticket.getUrgency()),
                String.valueOf(ticket.getTakenAt()),
                address);
    }
}

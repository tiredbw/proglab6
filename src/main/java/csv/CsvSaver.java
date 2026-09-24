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
            writer.write("type;cardNumber;fullName;room;urgency;takenAt;address");
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
            type = "HOME";
            address = homeTicket.getAddress();
        } else if (ticket instanceof ClosedTicket) {
            type = "CLOSED";
        } else {
            throw new IllegalArgumentException("Неподдерживаемый тип талона");
        }

        return type + ";" + ticket.getCardNumber() + ";" + ticket.getFullName() + ";"
                + ticket.getRoom() + ";" + ticket.getUrgency() + ";"
                + ticket.getTakenAt() + ";" + address;
    }
}

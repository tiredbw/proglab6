package csv;

import model.ClosedTicket;
import model.HomeVisitTicket;
import model.Ticket;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class CsvLoader {
    private final ArrayList<String> errors = new ArrayList<>();

    public ArrayList<Ticket> load(Path file) throws IOException {
        ArrayList<Ticket> tickets = new ArrayList<>();
        errors.clear();

        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (lineNumber == 1 && line.equals("type;cardNumber;fullName;room;urgency;takenAt;address")) {
                    continue;
                }
                if (line.isBlank()) {
                    continue;
                }

                try {
                    tickets.add(parseLine(line));
                } catch (CsvLoadException e) {
                    errors.add("Строка " + lineNumber + " [" + e.getCode() + "]: " + e.getMessage());
                }
            }
        }

        return tickets;
    }

    public Ticket parseLine(String line) throws CsvLoadException {
        String[] parts = line.split(";", -1);
        if (parts.length != 7) {
            throw new CsvLoadException(CsvErrorCode.WRONG_FIELD_COUNT,
                    "ожидалось 7 полей, получено " + parts.length);
        }

        String type = parts[0].trim();
        String cardNumber = parts[1].trim();
        String fullName = parts[2].trim();
        String room = parts[3].trim();
        String address = parts[6].trim();

        if (type.isEmpty() || cardNumber.isEmpty() || fullName.isEmpty() || room.isEmpty()
                || parts[4].isBlank() || parts[5].isBlank()) {
            throw new CsvLoadException(CsvErrorCode.EMPTY_REQUIRED_FIELD,
                    "не заполнено обязательное поле");
        }

        int urgency;
        try {
            urgency = Integer.parseInt(parts[4].trim());
        } catch (NumberFormatException e) {
            throw new CsvLoadException(CsvErrorCode.BAD_NUMBER, "срочность должна быть целым числом");
        }
        if (urgency < 0 || urgency > 3) {
            throw new CsvLoadException(CsvErrorCode.URGENCY_OUT_OF_RANGE,
                    "срочность должна быть от 0 до 3");
        }

        LocalDateTime takenAt;
        try {
            takenAt = LocalDateTime.parse(parts[5].trim());
        } catch (DateTimeParseException e) {
            throw new CsvLoadException(CsvErrorCode.BAD_DATE,
                    "неверный формат даты и времени");
        }

        return switch (type) {
            case "HOME" -> {
                if (address.isEmpty()) {
                    throw new CsvLoadException(CsvErrorCode.EMPTY_REQUIRED_FIELD,
                            "для талона на дом нужен адрес");
                }
                yield new HomeVisitTicket(cardNumber, fullName, room, urgency, takenAt, address);
            }
            case "CLOSED" -> new ClosedTicket(cardNumber, fullName, room, urgency, takenAt);
            default -> throw new CsvLoadException(CsvErrorCode.UNKNOWN_TYPE,
                    "неизвестный тип " + type);
        };
    }

    public List<String> getErrors() {
        return new ArrayList<>(errors);
    }
}

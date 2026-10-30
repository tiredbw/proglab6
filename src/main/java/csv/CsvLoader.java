package csv;

import model.Ticket;
import model.TicketType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

public class CsvLoader {

    public LoadResult loadWithReport(Path file) throws IOException {
        return loadWithReport(file, (done, total) -> { });
    }

    // onProgress получает (обработано строк, всего строк) и может прервать загрузку исключением
    public LoadResult loadWithReport(Path file, BiConsumer<Integer, Integer> onProgress) throws IOException {
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        List<Ticket> tickets = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (int i = 0; i < lines.size(); i++) {
            onProgress.accept(i, lines.size());
            String line = lines.get(i);
            int lineNumber = i + 1;
            if ((lineNumber == 1 && isHeader(line)) || line.isBlank()) {
                continue;
            }
            try {
                tickets.add(parseLine(line));
            } catch (CsvLoadException e) {
                errors.add("Строка " + lineNumber + " [" + e.getCode() + "]: " + e.getMessage());
            }
        }

        onProgress.accept(lines.size(), lines.size());
        return new LoadResult(tickets, errors);
    }

    public Ticket parseLine(String line) throws CsvLoadException {
        Objects.requireNonNull(line, "line");
        String[] parts = line.split(CsvFormat.DELIMITER, -1);
        if (parts.length != CsvFormat.COLUMN_COUNT) {
            throw new CsvLoadException(CsvErrorCode.WRONG_FIELD_COUNT,
                    "Ожидалось " + CsvFormat.COLUMN_COUNT + " колонок, получено " + parts.length);
        }

        String typeName = parts[0].trim();
        String cardNumber = parts[1].trim();
        String fullName = parts[2].trim();
        String room = parts[3].trim();
        String address = parts[6].trim();

        int urgency;
        try {
            urgency = Integer.parseInt(parts[4].trim());
        } catch (NumberFormatException e) {
            throw new CsvLoadException(CsvErrorCode.BAD_NUMBER, "Срочность должна быть числом");
        }

        LocalDateTime takenAt;
        try {
            takenAt = LocalDateTime.parse(parts[5].trim());
        } catch (DateTimeParseException e) {
            throw new CsvLoadException(CsvErrorCode.BAD_DATE,
                    "Неверный формат даты/времени, ожидается ISO-8601");
        }

        TicketType type = TicketType.parse(typeName).orElseThrow(() ->
                new CsvLoadException(CsvErrorCode.UNKNOWN_TYPE, "Неизвестный тип талона: " + typeName));

        try {
            return type.create(cardNumber, fullName, room, urgency, takenAt, address);
        } catch (IllegalArgumentException e) {
            throw new CsvLoadException(CsvErrorCode.INVALID_DATA, e.getMessage());
        }
    }

    private boolean isHeader(String line) {
        return normalizeLine(line).equalsIgnoreCase(normalizeLine(CsvFormat.HEADER));
    }

    private String normalizeLine(String line) {
        String normalized = line == null ? "" : line.trim();
        if (!normalized.isEmpty() && normalized.charAt(0) == '﻿') {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    public record LoadResult(List<Ticket> tickets, List<String> errors) {
    }
}

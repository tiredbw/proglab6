package serialization;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import model.Ticket;
import model.TicketType;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class JsonSerializer implements Serializer {
    static final int VERSION = 1;
    private static final String EXTENSION = "json";
    private static final String VERSION_FIELD = "formatVersion";
    private static final String TICKETS_FIELD = "tickets";

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private record QueueFile(int formatVersion, List<TicketDto> tickets) {
    }

    @Override
    public String extension() {
        return EXTENSION;
    }

    @Override
    public void write(Path file, List<Ticket> tickets) throws IOException {
        List<TicketDto> records = new ArrayList<>(tickets.size());
        for (Ticket ticket : tickets) {
            records.add(TicketDto.from(ticket));
        }
        MAPPER.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), new QueueFile(VERSION, records));
    }

    @Override
    public List<Ticket> read(Path file) throws IOException, SerializationException {
        JsonNode root;
        try {
            root = MAPPER.readTree(file.toFile());
        } catch (JsonProcessingException e) {
            throw corrupted("неверный синтаксис JSON");
        }

        // Сначала версия: в файле другой версии записи могут быть устроены иначе
        JsonNode version = root.path(VERSION_FIELD);
        if (!version.isInt()) {
            throw corrupted("нет поля " + VERSION_FIELD);
        }
        if (version.intValue() != VERSION) {
            throw new SerializationException(SerializationErrorCode.UNSUPPORTED_VERSION,
                    "Версия файла " + version.intValue() + " не поддерживается, ожидается " + VERSION);
        }

        JsonNode records = root.path(TICKETS_FIELD);
        if (!records.isArray()) {
            throw corrupted("нет массива " + TICKETS_FIELD);
        }

        List<Ticket> tickets = new ArrayList<>(records.size());
        for (JsonNode record : records) {
            tickets.add(toTicket(record, tickets.size() + 1));
        }
        return tickets;
    }

    private Ticket toTicket(JsonNode record, int number) throws SerializationException {
        TicketDto dto;
        try {
            dto = MAPPER.treeToValue(record, TicketDto.class);
        } catch (JsonProcessingException e) {
            throw corrupted("запись " + number + " не разобрана");
        }
        if (dto == null) {
            throw corrupted("запись " + number + " пуста");
        }

        TicketType type = TicketType.parse(dto.type()).orElseThrow(() ->
                new SerializationException(SerializationErrorCode.UNKNOWN_TYPE,
                        "Запись " + number + ": неизвестный тип " + dto.type()));
        if (dto.urgency() == null) {
            throw corrupted("запись " + number + ": нет срочности");
        }

        try {
            return type.create(dto.cardNumber(), dto.fullName(), dto.room(), dto.urgency(),
                    dto.takenAt(), dto.address());
        } catch (IllegalArgumentException e) {
            throw corrupted("запись " + number + ": " + e.getMessage());
        }
    }

    private SerializationException corrupted(String details) {
        return new SerializationException(SerializationErrorCode.CORRUPTED_DATA, "Файл повреждён: " + details);
    }
}

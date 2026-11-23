package serialization;

import model.Ticket;
import model.TicketType;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.UTFDataFormatException;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

// Формат файла:
//   заголовок: magic (4 байта), версия (short), количество записей (int);
//   запись: код типа (byte), карта, ФИО, кабинет (UTF), срочность (byte),
//           время (long секунды и int наносекунды), адрес (UTF, только у HOME).
final class LegacyBinaryReader {
    static final int MAGIC = 0x51554555; // Байты ASCII "QUEU"
    static final short VERSION = 1;
    List<Ticket> read(DataInputStream in) throws IOException, SerializationException {
        try {
            int count = readHeader(in);
            // Память под записи не выделяем заранее: в испорченном заголовке может быть огромное число
            List<Ticket> tickets = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                tickets.add(readTicket(in, i + 1));
            }
            if (in.read() != -1) {
                throw corrupted("после последней записи остались лишние байты");
            }
            return tickets;
        } catch (EOFException e) {
            throw corrupted("файл обрезан");
        } catch (UTFDataFormatException e) {
            throw corrupted("испорчена строка");
        }
    }

    private int readHeader(DataInputStream in) throws IOException, SerializationException {
        int magic = in.readInt();
        if (magic != MAGIC) {
            throw new SerializationException(SerializationErrorCode.BAD_MAGIC,
                    "Файл не является очередью талонов");
        }
        short version = in.readShort();
        if (version != VERSION) {
            throw new SerializationException(SerializationErrorCode.UNSUPPORTED_VERSION,
                    "Версия файла " + version + " не поддерживается, ожидается " + VERSION);
        }
        int count = in.readInt();
        if (count < 0) {
            throw corrupted("отрицательное количество записей");
        }
        return count;
    }

    private Ticket readTicket(DataInputStream in, int number) throws IOException, SerializationException {
        int typeCode = in.readByte();
        TicketType type = TicketType.fromCode(typeCode).orElseThrow(() ->
                new SerializationException(SerializationErrorCode.UNKNOWN_TYPE,
                        "Запись " + number + ": неизвестный код типа " + typeCode));

        String cardNumber = in.readUTF();
        String fullName = in.readUTF();
        String room = in.readUTF();
        int urgency = in.readByte();
        long seconds = in.readLong();
        int nanos = in.readInt();
        String address = type == TicketType.HOME ? in.readUTF() : "";

        try {
            LocalDateTime takenAt = LocalDateTime.ofEpochSecond(seconds, nanos, ZoneOffset.UTC);
            return type.create(cardNumber, fullName, room, urgency, takenAt, address);
        } catch (DateTimeException | IllegalArgumentException e) {
            throw corrupted("запись " + number + ": " + e.getMessage());
        }
    }

    private SerializationException corrupted(String details) {
        return new SerializationException(SerializationErrorCode.CORRUPTED_DATA, "Файл повреждён: " + details);
    }
}

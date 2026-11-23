package serialization;

import model.Ticket;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamConstants;
import java.io.ObjectStreamException;
import java.io.Serial;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class BinarySerializer implements Serializer {
    static final int VERSION = 1;
    private static final int MAGIC = Short.toUnsignedInt(ObjectStreamConstants.STREAM_MAGIC);
    private static final ObjectInputFilter FILTER = ObjectInputFilter.Config.createFilter(
            "maxdepth=8;serialization.BinarySerializer$QueueFile;"
                    + "serialization.TicketDto;java.lang.Integer;java.lang.Number;"
                    + "java.time.Ser;java.time.LocalDateTime;!*");

    record QueueFile(int formatVersion, TicketDto[] tickets) implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
    }

    @Override
    public String extension() {
        return "bin";
    }

    @Override
    public void write(Path file, List<Ticket> tickets) throws IOException {
        TicketDto[] records = tickets.stream().map(TicketDto::from).toArray(TicketDto[]::new);
        try (var out = new ObjectOutputStream(new BufferedOutputStream(Files.newOutputStream(file)))) {
            out.writeObject(new QueueFile(VERSION, records));
        }
    }

    @Override
    public List<Ticket> read(Path file) throws IOException, SerializationException {
        long fileSize = Files.size(file);
        try (var stream = new BufferedInputStream(Files.newInputStream(file))) {
            var header = new DataInputStream(stream);
            stream.mark(Integer.BYTES);
            int prefix = header.readUnsignedShort();
            stream.reset();
            if (prefix != MAGIC) {
                return new LegacyBinaryReader().read(header);
            }
            try (var in = new ObjectInputStream(stream)) {
                in.setObjectInputFilter(info -> info.arrayLength() > fileSize
                        ? ObjectInputFilter.Status.REJECTED : FILTER.checkInput(info));
                if (!(in.readObject() instanceof QueueFile queue)) {
                    throw corrupted("ожидался снимок очереди");
                }
                List<Ticket> tickets = restore(queue);
                checkEnd(in);
                return tickets;
            }
        } catch (EOFException e) {
            throw corrupted("файл обрезан");
        } catch (ObjectStreamException | ClassNotFoundException e) {
            throw corrupted("неверное содержимое бинарного потока");
        }
    }

    private List<Ticket> restore(QueueFile queue) throws SerializationException {
        if (queue.formatVersion() != VERSION) {
            throw new SerializationException(SerializationErrorCode.UNSUPPORTED_VERSION,
                    "Версия файла " + queue.formatVersion() + " не поддерживается, ожидается " + VERSION);
        }
        TicketDto[] records = queue.tickets();
        if (records == null) {
            throw corrupted("нет массива талонов");
        }
        List<Ticket> tickets = new ArrayList<>(records.length);
        for (TicketDto record : records) {
            int number = tickets.size() + 1;
            if (record == null) {
                throw TicketDto.invalidRecord(number, "пустой талон");
            }
            tickets.add(record.toTicket(number));
        }
        return tickets;
    }

    private void checkEnd(ObjectInputStream in) throws IOException, ClassNotFoundException, SerializationException {
        try {
            in.readObject();
        } catch (EOFException endOfFile) {
            return;
        }
        throw corrupted("после снимка очереди остались лишние данные");
    }

    private SerializationException corrupted(String details) {
        return new SerializationException(SerializationErrorCode.CORRUPTED_DATA, "Файл повреждён: " + details);
    }
}

package benchmark;

import model.Ticket;
import serialization.BinarySerializer;
import serialization.JsonSerializer;
import serialization.SerializationException;
import serialization.Serializer;
import service.TicketGenerator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

// Сравнение форматов на 10^4 записей: размер файла, среднее время сохранения и загрузки по 5 прогонам, фрагмент файла.
public final class FormatComparison {
    private static final int TICKETS = 10_000;
    private static final int WARMUP_RUNS = 20;
    private static final int MEASURED_RUNS = 5;
    private static final long SEED = 2026;
    private static final int FRAGMENT_CHARS = 420;
    private static final int HEX_BYTES = 96;
    private static final int HEX_LINE = 16;
    private static final double NANOS_IN_MILLI = 1_000_000.0;
    private static final double BYTES_IN_KIB = 1024.0;

    private FormatComparison() {
    }

    public static void main(String[] args) throws IOException, SerializationException {
        List<Ticket> tickets = new TicketGenerator(SEED).generate(TICKETS);
        Path dir = Files.createTempDirectory("format-comparison");
        Serializer binary = new BinarySerializer();
        Serializer json = new JsonSerializer();

        double[] binaryTimes = measure(binary, tickets, dir);
        double[] jsonTimes = measure(json, tickets, dir);
        long binarySize = Files.size(fileFor(binary, dir));
        long jsonSize = Files.size(fileFor(json, dir));

        System.out.println("Записей: " + TICKETS + ", прогрев: " + WARMUP_RUNS
                + ", замер: среднее по " + MEASURED_RUNS + " прогонам. Java " + Runtime.version() + ".");
        System.out.println();
        System.out.println("| Формат | Размер, байт | Размер, КиБ | Сохранение, мс | Загрузка, мс |");
        System.out.println("|---|---|---|---|---|");
        printRow("bin", binarySize, binaryTimes);
        printRow("json", jsonSize, jsonTimes);
        System.out.printf(Locale.ROOT, "%nJSON больше бинарного файла в %.2f раза.%n", (double) jsonSize / binarySize);

        System.out.println();
        System.out.println("Начало bin:");
        System.out.println(hexDump(Files.readAllBytes(fileFor(binary, dir))));
        System.out.println();
        System.out.println("Начало json:");
        System.out.println(Files.readString(fileFor(json, dir)).substring(0, FRAGMENT_CHARS));

        Files.delete(fileFor(binary, dir));
        Files.delete(fileFor(json, dir));
        Files.delete(dir);
    }

    private static Path fileFor(Serializer serializer, Path dir) {
        return dir.resolve("queue." + serializer.extension());
    }

    // Возвращает пару {среднее сохранение, среднее чтение} в миллисекундах
    private static double[] measure(Serializer serializer, List<Ticket> tickets, Path dir)
            throws IOException, SerializationException {
        Path file = fileFor(serializer, dir);
        for (int i = 0; i < WARMUP_RUNS; i++) {
            serializer.write(file, tickets);
            serializer.read(file);
        }

        long writeNanos = 0;
        long readNanos = 0;
        for (int i = 0; i < MEASURED_RUNS; i++) {
            long start = System.nanoTime();
            serializer.write(file, tickets);
            long middle = System.nanoTime();
            serializer.read(file);
            long end = System.nanoTime();
            writeNanos += middle - start;
            readNanos += end - middle;
        }
        return new double[]{
                writeNanos / NANOS_IN_MILLI / MEASURED_RUNS,
                readNanos / NANOS_IN_MILLI / MEASURED_RUNS};
    }

    private static void printRow(String name, long size, double[] times) {
        System.out.printf(Locale.ROOT, "| %s | %d | %.1f | %.3f | %.3f |%n",
                name, size, size / BYTES_IN_KIB, times[0], times[1]);
    }

    private static String hexDump(byte[] data) {
        StringBuilder result = new StringBuilder();
        for (int offset = 0; offset < HEX_BYTES; offset += HEX_LINE) {
            StringBuilder hex = new StringBuilder();
            StringBuilder ascii = new StringBuilder();
            for (int i = offset; i < offset + HEX_LINE; i++) {
                hex.append(String.format("%02x ", data[i]));
                boolean printable = data[i] >= ' ' && data[i] < 127;
                ascii.append(printable ? (char) data[i] : '.');
            }
            result.append(String.format("%04x  ", offset)).append(hex).append(' ').append(ascii).append('\n');
        }
        return result.toString().stripTrailing();
    }
}

package csv;

import model.Ticket;
import model.TicketType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CsvLoaderTest {

    private final CsvLoader loader = new CsvLoader();

    private Path resource(String name) throws URISyntaxException {
        return Path.of(getClass().getResource("/" + name).toURI());
    }

    @Test
    void validFixtureLoadsAllTicketTypes() throws Exception {
        List<TicketType> types = loader.loadWithReport(resource("tickets_valid.csv")).tickets()
                .stream().map(Ticket::type).toList();

        assertEquals(List.of(TicketType.TICKET, TicketType.HOME, TicketType.CLOSED), types);
    }

    @Test
    void homeTicketKeepsAddressFromCsv() throws Exception {
        Ticket home = loader.loadWithReport(resource("tickets_valid.csv")).tickets().get(1);

        assertEquals("ул. Ленина, 12", home.getAddress());
    }

    @Test
    void validFixtureProducesNoErrors() throws Exception {
        assertEquals(List.of(), loader.loadWithReport(resource("tickets_valid.csv")).errors());
    }

    @Test
    void brokenLinesAreSkippedAndGoodOnesKept() throws Exception {
        assertEquals(2, loader.loadWithReport(resource("tickets_broken.csv")).tickets().size());
    }

    @Test
    void everyBrokenLineIsReported() throws Exception {
        assertEquals(6, loader.loadWithReport(resource("tickets_broken.csv")).errors().size());
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "TICKET;K-1;Иванов                              | WRONG_FIELD_COUNT",
            "TICKET;K-1;Иванов;101;срочно;2026-09-24T10:30; | BAD_NUMBER",
            "TICKET;K-1;Иванов;101;1;вчера;                 | BAD_DATE",
            "PATIENT;K-1;Иванов;101;1;2026-09-24T10:30;     | UNKNOWN_TYPE",
            "TICKET;K-1;Иванов;101;7;2026-09-24T10:30;      | INVALID_DATA"
    })
    void parseLineReportsErrorCode(String line, CsvErrorCode expected) {
        CsvLoadException error = assertThrows(CsvLoadException.class, () -> loader.parseLine(line));

        assertEquals(expected, error.getCode());
    }

    @Test
    void numberIsCheckedBeforeTypeAsInLab1() {
        CsvLoadException error = assertThrows(CsvLoadException.class,
                () -> loader.parseLine("PATIENT;K-1;Иванов;101;x;2026-09-24T10:30;"));

        assertEquals(CsvErrorCode.BAD_NUMBER, error.getCode());
    }

    @Test
    void progressReachesTotal() throws Exception {
        List<Integer> done = new ArrayList<>();

        loader.loadWithReport(resource("tickets_valid.csv"), (current, total) -> done.add(current));

        assertEquals(4, done.getLast());
    }

    @Test
    void cancellationFromProgressCallbackStopsLoading() {
        assertThrows(CancellationException.class, () ->
                loader.loadWithReport(resource("tickets_valid.csv"), (current, total) -> {
                    throw new CancellationException();
                }));
    }

    @Test
    void savedFileLoadsBackInSameFormat(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("saved.csv");
        List<Ticket> original = loader.loadWithReport(resource("tickets_valid.csv")).tickets();

        new CsvSaver().save(file, original);

        List<Ticket> loaded = loader.loadWithReport(file).tickets();
        assertEquals(original.toString(), loaded.toString());
    }

    @Test
    void missingFileThrowsIoException() {
        assertThrows(IOException.class, () -> loader.loadWithReport(Path.of("нет_такого_файла.csv")));
    }
}

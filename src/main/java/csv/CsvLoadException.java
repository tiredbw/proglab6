package csv;

public class CsvLoadException extends Exception {
    private final CsvErrorCode code;

    public CsvLoadException(CsvErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public CsvErrorCode getCode() {
        return code;
    }
}

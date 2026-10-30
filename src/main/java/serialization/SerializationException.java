package serialization;

public class SerializationException extends Exception {
    private final SerializationErrorCode code;

    public SerializationException(SerializationErrorCode code, String message) {
        super(code + ": " + message);
        this.code = code;
    }

    public SerializationErrorCode getCode() {
        return code;
    }
}

package serialization;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class Serializers {
    private static final List<Serializer> ALL = List.of(new BinarySerializer(), new JsonSerializer());

    private Serializers() {
    }

    public static List<Serializer> all() {
        return ALL;
    }

    public static Optional<Serializer> forFile(Path file) {
        String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
        return ALL.stream()
                .filter(serializer -> name.endsWith("." + serializer.extension()))
                .findFirst();
    }
}

package lang.qkm.match;

import java.util.stream.*;
import lang.qkm.type.Type;

public final class MatchComplete implements Match {

    public final String capture;
    public final Type type;

    public MatchComplete(String capture, Type type) {
        this.capture = capture;
        this.type = type;
    }

    public static MatchComplete wildcard(Type type) {
        return new MatchComplete(null, type);
    }

    @Override
    public Stream<String> getCaptures() {
        return this.capture == null
                ? Stream.empty()
                : Stream.of(this.capture);
    }

    @Override
    public Type getType() {
        return this.type;
    }

    @Override
    public String toString() {
        return this.capture == null ? "_" : this.capture;
    }
}

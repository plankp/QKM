package lang.qkm.match;

import java.util.*;
import java.util.stream.*;
import lang.qkm.type.Type;

public final class MatchAll implements Match {

    public final String capture;
    public final Type type;

    public MatchAll(Type type) {
        this.capture = null;
        this.type = type;
    }

    public MatchAll(String capture, Type type) {
        if (capture == null)
            throw new IllegalArgumentException("Illegal null capture variable");

        this.capture = capture;
        this.type = type;
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
    public Object getCtor() {
        return null;
    }

    @Override
    public List<Match> getArgs() {
        return null;
    }

    @Override
    public String toString() {
        return this.capture == null ? "_" : this.capture;
    }
}

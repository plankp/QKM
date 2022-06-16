package lang.qkm.match;

import java.util.*;
import java.util.stream.*;

public final class MatchAll implements Match {

    public final String capture;

    public MatchAll() {
        this.capture = null;
    }

    public MatchAll(String capture) {
        if (capture == null)
            throw new IllegalArgumentException("Illegal null capture variable");

        this.capture = capture;
    }

    @Override
    public Stream<String> getCaptures() {
        return this.capture == null
                ? Stream.empty()
                : Stream.of(this.capture);
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

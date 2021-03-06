package lang.qkm.match;

import java.util.*;
import java.util.stream.*;

public final class MatchBool implements Match {

    public final boolean value;

    public MatchBool(boolean value) {
        this.value = value;
    }

    @Override
    public <R> R accept(Visitor<R> v) {
        return v.visitMatchBool(this);
    }

    @Override
    public Stream<String> getCaptures() {
        return Stream.empty();
    }

    @Override
    public Object getCtor() {
        return this.value;
    }

    @Override
    public List<Match> getArgs() {
        return List.of();
    }

    @Override
    public String toString() {
        return this.value ? "true" : "false";
    }
}

package lang.qkm.match;

import java.util.*;
import java.util.stream.*;

public final class MatchString implements Match {

    public final String value;

    public MatchString(String value) {
        this.value = value;
    }

    @Override
    public <R> R accept(Visitor<R> v) {
        return v.visitMatchString(this);
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
        return this.value;
    }
}

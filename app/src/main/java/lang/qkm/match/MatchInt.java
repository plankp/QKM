package lang.qkm.match;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.*;

public final class MatchInt implements Match {

    public final BigInteger value;

    public MatchInt(BigInteger value) {
        this.value = value;
    }

    @Override
    public <R> R accept(Visitor<R> v) {
        return v.visitMatchInt(this);
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
        return this.value.toString();
    }
}

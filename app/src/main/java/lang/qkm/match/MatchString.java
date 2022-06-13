package lang.qkm.match;

import java.util.*;
import java.util.stream.*;
import lang.qkm.type.*;

public final class MatchString implements Match {

    public final String value;

    public MatchString(String value) {
        this.value = value;
    }

    @Override
    public Type getType() {
        return TyString.INSTANCE;
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

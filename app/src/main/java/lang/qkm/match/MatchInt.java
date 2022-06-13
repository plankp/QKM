package lang.qkm.match;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.*;
import lang.qkm.type.*;

public final class MatchInt implements Match {

    public final TyInt type;
    public final BigInteger value;

    public MatchInt(TyInt type, BigInteger value) {
        this.type = type;
        this.value = type.signed(value);
    }

    @Override
    public Type getType() {
        return this.type;
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
        return this.value.toString() + this.type;
    }
}

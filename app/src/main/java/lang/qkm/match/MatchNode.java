package lang.qkm.match;

import java.util.*;
import java.util.stream.*;
import lang.qkm.type.Type;

public final class MatchNode implements Match {

    public final Type type;
    public final Object id;
    public final List<Match> args;

    public MatchNode(Type type, Object id, List<Match> args) {
        if (id == null)
            // just in case
            throw new IllegalArgumentException("Node id cannot be null");

        this.type = type;
        this.id = id;
        this.args = args;
    }

    @Override
    public Stream<String> getCaptures() {
        return this.args.stream().flatMap(Match::getCaptures);
    }

    @Override
    public Type getType() {
        return this.type;
    }

    @Override
    public String toString() {
        return this.id + " " + this.args;
    }
}

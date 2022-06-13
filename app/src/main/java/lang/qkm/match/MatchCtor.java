package lang.qkm.match;

import java.util.*;
import java.util.stream.*;
import lang.qkm.type.*;

public final class MatchCtor implements Match {

    public final TyCtor type;
    public final String ctor;
    public final List<Match> args;

    public MatchCtor(TyCtor type, String ctor, List<Match> args) {
        this.type = type;
        this.ctor = ctor;
        this.args = args;
    }

    @Override
    public Type getType() {
        return this.type;
    }

    @Override
    public Stream<String> getCaptures() {
        return this.args.stream().flatMap(Match::getCaptures);
    }

    @Override
    public Object getCtor() {
        return this.ctor;
    }

    @Override
    public List<Match> getArgs() {
        return this.args;
    }

    @Override
    public String toString() {
        if (this.args.isEmpty())
            return this.ctor;

        return this.args.stream()
                .map(Object::toString)
                .collect(Collectors.joining(", ", "(", ")"));
    }
}

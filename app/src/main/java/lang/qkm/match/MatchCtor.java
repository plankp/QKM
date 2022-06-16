package lang.qkm.match;

import java.util.*;
import java.util.stream.*;
import java.util.function.*;

public final class MatchCtor implements Match {

    public final String ctor;
    public final List<Match> args;

    public MatchCtor(String ctor, List<Match> args) {
        this.ctor = ctor;
        this.args = args;
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
    public Match toWildcard(Supplier<? extends Match> gen) {
        return new MatchCtor(this.ctor, this.args.stream()
                .map(k -> gen.get())
                .collect(Collectors.toList()));
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

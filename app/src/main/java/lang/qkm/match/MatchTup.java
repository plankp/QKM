package lang.qkm.match;

import java.util.*;
import java.util.stream.*;
import java.util.function.*;
import lang.qkm.type.*;

public final class MatchTup implements Match {

    public final TyTup type;
    public final List<Match> elements;

    public MatchTup(TyTup type, List<Match> elements) {
        this.type = type;
        this.elements = elements;
    }

    @Override
    public Type getType() {
        return this.type;
    }

    @Override
    public Stream<String> getCaptures() {
        return this.elements.stream().flatMap(Match::getCaptures);
    }

    @Override
    public Object getCtor() {
        return TyTup.class;
    }

    @Override
    public List<Match> getArgs() {
        return this.elements;
    }

    @Override
    public Match toWildcard(Function<? super Type, ? extends Match> gen) {
        return new MatchTup(this.type, this.elements.stream()
                .map(m -> gen.apply(m.getType()))
                .collect(Collectors.toList()));
    }

    @Override
    public String toString() {
        if (this.elements.isEmpty())
            return "()";

        return this.elements.stream()
                .map(Object::toString)
                .collect(Collectors.joining(", ", "(", ")"));
    }
}

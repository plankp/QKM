package lang.qkm.match;

import java.util.*;
import java.util.stream.*;
import java.util.function.*;
import lang.qkm.type.TyTup;

public final class MatchTup implements Match {

    public final List<Match> elements;

    public MatchTup(List<Match> elements) {
        this.elements = elements;
    }

    @Override
    public <R> R accept(Visitor<R> v) {
        return v.visitMatchTup(this);
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
    public Match toWildcard(Supplier<? extends Match> gen) {
        return new MatchTup(this.elements.stream()
                .map(k -> gen.get())
                .collect(Collectors.toList()));
    }

    @Override
    public Match simplify() {
        if (this.elements.isEmpty())
            return this;

        boolean captureless = true;
        final List<Match> result = new ArrayList<Match>(this.elements.size());
        for (Match m : this.elements) {
            m = m.simplify();
            result.add(m);
            captureless &= m instanceof MatchAll && ((MatchAll) m).capture == null;
        }

        if (captureless)
            // safe because
            // 1.  there are no captures
            // 2.  tuples only have one constructor
            return new MatchAll();

        return new MatchTup(result);
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

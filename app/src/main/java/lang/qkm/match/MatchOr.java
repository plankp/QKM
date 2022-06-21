package lang.qkm.match;

import java.util.*;
import java.util.stream.*;
import lang.qkm.util.SList;

public final class MatchOr implements Match {

    public final SList<Match> submatches;

    public MatchOr(SList<Match> submatches) {
        this.submatches = submatches;
    }

    @Override
    public <R> R accept(Visitor<R> v) {
        return v.visitMatchOr(this);
    }

    @Override
    public Stream<String> getCaptures() {
        // a capture must appear on both sides of the or
        return this.submatches.head().getCaptures();
    }

    @Override
    public Object getCtor() {
        return null;
    }

    @Override
    public List<Match> getArgs() {
        return null;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append('(').append(this.submatches.head());
        for (SList<?> k = this.submatches.tail(); k.nonEmpty(); k = k.tail())
            sb.append(" | ").append(k.head());
        return sb.append(')').toString();
    }
}

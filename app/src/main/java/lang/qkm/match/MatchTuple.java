package lang.qkm.match;

import java.util.*;
import java.util.stream.*;

public final class MatchTuple implements Match {

    public final List<Match> elements;

    public MatchTuple(List<Match> elements) {
        this.elements = elements;
    }

    @Override
    public String toString() {
        return this.elements.stream()
                .map(Object::toString)
                .collect(Collectors.joining(", ", "(", ")"));
    }

    @Override
    public int hashCode() {
        return this.elements.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (!(obj instanceof MatchTuple))
            return false;

        final MatchTuple m = (MatchTuple) obj;
        return this.elements.equals(m.elements);
    }
}

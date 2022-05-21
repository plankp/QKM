package lang.qkm.match;

import java.util.*;

public final class MatchNode implements Match {

    public final Object id;
    public final List<Match> args;

    public MatchNode(Object id) {
        this(id, List.of());
    }

    public MatchNode(Object id, Match args) {
        this(id, List.of(args));
    }

    public MatchNode(Object id, List<Match> args) {
        if (id == null)
            // just in case
            throw new IllegalArgumentException("Node id cannot be null");

        this.id = id;
        this.args = args;
    }

    @Override
    public String toString() {
        return this.id + " " + this.args;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id, this.args);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (!(obj instanceof MatchNode))
            return false;

        final MatchNode m = (MatchNode) obj;
        return this.id.equals(m.id) && this.args.equals(m.args);
    }
}

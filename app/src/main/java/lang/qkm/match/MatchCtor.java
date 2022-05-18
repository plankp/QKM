package lang.qkm.match;

import java.util.*;

public final class MatchCtor implements Match {

    public final String id;
    public final Match arg;

    public MatchCtor(String id, Match arg) {
        this.id = id;
        this.arg = arg;
    }

    @Override
    public String toString() {
        return this.id + " " + this.arg;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id, this.arg);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (!(obj instanceof MatchCtor))
            return false;

        final MatchCtor m = (MatchCtor) obj;
        return this.id.equals(m.id) && this.arg.equals(m.arg);
    }
}

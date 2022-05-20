package lang.qkm.match;

import java.util.*;
import lang.qkm.type.*;

public final class MatchAtom implements Match {

    public final Object value;

    public MatchAtom(Object value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return this.value.toString();
    }

    @Override
    public int hashCode() {
        return this.value.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof MatchAtom))
            return false;

        final MatchAtom m = (MatchAtom) obj;
        return this.value.equals(m.value);
    }
}

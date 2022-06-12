package lang.qkm.expr;

import java.util.*;
import java.util.stream.*;

public final class EVar implements Expr {

    public final String name;

    public EVar(String name) {
        this.name = name;
    }

    @Override
    public Stream<EVar> fv() {
        return Stream.of(this);
    }

    @Override
    public String toString() {
        return this.name;
    }

    @Override
    public int hashCode() {
        return this.name.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof EVar))
            return false;

        final EVar v = (EVar) o;
        return this.name.equals(v.name);
    }
}

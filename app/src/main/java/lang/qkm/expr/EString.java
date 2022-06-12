package lang.qkm.expr;

import java.util.*;
import java.util.stream.*;

public final class EString implements Expr {

    public final String value;

    public EString(String value) {
        this.value = value;
    }

    @Override
    public <R> R accept(Visitor<R> v) {
        return v.visitEString(this);
    }

    @Override
    public Stream<EVar> fv() {
        return Stream.empty();
    }

    @Override
    public String toString() {
        return this.value;
    }
}

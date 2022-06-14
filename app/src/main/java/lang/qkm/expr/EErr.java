package lang.qkm.expr;

import java.util.*;
import java.util.stream.*;

// for now...
public final class EErr implements Expr {

    public final Expr value;

    public EErr(Expr value) {
        this.value = value;
    }

    @Override
    public <R> R accept(Visitor<R> v) {
        return v.visitEErr(this);
    }

    @Override
    public Stream<EVar> fv() {
        return this.value.fv();
    }

    @Override
    public String toString() {
        return "!! " + this.value;
    }
}

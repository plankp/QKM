package lang.qkm.expr;

import java.util.*;
import java.util.stream.*;

public final class EBool implements Expr {

    public final boolean value;

    public EBool(boolean value) {
        this.value = value;
    }

    @Override
    public <R> R accept(Visitor<R> v) {
        return v.visitEBool(this);
    }

    @Override
    public Stream<EVar> fv() {
        return Stream.empty();
    }

    @Override
    public String toString() {
        return this.value ? "true" : "false";
    }
}

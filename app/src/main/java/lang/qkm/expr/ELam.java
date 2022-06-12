package lang.qkm.expr;

import java.util.*;
import java.util.stream.*;

public final class ELam implements Expr {

    public final EVar arg;
    public final Expr body;

    public ELam(EVar arg, Expr body) {
        this.arg = arg;
        this.body = body;
    }

    @Override
    public Stream<EVar> fv() {
        return this.body.fv().filter(v -> !v.equals(this.arg));
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("(\\").append(this.arg);

        Expr f = this.body;
        while (f instanceof ELam) {
            final ELam next = (ELam) f;
            sb.append(' ').append(next.arg);
            f = next.body;
        }
        return sb.append(". ").append(f).append(')').toString();
    }
}

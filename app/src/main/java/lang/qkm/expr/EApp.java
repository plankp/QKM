package lang.qkm.expr;

import java.util.*;
import java.util.stream.*;

public final class EApp implements Expr {

    public final Expr f;
    public final Expr arg;

    public EApp(Expr f, Expr arg) {
        this.f = f;
        this.arg = arg;
    }

    @Override
    public <R> R accept(Visitor<R> v) {
        return v.visitEApp(this);
    }

    @Override
    public Stream<EVar> fv() {
        return Stream.concat(this.f.fv(), this.arg.fv());
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append('(').append(this.f).append(' ');

        Expr arg = this.arg;
        while (arg instanceof EApp) {
            final EApp a = (EApp) arg;
            sb.append(a.f).append(' ');
            arg = a.arg;
        }
        return sb.append(arg).append(')').toString();
    }
}

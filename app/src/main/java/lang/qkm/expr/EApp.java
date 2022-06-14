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
        final ArrayDeque<Expr> stack = new ArrayDeque<>();
        stack.push(this.arg);

        Expr f = this.f;
        while (f instanceof EApp) {
            final EApp a = (EApp) f;
            stack.push(a.arg);
            f = a.f;
        }

        final StringBuilder sb = new StringBuilder();
        sb.append('(').append(f);

        while (!stack.isEmpty())
            sb.append(' ').append(stack.pop());

        return sb.append(')').toString();
    }
}

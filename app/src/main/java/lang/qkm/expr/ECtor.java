package lang.qkm.expr;

import java.util.*;
import java.util.stream.*;

public final class ECtor implements Expr {

    public final String id;
    public final List<? extends Expr> args;

    public ECtor(String id, List<? extends Expr> args) {
        this.id = id;
        this.args = args;
    }

    @Override
    public <R> R accept(Visitor<R> v) {
        return v.visitECtor(this);
    }

    @Override
    public Stream<EVar> fv() {
        return this.args.stream().flatMap(Expr::fv);
    }

    @Override
    public String toString() {
        if (this.args.isEmpty())
            return this.id;

        final StringBuilder sb = new StringBuilder();
        sb.append('(').append(this.id);

        for (final Expr arg : this.args)
            sb.append(' ').append(arg);
        return sb.append(')').toString();
    }
}

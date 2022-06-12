package lang.qkm.expr;

import java.util.*;
import java.util.stream.*;

public final class ELetrec implements Expr {

    public final Map<EVar, Expr> binds;
    public final Expr body;

    public ELetrec(Map<EVar, Expr> binds, Expr body) {
        this.binds = binds;
        this.body = body;
    }

    @Override
    public Stream<EVar> fv() {
        if (this.binds.isEmpty())
            return this.body.fv();

        return Stream.of(this.binds.values(), List.of(this.body))
                .flatMap(Collection::stream)
                .flatMap(Expr::fv)
                .filter(v -> !binds.containsKey(v));
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("(letrec (");

        final Iterator<Map.Entry<EVar, Expr>> it = this.binds.entrySet().iterator();
        if (it.hasNext()) {
            for (;;) {
                final Map.Entry<EVar, Expr> pair = it.next();
                sb.append('(').append(pair.getKey())
                        .append(' ')
                        .append(pair.getValue()).append(')');

                if (!it.hasNext())
                    break;

                sb.append(' ');
            }
        }

        return sb.append(") ").append(this.body).append(')').toString();
    }
}

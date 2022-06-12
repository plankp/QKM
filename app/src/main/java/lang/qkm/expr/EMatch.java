package lang.qkm.expr;

import java.util.*;
import java.util.stream.*;
import lang.qkm.match.Match;

public final class EMatch implements Expr {

    public final Expr scrutinee;
    public final List<Map.Entry<Match, Expr>> cases;

    public EMatch(Expr scrutinee, List<Map.Entry<Match, Expr>> cases) {
        this.scrutinee = scrutinee;
        this.cases = cases;
    }

    @Override
    public <R> R accept(Visitor<R> v) {
        return v.visitEMatch(this);
    }

    @Override
    public Stream<EVar> fv() {
        return Stream.concat(
                this.scrutinee.fv(),
                this.cases.stream().flatMap(EMatch::fv));
    }

    private static Stream<EVar> fv(Map.Entry<Match, Expr> p) {
        final Set<String> defs = p.getKey()
                .getCaptures()
                .collect(Collectors.toSet());

        return p.getValue().fv().filter(n -> !defs.contains(n.name));
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("(match ").append(this.scrutinee);

        for (final Map.Entry<Match, Expr> p : this.cases)
            sb.append(" (").append(p.getKey())
                    .append(' ')
                    .append(p.getValue()).append(')');

        return sb.append(')').toString();
    }
}

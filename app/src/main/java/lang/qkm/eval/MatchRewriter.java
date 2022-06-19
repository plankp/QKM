package lang.qkm.eval;

import java.util.*;
import lang.qkm.expr.*;
import lang.qkm.match.*;

public final class MatchRewriter implements ExprRewriter {

    private boolean compiling;

    @Override
    public Expr rewrite(Expr e) {
        this.compiling = true;
        e = e.accept(this);
        this.compiling = false;
        e = e.accept(this);

        return e;
    }

    @Override
    public Expr visitEMatch(EMatch e) {
        final Expr scrutinee = e.scrutinee.accept(this);
        List<Map.Entry<Match, Expr>> cases = new ArrayList<>(e.cases.size());
        for (final Map.Entry<Match, Expr> k : e.cases)
            cases.add(Map.entry(k.getKey(), k.getValue().accept(this)));

        if (this.compiling)
            return new MatchCompiler().compile(scrutinee, cases);

        // try to remove the defensive wildcard match.
        final Match m = cases.get(0).getKey();
        if (m instanceof MatchTup)
            // must be a unpack, only first case is needed
            cases = List.of(cases.get(0));
        if (m instanceof MatchBool)
            // must either be true or false, so only keep the first two cases.
            // the match compiler will always defensively insert wildcards,
            // so there are always at least two cases.
            cases = List.of(cases.get(0), cases.get(1));

        return new EMatch(scrutinee, cases);
    }
}

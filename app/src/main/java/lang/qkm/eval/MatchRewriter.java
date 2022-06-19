package lang.qkm.eval;

import java.util.*;
import lang.qkm.expr.*;
import lang.qkm.match.*;

public final class MatchRewriter implements ExprRewriter {

    @Override
    public Expr visitEMatch(EMatch e) {
        final Expr scrutinee = e.scrutinee.accept(this);
        final List<Map.Entry<Match, Expr>> cases = new ArrayList<>(e.cases.size());
        for (final Map.Entry<Match, Expr> k : e.cases)
            cases.add(Map.entry(k.getKey(), k.getValue().accept(this)));

        final MatchCompiler k = new MatchCompiler();
        return k.compile(scrutinee, cases);
    }
}

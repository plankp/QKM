package lang.qkm.eval;

import java.util.*;
import lang.qkm.expr.*;
import lang.qkm.match.*;

public final class MatchRewriter implements Evaluator, ExprRewriter {

    public final Evaluator core;

    public MatchRewriter(Evaluator core) {
        this.core = core;
    }

    @Override
    public void define(Map<EVar, Expr> defs) {
        final Map<EVar, Expr> m = new HashMap<>();
        for (final Map.Entry<EVar, Expr> pair : defs.entrySet())
            m.put(pair.getKey(), this.rewrite(pair.getValue()));

        this.core.define(m);
    }

    @Override
    public void eval(Expr e) {
        this.core.eval(this.rewrite(e));
    }

    public Expr rewrite(Expr e) {
        return e.accept(this);
    }

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

package lang.qkm.eval;

import java.util.*;
import lang.qkm.expr.*;

public final class RewriteGroup implements Evaluator {

    public final Evaluator core;
    public final List<ExprRewriter> rewrites;

    public RewriteGroup(Evaluator core, List<ExprRewriter> rewrites) {
        this.core = core;
        this.rewrites = rewrites;
    }

    @Override
    public void define(Map<EVar, Expr> defs) {
        final Map<EVar, Expr> m = new HashMap<>();
        for (final Map.Entry<EVar, Expr> pair : defs.entrySet()) {
            Expr e = pair.getValue();
            for (final ExprRewriter rewrite : this.rewrites)
                e = rewrite.rewrite(e);

            m.put(pair.getKey(), e);
        }

        this.core.define(m);
    }

    @Override
    public void eval(Expr e) {
        for (final ExprRewriter rewrite : this.rewrites)
            e = rewrite.rewrite(e);

        this.core.eval(e);
    }
}

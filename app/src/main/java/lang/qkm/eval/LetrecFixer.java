package lang.qkm.eval;

import java.util.*;
import java.util.stream.*;
import lang.qkm.expr.*;

public final class LetrecFixer implements Evaluator, ExprRewriter {

    public final Evaluator core;

    public LetrecFixer(Evaluator core) {
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
    public Expr visitELetrec(ELetrec e) {
        if (e.binds.isEmpty())
            return e.body.accept(this);

        final Deque<Map.Entry<EVar, Expr>> simple = new ArrayDeque<>();
        final Map<EVar, Expr> binds = new HashMap<>(e.binds);

        final Iterator<Map.Entry<EVar, Expr>> it = binds.entrySet().iterator();
        while (it.hasNext()) {
            final Map.Entry<EVar, Expr> pair = it.next();
            final Expr init = pair.getValue().accept(this);
            pair.setValue(init);

            if (init.fv().noneMatch(binds::containsKey)) {
                // definition is simple, hoist it out
                it.remove();
                simple.push(pair);
                continue;
            }

            if (init instanceof ELet) {
                // we have (letrec (... [k (let ((s t)) v)]) ...) and try to
                // flatten it to (letrec (... [s t] [k v]) ...)
                final ELet elet = (ELet) init;
                if (elet.value.fv().noneMatch(binds::containsKey)) {
                    simple.push(Map.entry(elet.bind, elet.value));
                    pair.setValue(elet.body);
                    continue;
                }
            }
        }

        if (simple.isEmpty())
            return new ELetrec(binds, e.body.accept(this));

        Expr acc = binds.isEmpty() ? e.body : new ELetrec(binds, e.body);
        while (!simple.isEmpty()) {
            final Map.Entry<EVar, Expr> pair = simple.pop();
            acc = new ELet(pair.getKey(), pair.getValue(), acc);
        }

        // try again in case we missed something, which is possible.
        return acc.accept(this);
    }
}

package lang.qkm.eval;

import java.util.*;
import lang.qkm.expr.*;

public final class ExprPrinter implements Evaluator {

    public final Evaluator core;

    public ExprPrinter(Evaluator core) {
        this.core = core;
    }

    @Override
    public void define(Map<EVar, Expr> defs) {
        System.out.println("define ::");
        for (final Map.Entry<EVar, Expr> pair : defs.entrySet())
            System.out.println("  " + pair.getKey() + " = " + pair.getValue());

        this.core.define(defs);
    }

    @Override
    public void eval(Expr e) {
        System.out.println("eval :: " + e);
        this.core.eval(e);
    }
}

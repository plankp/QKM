package lang.qkm.sem;

import java.util.*;
import java.util.stream.*;
import lang.qkm.QKMBaseVisitor;
import static lang.qkm.QKMParser.*;

public final class RecBindChecker extends QKMBaseVisitor<Void> {

    private enum Position {
        TAIL,
        IMMEDIATE,
        INTERMEDIATE;
    }

    private Set<String> recBinds = new HashSet<>();
    private Position position = Position.INTERMEDIATE;

    @Override
    public Void visitDefBind(DefBindContext ctx) {
        for (final BindingContext b : ctx.b)
            this.recBinds.add(b.n.getText());
        for (final BindingContext b : ctx.b) {
            this.position = Position.IMMEDIATE;
            this.visit(b.e);
        }

        return null;
    }

    @Override
    public Void visitExprLetrec(ExprLetrecContext ctx) {
        final Position oldPosition = this.position;
        final Set<String> oldRecBinds = this.recBinds;
        this.recBinds = new HashSet<>(oldRecBinds);

        try {
            for (final BindingContext b : ctx.b)
                this.recBinds.add(b.n.getText());
            for (final BindingContext b : ctx.b) {
                this.position = Position.IMMEDIATE;
                this.visit(b.e);
            }
        } finally {
            this.position = oldPosition;
            this.recBinds = oldRecBinds;
        }

        this.visit(ctx.e);
        return null;
    }

    @Override
    public Void visitExprApply(ExprApplyContext ctx) {
        if (ctx.args.isEmpty())
            return this.visit(ctx.f);

        final Position oldPosition = this.position;

        try {
            if (!(ctx.f instanceof ExprCtorContext)) {
                this.position = Position.INTERMEDIATE;
                this.visit(ctx.f);
            } else if (this.position == Position.IMMEDIATE)
                // promote the position to handle cyclic data:
                // data List a = #nil | #cons a (List a)
                // let x = #cons 1 x
                //
                // note that we don't handle `(#cons) 1 x` due to implicit
                // ctor promotion to a partially applied function.
                this.position = Position.TAIL;

            for (final Expr0Context arg : ctx.args)
                this.visit(arg);
        } finally {
            this.position = oldPosition;
        }

        return null;
    }

    @Override
    public Void visitExprIdent(ExprIdentContext ctx) {
        if (!this.recBinds.contains(ctx.n.getText()))
            return null;

        // only tail position constructs can reference recursive bindings.
        if (this.position == Position.TAIL)
            return null;

        throw new RuntimeException("Illegal use of recursive binding");
    }

    @Override
    public Void visitExprCtor(ExprCtorContext ctx) {
        return null;
    }

    @Override
    public Void visitExprTrue(ExprTrueContext ctx) {
        return null;
    }

    @Override
    public Void visitExprFalse(ExprFalseContext ctx) {
        return null;
    }

    @Override
    public Void visitExprChar(ExprCharContext ctx) {
        return null;
    }

    @Override
    public Void visitExprText(ExprTextContext ctx) {
        return null;
    }

    @Override
    public Void visitExprFunction(ExprFunctionContext ctx) {
        // let rec x = function p -> e
        // e can definitely use x.
        return null;
    }

    @Override
    public Void visitExprFun(ExprFunContext ctx) {
        // let rec x = fun k -> e
        // e can definitely use x.
        return null;
    }

    @Override
    public Void visitExprGroup(ExprGroupContext ctx) {
        if (ctx.es.size() == 1)
            return this.visit(ctx.es.get(0));

        // this is a tuple constructor, meaning it can also be cyclic (though
        // it usually fails during type checking)
        final Position oldPosition = this.position;
        if (this.position == Position.IMMEDIATE)
            this.position = Position.TAIL;

        try {
            for (final ExprContext e : ctx.es)
                this.visit(e);
        } finally {
            this.position = oldPosition;
        }

        return null;
    }

    @Override
    public Void visitExprMatch(ExprMatchContext ctx) {
        final Position oldPosition = this.position;

        try {
            this.position = Position.INTERMEDIATE;
            this.visit(ctx.v);
        } finally {
            this.position = oldPosition;
        }

        for (final MatchCaseContext k : ctx.k)
            this.visit(k);

        return null;
    }

    @Override
    public Void visitMatchCase(MatchCaseContext ctx) {
        final Set<String> old = this.recBinds;
        this.recBinds = new HashSet<>(old);

        try {
            this.visit(ctx.p);
            this.visit(ctx.e);
        } finally {
            this.recBinds = old;
        }

        return null;
    }

    @Override
    public Void visitPatIgnore(PatIgnoreContext ctx) {
        return null;
    }

    @Override
    public Void visitPatBind(PatBindContext ctx) {
        // binding gets shadowed, so whatever was there can't possibly be
        // reference anymore.
        this.recBinds.remove(ctx.n.getText());
        return null;
    }

    @Override
    public Void visitPatTrue(PatTrueContext ctx) {
        return null;
    }

    @Override
    public Void visitPatChar(PatCharContext ctx) {
        return null;
    }

    @Override
    public Void visitPatText(PatTextContext ctx) {
        return null;
    }

    @Override
    public Void visitPatGroup(PatGroupContext ctx) {
        for (final PatternContext p : ctx.ps)
            this.visit(p);

        return null;
    }
}

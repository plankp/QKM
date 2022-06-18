package lang.qkm.expr;

import java.util.*;

public abstract class ExprBaseVisitor<R> implements Expr.Visitor<R> {

    @Override
    public R visitEBool(EBool e) {
        return null;
    }

    @Override
    public R visitECtor(ECtor e) {
        for (final Expr arg : e.args)
            arg.accept(this);

        return null;
    }

    @Override
    public R visitEInt(EInt e) {
        return null;
    }

    @Override
    public R visitEString(EString e) {
        return null;
    }

    @Override
    public R visitETup(ETup e) {
        for (final Expr element : e.elements)
            element.accept(this);

        return null;
    }

    @Override
    public R visitEMatch(EMatch e) {
        for (final Map.Entry<?, Expr> pair : e.cases)
            pair.getValue().accept(this);

        return null;
    }

    @Override
    public R visitEVar(EVar e) {
        return null;
    }

    @Override
    public R visitELam(ELam e) {
        e.body.accept(this);
        return null;
    }

    @Override
    public R visitEApp(EApp e) {
        e.f.accept(this);
        e.arg.accept(this);
        return null;
    }

    @Override
    public R visitELet(ELet e) {
        e.value.accept(this);
        e.body.accept(this);
        return null;
    }

    @Override
    public R visitELetrec(ELetrec e) {
        for (final Expr init : e.binds.values())
            init.accept(this);
        e.body.accept(this);
        return null;
    }

    @Override
    public R visitEErr(EErr e) {
        e.value.accept(this);
        return null;
    }
}

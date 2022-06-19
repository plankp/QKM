package lang.qkm.eval;

import java.util.*;
import lang.qkm.expr.*;
import lang.qkm.match.*;

public interface ExprRewriter extends Expr.Visitor<Expr> {

    public default Expr rewrite(Expr e) {
        return e.accept(this);
    }

    @Override
    public default Expr visitEBool(EBool e) {
        return e;
    }

    @Override
    public default Expr visitECtor(ECtor e) {
        if (e.args.isEmpty())
            return e;

        boolean modified = false;
        final ArrayList<Expr> args = new ArrayList<>(e.args.size());
        for (final Expr arg : e.args) {
            final Expr rarg = arg.accept(this);
            modified |= arg != rarg;
            args.add(rarg);
        }

        return !modified ? e : new ECtor(e.id, args);
    }

    @Override
    public default Expr visitEInt(EInt e) {
        return e;
    }

    @Override
    public default Expr visitEString(EString e) {
        return e;
    }

    @Override
    public default Expr visitETup(ETup e) {
        if (e.elements.isEmpty())
            return e;

        boolean modified = false;
        final ArrayList<Expr> elements = new ArrayList<>(e.elements.size());
        for (final Expr element : e.elements) {
            final Expr relement = element.accept(this);
            modified |= element != relement;
            elements.add(relement);
        }

        return !modified ? e : new ETup(elements);
    }

    @Override
    public default Expr visitEMatch(EMatch e) {
        final Expr scrutinee = e.scrutinee.accept(this);

        boolean modified = e.scrutinee != scrutinee;
        final List<Map.Entry<Match, Expr>> cases = new ArrayList<>(e.cases.size());
        for (final Map.Entry<Match, Expr> k : e.cases) {
            final Expr action = k.getValue();
            final Expr raction = action.accept(this);
            modified |= action != raction;
            cases.add(Map.entry(k.getKey(), raction));
        }

        return !modified ? e : new EMatch(scrutinee, cases);
    }

    @Override
    public default Expr visitEVar(EVar e) {
        return e;
    }

    @Override
    public default Expr visitELam(ELam e) {
        final Expr body = e.body.accept(this);
        return e.body == body
                ? e
                : new ELam(e.arg, body);
    }

    @Override
    public default Expr visitEApp(EApp e) {
        final Expr f = e.f.accept(this);
        final Expr arg = e.arg.accept(this);

        return e.f == f && e.arg == arg
                ? e
                : new EApp(f, arg);
    }

    @Override
    public default Expr visitELet(ELet e) {
        final Expr value = e.value.accept(this);
        final Expr body = e.body.accept(this);

        return e.value == value && e.body == body
                ? e
                : new ELet(e.bind, value, body);
    }

    @Override
    public default Expr visitELetrec(ELetrec e) {
        boolean modified = false;
        final Map<EVar, Expr> binds = new HashMap<>();
        for (final Map.Entry<EVar, Expr> bind : e.binds.entrySet()) {
            final Expr init = bind.getValue();
            final Expr rinit = init.accept(this);
            modified |= init != rinit;
            binds.put(bind.getKey(), rinit);
        }

        final Expr body = e.body.accept(this);
        modified |= e.body != body;
        return !modified ? e : new ELetrec(binds, body);
    }

    @Override
    public default Expr visitEErr(EErr e) {
        final Expr value = e.value.accept(this);
        return e.value == value
                ? e
                : new EErr(value);
    }
}

package lang.qkm.eval;

import java.util.*;
import lang.qkm.expr.*;
import lang.qkm.match.*;

public final class MatchRewriter implements Evaluator, Expr.Visitor<Expr> {

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
System.out.println("Before~m :: " + e);
        e = e.accept(this);
System.out.println("After~m  :: " + e);
        return e;
    }

    @Override
    public Expr visitEBool(EBool e) {
        return e;
    }

    @Override
    public Expr visitECtor(ECtor e) {
        if (e.args.isEmpty())
            return e;

        final ArrayList<Expr> args = new ArrayList<>(e.args.size());
        for (final Expr arg : e.args)
            args.add(arg.accept(this));

        return new ECtor(e.id, args);
    }

    @Override
    public Expr visitEInt(EInt e) {
        return e;
    }

    @Override
    public Expr visitEString(EString e) {
        return e;
    }

    @Override
    public Expr visitETup(ETup e) {
        if (e.elements.isEmpty())
            return e;

        final ArrayList<Expr> elements = new ArrayList<>(e.elements.size());
        for (final Expr element : e.elements)
            elements.add(element.accept(this));

        return new ETup(elements);
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

    @Override
    public Expr visitEVar(EVar e) {
        return e;
    }

    @Override
    public Expr visitELam(ELam e) {
        return new ELam(e.arg, e.body.accept(this));
    }

    @Override
    public Expr visitEApp(EApp e) {
        final Expr f = e.f.accept(this);
        final Expr arg = e.arg.accept(this);

        return new EApp(f, arg);
    }

    @Override
    public Expr visitELet(ELet e) {
        return new ELet(e.bind, e.value.accept(this), e.body.accept(this));
    }

    @Override
    public Expr visitELetrec(ELetrec e) {
        final Map<EVar, Expr> binds = new HashMap<>();
        for (final Map.Entry<EVar, Expr> bind : e.binds.entrySet())
            binds.put(bind.getKey(), bind.getValue().accept(this));

        return new ELetrec(binds, e.body.accept(this));
    }

    @Override
    public Expr visitEErr(EErr e) {
        final Expr value = e.value.accept(this);
        return new EErr(value);
    }
}

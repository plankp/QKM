package lang.qkm.eval;

import java.util.*;
import lang.qkm.expr.*;
import lang.qkm.match.*;

public final class PartialEvaluator implements ExprRewriter {

    private Map<EVar, Map.Entry<Expr, Boolean>> valueTable = new HashMap<>();

    public boolean shouldPropagate(Expr e) {
        return e instanceof EVar
            || e instanceof EBool
            || e instanceof EInt
            || e instanceof EString
            || e instanceof ETup && ((ETup) e).elements.isEmpty()
            || e instanceof ECtor && ((ECtor) e).args.isEmpty();
    }

    public boolean isSimpleValue(Expr e) {
        return e.isAtom()
            || e instanceof ETup
            || e instanceof ECtor;
    }

    public boolean isTupOrCtor(Expr e) {
        return e instanceof ETup || e instanceof ECtor;
    }

    @Override
    public Expr visitELet(ELet e) {
        for (;;) {
            final Expr value = e.value.accept(this);
            final Map.Entry<Expr, Boolean> oldValue = this.valueTable.get(e.bind);
            try {
                boolean shouldInline = shouldPropagate(value);
                if (!shouldInline && isSimpleValue(value))
                    // for certain larger values, inline them anyway
                    shouldInline = !e.body.fv()
                            .filter(e.bind::equals)
                            .skip(1).findAny().isPresent();

                this.valueTable.put(e.bind, Map.entry(value, shouldInline));
                final Expr body = e.body.accept(this);

                if (body.fv().noneMatch(e.bind::equals))
                    // binding is unused, drop it
                    return body;

                if (value == e.value && body == e.body)
                    return e;

                e = new ELet(e.bind, value, body);
            } finally {
                this.valueTable.put(e.bind, oldValue);
            }
        }
    }

    @Override
    public Expr visitEVar(EVar e) {
        final Map.Entry<Expr, Boolean> pair = this.valueTable.get(e);
        return pair != null && pair.getValue() ? pair.getKey() : e;
    }

    @Override
    public Expr visitEApp(EApp e) {
        final Expr f = e.f.accept(this);
        final Expr arg = e.arg.accept(this);

        if (f instanceof ELam) {
            // (\x. e) v => let x = v in e
            final ELam lam = (ELam) f;
            return new ELet(lam.arg, arg, lam.body).accept(this);
        }

        return f == e.f && arg == e.arg
                ? e
                : new EApp(f, arg);
    }

    @Override
    public Expr visitEMatch(EMatch e) {
        final Expr expr = ExprRewriter.super.visitEMatch(e);
        if (expr instanceof EMatch) {
            // try to avoid the match completely
            final EMatch match = (EMatch) expr;
            Expr scrutinee = match.scrutinee;
            if (scrutinee instanceof EVar) {
                final Map.Entry<Expr, ?> pair = this.valueTable.get(scrutinee);
                if (pair != null && isTupOrCtor(pair.getKey()))
                    scrutinee = pair.getKey();
            }

            List<? extends Expr> inputs = List.of();
            List<Match> matches = null;
            Expr action = null;

            Object id = null;
            if (scrutinee instanceof EBool)
                id = ((EBool) scrutinee).value;
            else if (scrutinee instanceof EString)
                id = ((EString) scrutinee).value;
            else if (scrutinee instanceof EInt)
                id = ((EInt) scrutinee).value;
            else if (scrutinee instanceof ETup) {
                id = lang.qkm.type.TyTup.class;
                inputs = ((ETup) scrutinee).elements;
            } else if (scrutinee instanceof ECtor) {
                final ECtor data = (ECtor) scrutinee;
                id = data.id;
                inputs = data.args;
            }

            if (id != null) {
                for (final Map.Entry<Match, Expr> pattern : match.cases) {
                    final Match m = pattern.getKey();
                    if (m instanceof MatchAll) {
                        inputs = List.of(scrutinee);
                        matches = List.of(m);
                        action = pattern.getValue();
                        break;
                    }

                    if (id.equals(m.getCtor())) {
                        matches = m.getArgs();
                        action = pattern.getValue();
                        break;
                    }
                }
            }

            if (action != null) {
                final Iterator<? extends Expr> itInput = inputs.iterator();
                final Iterator<Match> itMatch = matches.iterator();
                while (itInput.hasNext() && itMatch.hasNext()) {
                    final Expr sub = itInput.next();
                    final String bind = ((MatchAll) itMatch.next()).capture;
                    action = new ELet(new EVar(bind), sub, action);
                }

                if (itInput.hasNext() != itMatch.hasNext())
                    throw new IllegalStateException("MALFORMED REWRITE");

                return action.accept(this);
            }
        }

        return expr;
    }
}

package lang.qkm.sem;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.*;
import lang.qkm.eval.Evaluator;
import lang.qkm.expr.*;
import lang.qkm.type.*;
import lang.qkm.util.*;
import lang.qkm.match.*;
import lang.qkm.QKMBaseVisitor;
import static lang.qkm.QKMParser.*;

public final class ExprChecker extends QKMBaseVisitor<ExprChecker.Result> {

    public static final class Result {

        public final Expr expr;
        public final Type type;

        public Result(Expr expr, Type type) {
            this.expr = expr;
            this.type = type;
        }

        @Override
        public String toString() {
            return this.expr + " :: " + this.type;
        }
    }

    private final TypeState state = new TypeState();
    private final KindChecker kindChecker = new KindChecker();
    private final Evaluator exec;

    private Map<String, Type> env = new HashMap<>();

    public ExprChecker(Evaluator exec) {
        this.exec = exec;
    }

    @Override
    public Result visitDefType(DefTypeContext ctx) {
        this.kindChecker.visit(ctx);
        return null;
    }

    @Override
    public Result visitDefData(DefDataContext ctx) {
        this.kindChecker.visit(ctx);
        return null;
    }

    @Override
    public Result visitDefBind(DefBindContext ctx) {
        // collect the free variables of the current context.
        Set<TyVar> fv = this.env.values().stream()
                .filter(k -> k != null)
                .flatMap(Type::fv)
                .collect(Collectors.toSet());

        final Map<String, Type> defs = new HashMap<>();
        for (final BindingContext b : ctx.b) {
            final String name = b.n.getText();
            if (defs.containsKey(name))
                throw new RuntimeException("Illegal duplicate binding " + name);

            // unless annotated, bindings exist as monotypes in rhs of the
            // binding declarations.
            final Type ty;
            if (b.t == null)
                ty = this.state.freshType();
            else
                ty = this.kindChecker.visit(b.t).type.eval(Map.of());
            defs.put(name, ty);
        }

        final Map<String, Type> old = this.env;
        this.env = new HashMap<>(old);
        this.env.putAll(defs);

        try {
            final Map<EVar, Expr> bindings = new HashMap<>();
            final Set<TyVar> scoped = new HashSet<>();
            for (final BindingContext b : ctx.b) {
                final String name = b.n.getText();
                final Result t = this.visit(b.e);
                bindings.put(new EVar(name), t.expr);

                Type k = this.env.get(name);
                while (k instanceof TyPoly) {
                    final TyPoly p = (TyPoly) k;
                    scoped.add(p.arg);
                    k = p.body;
                }

                k.unify(t.type);
            }

            // generalize based on the previously collected free variables.
            final Set<TyVar> monomorphic = fv = fv.stream()
                    .flatMap(Type::fv)
                    .collect(Collectors.toSet());

            for (final TyVar t : scoped)
                if (monomorphic.contains(t))
                    throw new RuntimeException("Illegal grounded type " + t + " would escape its scope");

            for (final BindingContext b : ctx.b) {
                final String name = b.n.getText();
                final Type k = this.env.get(name);
                final List<TyVar> quants = k.fv()
                        .filter(tv -> !monomorphic.contains(tv))
                        .distinct()
                        .collect(Collectors.toList());

                this.env.put(name, this.state.gen(k, quants));
            }

            // make sure the recursive binding is well-formed (example of
            // something illegal is let x = x in ...)
            new RecBindChecker().visit(ctx);
            this.exec.define(bindings);
            return null;
        } catch (Throwable ex) {
            this.env = old;
            throw ex;
        }
    }

    @Override
    public Result visitTopExpr(TopExprContext ctx) {
        final Result r = this.visit(ctx.e);
        this.exec.eval(r.expr);
        return null;
    }

    @Override
    public Result visitExprApply(ExprApplyContext ctx) {
        Result res = this.visit(ctx.f);
        for (final Expr0Context arg : ctx.args) {
            final Result k = this.visit(arg);
            final Type v = this.state.freshType();

            res.type.unify(new TyArr(k.type, v));
            res = new Result(new EApp(res.expr, k.expr), v.unwrap());
        }

        return res;
    }

    @Override
    public Result visitExprLetrec(ExprLetrecContext ctx) {
        // collect the free variables of the current context.
        Set<TyVar> fv = this.env.values().stream()
                .filter(k -> k != null)
                .flatMap(Type::fv)
                .collect(Collectors.toSet());

        final Map<String, Type> defs = new HashMap<>();
        for (final BindingContext b : ctx.b) {
            final String name = b.n.getText();
            if (defs.containsKey(name))
                throw new RuntimeException("Illegal duplicate binding " + name);;

            // unless annotated, bindings exist as monotypes in rhs of the
            // binding declarations.
            final Type ty;
            if (b.t == null)
                ty = this.state.freshType();
            else
                ty = this.kindChecker.visit(b.t).type.eval(Map.of());
            defs.put(name, ty);
        }

        final Map<String, Type> old = this.env;
        this.env = new HashMap<>(old);
        this.env.putAll(defs);

        try {
            final Map<EVar, Expr> bindings = new HashMap<>();
            final Set<TyVar> scoped = new HashSet<>();
            for (final BindingContext b : ctx.b) {
                final String name = b.n.getText();
                final Result t = this.visit(b.e);
                bindings.put(new EVar(name), t.expr);

                Type k = this.env.get(name);
                while (k instanceof TyPoly) {
                    final TyPoly p = (TyPoly) k;
                    scoped.add(p.arg);
                    k = p.body;
                }

                k.unify(t.type);
            }

            // generalize based on the previously collected free variables.
            final Set<TyVar> monomorphic = fv = fv.stream()
                    .flatMap(Type::fv)
                    .collect(Collectors.toSet());

            for (final TyVar t : scoped)
                if (monomorphic.contains(t))
                    throw new RuntimeException("Illegal grounded type " + t + " would escape its scope");

            for (final BindingContext b : ctx.b) {
                final String name = b.n.getText();
                final Type k = this.env.get(name);
                final List<TyVar> quants = k.fv()
                        .filter(tv -> !monomorphic.contains(tv))
                        .distinct()
                        .collect(Collectors.toList());

                this.env.put(name, this.state.gen(k, quants));
            }

            // make sure the recursive binding is well-formed (example of
            // something illegal is let x = x in ...)
            new RecBindChecker().visit(ctx);
            final Result r = this.visit(ctx.e);
            return new Result(new ELetrec(bindings, r.expr), r.type);
        } finally {
            this.env = old;
        }
    }

    @Override
    public Result visitExprLambda(ExprLambdaContext ctx) {
        final TyVar arg = this.state.freshType();
        final TyVar res = this.state.freshType();

        // (fun p1 -> e1 | p2 -> e2 | ...) is desugared as follows:
        // (\k. (match k (p1 e1) (p2 e2) ...))
        // where k is unique, but because shadowing rules are applied, so any
        // name that cannot exist in the source language will work
        final EVar desugared = new EVar("`1");
        final Map<String, Type> old = this.env;
        final List<SList<Match>> patterns = new LinkedList<>();
        final List<Map.Entry<Match, Expr>> cases = new ArrayList<>(ctx.k.size());
        for (final MatchCaseContext k : ctx.k) {
            final PatternChecker p = new PatternChecker(this.state, this.kindChecker);
            final Match m = p.visit(k.p);
            arg.unify(m.getType());

            if (Match.covers(patterns, SList.of(m)))
                System.out.println("Useless match pattern");
            patterns.add(SList.of(m));

            try {
                this.env = new HashMap<>(old);
                this.env.putAll(p.getBindings());

                final Result e = this.visit(k.e);
                res.unify(e.type);
                cases.add(Map.entry(m, e.expr));
            } finally {
                this.env = old;
            }
        }

        if (!Match.covers(patterns, SList.of(new MatchAll(arg))))
            System.out.println("Non-exhaustive match pattern");

        return new Result(new ELam(desugared, new EMatch(desugared, cases)),
                          new TyArr(arg, res));
    }

    @Override
    public Result visitExprMatch(ExprMatchContext ctx) {
        // match bindings can generalize
        Set<TyVar> fv = this.env.values().stream()
                .filter(k -> k != null)
                .flatMap(Type::fv)
                .collect(Collectors.toSet());

        final Result v = this.visit(ctx.v);
        final TyVar res = this.state.freshType();

        final Map<String, Type> old = this.env;
        final List<SList<Match>> patterns = new LinkedList<>();
        final List<Map.Entry<Match, Expr>> cases = new ArrayList<>(ctx.k.size());
        for (final MatchCaseContext k : ctx.k) {
            final PatternChecker p = new PatternChecker(this.state, this.kindChecker);
            final Match m = p.visit(k.p);
            v.type.unify(m.getType());

            if (Match.covers(patterns, SList.of(m)))
                System.out.println("Useless match pattern");
            patterns.add(SList.of(m));

            // rebuild the set of monomorphic type variables because the
            // pattern might have introduced new ones.
            final Set<TyVar> monomorphic = fv = fv.stream()
                    .flatMap(Type::fv)
                    .collect(Collectors.toSet());

            try {
                this.env = new HashMap<>(old);
                for (final Map.Entry<String, TyVar> pair : p.getBindings().entrySet()) {
                    final Type t = pair.getValue();
                    final List<TyVar> quants = t.fv()
                            .filter(tv -> !monomorphic.contains(tv))
                            .distinct()
                            .collect(Collectors.toList());

                    this.env.put(pair.getKey(), this.state.gen(t, quants));
                }

                final Result e = this.visit(k.e);
                res.unify(e.type);
                cases.add(Map.entry(m, e.expr));
            } finally {
                this.env = old;
            }
        }

        if (!Match.covers(patterns, SList.of(new MatchAll(v.type))))
            System.out.println("Non-exhaustive match pattern");

        return new Result(new EMatch(v.expr, cases), res);
    }

    @Override
    public Result visitExprIdent(ExprIdentContext ctx) {
        final String name = ctx.n.getText();
        final Type scheme = this.env.get(name);
        if (scheme == null)
            throw new RuntimeException("Illegal use of undeclared binding " + name);

        return new Result(new EVar(name), this.state.inst(scheme));
    }

    @Override
    public Result visitExprCtor(ExprCtorContext ctx) {
        final String ctor = ctx.k.getText();
        final Type scheme = this.kindChecker.getCtor(ctor);
        if (scheme == null)
            throw new RuntimeException("Illegal use of undeclared constructor " + ctor);

        final Type t = this.state.inst(scheme);
        if (t instanceof TyCtor)
            return new Result(new ECtor(ctor, List.of()), t);

        final List<EVar> args = new ArrayList<>();
        Type v = t;
        long id = 0;
        while (!(v instanceof TyCtor)) {
            final TyArr f = (TyArr) v;
            args.add(new EVar("`" + ++id));
            v = f.ret;
        }

        Expr f = new ECtor(ctor, args);
        for (int i = args.size(); i-- > 0; )
            f = new ELam(args.get(i), f);
        return new Result(f, t);
    }

    @Override
    public Result visitExprTrue(ExprTrueContext ctx) {
        return new Result(new EBool(true), TyBool.INSTANCE);
    }

    @Override
    public Result visitExprFalse(ExprFalseContext ctx) {
        return new Result(new EBool(false), TyBool.INSTANCE);
    }

    @Override
    public Result visitExprInt(ExprIntContext ctx) {
        String lit = ctx.getText();

        final int offs = lit.indexOf('i');
        final TyInt ty;
        if (offs < 0)
            ty = new TyInt(32);
        else if (lit.charAt(offs + 1) == '0')
            throw new RuntimeException("Illegal i0xxx");
        else {
            ty = new TyInt(Integer.parseInt(lit.substring(offs + 1)));
            lit = lit.substring(0, offs);
        }

        boolean negative = false;
        switch (lit.charAt(0)) {
        case '-':
            negative = true;
        case '+':
            lit = lit.substring(1);
        }

        int base = 10;
        if (lit.length() >= 2) {
            switch (lit.charAt(1)) {
            case 'b':
                base = 2;
                lit = lit.substring(2);
                break;
            case 'c':
                base = 8;
                lit = lit.substring(2);
                break;
            case 'x':
                base = 16;
                lit = lit.substring(2);
                break;
            }
        }

        BigInteger v = new BigInteger(lit.replace("_", ""));
        if (negative)
            v = v.negate();
        v = ty.signed(v);

        return new Result(new EInt(v, ty), ty);
    }

    @Override
    public Result visitExprChar(ExprCharContext ctx) {
        // TODO: want to make char a distinct variant type over all valid code
        // points, meaning surrogate pair values are illegal.

        final String t = ctx.getText();
        final StrEscape encoder = new StrEscape(t, 1, t.length() - 1);
        final int cp = encoder.next();

        final TyInt ty = new TyInt(32);
        return new Result(new EInt(BigInteger.valueOf(cp), ty), ty);
    }

    @Override
    public Result visitExprText(ExprTextContext ctx) {
        final String t = ctx.getText();
        if (t.length() == 2)
            return new Result(new EString(""), TyString.INSTANCE);

        final StrEscape encoder = new StrEscape(t, 1, t.length() - 1);
        final StringBuilder sb = new StringBuilder();
        while (encoder.hasNext())
            sb.appendCodePoint(encoder.next());
        return new Result(new EString(sb.toString()), TyString.INSTANCE);
    }

    @Override
    public Result visitExprGroup(ExprGroupContext ctx) {
        final int sz = ctx.es.size();
        switch (sz) {
        case 1:
            return this.visit(ctx.es.get(0));
        case 0:
            return new Result(new ETup(List.of()), new TyTup(List.of()));
        default:
            final List<Expr> values = new ArrayList<>(sz);
            final List<Type> types = new ArrayList<>(sz);
            for (final ExprContext e : ctx.es) {
                final Result r = this.visit(e);
                values.add(r.expr);
                types.add(r.type);
            }

            return new Result(new ETup(values), new TyTup(types));
        }
    }
}

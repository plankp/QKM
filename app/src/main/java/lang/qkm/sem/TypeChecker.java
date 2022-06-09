package lang.qkm.sem;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.*;
import lang.qkm.type.*;
import lang.qkm.util.*;
import lang.qkm.match.*;
import lang.qkm.QKMBaseVisitor;
import static lang.qkm.QKMParser.*;

public final class TypeChecker extends QKMBaseVisitor<Type> {

    private final TypeState state = new TypeState();
    private final KindChecker kindChecker = new KindChecker();

    private Map<String, Type> env = new HashMap<>();

    @Override
    public Type visitDefType(DefTypeContext ctx) {
        return this.kindChecker.visit(ctx).kind;
    }

    @Override
    public Type visitDefData(DefDataContext ctx) {
        return this.kindChecker.visit(ctx).kind;
    }

    @Override
    public Type visitDefBind(DefBindContext ctx) {
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
            final Set<TyVar> scoped = new HashSet<>();
            for (final BindingContext b : ctx.b) {
                final Type t = this.visit(b.e);

                Type k = this.env.get(b.n.getText());
                while (k instanceof TyPoly) {
                    final TyPoly p = (TyPoly) k;
                    scoped.add(p.arg);
                    k = p.body;
                }

                k.unify(t);
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
            return this.env.get(ctx.b.get(0).n.getText());
        } catch (Throwable ex) {
            this.env = old;
            throw ex;
        }
    }

    @Override
    public Type visitExprApply(ExprApplyContext ctx) {
        Type res = this.visit(ctx.f);
        for (final Expr0Context arg : ctx.args) {
            final Type k = this.visit(arg);
            final Type v = this.state.freshType();
            res.unify(new TyArr(k, v));
            res = v.unwrap();
        }

        return res;
    }

    @Override
    public Type visitExprLetrec(ExprLetrecContext ctx) {
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
            final Set<TyVar> scoped = new HashSet<>();
            for (final BindingContext b : ctx.b) {
                final Type t = this.visit(b.e);

                Type k = this.env.get(b.n.getText());
                while (k instanceof TyPoly) {
                    final TyPoly p = (TyPoly) k;
                    scoped.add(p.arg);
                    k = p.body;
                }

                k.unify(t);
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
            return this.visit(ctx.e);
        } finally {
            this.env = old;
        }
    }

    @Override
    public Type visitExprLambda(ExprLambdaContext ctx) {
        final TyVar arg = this.state.freshType();
        final TyVar res = this.state.freshType();

        final Map<String, Type> old = this.env;
        final List<SList<Match>> patterns = new LinkedList<>();
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

                res.unify(this.visit(k.e));
            } finally {
                this.env = old;
            }
        }

        if (!Match.covers(patterns, SList.of(new MatchComplete(arg))))
            System.out.println("Non-exhaustive match pattern");

        return new TyArr(arg, res);
    }

    @Override
    public Type visitExprMatch(ExprMatchContext ctx) {
        // match bindings can generalize
        Set<TyVar> fv = this.env.values().stream()
                .filter(k -> k != null)
                .flatMap(Type::fv)
                .collect(Collectors.toSet());

        final Type v = this.visit(ctx.v);
        final TyVar res = this.state.freshType();

        final Map<String, Type> old = this.env;
        final List<SList<Match>> patterns = new LinkedList<>();
        for (final MatchCaseContext k : ctx.k) {
            final PatternChecker p = new PatternChecker(this.state, this.kindChecker);
            final Match m = p.visit(k.p);
            v.unify(m.getType());

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

                res.unify(this.visit(k.e));
            } finally {
                this.env = old;
            }
        }

        if (!Match.covers(patterns, SList.of(new MatchComplete(v))))
            System.out.println("Non-exhaustive match pattern");

        return res;
    }

    @Override
    public Type visitExprIdent(ExprIdentContext ctx) {
        final String name = ctx.n.getText();
        final Type scheme = this.env.get(name);
        if (scheme == null)
            throw new RuntimeException("Illegal use of undeclared binding " + name);

        return this.state.inst(scheme);
    }

    @Override
    public Type visitExprCtor(ExprCtorContext ctx) {
        final String ctor = ctx.k.getText();
        final Type scheme = this.kindChecker.getCtor(ctor);
        if (scheme == null)
            throw new RuntimeException("Illegal use of undeclared constructor " + ctor);

        return this.state.inst(scheme);
    }

    @Override
    public Type visitExprTrue(ExprTrueContext ctx) {
        return TyBool.INSTANCE;
    }

    @Override
    public Type visitExprFalse(ExprFalseContext ctx) {
        return TyBool.INSTANCE;
    }

    @Override
    public Type visitExprInt(ExprIntContext ctx) {
        final String n = ctx.getText();
        final int k = n.indexOf('i');
        if (k < 0)
            return new TyInt(32);
        final String v = n.substring(k + 1);
        if (v.charAt(0) == '0')
            throw new RuntimeException("Illegal i0xxx");
        return new TyInt(Integer.parseInt(v));
    }

    @Override
    public Type visitExprChar(ExprCharContext ctx) {
        // TODO: want to make char a distinct variant type over all valid code
        // points, meaning surrogate pair values are illegal.
        return new TyInt(32);
    }

    @Override
    public Type visitExprText(ExprTextContext ctx) {
        return TyString.INSTANCE;
    }

    @Override
    public Type visitExprGroup(ExprGroupContext ctx) {
        final int sz = ctx.es.size();
        switch (sz) {
        case 1:
            return this.visit(ctx.es.get(0));
        case 0:
            return new TyTup(List.of());
        default:
            final List<Type> elements = new ArrayList<>(sz);
            for (final ExprContext e : ctx.es)
                elements.add(this.visit(e));
            return new TyTup(elements);
        }
    }
}

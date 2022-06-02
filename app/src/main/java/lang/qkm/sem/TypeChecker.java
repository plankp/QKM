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

    private final Map<String, PolyType> env = new HashMap<>();
    private final TypeState state = new TypeState();
    private final KindChecker kindChecker = new KindChecker(this.state);

    private VarType freshType() {
        return this.state.freshType();
    }

    @Override
    public Type visitDefType(DefTypeContext ctx) {
        return this.kindChecker.visit(ctx).body;
    }

    @Override
    public Type visitDefData(DefDataContext ctx) {
        return this.kindChecker.visit(ctx).body;
    }

    @Override
    public Type visitDefBind(DefBindContext ctx) {
        // collect the free variables of the current context.
        Set<VarType> fv = this.env.values().stream()
                .filter(k -> k != null)
                .flatMap(PolyType::fv)
                .collect(Collectors.toSet());

        final Map<String, PolyType> old = new HashMap<>();
        try {
            // bindings exist as monotypes in rhs of the binding declarations.
            for (final BindingContext b : ctx.b) {
                final String name = b.n.getText();
                if (old.containsKey(name))
                    throw new RuntimeException("Illegal duplicate binding " + name);;

                final PolyType ty = new PolyType(List.of(), this.freshType());
                old.put(name, this.env.put(name, ty));
            }

            for (final BindingContext b : ctx.b) {
                final Type k = this.env.get(b.n.getText()).body;
                final Type t = this.visit(b.e);
                k.unify(t);
            }

            // generalize based on the previously collected free variables.
            fv = fv.stream().flatMap(Type::fv).collect(Collectors.toSet());
            final Set<VarType> outer = fv;  // explicit final for Java!
            for (final BindingContext b : ctx.b) {
                final String name = b.n.getText();
                final Type k = this.env.get(name).body;
                final List<VarType> quants = k.fv()
                        .filter(tv -> !outer.contains(tv))
                        .distinct()
                        .collect(Collectors.toList());

                this.env.put(name, new PolyType(quants, k));
            }

            // make sure the recursive binding is well-formed (example of
            // something illegal is let x = x in ...)
            new RecBindChecker().visit(ctx);
            return this.env.get(ctx.b.get(0).n.getText()).body;
        } catch (Throwable ex) {
            // Only restore the environment when something goes wrong
            this.env.putAll(old);
            throw ex;
        }
    }

    @Override
    public Type visitExprApply(ExprApplyContext ctx) {
        Type res = this.visit(ctx.f);
        for (final Expr0Context arg : ctx.args) {
            final Type k = this.visit(arg);
            final Type v = this.freshType();
            res.unify(new FuncType(k, v));
            res = v.get();
        }

        return res;
    }

    @Override
    public Type visitExprLetrec(ExprLetrecContext ctx) {
        // collect the free variables of the current context.
        Set<VarType> fv = this.env.values().stream()
                .filter(k -> k != null)
                .flatMap(PolyType::fv)
                .collect(Collectors.toSet());

        final Map<String, PolyType> old = new HashMap<>();
        try {
            // bindings exist as monotypes in rhs of the binding declarations.
            for (final BindingContext b : ctx.b) {
                final String name = b.n.getText();
                if (old.containsKey(name))
                    throw new RuntimeException("Illegal duplicate binding " + name);;

                final PolyType ty = new PolyType(List.of(), this.freshType());
                old.put(name, this.env.put(name, ty));
            }

            for (final BindingContext b : ctx.b) {
                final Type k = this.env.get(b.n.getText()).body;
                final Type t = this.visit(b.e);
                k.unify(t);
            }

            // generalize based on the previously collected free variables.
            fv = fv.stream().flatMap(Type::fv).collect(Collectors.toSet());
            final Set<VarType> outer = fv;  // explicit final for Java!
            for (final BindingContext b : ctx.b) {
                final String name = b.n.getText();
                final Type k = this.env.get(name).body;
                final List<VarType> quants = k.fv()
                        .filter(tv -> !outer.contains(tv))
                        .distinct()
                        .collect(Collectors.toList());

                this.env.put(name, new PolyType(quants, k));
            }

            // make sure the recursive binding is well-formed (example of
            // something illegal is let x = x in ...)
            new RecBindChecker().visit(ctx);
            return this.visit(ctx.e);
        } finally {
            this.env.putAll(old);
        }
    }

    @Override
    public Type visitExprLambda(ExprLambdaContext ctx) {
        final VarType arg = this.freshType();
        final VarType res = this.freshType();

        final Map<String, PolyType> old = new HashMap<>(this.env);
        final List<SList<Match>> patterns = new LinkedList<>();
        for (final MatchCaseContext k : ctx.k) {
            try {
                final PatternChecker p = new PatternChecker(this.state, this.kindChecker);
                final Match m = p.visit(k.p);
                arg.unify(m.getType());

                if (Match.covers(patterns, SList.of(m)))
                    System.out.println("Useless match pattern");
                patterns.add(SList.of(m));

                for (final Map.Entry<String, VarType> pair : p.getBindings().entrySet())
                    this.env.put(pair.getKey(), new PolyType(List.of(), pair.getValue()));

                res.unify(this.visit(k.e));
            } finally {
                this.env.clear();
                this.env.putAll(old);
            }
        }

        if (!Match.covers(patterns, SList.of(new MatchComplete(arg))))
            System.out.println("Non-exhaustive match pattern");

        return new FuncType(arg, res);
    }

    @Override
    public Type visitExprMatch(ExprMatchContext ctx) {
        // match bindings can generalize
        Set<VarType> fv = this.env.values().stream()
                .filter(k -> k != null)
                .flatMap(PolyType::fv)
                .collect(Collectors.toSet());

        final Type v = this.visit(ctx.v);
        final VarType res = this.freshType();

        final Map<String, PolyType> old = new HashMap<>(this.env);
        final List<SList<Match>> patterns = new LinkedList<>();
        for (final MatchCaseContext k : ctx.k) {
            try {
                final PatternChecker p = new PatternChecker(this.state, this.kindChecker);
                final Match m = p.visit(k.p);
                v.unify(m.getType());

                if (Match.covers(patterns, SList.of(m)))
                    System.out.println("Useless match pattern");
                patterns.add(SList.of(m));

                fv = fv.stream().flatMap(Type::fv).collect(Collectors.toSet());
                final Set<VarType> outer = fv;
                for (final Map.Entry<String, VarType> pair : p.getBindings().entrySet()) {
                    final Type t = pair.getValue();
                    final List<VarType> quants = t.fv()
                            .filter(tv -> !outer.contains(tv))
                            .distinct()
                            .collect(Collectors.toList());

                    this.env.put(pair.getKey(), new PolyType(quants, t));
                }

                res.unify(this.visit(k.e));
            } finally {
                this.env.clear();
                this.env.putAll(old);
            }
        }

        if (!Match.covers(patterns, SList.of(new MatchComplete(v))))
            System.out.println("Non-exhaustive match pattern");

        return res;
    }

    @Override
    public Type visitExprIdent(ExprIdentContext ctx) {
        final String name = ctx.n.getText();
        final PolyType scheme = this.env.get(name);
        if (scheme == null)
            throw new RuntimeException("Illegal use of undeclared binding " + name);

        return this.state.inst(scheme);
    }

    @Override
    public Type visitExprCtor(ExprCtorContext ctx) {
        final String ctor = ctx.k.getText();
        final PolyType scheme = this.kindChecker.getCtor(ctor);
        if (scheme == null)
            throw new RuntimeException("Illegal use of undeclared constructor " + ctor);

        return this.state.inst(scheme);
    }

    @Override
    public Type visitExprTrue(ExprTrueContext ctx) {
        return BoolType.INSTANCE;
    }

    @Override
    public Type visitExprFalse(ExprFalseContext ctx) {
        return BoolType.INSTANCE;
    }

    @Override
    public Type visitExprInt(ExprIntContext ctx) {
        final String n = ctx.getText();
        final int k = n.indexOf('i');
        if (k < 0)
            return new IntType(32);
        final String v = n.substring(k + 1);
        if (v.charAt(0) == '0')
            throw new RuntimeException("Illegal i0xxx");
        return new IntType(Integer.parseInt(v));
    }

    @Override
    public Type visitExprChar(ExprCharContext ctx) {
        return new IntType(32);
    }

    @Override
    public Type visitExprText(ExprTextContext ctx) {
        return StringType.INSTANCE;
    }

    @Override
    public Type visitExprGroup(ExprGroupContext ctx) {
        final int sz = ctx.es.size();
        switch (sz) {
        case 0:
            return new TupleType(List.of());
        case 1:
            return this.visit(ctx.es.get(0));
        default:
            final List<Type> elements = new ArrayList<>(sz);
            for (final ExprContext e : ctx.es)
                elements.add(this.visit(e));
            return new TupleType(elements);
        }
    }
}

package lang.qkm.sem;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.*;
import org.antlr.v4.runtime.Token;
import lang.qkm.type.*;
import lang.qkm.QKMBaseVisitor;
import static lang.qkm.QKMParser.*;

public final class KindChecker extends QKMBaseVisitor<PolyType> {

    private final Map<String, PolyType> env = new HashMap<>();
    private final Map<String, PolyType> ctors = new HashMap<>();
    private final TypeState state;

    public KindChecker(TypeState state) {
        this.state = state;
    }

    public PolyType getCtor(String name) {
        return this.ctors.get(name);
    }

    public Map<String, PolyType> getCtors() {
        return Collections.unmodifiableMap(this.ctors);
    }

    @Override
    public PolyType visitDefType(DefTypeContext ctx) {
        final String name = ctx.n.getText();
        final Map<String, VarType> quants = new LinkedHashMap<>();
        for (final Token q : ctx.qs) {
            final String quant = q.getText();
            if (quants.put(quant, this.state.freshType(quant)) != null)
                throw new RuntimeException("Illegal duplicate quantifier name " + quant);
        }

        final Map<String, PolyType> old = new HashMap<>(this.env);
        final VarType u = this.state.freshType();
        final List<VarType> qs = new ArrayList<>(quants.values());
        try {
            this.env.put(name, new PolyType(qs, u));
            for (final Map.Entry<String, VarType> pair : quants.entrySet())
                this.env.put(pair.getKey(), new PolyType(List.of(), pair.getValue()));

            final PolyType t = this.visit(ctx.t);
            if (!t.quants.isEmpty())
                throw new RuntimeException("Unsupported partial application on types");

            u.unify(t.body);
            if (!u.hasRef())
                // only possibility is if we had type a = a which is illegal
                throw new RuntimeException("Illegal recursive type");
        } finally {
            this.env.clear();
            this.env.putAll(old);
        }

        final PolyType res = new PolyType(qs, u.expand());
        this.env.put(name, res);
        return res;
    }

    @Override
    public PolyType visitDefData(DefDataContext ctx) {
        final Map<String, PolyType> defs = new HashMap<>();
        for (final EnumDefContext d : ctx.d) {
            final String name = d.n.getText();
            final Map<String, VarType> quants = new LinkedHashMap<>();
            for (final Token q : d.qs) {
                final String quant = q.getText();
                if (quants.put(quant, this.state.freshType(quant)) != null)
                    throw new RuntimeException("Illegal duplicate quantifier name " + quant);
            }

            final List<VarType> qs = new ArrayList<>(quants.values());
            final EnumType.Template fields = new EnumType.Template(name, qs, new LinkedHashMap<>());
            if (defs.put(name, new PolyType(qs, new EnumType(fields, qs))) != null)
                throw new RuntimeException("Illegal duplicate data name " + name);
        }

        final Map<String, PolyType> old = new HashMap<>(this.env);
        for (final EnumDefContext d : ctx.d) {
            try {
                final EnumType.Template fields = ((EnumType) defs.get(d.n.getText()).body).template;
                this.env.putAll(defs);
                for (final VarType quant : fields.quants)
                    this.env.put(quant.name, new PolyType(List.of(), quant));

                for (final EnumCaseContext k : d.k) {
                    final String ctor = k.k.getText();
                    final List<Type> args;
                    if (k.args.isEmpty())
                        args = List.of();
                    else {
                        args = new ArrayList<>(k.args.size());
                        for (final Type0Context arg : k.args) {
                            final PolyType p = this.visit(arg);
                            if (!p.quants.isEmpty())
                                throw new RuntimeException("Illegal use of polymorphic type");
                            args.add(p.body);
                        }
                    }

                    if (fields.cases.put(ctor, args) != null)
                        throw new RuntimeException("Illegal duplicate constructor name " + ctor);

                    // data K q1 q2 ... = #ctor t1 t2 ... tN
                    // forall q1 q2 ... t1 -> t2 -> ... -> tN -> K q1 q2 ...
                    Type base = new EnumType(fields, fields.quants);
                    for (int i = args.size(); i-- > 0; )
                        base = new FuncType(args.get(i), base);
                    this.ctors.put(ctor, new PolyType(fields.quants, base));
                }
            } finally {
                this.env.clear();
                this.env.putAll(old);
            }
        }

        this.env.putAll(defs);
        return defs.get(ctx.d.get(0).n.getText());
    }

    @Override
    public PolyType visitTypeApply(TypeApplyContext ctx) {
        PolyType res = this.visit(ctx.f);
        for (final Type0Context arg : ctx.args) {
            final PolyType k = this.visit(arg);
            if (!k.quants.isEmpty())
                throw new RuntimeException("Illegal use of polymorphic type");
            if (res.quants.isEmpty())
                throw new RuntimeException("Illegal use of monomorphic type");

            // (forall a b c. T) k --> forall b c. (T[a := k])
            final VarType hd = res.quants.get(0);
            final List<VarType> tl = res.quants.subList(1, res.quants.size());
            res = new PolyType(tl, res.body.replace(Map.of(hd, k.body)));
        }

        return res;
    }

    @Override
    public PolyType visitTypeFunc(TypeFuncContext ctx) {
        final PolyType p = this.visit(ctx.p);
        if (!p.quants.isEmpty())
            throw new RuntimeException("Illegal use of polymorphic type");

        final PolyType q = this.visit(ctx.q);
        if (!q.quants.isEmpty())
            throw new RuntimeException("Illegal use of polymorphic type");

        return new PolyType(List.of(), new FuncType(p.body, q.body));
    }

    @Override
    public PolyType visitTypeName(TypeNameContext ctx) {
        final String name = ctx.n.getText();
        final PolyType ty = this.env.get(name);
        if (ty != null)
            return ty;

        switch (name) {
        case "bool":
            return new PolyType(List.of(), BoolType.INSTANCE);
        case "string":
            return new PolyType(List.of(), StringType.INSTANCE);
        case "i8":
            return new PolyType(List.of(), new IntType(8));
        case "i16":
            return new PolyType(List.of(), new IntType(16));
        case "i32":
            return new PolyType(List.of(), new IntType(32));
        case "i64":
            return new PolyType(List.of(), new IntType(64));
        default:
            // TODO: handle other iN types
            throw new RuntimeException("Illegal use of undeclared binding " + name);
        }
    }

    @Override
    public PolyType visitTypeGroup(TypeGroupContext ctx) {
        final int sz = ctx.ts.size();
        switch (sz) {
        case 0:
            return new PolyType(List.of(), new TupleType(List.of()));
        case 1:
            return this.visit(ctx.ts.get(0));
        default:
            final List<Type> elements = new ArrayList<>(sz);
            for (final TypeContext t : ctx.ts) {
                final PolyType p = this.visit(t);
                if (!p.quants.isEmpty())
                    throw new RuntimeException("Illegal use of polymorphic type");

                elements.add(p.body);
            }
            return new PolyType(List.of(), new TupleType(elements));
        }
    }
}

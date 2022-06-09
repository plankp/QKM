package lang.qkm.sem;

import java.util.*;
import java.util.stream.*;
import org.antlr.v4.runtime.Token;
import lang.qkm.type.*;
import lang.qkm.QKMBaseVisitor;
import static lang.qkm.QKMParser.*;

public final class KindChecker extends QKMBaseVisitor<KindChecker.Result> {

    public static final class Result {

        public final Type type;
        public final Type kind;

        public Result(Type type, Type kind) {
            this.type = type;
            this.kind = kind;
        }

        @Override
        public String toString() {
            return this.type + " :: " + this.kind;
        }
    }

    private Map<String, Result> scope = new HashMap<>();
    private Map<String, Type> ctors = new HashMap<>();

    public Type getCtor(String name) {
        return this.ctors.get(name);
    }

    public Map<String, Type> getCtors() {
        return Collections.unmodifiableMap(this.ctors);
    }

    @Override
    public Result visitDefType(DefTypeContext ctx) {
        // type alias construct is not recursive!
        final String name = ctx.n.getText();
        Result type = this.visit(ctx.t);

        final Iterator<TyVar> it = type.kind.fv().distinct().iterator();
        while (it.hasNext())
            it.next().unify(TyKind.VALUE);

        type = new Result(type.type.eval(Map.of()), type.kind);
        this.scope.put(name, type);
        return type;
    }

    @Override
    public Result visitTypePoly(TypePolyContext ctx) {
        if (ctx.qs.isEmpty())
            return this.visit(ctx.t);

        final Map<String, Result> old = this.scope;
        this.scope = new HashMap<>(old);

        try {
            final LinkedList<Result> args = new LinkedList<>();
            for (final Token q : ctx.qs) {
                final String name = q.getText();
                final Result info = new Result(TyVar.grounded(name),
                                               TyVar.unifiable(name));

                args.push(info);
                this.scope.put(name, info);
            }

            Result t = this.visit(ctx.t);
            while (!args.isEmpty()) {
                final Result arg = args.pop();
                t = new Result(new TyPoly((TyVar) arg.type, t.type),
                               new TyArr(arg.kind, t.kind));
            }
            return t;
        } finally {
            this.scope = old;
        }
    }

    @Override
    public Result visitTypeApply(TypeApplyContext ctx) {
        Result res = this.visit(ctx.f);
        for (final Type0Context arg : ctx.args) {
            final Result k = this.visit(arg);
            final Type v = TyVar.unifiable("_");

            res.kind.unify(new TyArr(k.kind, v));
            res = new Result(new TyApp(res.type, k.type), v.unwrap());
        }

        return res;
    }

    @Override
    public Result visitTypeFunc(TypeFuncContext ctx) {
        final Result p = this.visit(ctx.p);
        p.kind.unify(TyKind.VALUE);
        final Result q = this.visit(ctx.q);
        q.kind.unify(TyKind.VALUE);

        return new Result(new TyArr(p.type, q.type), TyKind.VALUE);
    }

    @Override
    public Result visitTypeName(TypeNameContext ctx) {
        final String name = ctx.n.getText();
        final Result type = this.scope.get(name);
        if (type != null)
            return type;

        switch (name) {
        case "string":
            return new Result(TyString.INSTANCE, TyKind.VALUE);
        case "bool":
            return new Result(TyBool.INSTANCE, TyKind.VALUE);
        case "i8":
            return new Result(new TyInt(8), TyKind.VALUE);
        case "i16":
            return new Result(new TyInt(16), TyKind.VALUE);
        case "i32":
            return new Result(new TyInt(32), TyKind.VALUE);
        case "i64":
            return new Result(new TyInt(64), TyKind.VALUE);
        default:
            // TODO: handle other iN types
            throw new RuntimeException("Illegal use of undeclared type " + name);
        }
    }

    @Override
    public Result visitTypeGroup(TypeGroupContext ctx) {
        final int sz = ctx.ts.size();
        switch (sz) {
        case 1:
            // (t) is the same as t.
            return this.visit(ctx.ts.get(0));
        case 0:
            // () :: *
            return new Result(new TyTup(List.of()), TyKind.VALUE);
        default:
            // (t1, ..., tN) :: * where t1, ..., tN :: *
            final List<Type> elements = new ArrayList<>(sz);
            for (final TypeContext t : ctx.ts) {
                final Result r = this.visit(t);
                r.kind.unify(TyKind.VALUE);
                elements.add(r.type);
            }

            final TyTup cons = new TyTup(Collections.unmodifiableList(elements));
            return new Result(cons, TyKind.VALUE);
        }
    }

    @Override
    public Result visitDefData(DefDataContext ctx) {
        final Map<String, Map.Entry<Type, Map<String, Result>>> defs = new HashMap<>();
        for (final EnumDefContext d : ctx.d) {
            final String name = d.n.getText();
            if (defs.containsKey(name))
                throw new RuntimeException("Illegal duplicate data name " + name);

            final LinkedList<TyVar> kargs = new LinkedList<>();
            final Map<String, Result> quants = new LinkedHashMap<>();
            for (final Token q : d.qs) {
                final String quant = q.getText();
                final Result info = new Result(TyVar.grounded(quant),
                                               TyVar.unifiable(quant));
                kargs.push((TyVar) info.kind);
                if (quants.put(quant, info) != null)
                    throw new RuntimeException("Illegal duplicate quantifier name " + quant);
            }

            Type t = TyKind.VALUE;
            while (!kargs.isEmpty())
                t = new TyArr(kargs.pop(), t);
            defs.put(name, Map.entry(t, quants));
        }

        final Map<String, Result> augmented = new HashMap<>(this.scope);
        for (final EnumDefContext d : ctx.d) {
            final String name = d.n.getText();
            final Map.Entry<Type, Map<String, Result>> info = defs.get(name);
            final List<TyVar> ctorQuants = info.getValue()
                    .values()
                    .stream()
                    .map(r -> (TyVar) r.type)
                    .collect(Collectors.toList());

            final TyCtor.Template fields = new TyCtor.Template(name, ctorQuants, new LinkedHashMap<>());
            Type t = new TyCtor(fields, ctorQuants);
            for (int i = ctorQuants.size(); i-- > 0; )
                t = new TyPoly(ctorQuants.get(i), t);

            augmented.put(name, new Result(t, info.getKey()));
        }

        final Map<String, Result> oldScope = this.scope;
        final Map<String, Type> oldCtors = this.ctors;
        this.ctors = new HashMap<>(oldCtors);

        try {
            for (final EnumDefContext d : ctx.d) {
                this.scope = new HashMap<>(augmented);

                final String name = d.n.getText();
                this.scope.putAll(defs.get(name).getValue());

                final List<TyVar> ctorQuants = new ArrayList<>();
                Type info = augmented.get(name).type;
                while (info instanceof TyPoly) {
                    final TyPoly p = (TyPoly) info;
                    ctorQuants.add(p.arg);
                    info = p.body;
                }

                final TyCtor.Template fields = ((TyCtor) info).template;
                for (final EnumCaseContext k : d.k) {
                    final String ctor = k.k.getText();
                    final List<Type> args;
                    if (k.args.isEmpty())
                        args = List.of();
                    else {
                        args = new ArrayList<>(k.args.size());
                        for (final Type0Context arg : k.args) {
                            final Result v = this.visit(arg);
                            v.kind.unify(TyKind.VALUE);
                            args.add(v.type.eval(Map.of()));
                        }
                    }

                    if (fields.cases.put(ctor, args) != null)
                        throw new RuntimeException("Illegal duplicate constructor name " + ctor);

                    Type t = new TyCtor(fields, ctorQuants);
                    for (int i = args.size(); i-- > 0; )
                        t = new TyArr(args.get(i), t);
                    for (int i = ctorQuants.size(); i-- > 0; )
                        t = new TyPoly(ctorQuants.get(i), t);
                    this.ctors.put(ctor, t);
                }
            }
        } catch (Throwable ex) {
            this.scope = oldScope;
            this.ctors = oldCtors;
            throw ex;
        }

        final Iterator<TyVar> it = defs.values().stream()
                .map(Map.Entry::getKey)
                .flatMap(Type::fv)
                .distinct()
                .iterator();
        while (it.hasNext())
            it.next().unify(TyKind.VALUE);

        this.scope = augmented;
        return this.scope.get(ctx.d.get(0).n.getText());
    }
}

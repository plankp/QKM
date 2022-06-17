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

    private Map<String, Result> typeCtors = new HashMap<>();
    private Map<String, Type> dataCtors = new HashMap<>();

    private boolean allowFreeTypes = true;
    private Map<String, Result> env = new HashMap<>();
    private Map<TyVar, Type> kinds = new HashMap<>();

    public KindChecker newScope() {
        final KindChecker k = new KindChecker();

        k.typeCtors.putAll(this.typeCtors);
        k.dataCtors.putAll(this.dataCtors);
        k.allowFreeTypes = this.allowFreeTypes;
        k.env.putAll(this.env);
        k.kinds.putAll(this.kinds);
        return k;
    }

    public Type getCtor(String name) {
        return this.dataCtors.get(name);
    }

    public Map<String, Type> getCtors() {
        return Collections.unmodifiableMap(this.dataCtors);
    }

    @Override
    public Result visitTypeApply(TypeApplyContext ctx) {
        Result res = this.visit(ctx.f);
        for (final Type0Context arg : ctx.args) {
            final Result k = this.visit(arg);
            final Type v = TyVar.unifiable("?");

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

    private Result newGroundedType(String name) {
        final TyVar t = TyVar.grounded(name);
        final Type kind = TyVar.unifiable("?");
        this.kinds.put(t, kind);
        return new Result(t, kind);
    }

    @Override
    public Result visitTypeIgnore(TypeIgnoreContext ctx) {
        if (!this.allowFreeTypes)
            throw new RuntimeException("Wildcard type not allowed here");

        return this.newGroundedType("_");
    }

    @Override
    public Result visitTypeName(TypeNameContext ctx) {
        return this.env.computeIfAbsent(ctx.n.getText(), name -> {
            if (!this.allowFreeTypes)
                throw new RuntimeException("Free type " + name + " not allowed here");
            return this.newGroundedType(name);
        });
    }

    @Override
    public Result visitTypeCtor(TypeCtorContext ctx) {
        final String name = ctx.n.getText();
        final Result type = this.typeCtors.get(name);
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
    public Result visitTypePoly(TypePolyContext ctx) {
        // keep track of a set of rigid types that should be monomorphic
        //
        // since we only create grounded type variables (never unifiable),
        // it's impossible to have types escaping their scope.
        final Set<TyVar> monomorphic = this.env.values().stream()
                .filter(k -> k != null)
                .flatMap(k -> k.type.fv())
                .collect(Collectors.toSet());

        // the quantifiers are just for scoping purposes, meaning
        // a b. a -> a has kind * -> * instead of * -> * -> *
        for (final Token q : ctx.qs) {
            final String name = q.getText();
            this.env.put(name, this.newGroundedType(name));
        }

        // generalize the type expression
        final Result r = this.visit(ctx.t);
        final Deque<TyVar> quants = r.type.fv()
                .filter(tv -> !monomorphic.contains(tv))
                .distinct()
                .collect(Collectors.toCollection(ArrayDeque::new));

        Type t = r.type.unwrap();
        Type kind = r.kind.unwrap();
        final Iterator<TyVar> it = quants.descendingIterator();
        while (it.hasNext()) {
            final TyVar q = it.next();
            t = new TyPoly(q, t);
            kind = new TyArr(this.kinds.get(q).unwrap(), kind);
        }

        // all unconstrainted kinds are set to *.
        final Iterator<TyVar> unconstrainted = kind.fv().distinct().iterator();
        while (unconstrainted.hasNext())
            unconstrainted.next().unify(TyKind.VALUE);

        return new Result(t, kind);
    }

    @Override
    public Result visitDefType(DefTypeContext ctx) {
        final Map<String, Result> old = this.env;
        final boolean allowed = this.allowFreeTypes;
        try {
            this.env = new LinkedHashMap<>();

            // Quantifiers here *do* contribute to the kind sigature, meaning
            // type Foo a b = a has kind * -> * -> *.
            for (Token q : ctx.qs) {
                final String name = q.getText();
                if (this.env.containsKey(name))
                    throw new RuntimeException("Illegal duplicate quantifier " + name);

                this.env.put(name, this.newGroundedType(name));
            }

            // type aliases are not recursive!
            this.allowFreeTypes = false;
            final Result r = this.visit(ctx.t);

            Type t = r.type;
            Type kind = r.kind;
            final Iterator<Result> quants = new ArrayDeque<>(this.env.values()).descendingIterator();
            while (quants.hasNext()) {
                final Result q = quants.next();
                t = new TyPoly((TyVar) q.type, t);
                kind = new TyArr(q.kind, kind);
            }

            // all unconstrainted kinds are set to *.
            final Iterator<TyVar> unconstrainted = kind.fv().distinct().iterator();
            while (unconstrainted.hasNext())
                unconstrainted.next().unify(TyKind.VALUE);

            final Result result = new Result(t.eval(Map.of()), kind);
            this.typeCtors.put(ctx.n.getText(), result);
            return result;
        } finally {
            this.env = old;
            this.allowFreeTypes = allowed;
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
                final Result info = this.newGroundedType(quant);
                kargs.push((TyVar) info.kind);
                if (quants.put(quant, info) != null)
                    throw new RuntimeException("Illegal duplicate quantifier name " + quant);
            }

            Type t = TyKind.VALUE;
            while (!kargs.isEmpty())
                t = new TyArr(kargs.pop(), t);
            defs.put(name, Map.entry(t, quants));
        }

        final Map<String, Result> augmented = new HashMap<>(this.typeCtors);
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

        final Map<String, Result> oldTypeCtors = this.typeCtors;
        final Map<String, Type> oldDataCtors = this.dataCtors;
        final Map<String, Result> oldEnv = this.env;
        final boolean oldAllowFreeTypes = this.allowFreeTypes;
        this.dataCtors = new HashMap<>(oldDataCtors);

        try {
            this.typeCtors = augmented;
            this.allowFreeTypes = false;

            for (final EnumDefContext d : ctx.d) {
                final String name = d.n.getText();
                this.env = new HashMap<>(defs.get(name).getValue());

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
                    this.dataCtors.put(ctor, t);
                }
            }
        } catch (Throwable ex) {
            this.typeCtors = oldTypeCtors;
            this.dataCtors = oldDataCtors;
            throw ex;
        } finally {
            this.allowFreeTypes = oldAllowFreeTypes;
            this.env = oldEnv;
        }

        final Iterator<TyVar> it = defs.values().stream()
                .map(Map.Entry::getKey)
                .flatMap(Type::fv)
                .distinct()
                .iterator();
        while (it.hasNext())
            it.next().unify(TyKind.VALUE);

        return this.typeCtors.get(ctx.d.get(0).n.getText());
    }
}

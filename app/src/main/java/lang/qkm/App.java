package lang.qkm;

import java.util.*;
import java.util.stream.*;
import java.math.BigInteger;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import lang.qkm.sem.*;
import lang.qkm.util.*;
import lang.qkm.type.*;
import lang.qkm.match.*;
import static lang.qkm.QKMParser.*;

public class App extends QKMBaseVisitor<Object> {

    public static void main(String[] args) {
        final App state = new App();
        try (final BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
            StringBuilder buffer = new StringBuilder();
            String prompt = ">> ";
            String line = "";
            while (line != null) {
                System.out.print(prompt);

                prompt = "\\> ";
                line = br.readLine();
                if (line != null && !line.isEmpty()) {
                    buffer.append(line).append('\n');
                    continue;
                }
                if (buffer.isEmpty())
                    continue;

                final CharStream stream = CharStreams.fromString(buffer.toString());
                buffer = new StringBuilder();

                final CommonTokenStream tokens = new CommonTokenStream(new QKMLexer(stream));
                final QKMParser parser = new QKMParser(tokens);
                try {
                    final Object res = parser.lines().accept(state);
                    if (res == null)
                        System.out.println("(done)");
                    else
                        System.out.println("=> " + res);
                } catch (RuntimeException ex) {
                    System.out.println("!! " + ex.getMessage());
                }

                prompt = ">> ";
            }
        } catch (IOException ex) {
        }
    }

    private final LinkedList<Map<String, Type>> types = new LinkedList<>();
    private final Map<String, PolyType> enumKeys = new HashMap<>();

    private final LinkedList<Map<String, Type>> scope = new LinkedList<>();
    private final Map<BigInteger, Type> bounds = new HashMap<>();

    private final Set<Type> fixNum = new HashSet<>();

    private BigInteger counter = BigInteger.ZERO;

    public App() {
        // make sure we have at least one scope depth to work with.
        this.types.addFirst(new HashMap<>());
        this.scope.addFirst(new HashMap<>());
    }

    private VarType freshType() {
        return new VarType((this.counter = this.counter.add(BigInteger.ONE)));
    }

    private Type freshType(PolyType p) {
        if (p.quant.isEmpty())
            return p.base;

        // each quantifier is re-bound to a fresh type variable.
        final Map<VarType, Type> m = new HashMap<>();
        for (final VarType q : p.quant)
            m.put(q, this.freshType());
        return p.base.replace(m);
    }

    private void constraintFixNum() {
        final Iterator<Type> it = this.fixNum.iterator();
        while (it.hasNext()) {
            final Type t = it.next().getCompress(this.bounds);
            if (t instanceof VarType)
                // keep the constraint
                continue;

            if (!(t instanceof IntType)) {
                this.fixNum.clear();
                throw new RuntimeException("Illegal numeric constraint on " + t.expand(this.bounds));
            }

            // otherwise the constraint is on a known correct type.
            it.remove();
        }
    }

    @Override
    public EnumType.Template visitDefEnum(DefEnumContext ctx) {
        // because enums are recursive, we stuff it in the global mapping
        // first!
        //
        // (it's also pretty sketchy how we are modifying the underlying map
        // of an unmodifiable map view...)
        final String name = ctx.n.getText();
        final Map<String, Type> defm = this.types.getFirst();
        this.types.addFirst(new HashMap<>());
        try {
            final List<VarType> lst = ctx.p == null
                    ? List.of()
                    : (List<VarType>) this.visit(ctx.p);
            final Map<String, Optional<Type>> m = new HashMap<>();
            final EnumType ty = new EnumType(new EnumType.Template(name, lst, m), lst);
            defm.put(name, ty);

            for (EnumCaseContext p : ctx.r) {
                final Map.Entry<String, Optional<Type>> entry = (Map.Entry<String, Optional<Type>>) this.visit(p);
                if (m.put(entry.getKey(), entry.getValue()) != null) {
                    // because the enum's definition is broken, discard it from
                    // the global mapping.
                    defm.remove(name);
                    throw new RuntimeException("Illegal duplicate enum case " + entry.getKey());
                }
            }

            // register each constructor as polytype functions
            final Set<VarType> quants = new HashSet<>(lst);
            for (final Map.Entry<String, Optional<Type>> p : m.entrySet()) {
                // treat each constructor as a polytype:
                // type type[qs...] = C1 arg | C2
                // C1 :: \forall qs. arg -> enum type[qs...]
                // C2 :: \forall qs. enum type[qs...]
                final String ctor = p.getKey();
                final Optional<Type> arg = p.getValue();
                final PolyType pf = arg.isEmpty()
                        ? new PolyType(quants, ty)
                        : new PolyType(quants, new FuncType(arg.get(), ty));

                this.enumKeys.put(ctor, pf);
            }

            return ty.body;
        } finally {
            this.types.removeFirst();
        }
    }

    @Override
    public List<VarType> visitPoly(PolyContext ctx) {
        final Map<String, Type> defm = this.types.getFirst();
        final List<VarType> m = new ArrayList<>(ctx.qs.size());
        for (Token t : ctx.qs) {
            final String name = t.getText();
            final VarType tv = this.freshType();
            if (defm.put(name, tv) != null)
                throw new RuntimeException("Duplicate quantifier type " + name);
            m.add(tv);
        }
        return m;
    }

    @Override
    public Map.Entry<String, Optional<Type>> visitEnumCase(EnumCaseContext ctx) {
        final String name = ctx.k.getText();
        return ctx.arg == null
                ? Map.entry(name, Optional.empty())
                : Map.entry(name, Optional.of((Type) this.visit(ctx.arg)));
    }

    @Override
    public Type visitTypeFunc(TypeFuncContext ctx) {
        final Type p = (Type) this.visit(ctx.p);
        if (ctx.q == null)
            return p;

        final Type q = (Type) this.visit(ctx.q);
        return new FuncType(p, q);
    }

    @Override
    public Type visitTypeName(TypeNameContext ctx) {
        final List<Type> list = ctx.ts.isEmpty()
                ? List.of()
                : new ArrayList<>(ctx.ts.size());
        for (final TypeContext t : ctx.ts)
            list.add((Type) this.visit(t));

        final String name = ctx.n.getText();
        for (final Map<String, Type> depth : this.types) {
            final Type t = depth.get(name);
            if (t == null)
                continue;

            if (t instanceof EnumType) {
                final EnumType et = (EnumType) t;

                if (list.size() != et.args.size())
                    throw new RuntimeException("Illegal instantiation of type " + et);
                return new EnumType(et.body, list);
            }
            return t;
        }

        final Type t = this.lookupCoreType(name);
        if (!list.isEmpty())
            throw new RuntimeException("Illegal instantiation of type " + t);
        return t;
    }

    private Type lookupCoreType(String name) {
        switch (name) {
        case "bool":    return BoolType.INSTANCE;
        case "string":  return StringType.INSTANCE;
        case "i32":     return new IntType(32);
        default:
            throw new RuntimeException("Unknown type " + name);
        }
    }

    @Override
    public Type visitTypeGroup(TypeGroupContext ctx) {
        switch (ctx.ts.size()) {
        case 0:
            return new TupleType(List.of());
        case 1:
            // never create single-element tuples
            return (Type) this.visit(ctx.ts.get(0));
        default:
            final ArrayList<Type> ts = new ArrayList<>(ctx.ts.size());
            for (final TypeContext el : ctx.ts)
                ts.add((Type) this.visit(el));
            return new TupleType(ts);
        }
    }

    @Override
    public Type visitDefRecBind(DefRecBindContext ctx) {
        final BigInteger freevar = this.counter;
        final String name = ctx.n.getText();
        final Type t = this.freshType();
        final Map<String, Type> defm = this.scope.getFirst();
        defm.put(name, t);

        try {
            final Type e = (Type) this.visit(ctx.e);
            if (Type.unify(t, e, this.bounds) == null)
                throw new RuntimeException("Illegal types for let " + name + " = " + e.getCompress(this.bounds));

            // check for illegal recursive binding uses
            new RecBindChecker().visit(ctx);
        } catch (RuntimeException ex) {
            // conservatively remove the binding on error
            defm.remove(name);
            throw ex;
        }

        // update the constraint
        Type refined = t.expand(this.bounds);
        final Set<VarType> poly = refined.collectVars()
                .filter(p -> p.key.compareTo(freevar) > 0)
                .collect(Collectors.toSet());
        refined = this.fixNum.stream()
                .map(p -> p.getCompress(this.bounds))
                .distinct()
                .filter(poly::contains)
                .reduce(refined, (inner, k) -> new FixNum(k, inner));

        refined = new PolyType(poly, refined);
        defm.put(name, refined);
        return refined;
    }

    @Override
    public Type visitExprApply(ExprApplyContext ctx) {
        final Type f = (Type) this.visit(ctx.f);
        if (ctx.arg == null)
            return f;

        final Type arg = (Type) this.visit(ctx.arg);

        // given f   :: a -> b
        //       arg :: a
        // return       b

        final VarType res = this.freshType();
        if (Type.unify(f, new FuncType(arg, res), this.bounds) == null)
            throw new RuntimeException("Illegal types for (" + f.getCompress(this.bounds) + ") " + arg.getCompress(this.bounds));

        this.constraintFixNum();
        return res.expand(this.bounds);
    }

    @Override
    public Type visitExprCons(ExprConsContext ctx) {
        final String id = ctx.k.getText();
        final PolyType ty = this.enumKeys.get(id);
        if (ty == null)
            throw new RuntimeException("Unknown enum constructor");

        final Type fresh = this.freshType(ty);

        // argless constructors are encoded as polytype enum.
        if (ctx.arg == null) {
            if (!(fresh instanceof EnumType))
                throw new RuntimeException("Illegal type for " + id);
            return fresh;
        }

        // constructors with arguments are encoded as polytype functions.
        final Type arg = (Type) this.visit(ctx.arg);
        final VarType res = this.freshType();
        if (Type.unify(fresh, new FuncType(arg, res), this.bounds) == null)
            throw new RuntimeException("Illegal types for " + id + " " + arg);

        this.constraintFixNum();
        return res.expand(this.bounds);
    }

    @Override
    public Type visitExprAdd(ExprAddContext ctx) {
        final Type l = (Type) this.visit(ctx.l);
        final Type r = (Type) this.visit(ctx.r);

        Type res = Type.unify(l, r, this.bounds);
        if (res == null)
            throw new RuntimeException("Illegal types for " + l + " + " + r);

        res = res.expand(this.bounds);
        this.fixNum.add(res);
        this.constraintFixNum();
        return res;
    }

    @Override
    public Type visitExprIdent(ExprIdentContext ctx) {
        final String name = ctx.n.getText();
        for (final Map<String, Type> depth : this.scope) {
            Type t = depth.get(name);
            if (t != null) {
                if (t instanceof PolyType)
                    t = this.freshType((PolyType) t);

                // fill constraints
                while (t instanceof FixNum) {
                    final FixNum f = (FixNum) t;
                    this.fixNum.add(f.fix);
                    t = f.tail;
                }
                return t;
            }
        }
        throw new RuntimeException("Unknown binding " + name);
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
        return convertInt(ctx.getText()).getValue();
    }

    private static Map.Entry<BigInteger, IntType> convertInt(String lit) {
        final int offs = lit.indexOf('i');
        final IntType ty;
        if (offs < 0)
            ty = new IntType(32);
        else if (lit.charAt(offs + 1) == '0')
            throw new RuntimeException("Malformed integer literal");
        else {
            ty = new IntType(Integer.parseInt(lit.substring(offs + 1)));
            lit = lit.substring(0, offs);
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

        return Map.entry(ty.signed(new BigInteger(lit.replace("_", ""))), ty);
    }

    @Override
    public Type visitExprChar(ExprCharContext ctx) {
        // make sure the literal is well formed
        final String lit = ctx.getText();
        final StrEscape decoder = new StrEscape(lit, 1, lit.length() - 1);
        decoder.next();

        return new IntType(32);
    }

    @Override
    public Type visitExprText(ExprTextContext ctx) {
        // make sure the literal is well formed
        final String lit = ctx.getText();
        final StrEscape decoder = new StrEscape(lit, 1, lit.length() - 1);
        while (decoder.hasNext())
            decoder.next();

        return StringType.INSTANCE;
    }

    @Override
    public Type visitExprLambda(ExprLambdaContext ctx) {
        final Map<String, Type> depth = new HashMap<>();
        this.scope.addFirst(depth);

        try {
            final Map.Entry<Match, Type> pair = (Map.Entry<Match, Type>) this.visit(ctx.f.p);
            final Match args = pair.getKey();
            final Type type = pair.getValue();

            if (!Match.covers(List.of(SList.of(args)), SList.of(new MatchComplete()), SList.of(type)))
                throw new RuntimeException("Illegal anonymous function with refutable pattern");

            final Type body = (Type) this.visit(ctx.f.e);
            return new FuncType(type, body).expand(this.bounds);
        } finally {
            this.scope.removeFirst();
        }
    }

    @Override
    public Type visitExprGroup(ExprGroupContext ctx) {
        switch (ctx.es.size()) {
        case 0:
            return new TupleType(List.of());
        case 1:
            // never create single-element tuples
            return (Type) this.visit(ctx.es.get(0));
        default:
            final ArrayList<Type> es = new ArrayList<>(ctx.es.size());
            for (final ExprContext el : ctx.es)
                es.add((Type) this.visit(el));
            return new TupleType(es);
        }
    }

    @Override
    public Type visitExprMatch(ExprMatchContext ctx) {
        final BigInteger freevar = this.counter;
        Type i = (Type) this.visit(ctx.i);

        final boolean generalize = this.counter.compareTo(freevar) > 0;
        final LinkedList<SList<Match>> patterns = new LinkedList<>();
        final LinkedList<Map<String, Type>> scopes = new LinkedList<>();
        for (final MatchCaseContext r : ctx.r) {
            final Map<String, Type> depth = new HashMap<>();
            scopes.addLast(depth);
            this.scope.addFirst(depth);

            final Match m;
            final Type p;
            try {
                final Map.Entry<Match, Type> mp = (Map.Entry<Match, Type>) this.visit(r.p);
                m = mp.getKey();
                p = mp.getValue();
            } finally {
                this.scope.removeFirst();
            }

            final Type inf = Type.unify(i, p, this.bounds);
            if (inf == null)
                throw new RuntimeException("Illegal types for match " + i + " with " + p);

            i = inf;
            if (Match.covers(patterns, SList.of(m), SList.of(inf)))
                System.out.println("Useless pattern");

            patterns.add(SList.of(m));
        }

        if (!Match.covers(patterns, SList.of(new MatchComplete()), SList.of(i)))
            System.out.println("Unexhaustive match");

        final VarType res = this.freshType();
        for (final MatchCaseContext r : ctx.r) {
            final Map<String, Type> depth = scopes.removeFirst();
            this.scope.addFirst(depth);

            for (final Map.Entry<String, Type> pair : depth.entrySet()) {
                Type expanded = pair.getValue().expand(this.bounds);
                final Set<VarType> poly;
                if (!generalize)
                    poly = Set.of();
                else {
                    poly = expanded.collectVars()
                            .filter(p -> p.key.compareTo(freevar) > 0)
                            .collect(Collectors.toSet());
                    expanded = this.fixNum.stream()
                            .map(p -> p.getCompress(this.bounds))
                            .distinct()
                            .filter(poly::contains)
                            .reduce(expanded, (inner, k) -> new FixNum(k, inner));
                }

                pair.setValue(new PolyType(poly, expanded));
            }

            final Type t;
            try {
                t = (Type) this.visit(r.e);
            } finally {
                this.scope.removeFirst();
            }

            if (Type.unify(res, t, this.bounds) == null)
                throw new RuntimeException("Illegal types for { => " + res + ", => " + t + " }");
        }

        return res.getCompress(this.bounds);
    }

    @Override
    public Void visitMatchCase(MatchCaseContext ctx) {
        throw new UnsupportedOperationException("Don't call this directly");
    }

    @Override
    public Map.Entry<Match, Type> visitPatIgnore(PatIgnoreContext ctx) {
        return Map.entry(new MatchComplete(), this.freshType());
    }

    @Override
    public Map.Entry<Match, Type> visitPatTrue(PatTrueContext ctx) {
        return Map.entry(new MatchNode(true), BoolType.INSTANCE);
    }

    @Override
    public Map.Entry<Match, Type> visitPatFalse(PatFalseContext ctx) {
        return Map.entry(new MatchNode(false), BoolType.INSTANCE);
    }

    @Override
    public Map.Entry<Match, Type> visitPatInt(PatIntContext ctx) {
        final Map.Entry<BigInteger, IntType> pair = convertInt(ctx.getText());
        return Map.entry(new MatchNode(pair.getKey()), pair.getValue());
    }

    @Override
    public Map.Entry<Match, Type> visitPatChar(PatCharContext ctx) {
        final String lit = ctx.getText();
        final StrEscape decoder = new StrEscape(lit, 1, lit.length() - 1);
        final int cp = decoder.next();

        return Map.entry(new MatchNode(BigInteger.valueOf(cp)), new IntType(32));
    }

    @Override
    public Map.Entry<Match, Type> visitPatText(PatTextContext ctx) {
        final String lit = ctx.getText();
        final StrEscape decoder = new StrEscape(lit, 1, lit.length() - 1);
        final StringBuilder sb = new StringBuilder();
        while (decoder.hasNext())
            sb.appendCodePoint(decoder.next());

        return Map.entry(new MatchNode(sb.toString()), StringType.INSTANCE);
    }

    @Override
    public Map.Entry<Match, Type> visitPatDecons(PatDeconsContext ctx) {
        final String id = ctx.id.getText();
        final PolyType ty = this.enumKeys.get(id);
        if (ty == null)
            throw new RuntimeException("Unknown enum constructor");

        final Type fresh = this.freshType(ty);

        // argless constructors are encoded as polytype enum.
        if (ctx.arg == null) {
            if (!(fresh instanceof EnumType))
                throw new RuntimeException("Illegal type for " + id);
            return Map.entry(new MatchNode(id), fresh);
        }

        final Map.Entry<Match, Type> pair = (Map.Entry<Match, Type>) this.visit(ctx.arg);
        final MatchNode node = new MatchNode(id, pair.getKey());

        // constructors with arguments are encoded as polytype functions.
        final Type arg = pair.getValue();
        final VarType res = this.freshType();
        if (Type.unify(fresh, new FuncType(arg, res), this.bounds) == null)
            throw new RuntimeException("Illegal types for " + ty + " " + arg);

        return Map.entry(node, res.expand(this.bounds));
    }

    @Override
    public Map.Entry<Match, Type> visitPatBind(PatBindContext ctx) {
        final String name = ctx.n.getText();
        final Map<String, Type> level = this.scope.getFirst();
        final VarType ty = this.freshType();
        if (level.putIfAbsent(name, ty) != null)
            throw new RuntimeException("Illegal duplicate binding within the same case");
        return Map.entry(new MatchComplete(), ty);
    }

    @Override
    public Map.Entry<Match, Type> visitPatGroup(PatGroupContext ctx) {
        switch (ctx.ps.size()) {
        case 0:
            // note that matching unit is treated as matching a wildcard to
            // allow collapsing tuples of known complete matches.
            return Map.entry(new MatchComplete(), new TupleType(List.of()));
        case 1:
            // never create single-element tuples
            return (Map.Entry<Match, Type>) this.visit(ctx.ps.get(0));
        default:
            boolean isComplete = true;
            final ArrayList<Match> ps = new ArrayList<>(ctx.ps.size());
            final ArrayList<Type> ts = new ArrayList<>(ctx.ps.size());
            for (final PatternContext el : ctx.ps) {
                final Map.Entry<Match, Type> pair = (Map.Entry<Match, Type>) this.visit(el);
                final Match m = pair.getKey();
                isComplete &= m instanceof MatchComplete;
                ps.add(m);
                ts.add(pair.getValue());
            }
            final Match res = isComplete
                    ? new MatchComplete()
                    : new MatchNode(TupleType.class, Collections.unmodifiableList(ps));
            final TupleType ty = new TupleType(ts);
            return Map.entry(res, ty);
        }
    }
}

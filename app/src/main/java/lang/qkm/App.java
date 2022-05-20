package lang.qkm;

import java.util.*;
import java.math.BigInteger;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
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

    private final Map<String, EnumType> enums = new HashMap<>();
    private final Map<String, EnumType> enumKeys = new HashMap<>();

    private final LinkedList<Map<String, Type>> scope = new LinkedList<>();
    private final Map<BigInteger, Type> bounds = new HashMap<>();

    private BigInteger counter = BigInteger.ZERO;

    public App() {
        // make sure we have at least one scope depth to work with.
        this.scope.addFirst(new HashMap<>());
    }

    private VarType freshType() {
        return new VarType((this.counter = this.counter.add(BigInteger.ONE)));
    }

    @Override
    public Void visitDefEnum(DefEnumContext ctx) {
        // because enums are recursive, we stuff it in the global mapping
        // first!
        //
        // (it's also pretty sketchy how we are modifying the underlying map
        // of an unmodifiable map view...)
        final String name = ctx.n.getText();
        final Map<String, Type> m = new HashMap<>();
        final EnumType ty = new EnumType(name, Collections.unmodifiableMap(m));
        this.enums.put(name, ty);

        for (EnumCaseContext p : ctx.r) {
            final Map.Entry<String, Type> entry = (Map.Entry<String, Type>) this.visit(p);
            if (m.put(entry.getKey(), entry.getValue()) != null) {
                // because the enum's definition is broken, discard it from
                // the global mapping.
                this.enums.remove(name);
                throw new RuntimeException("Illegal duplicate enum case " + entry.getKey());
            }
        }

        // register it for the deconstruction pattern to work.
        for (final String key : m.keySet())
            this.enumKeys.put(key, ty);

        // we also need to add each constructor to the scope.
        final Map<String, Type> level = this.scope.getFirst();
        for (final Map.Entry<String, Type> p : m.entrySet())
            // treat each constructor as a function:
            // enum color { Red() }
            // Red :: () -> enum color
            level.put(p.getKey(), new FuncType(p.getValue(), ty));
        return null;
    }

    @Override
    public Map.Entry<String, Type> visitEnumCase(EnumCaseContext ctx) {
        final Type t = (Type) this.visit(ctx.arg);
        return Map.entry(ctx.k.getText(), t);
    }

    @Override
    public Type visitTypeName(TypeNameContext ctx) {
        final String name = ctx.n.getText();
        switch (name) {
        case "bool":    return BoolType.INSTANCE;
        default:
            final EnumType t = enums.get(name);
            if (t != null)
                return t;

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
            return new TupleType(Collections.unmodifiableList(ts));
        }
    }

    @Override
    public Type visitExprApply(ExprApplyContext ctx) {
        final Type f = (Type) this.visit(ctx.f);
        final Type arg = (Type) this.visit(ctx.arg);

        // given f   :: a -> b
        //       arg :: a
        // return       b

        final VarType res = this.freshType();
        if (Type.unify(f, new FuncType(arg, res), this.bounds) == null)
            throw new RuntimeException("Illegal types for (" + f + ") " + arg);

        return res.getCompress(this.bounds);
    }

    @Override
    public Type visitExprIdent(ExprIdentContext ctx) {
        final String name = ctx.n.getText();
        for (final Map<String, Type> depth : this.scope) {
            final Type t = depth.get(name);
            if (t != null)
                return t;
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
            return new TupleType(Collections.unmodifiableList(es));
        }
    }

    @Override
    public Type visitExprMatch(ExprMatchContext ctx) {
        Type i = (Type) this.visit(ctx.i);

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
            this.scope.addFirst(scopes.removeFirst());

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
        return Map.entry(new MatchAtom(true), BoolType.INSTANCE);
    }

    @Override
    public Map.Entry<Match, Type> visitPatFalse(PatFalseContext ctx) {
        return Map.entry(new MatchAtom(false), BoolType.INSTANCE);
    }

    @Override
    public Map.Entry<Match, Type> visitPatChar(PatCharContext ctx) {
        final String lit = ctx.getText();
        final StrEscape decoder = new StrEscape(lit, 1, lit.length() - 1);
        final int cp = decoder.next();

        return Map.entry(new MatchAtom(BigInteger.valueOf(cp)), new IntType(32));
    }

    @Override
    public Map.Entry<Match, Type> visitPatText(PatTextContext ctx) {
        final String lit = ctx.getText();
        final StrEscape decoder = new StrEscape(lit, 1, lit.length() - 1);
        final StringBuilder sb = new StringBuilder();
        while (decoder.hasNext())
            sb.appendCodePoint(decoder.next());

        return Map.entry(new MatchAtom(sb.toString()), StringType.INSTANCE);
    }

    @Override
    public Map.Entry<Match, Type> visitPatDecons(PatDeconsContext ctx) {
        final String id = ctx.id.getText();
        final EnumType ty = this.enumKeys.get(id);
        if (ty == null)
            throw new RuntimeException("Unknown enum constructor");

        final Map.Entry<Match, Type> arg = (Map.Entry<Match, Type>) this.visit(ctx.arg);
        return Map.entry(new MatchCtor(id, arg.getKey()), ty);
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
                    : new MatchTuple(Collections.unmodifiableList(ps));
            final TupleType ty = new TupleType(Collections.unmodifiableList(ts));
            return Map.entry(res, ty);
        }
    }
}

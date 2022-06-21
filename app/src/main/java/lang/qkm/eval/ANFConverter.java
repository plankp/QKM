package lang.qkm.eval;

import java.math.BigInteger;
import java.util.*;
import lang.qkm.expr.*;
import lang.qkm.match.*;
import lang.qkm.util.SList;

public final class ANFConverter implements ExprRewriter, Match.Visitor<Match> {

    private interface BindingInfo {

        public Expr apply(Expr e);
    }

    private final class LetBinding implements BindingInfo {

        public final EVar name;
        public final Expr value;

        public LetBinding(EVar name, Expr value) {
            this.name = name;
            this.value = value;
        }

        @Override
        public Expr apply(Expr e) {
            if (!(this.value instanceof EMatch))
                return new ELet(this.name, this.value, e);

            // let v = match s with m -> e in Q v =>
            // let k = \v. Q v in match s with m -> k e

            final EVar cont = new EVar(ANFConverter.this.newName());
            return new ELet(cont, new ELam(this.name, e),
                            this.value.accept(new FixupCont(cont)));
        }
    }

    private final class LetrecBinding implements BindingInfo {

        public final Map<EVar, Expr> binds;

        public LetrecBinding(Map<EVar, Expr> binds) {
            this.binds = binds;
        }

        @Override
        public Expr apply(Expr e) {
            return new ELetrec(this.binds, e);
        }
    }

    private final class FixupCont implements Expr.Visitor<Expr> {

        public final Expr joinPoint;

        public FixupCont(Expr joinPoint) {
            this.joinPoint = joinPoint;
        }

        @Override
        public Expr visitEBool(EBool e) {
            return new EApp(this.joinPoint, e);
        }

        @Override
        public Expr visitECtor(ECtor e) {
            // we were already in monadic intermediate form, so
            // k (#k e...) => let t = (#k e...) in k t
            final EVar t = new EVar(ANFConverter.this.newName());
            return new ELet(t, e, new EApp(this.joinPoint, t));
        }

        @Override
        public Expr visitEInt(EInt e) {
            return new EApp(this.joinPoint, e);
        }

        @Override
        public Expr visitEString(EString e) {
            return new EApp(this.joinPoint, e);
        }

        @Override
        public Expr visitETup(ETup e) {
            // we were already in monadic intermediate form, so
            // k '(e...) => let t = '(e...) in k t
            final EVar t = new EVar(ANFConverter.this.newName());
            return new ELet(t, e, new EApp(this.joinPoint, t));
        }

        @Override
        public Expr visitEMatch(EMatch e) {
            // k (match s with m -> e) => match s with m -> k e
            final List<Map.Entry<Match, Expr>> cases = new ArrayList<>(e.cases.size());
            for (final Map.Entry<Match, Expr> k : e.cases)
                cases.add(Map.entry(k.getKey(), k.getValue().accept(this)));
            return new EMatch(e.scrutinee, cases);
        }

        @Override
        public Expr visitEVar(EVar e) {
            return new EApp(this.joinPoint, e);
        }

        @Override
        public Expr visitELam(ELam e) {
            return new EApp(this.joinPoint, e);
        }

        @Override
        public Expr visitEApp(EApp e) {
            // we were already in monadic intermediate form, so
            // k (f a) => let t = f a in k t
            final EVar t = new EVar(ANFConverter.this.newName());
            return new ELet(t, e, new EApp(this.joinPoint, t));
        }

        @Override
        public Expr visitELet(ELet e) {
            // we were already in monadic intermediate form, so
            // k (let t = ... in e) => let t = ... in k e
            return new ELet(e.bind, e.value, e.body.accept(this));
        }

        @Override
        public Expr visitELetrec(ELetrec e) {
            // k (letrec t = ... in e) => letrec t = ... in k e
            return new ELetrec(e.binds, e.body.accept(this));
        }

        @Override
        public Expr visitEErr(EErr e) {
            // it's going to error anyway...
            // k (!! q) => !! q
            return e;
        }
    }

    private BigInteger id = BigInteger.ZERO;
    private Map<String, String> mapping = new HashMap<>();
    private Deque<BindingInfo> seq = new ArrayDeque<>();

    @Override
    public Expr rewrite(Expr e) {
        e = e.accept(this);
        while (!this.seq.isEmpty())
            e = this.seq.pop().apply(e);
        return e;
    }

    public String newName() {
        return "`v" + (this.id = this.id.add(BigInteger.ONE));
    }

    private Expr rewriteAtom(Expr e) {
        e = e.accept(this);
        if (!e.isAtom()) {
            final EVar capture = new EVar(this.newName());
            this.seq.push(new LetBinding(capture, e));
            e = capture;
        }

        return e;
    }

    @Override
    public Expr visitECtor(ECtor e) {
        if (e.args.isEmpty())
            return e;

        final ArrayList<Expr> args = new ArrayList<>(e.args.size());
        for (final Expr arg : e.args)
            args.add(this.rewriteAtom(arg));

        return new ECtor(e.id, args);
    }

    @Override
    public Expr visitETup(ETup e) {
        if (e.elements.isEmpty())
            return e;

        final ArrayList<Expr> elements = new ArrayList<>(e.elements.size());
        for (final Expr element : e.elements)
            elements.add(this.rewriteAtom(element));

        return new ETup(elements);
    }

    @Override
    public Expr visitEMatch(EMatch e) {
        final Expr scrutinee = this.rewriteAtom(e.scrutinee);

        final List<Map.Entry<Match, Expr>> cases = new ArrayList<>(e.cases.size());
        final Map<String, String> old = this.mapping;
        final Deque<BindingInfo> outer = this.seq;
        for (final Map.Entry<Match, Expr> k : e.cases) {
            final Map<String, String> captures = new HashMap<>();
            this.mapping = captures;
            final Match m = k.getKey().accept(this);

            this.mapping = new HashMap<>(old);
            this.mapping.putAll(captures);
            this.seq = new ArrayDeque<>();
            final Expr body = this.rewrite(k.getValue());

            cases.add(Map.entry(m, body));
        }

        this.mapping = old;
        this.seq = outer;

        return new EMatch(scrutinee, cases);
    }

    @Override
    public Expr visitEVar(EVar e) {
        // it might be null because we don't alpha rename globals
        final String remapped = this.mapping.get(e.name);
        return remapped == null ? e : new EVar(remapped);
    }

    @Override
    public Expr visitELam(ELam e) {
        final String oldMapping = this.mapping.get(e.arg.name);
        final String newMapping = this.newName();

        this.mapping.put(e.arg.name, newMapping);
        final Deque<BindingInfo> outer = this.seq;
        this.seq = new ArrayDeque<>();
        final Expr body = this.rewrite(e.body);

        this.seq = outer;
        this.mapping.put(e.arg.name, oldMapping);
        return new ELam(new EVar(newMapping), body);
    }

    @Override
    public Expr visitEApp(EApp e) {
        final Expr f = this.rewriteAtom(e.f);
        final Expr arg = this.rewriteAtom(e.arg);

        return new EApp(f, arg);
    }

    @Override
    public Expr visitELet(ELet e) {
        final Expr value = e.value.accept(this);

        final String oldMapping = this.mapping.get(e.bind.name);
        final String newMapping = this.newName();
        this.mapping.put(e.bind.name, newMapping);
        this.seq.push(new LetBinding(new EVar(newMapping), value));

        final Expr body = e.body.accept(this);
        this.mapping.put(e.bind.name, oldMapping);
        return body;
    }

    @Override
    public Expr visitELetrec(ELetrec e) {
        final Map<String, String> old = this.mapping;
        this.mapping = new HashMap<>(old);
        final Deque<BindingInfo> outer = this.seq;
        for (final EVar oldName : e.binds.keySet())
            this.mapping.put(oldName.name, this.newName());

        final Map<EVar, Expr> binds = new HashMap<>();
        for (final Map.Entry<EVar, Expr> bind : e.binds.entrySet()) {
            final EVar name = new EVar(this.mapping.get(bind.getKey().name));
            this.seq = new ArrayDeque<>();
            final Expr init = this.rewrite(bind.getValue());

            binds.put(name, init);
        }

        this.seq = outer;
        this.seq.push(new LetrecBinding(binds));
        final Expr body = e.body.accept(this);
        this.mapping = old;
        return body;
    }

    @Override
    public Expr visitEErr(EErr e) {
        final Expr value = this.rewriteAtom(e.value);
        return new EErr(value);
    }

    @Override
    public Match visitMatchAll(MatchAll m) {
        if (m.capture == null)
            return m;

        // or patterns need captures on both sides to keep the same name...
        final String repl = this.mapping.computeIfAbsent(m.capture, k -> this.newName());
        return new MatchAll(repl);
    }

    @Override
    public Match visitMatchBool(MatchBool m) {
        return m;
    }

    @Override
    public Match visitMatchCtor(MatchCtor m) {
        if (m.args.isEmpty())
            return m;

        boolean modified = false;
        final List<Match> args = new ArrayList<>(m.args.size());
        for (final Match before : m.args) {
            final Match after = before.accept(this);
            modified |= before != after;
            args.add(after);
        }

        if (!modified)
            return m;

        return new MatchCtor(m.ctor, args);
    }

    @Override
    public Match visitMatchInt(MatchInt m) {
        return m;
    }

    @Override
    public Match visitMatchString(MatchString m) {
        return m;
    }

    @Override
    public Match visitMatchTup(MatchTup m) {
        if (m.elements.isEmpty())
            return m;

        boolean modified = false;
        final List<Match> elements = new ArrayList<>(m.elements.size());
        for (final Match before : m.elements) {
            final Match after = before.accept(this);
            modified |= before != after;
            elements.add(after);
        }

        if (!modified)
            return m;

        return new MatchTup(elements);
    }

    @Override
    public Match visitMatchOr(MatchOr m) {
        final SList.Builder<Match> result = new SList.Builder<>();
        for (SList<Match> k = m.submatches; k.nonEmpty(); k = k.tail())
            result.addLast(k.head().accept(this));

        return new MatchOr(result.build());
    }
}

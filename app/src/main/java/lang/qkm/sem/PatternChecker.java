package lang.qkm.sem;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.*;
import lang.qkm.util.*;
import lang.qkm.type.*;
import lang.qkm.match.*;
import lang.qkm.QKMBaseVisitor;
import static lang.qkm.QKMParser.*;

public final class PatternChecker extends QKMBaseVisitor<Typed<Match>> {

    private final Map<String, TyVar> bindings = new HashMap<>();
    private final TypeState state;
    private final KindChecker kindChecker;

    public PatternChecker(TypeState state, KindChecker kindChecker) {
        this.state = state;
        this.kindChecker = kindChecker;
    }

    public Map<String, TyVar> getBindings() {
        return Collections.unmodifiableMap(this.bindings);
    }

    @Override
    public Typed<Match> visitPatDecons(PatDeconsContext ctx) {
        final String ctor = ctx.k.getText();
        final Type scheme = this.kindChecker.getCtor(ctor);
        if (scheme == null)
            throw new RuntimeException("Illegal use of undeclared constructor " + ctor);

        Type acc = this.state.inst(scheme);

        final List<Match> args;
        if (ctx.args.isEmpty())
            args = List.of();
        else {
            args = new ArrayList<>(ctx.args.size());
            for (final Pattern0Context arg : ctx.args) {
                final Typed<Match> m = this.visit(arg);
                args.add(m.value);

                final TyVar res = this.state.freshType();
                acc.unify(new TyArr(m.type, res));
                acc = res.unwrap();
            }
        }

        if (acc instanceof TyCtor)
            return new Typed<>(new MatchCtor(ctor, args), acc);

        throw new RuntimeException("Illegal incomplete constructor application");
    }

    @Override
    public Typed<Match> visitPatIgnore(PatIgnoreContext ctx) {
        return new Typed<>(new MatchAll(), this.state.freshType());
    }

    @Override
    public Typed<Match> visitPatBind(PatBindContext ctx) {
        final String name = ctx.n.getText();
        final TyVar type = this.state.freshType();
        if (this.bindings.put(name, type) != null)
            throw new RuntimeException("Illegal duplicate binding " + name + " within the same pattern");

        return new Typed<>(new MatchAll(name), type);
    }

    @Override
    public Typed<Match> visitPatTrue(PatTrueContext ctx) {
        return new Typed<>(new MatchBool(true), TyBool.INSTANCE);
    }

    @Override
    public Typed<Match> visitPatFalse(PatFalseContext ctx) {
        return new Typed<>(new MatchBool(false), TyBool.INSTANCE);
    }

    @Override
    public Typed<Match> visitPatInt(PatIntContext ctx) {
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

        return new Typed<>(new MatchInt(v), ty);
    }

    @Override
    public Typed<Match> visitPatChar(PatCharContext ctx) {
        final String t = ctx.getText();
        final StrEscape encoder = new StrEscape(t, 1, t.length() - 1);
        final int cp = encoder.next();

        return new Typed<>(new MatchInt(BigInteger.valueOf(cp)), new TyInt(32));
    }

    @Override
    public Typed<Match> visitPatText(PatTextContext ctx) {
        final String t = ctx.getText();
        if (t.length() == 2)
            return new Typed<>(new MatchString(""), TyString.INSTANCE);

        final StrEscape encoder = new StrEscape(t, 1, t.length() - 1);
        final StringBuilder sb = new StringBuilder();
        while (encoder.hasNext())
            sb.appendCodePoint(encoder.next());
        return new Typed<>(new MatchString(sb.toString()), TyString.INSTANCE);
    }

    @Override
    public Typed<Match> visitPatGroup(PatGroupContext ctx) {
        final int sz = ctx.ps.size();
        switch (sz) {
        case 1:
            return this.visit(ctx.ps.get(0));
        case 0:
            return new Typed<>(new MatchTup(List.of()), new TyTup(List.of()));
        default:
            final List<Match> ms = new ArrayList<>(sz);
            final List<Type> ts = new ArrayList<>(sz);
            for (final PatternContext e : ctx.ps) {
                final Typed<Match> r = this.visit(e);
                ms.add(r.value);
                ts.add(r.type);
            }

            return new Typed<>(new MatchTup(ms), new TyTup(ts));
        }
    }

    @Override
    public Typed<Match> visitPatTyped(PatTypedContext ctx) {
        final Typed<Match> p = this.visit(ctx.p);
        final KindChecker.Result r = this.kindChecker.visit(ctx.t);
        r.kind.unify(TyKind.VALUE);
        r.type.eval(Map.of()).unify(p.type);

        return p;
    }
}

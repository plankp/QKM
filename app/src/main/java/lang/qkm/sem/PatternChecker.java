package lang.qkm.sem;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.*;
import lang.qkm.util.*;
import lang.qkm.type.*;
import lang.qkm.match.*;
import lang.qkm.QKMBaseVisitor;
import static lang.qkm.QKMParser.*;

public final class PatternChecker extends QKMBaseVisitor<Match> {

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
    public Match visitPatDecons(PatDeconsContext ctx) {
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
                final Match m = this.visit(arg);
                args.add(m);

                final TyVar res = this.state.freshType();
                acc.unify(new TyArr(m.getType(), res));
                acc = res.unwrap();
            }
        }

        if (acc instanceof TyCtor)
            return new MatchNode(acc, ctor, args);

        throw new RuntimeException("Illegal incomplete constructor application");
    }

    @Override
    public Match visitPatIgnore(PatIgnoreContext ctx) {
        return MatchComplete.wildcard(this.state.freshType());
    }

    @Override
    public Match visitPatBind(PatBindContext ctx) {
        final String name = ctx.n.getText();
        final TyVar typ = this.state.freshType();
        if (this.bindings.put(name, typ) != null)
            throw new RuntimeException("Illegal duplicate binding " + name + " within the same pattern");

        return new MatchComplete(name, typ);
    }

    @Override
    public Match visitPatTrue(PatTrueContext ctx) {
        return new MatchNode(TyBool.INSTANCE, true, List.of());
    }

    @Override
    public Match visitPatFalse(PatFalseContext ctx) {
        return new MatchNode(TyBool.INSTANCE, false, List.of());
    }

    @Override
    public Match visitPatInt(PatIntContext ctx) {
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

        return new MatchNode(ty, v, List.of());
    }

    @Override
    public Match visitPatChar(PatCharContext ctx) {
        final String t = ctx.getText();
        final StrEscape encoder = new StrEscape(t, 1, t.length() - 1);
        final int cp = encoder.next();

        return new MatchNode(new TyInt(32), BigInteger.valueOf(cp), List.of());
    }

    @Override
    public Match visitPatText(PatTextContext ctx) {
        final String t = ctx.getText();
        if (t.length() == 2)
            return new MatchNode(TyString.INSTANCE, "", List.of());

        final StrEscape encoder = new StrEscape(t, 1, t.length() - 1);
        final StringBuilder sb = new StringBuilder();
        while (encoder.hasNext())
            sb.appendCodePoint(encoder.next());
        return new MatchNode(TyString.INSTANCE, sb.toString(), List.of());
    }

    @Override
    public Match visitPatGroup(PatGroupContext ctx) {
        final int sz = ctx.ps.size();
        switch (sz) {
        case 0:
            return new MatchNode(new TyTup(List.of()), TyTup.class, List.of());
        case 1:
            return this.visit(ctx.ps.get(0));
        default:
            final List<Match> elements = new ArrayList<>(sz);
            for (final PatternContext e : ctx.ps)
                elements.add(this.visit(e));

            final TyTup ty = new TyTup(elements.stream()
                    .map(Match::getType)
                    .collect(Collectors.toList()));
            return new MatchNode(ty, TyTup.class, elements);
        }
    }
}

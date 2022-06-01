package lang.qkm.sem;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.*;
import lang.qkm.type.*;
import lang.qkm.QKMBaseVisitor;
import static lang.qkm.QKMParser.*;

public final class PatternChecker extends QKMBaseVisitor<Type> {

    private final Map<String, VarType> bindings = new HashMap<>();
    private final TypeState state;
    private final KindChecker kindChecker;

    public PatternChecker(TypeState state, KindChecker kindChecker) {
        this.state = state;
        this.kindChecker = kindChecker;
    }

    public Map<String, VarType> getBindings() {
        return Collections.unmodifiableMap(this.bindings);
    }

    @Override
    public Type visitPatDecons(PatDeconsContext ctx) {
        final String ctor = ctx.k.getText();
        final PolyType scheme = this.kindChecker.getCtor(ctor);
        if (scheme == null)
            throw new RuntimeException("Illegal use of undeclared constructor " + ctor);

        Type acc = this.state.inst(scheme);
        for (final Pattern0Context arg : ctx.args) {
            final Type v = this.visit(arg);
            final VarType res = this.state.freshType();

            acc.unify(new FuncType(v, res));
            acc = res.get();
        }

        if (acc instanceof EnumType)
            return acc;

        throw new RuntimeException("Illegal incomplete constructor application");
    }

    @Override
    public Type visitPatIgnore(PatIgnoreContext ctx) {
        return this.state.freshType();
    }

    @Override
    public Type visitPatBind(PatBindContext ctx) {
        final String name = ctx.n.getText();
        final VarType typ = this.state.freshType();
        if (this.bindings.put(name, typ) != null)
            throw new RuntimeException("Illegal duplicate binding " + name + " within the same pattern");

        return typ;
    }

    @Override
    public Type visitPatTrue(PatTrueContext ctx) {
        return BoolType.INSTANCE;
    }

    @Override
    public Type visitPatFalse(PatFalseContext ctx) {
        return BoolType.INSTANCE;
    }

    @Override
    public Type visitPatInt(PatIntContext ctx) {
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
    public Type visitPatChar(PatCharContext ctx) {
        return new IntType(32);
    }

    @Override
    public Type visitPatText(PatTextContext ctx) {
        return StringType.INSTANCE;
    }

    @Override
    public Type visitPatGroup(PatGroupContext ctx) {
        final int sz = ctx.ps.size();
        switch (sz) {
        case 0:
            return new TupleType(List.of());
        case 1:
            return this.visit(ctx.ps.get(0));
        default:
            final List<Type> elements = new ArrayList<>(sz);
            for (final PatternContext e : ctx.ps)
                elements.add(this.visit(e));
            return new TupleType(elements);
        }
    }
}

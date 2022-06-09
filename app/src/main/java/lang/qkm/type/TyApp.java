package lang.qkm.type;

import java.util.*;
import java.util.stream.*;

public final class TyApp implements Type {

    public final Type f;
    public final Type arg;

    public TyApp(Type f, Type arg) {
        this.f = f;
        this.arg = arg;
    }

    @Override
    public Type unwrap() {
        return this;
    }

    @Override
    public TyApp unapply() {
        return this;
    }

    @Override
    public Stream<TyVar> fv() {
        return Stream.concat(this.f.fv(), this.arg.fv());
    }

    @Override
    public void unify(Type other) {
        other = other.unwrap();

        if (other == this)
            return;
        if (other instanceof TyVar) {
            ((TyVar) other).set(this);
            return;
        }

        final TyApp app = other.unapply();
        if (app == null)
            throw new RuntimeException("Cannot unify " + this + " and " + other);

        this.f.unify(app.f);
        this.arg.unify(app.arg);
    }

    @Override
    public Type eval(Map<TyVar, ? extends Type> env) {
        final Type f = this.f.eval(env);
        final Type arg = this.arg.eval(env);
        if (!(f instanceof TyPoly))
            return new TyApp(f, arg);

        final TyPoly p = (TyPoly) f;
        final Map<TyVar, Type> aug = new HashMap<>(env);
        aug.put(p.arg, arg);
        return p.body.eval(aug);
    }

    @Override
    public String toString() {
        Type f = this.f.unwrap();
        if (!(f instanceof TyApp))
            return "(" + f + " " + this.arg + ")";

        final LinkedList<Type> args = new LinkedList<>();
        args.push(this.arg);
        while (f instanceof TyApp) {
            final TyApp app = (TyApp) f;
            f = app.f;
            args.push(app.arg);
        }

        final StringBuilder sb = new StringBuilder();
        sb.append('(').append(f);
        while (!args.isEmpty())
            sb.append(' ').append(args.pop());
        return sb.append(')').toString();
    }
}

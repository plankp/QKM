package lang.qkm.type;

import java.util.*;
import java.util.stream.*;

public final class TyArr implements Type {

    public final Type arg;
    public final Type ret;

    public TyArr(Type arg, Type ret) {
        this.arg = arg;
        this.ret = ret;
    }

    @Override
    public TyApp unapply() {
        // (p -> q) ~ (a b. a -> b) p q
        final TyVar a = new TyVar();
        final TyVar b = new TyVar();

        final TyPoly p = new TyPoly(a, new TyPoly(b, new TyArr(a, b)));
        return new TyApp(new TyApp(p, this.arg), this.ret);
    }

    @Override
    public Type unwrap() {
        return this;
    }

    @Override
    public Stream<TyVar> fv() {
        return Stream.concat(this.arg.fv(), this.ret.fv());
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
        if (other instanceof TyApp) {
            this.unapply().unify(other);
            return;
        }

        if (other instanceof TyArr) {
            final TyArr arr = (TyArr) other;
            this.arg.unify(arr.arg);
            this.ret.unify(arr.ret);
            return;
        }

        throw new RuntimeException("Cannot unify " + this + " and " + other);
    }

    @Override
    public Type eval(Map<TyVar, ? extends Type> env) {
        return new TyArr(this.arg.eval(env), this.ret.eval(env));
    }

    @Override
    public String toString() {
        Type r = this.ret.unwrap();
        if (!(r instanceof TyArr))
            return "(" + this.arg + " -> " + this.ret + ")";

        final StringBuilder sb = new StringBuilder();
        sb.append('(').append(this.arg);
        while (r instanceof TyArr) {
            final TyArr a = (TyArr) r;
            sb.append(" -> ").append(a.arg);
            r = a.ret;
        }
        return sb.append(" -> ").append(r).append(')').toString();
    }
}

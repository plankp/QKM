package lang.qkm.type;

import java.util.*;
import java.util.stream.*;
import lang.qkm.match.CtorSet;

public final class TyVar implements Type {

    public final String name;

    private Type ref;

    /* package */ TyVar() {
        this.name = "«" + System.identityHashCode(this) + "»";
    }

    private TyVar(String name) {
        this.name = name;
    }

    public static TyVar unifiable(String name) {
        return new TyVar(name);
    }

    public static TyVar grounded(String name) {
        final TyVar t = new TyVar(name);
        t.ref = t;
        return t;
    }

    @Override
    public Type unwrap() {
        if (this.ref == this || this.ref == null)
            return this;
        if (!(this.ref instanceof TyVar))
            return this.ref;

        // path compression
        final TyVar chain = (TyVar) this.ref;
        final Type peak = chain.unwrap();
        this.ref = peak;
        return peak;
    }

    @Override
    public TyApp unapply() {
        final Type peak = this.unwrap();
        return peak instanceof TyVar ? null : peak.unapply();
    }

    @Override
    public Stream<TyVar> fv() {
        final Type peak = this.unwrap();
        return peak instanceof TyVar ? Stream.of((TyVar) peak) : peak.fv();
    }

    @Override
    public void unify(Type other) {
        other = other.unwrap();

        if (other == this.unwrap())
            return;
        if (this.ref == null)
            this.set(other);
        else if (this.ref != this)
            this.ref.unify(other);
        else {
            // we are a grounded type, meaning the only possibility is if the
            // other type is a unifiable type variable.
            if (other instanceof TyVar) {
                final TyVar v = (TyVar) other;
                if (v.ref != v) {
                    v.set(this);
                    return;
                }
            }

            // otherwise it's a fail because we are losing generality.
            throw new RuntimeException("Illegal unify with grounded type");
        }
    }

    public void set(Type ref) {
        if (ref == this)
            return;

        if (this.ref == this)
            throw new RuntimeException("Illegal unify with grounded type");
        if (this.ref != null)
            throw new IllegalStateException("Illegal set on bounded type variable");
        if (ref.fv().anyMatch(t -> t == this))
            throw new RuntimeException("Illegal recursive unify");

        this.ref = ref;
    }

    @Override
    public Type eval(Map<TyVar, ? extends Type> env) {
        final Type peak = this.unwrap();
        if (!(peak instanceof TyVar))
            return peak.eval(env);

        final Type t = env.get(peak);
        return t != null ? t : peak;
    }

    @Override
    public CtorSet getCtorSet() {
        final Type peak = this.unwrap();
        return peak instanceof TyVar ? null : peak.getCtorSet();
    }

    @Override
    public String toString() {
        final Type peak = this.unwrap();
        return peak instanceof TyVar ? ((TyVar) peak).name : peak.toString();
    }
}

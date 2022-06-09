package lang.qkm.type;

import java.util.*;
import java.util.stream.*;

public enum TyKind implements Type {

    VALUE;

    @Override
    public Type unwrap() {
        return this;
    }

    @Override
    public TyApp unapply() {
        return null;
    }

    @Override
    public Stream<TyVar> fv() {
        return Stream.empty();
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

        throw new RuntimeException("Cannot unify " + this + " and " + other);
    }

    @Override
    public Type eval(Map<TyVar, ? extends Type> env) {
        return this;
    }

    @Override
    public String toString() {
        return "*";
    }
}

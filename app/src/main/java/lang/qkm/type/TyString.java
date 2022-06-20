package lang.qkm.type;

import java.util.*;
import java.util.stream.*;
import lang.qkm.match.CtorSet;

public enum TyString implements Type, CtorSet {

    INSTANCE;

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
    public CtorSet getCtorSet() {
        return this;
    }

    @Override
    public String toString() {
        return "string";
    }

    @Override
    public Optional<Boolean> sameSize(int sz) {
        return Optional.of(false);
    }

    @Override
    public boolean spannedBy(Collection<?> c) {
        return false;
    }

    @Override
    public List<? extends Type> getArgs(Object id) {
        return List.of();
    }

    @Override
    public boolean isComplete() {
        return false;
    }
}

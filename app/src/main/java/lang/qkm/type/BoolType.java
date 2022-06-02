package lang.qkm.type;

import java.util.*;
import java.util.stream.*;
import lang.qkm.match.CtorSet;

public enum BoolType implements Type, CtorSet {

    INSTANCE;

    @Override
    public Type get() {
        return this;
    }

    @Override
    public Type expand() {
        return this;
    }

    @Override
    public Stream<VarType> fv() {
        return Stream.empty();
    }

    @Override
    public Type replace(Map<VarType, ? extends Type> map) {
        return this;
    }

    @Override
    public void unify(Type other) {
        other = other.get();

        if (other == this)
            return;
        if (other instanceof VarType) {
            ((VarType) other).set(this);
            return;
        }

        throw new RuntimeException("Cannot unify " + this + " and " + other);
    }

    @Override
    public CtorSet getCtorSet() {
        return this;
    }

    @Override
    public String toString() {
        return "bool";
    }

    // CtorSet stuff...

    @Override
    public Optional<Boolean> sameSize(int sz) {
        return Optional.of(sz == 2); // true and false
    }

    @Override
    public boolean spannedBy(Collection<?> c) {
        return c.contains(true) && c.contains(false);
    }

    @Override
    public List<Type> getArgs(Object id) {
        // both true and false do not take arguments
        return List.of();
    }
}

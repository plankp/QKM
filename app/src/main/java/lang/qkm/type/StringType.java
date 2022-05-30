package lang.qkm.type;

import java.util.*;
import java.util.stream.*;

public enum StringType implements Type {

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
    public String toString() {
        return "string";
    }
}

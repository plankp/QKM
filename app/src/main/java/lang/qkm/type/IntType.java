package lang.qkm.type;

import java.util.*;
import java.util.stream.*;

public final class IntType implements Type {

    public final int bits;

    public IntType(int bits) {
        if (bits < 1)
            throw new RuntimeException("iN type needs at least one bit");
        this.bits = bits;
    }

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

        if (this.equals(other))
            return;
        if (other instanceof VarType) {
            ((VarType) other).set(this);
            return;
        }

        throw new RuntimeException("Cannot unify " + this + " and " + other);
    }

    @Override
    public String toString() {
        return "i" + this.bits;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(this.bits);
    }

    @Override
    public boolean equals(Object k) {
        if (k == this)
            return true;
        if (!(k instanceof IntType))
            return false;

        final IntType ty = (IntType) k;
        return this.bits == ty.bits;
    }
}

package lang.qkm.type;

import java.util.*;
import java.util.stream.*;

public final class FuncType implements Type {

    public final Type arg;
    public final Type ret;

    public FuncType(Type arg, Type ret) {
        this.arg = arg;
        this.ret = ret;
    }

    @Override
    public Type get() {
        return this;
    }

    @Override
    public Type expand() {
        return new FuncType(this.arg.expand(), this.ret.expand());
    }

    @Override
    public Stream<VarType> fv() {
        return Stream.of(this.arg, this.ret).flatMap(Type::fv);
    }

    @Override
    public Type replace(Map<VarType, ? extends Type> map) {
        return new FuncType(this.arg.replace(map), this.ret.replace(map));
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

        if (!(other instanceof FuncType))
            throw new RuntimeException("Cannot unify " + this + " and " + other);

        final FuncType func = (FuncType) other;
        this.arg.unify(func.arg);
        this.ret.unify(func.ret);
    }

    @Override
    public String toString() {
        return this.arg instanceof FuncType
                ? "(" + this.arg + ") -> " + this.ret
                : this.arg + " -> " + this.ret;
    }
}

package lang.qkm.type;

import java.util.*;
import java.util.stream.*;

public final class VarType implements Type {

    public final String name;

    private Type ref;

    public VarType(String name) {
        this.name = name;
    }

    public void set(Type ref) {
        if (this.ref != null)
            throw new IllegalStateException("Illegal set on bounded type variable");
        if (ref.fv().anyMatch(this::equals))
            throw new IllegalStateException("Illegal recursive type");

        this.ref = ref;
    }

    @Override
    public Type get() {
        if (this.ref == null)
            return this;
        if (!(this.ref instanceof VarType))
            return this.ref;

        // apply path compression
        final VarType chain = (VarType) this.ref;
        final Type peak = chain.get();
        this.ref = peak;
        return peak;
    }

    @Override
    public Type expand() {
        final Type peak = this.get();
        return peak == this ? this : peak.expand();
    }

    @Override
    public Stream<VarType> fv() {
        final Type t = this.get();
        return t instanceof VarType ? Stream.of((VarType) t) : t.fv();
    }

    @Override
    public Type replace(Map<VarType, ? extends Type> map) {
        final Type t = this.get();
        if (!(t instanceof VarType))
            return t.replace(map);

        final Type k = map.get(t);
        return k != null ? k : t;
    }

    @Override
    public void unify(Type other) {
        this.get();
        other = other.get();

        if (other == this)
            return;
        if (this.ref == null)
            this.set(other);
        else
            this.ref.unify(other);
    }

    @Override
    public String toString() {
        final Type t = this.get();
        return t instanceof VarType ? ((VarType) t).name : t.toString();
    }
}

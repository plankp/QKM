package lang.qkm.type;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.*;

public final class VarType implements Type {

    public final BigInteger key;

    public VarType(BigInteger key) {
        this.key = key;
    }

    @Override
    public String toString() {
        return "'" + this.key;
    }

    @Override
    public Stream<VarType> collectVars() {
        return Stream.of(this);
    }

    @Override
    public Type replace(Map<VarType, Type> m) {
        return m.getOrDefault(this, this);
    }

    @Override
    public Type getCompress(Map<BigInteger, Type> m) {
        Type t = m.get(this.key);
        if (t == null)
            return this;
        if (t instanceof VarType) {
            t = ((VarType) t).getCompress(m);
            m.put(this.key, t);
        }
        return t;
    }

    @Override
    public Type expand(Map<BigInteger, Type> m) {
        final Type t = this.getCompress(m);
        if (t == this)
            return this;
        return t.expand(m);
    }

    @Override
    public int hashCode() {
        return this.key.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (!(obj instanceof VarType))
            return false;

        final VarType ty = (VarType) obj;
        return this.key.equals(ty.key);
    }
}

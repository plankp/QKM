package lang.qkm.type;

import java.math.BigInteger;
import java.util.*;

public final class FuncType implements Type {

    public final Type arg;
    public final Type ret;

    public FuncType(Type arg, Type ret) {
        this.arg = arg;
        this.ret = ret;
    }

    @Override
    public String toString() {
        if (this.arg instanceof FuncType)
            return "(" + this.arg + ") -> " + this.ret;
        return this.arg + " -> " + this.ret;
    }

    @Override
    public Set<VarType> collectVars() {
        final Set<VarType> s1 = this.arg.collectVars();
        final Set<VarType> s2 = this.ret.collectVars();
        if (s1.isEmpty())
            return s2;
        if (s2.isEmpty())
            return s1;

        final HashSet<VarType> s = new HashSet<>(s1);
        s.addAll(s2);
        return s;
    }

    @Override
    public Type replace(Map<VarType, Type> m) {
        final Type a = this.arg.replace(m);
        final Type r = this.ret.replace(m);
        return a == this.arg && r == this.ret
                ? this
                : new FuncType(a, r);
    }

    @Override
    public Type expand(Map<BigInteger, Type> m) {
        final Type a = this.arg.expand(m);
        final Type r = this.ret.expand(m);
        return a == this.arg && r == this.ret
                ? this
                : new FuncType(a, r);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.arg, this.ret);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (!(obj instanceof FuncType))
            return false;

        final FuncType ty = (FuncType) obj;
        return this.arg.equals(ty.arg) && this.ret.equals(ty.ret);
    }
}

package lang.qkm.type;

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
    public boolean contains(VarType vt) {
        return this.arg.contains(vt) || this.ret.contains(vt);
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

package lang.qkm.type;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.*;

public final class FixNum implements Type {

    public final Type fix;
    public final Type tail;

    public FixNum(Type fix, Type tail) {
        this.fix = fix;
        this.tail = tail;
    }

    @Override
    public String toString() {
        return "Num (" + this.fix + ") => " + this.tail;
    }

    @Override
    public Stream<VarType> collectVars() {
        return Stream.concat(this.fix.collectVars(), this.tail.collectVars());
    }

    @Override
    public Type replace(Map<VarType, Type> m) {
        final Type a = this.fix.replace(m);
        final Type r = this.tail.replace(m);
        return a == this.fix && r == this.tail
                ? this
                : new FixNum(a, r);
    }

    @Override
    public Type expand(Map<BigInteger, Type> m) {
        final Type a = this.fix.expand(m);
        final Type r = this.tail.expand(m);
        return a == this.fix && r == this.tail
                ? this
                : new FixNum(a, r);
    }
}

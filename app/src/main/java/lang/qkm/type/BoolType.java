package lang.qkm.type;

import java.util.*;
import java.math.BigInteger;
import lang.qkm.util.Range;

public enum BoolType implements Type, Range<Boolean> {

    INSTANCE;

    @Override
    public String toString() {
        return "bool";
    }

    @Override
    public BigInteger size() {
        return BigInteger.valueOf(2);
    }

    @Override
    public boolean contains(Object o) {
        return o instanceof Boolean;
    }

    @Override
    public Iterator<Boolean> iterator() {
        return List.of(true, false).iterator();
    }
}

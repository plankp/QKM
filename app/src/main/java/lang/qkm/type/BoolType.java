package lang.qkm.type;

import java.util.*;

public enum BoolType implements ClosedType {

    INSTANCE;

    @Override
    public String toString() {
        return "bool";
    }

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

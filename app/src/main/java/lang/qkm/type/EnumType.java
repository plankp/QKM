package lang.qkm.type;

import java.util.*;

public final class EnumType implements ClosedType {

    public final String name;
    public final Map<String, Type> cases;

    public EnumType(String name, Map<String, Type> cases) {
        this.name = name;
        this.cases = cases;
    }

    @Override
    public String toString() {
        return "enum " + this.name;
    }

    @Override
    public Optional<Boolean> sameSize(int sz) {
        final int refsz = this.cases.size();
        if (0 <= refsz && refsz < Integer.MAX_VALUE)
            return Optional.of(sz == refsz);

        // size might be capped, so use spans instead.
        return Optional.empty();
    }

    @Override
    public boolean spannedBy(Collection<?> c) {
        return c.containsAll(this.cases.keySet());
    }

    @Override
    public List<Type> getArgs(Object id) {
        // here we assume id is valid
        return List.of(this.cases.get(id));
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.name, this.cases);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (!(obj instanceof EnumType))
            return false;

        final EnumType ty = (EnumType) obj;
        return this.name.equals(ty.name) && this.cases.equals(ty.cases);
    }
}

package lang.qkm.type;

import java.util.*;

public final class EnumType implements Type {

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

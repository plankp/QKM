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

    // Identity equals and hash code for now
}

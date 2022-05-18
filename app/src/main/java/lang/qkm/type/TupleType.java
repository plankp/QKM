package lang.qkm.type;

import java.util.*;
import java.util.stream.*;

public final class TupleType implements Type {

    public final List<Type> elements;

    public TupleType(List<Type> elements) {
        this.elements = elements;
    }

    @Override
    public String toString() {
        return this.elements.stream()
                .map(Object::toString)
                .collect(Collectors.joining(", ", "(", ")"));
    }

    @Override
    public int hashCode() {
        return this.elements.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (!(obj instanceof TupleType))
            return false;

        final TupleType ty = (TupleType) obj;
        return this.elements.equals(ty.elements);
    }
}

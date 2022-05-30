package lang.qkm.type;

import java.util.*;
import java.util.stream.*;

public final class TupleType implements Type {

    public final List<Type> elements;

    public TupleType(List<Type> elements) {
        this.elements = elements;
    }

    @Override
    public Type get() {
        return this;
    }

    @Override
    public Type expand() {
        return new TupleType(this.elements.stream()
                .map(Type::expand)
                .collect(Collectors.toList()));
    }

    @Override
    public Stream<VarType> fv() {
        return this.elements.stream().flatMap(Type::fv);
    }

    @Override
    public Type replace(Map<VarType, ? extends Type> map) {
        return new TupleType(this.elements.stream()
                .map(t -> t.replace(map))
                .collect(Collectors.toList()));
    }

    @Override
    public void unify(Type other) {
        other = other.get();

        if (other == this)
            return;
        if (other instanceof VarType) {
            ((VarType) other).set(this);
            return;
        }

        error: {
            if (!(other instanceof TupleType))
                break error;

            final TupleType tuple = (TupleType) other;
            if (this.elements.isEmpty() && tuple.elements.isEmpty())
                return;
            if (tuple.elements.isEmpty())
                break error;

            if (this.elements.size() != tuple.elements.size())
                break error;

            final Iterator<Type> it1 = this.elements.iterator();
            final Iterator<Type> it2 = tuple.elements.iterator();
            while (it1.hasNext() && it2.hasNext())
                it1.next().unify(it2.next());
            if (it1.hasNext() == it2.hasNext())
                return;
        }

        throw new RuntimeException("Cannot unify " + this + " and " + other);
    }

    @Override
    public String toString() {
        return this.elements.stream()
                .map(Object::toString)
                .collect(Collectors.joining(", ", "(", ")"));
    }
}

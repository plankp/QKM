package lang.qkm.type;

import java.util.*;
import java.util.stream.*;

public final class TupleType implements ClosedType {

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
    public boolean contains(VarType vt) {
        for (final Type t : this.elements)
            if (t.contains(vt))
                return true;
        return false;
    }

    @Override
    public Type replace(Map<VarType, Type> m) {
        boolean changed = false;
        final ArrayList<Type> list = new ArrayList<>(this.elements);
        final ListIterator<Type> it = list.listIterator();
        while (it.hasNext()) {
            final Type t = it.next();
            final Type r = t.replace(m);
            changed |= t == r;
            it.set(r);
        }

        return !changed
                ? this
                : new TupleType(Collections.unmodifiableList(list));
    }

    @Override
    public Optional<Boolean> sameSize(int sz) {
        return Optional.of(sz == 1);
    }

    @Override
    public boolean spannedBy(Collection<?> c) {
        return c.contains(TupleType.class);
    }

    @Override
    public List<Type> getArgs(Object id) {
        return this.elements;
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

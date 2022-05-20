package lang.qkm.type;

import java.math.BigInteger;
import java.util.*;

public interface Type {

    public default Type getCompress(Map<BigInteger, Type> m) {
        return this;
    }

    public static Type unify(Type a, Type b, Map<BigInteger, Type> _m) {
        // allow path compression to modify the original map because we're not
        // adding new information, we're just simplifying the map.
        a = a.getCompress(_m);
        b = b.getCompress(_m);

        if (a == b)
            return a;

        // make modifications to the copy.
        final HashMap<BigInteger, Type> m = new HashMap<>(_m);
        final Type t = impl(a, b, m);

        // and update the original map only if unification was success
        if (t != null)
            _m.putAll(m);
        return t;
    }

    private static Type impl(Type a, Type b, HashMap<BigInteger, Type> m) {
        a = a.getCompress(m);
        b = b.getCompress(m);
        if (a == b)
            // handles things like enums and bools
            return a;

        if (b instanceof VarType) {
            final Type t = a;
            a = b;
            b = t;
        }

        if (a instanceof VarType) {
            if (b instanceof VarType) {
                final int cmp = ((VarType) a).key.compareTo(((VarType) b).key);
                if (cmp == 0)
                    // both are already equivalent, so just return one.
                    return a;

                if (cmp > 0) {
                    // otherwise always make sure the key of a is smaller to
                    // prevent cycles from forming.
                    final Type t = a;
                    a = b;
                    b = t;
                }
            }

            m.put(((VarType) a).key, b);
            return b;
        }

        if (a instanceof FuncType && b instanceof FuncType) {
            final FuncType fa = (FuncType) a;
            final FuncType fb = (FuncType) b;

            final Type arg = impl(fa.arg, fb.arg, m);
            if (arg == null)
                return null;
            final Type ret = impl(fa.ret, fb.ret, m);
            if (ret == null)
                return null;
            return new FuncType(arg, ret);
        }

        if (a instanceof TupleType && b instanceof TupleType) {
            final TupleType ta = (TupleType) a;
            final TupleType tb = (TupleType) b;

            if (ta.elements.size() != tb.elements.size())
                return null;
            final ArrayList<Type> elements = new ArrayList<>(ta.elements.size());
            final Iterator<Type> ia = ta.elements.iterator();
            final Iterator<Type> ib = tb.elements.iterator();
            while (ia.hasNext() && ib.hasNext()) {
                final Type u = impl(ia.next(), ib.next(), m);
                if (u == null)
                    return null;
                elements.add(u);
            }
            return ia.hasNext() != ib.hasNext()
                    ? null // just in case size > INTMAX
                    : new TupleType(Collections.unmodifiableList(elements));
        }

        if (a instanceof IntType && a.equals(b))
            return a;

        return null;
    }
}

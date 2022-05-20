package lang.qkm.util;

import java.math.BigInteger;
import java.util.Collection;

public interface Range<E> extends Iterable<E> {

    public BigInteger size();

    public boolean contains(Object o);

    public default boolean containsAll(Collection<?> c) {
        for (final Object e : c)
            if (!this.contains(e))
                return false;
        return true;
    }
}

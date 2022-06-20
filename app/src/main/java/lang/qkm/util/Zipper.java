package lang.qkm.util;

import java.util.Iterator;
import java.util.function.BiFunction;

public final class Zipper<S, T, U> implements Iterator<U> {

    public final Iterator<? extends S> it1;
    public final Iterator<? extends T> it2;
    public final BiFunction<? super S, ? super T, ? extends U> mapper;

    public Zipper(Iterator<? extends S> it1, Iterator<? extends T> it2, BiFunction<? super S, ? super T, ? extends U> mapper) {
        this.it1 = it1;
        this.it2 = it2;
        this.mapper = mapper;
    }

    @Override
    public boolean hasNext() {
        return this.it1.hasNext() && this.it2.hasNext();
    }

    @Override
    public U next() {
        return this.mapper.apply(this.it1.next(), this.it2.next());
    }
}

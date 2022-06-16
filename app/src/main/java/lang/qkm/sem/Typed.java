package lang.qkm.sem;

import lang.qkm.type.*;

public final class Typed<V> {

    public final V value;
    public final Type type;

    public Typed(V value, Type type) {
        this.value = value;
        this.type = type;
    }

    @Override
    public String toString() {
        return this.value + " : " + this.type;
    }
}

package lang.qkm.type;

import java.util.*;

public final class PolyType implements Type {

    // we don't override anything type specific because polytype is weird.

    public final Set<VarType> quant;
    public final Type base;

    public PolyType(Set<VarType> quant, Type base) {
        this.quant = Collections.unmodifiableSet(quant);
        this.base = base;
    }

    @Override
    public String toString() {
        if (this.quant.isEmpty())
            return this.base.toString();

        final StringBuilder sb = new StringBuilder();
        for (final VarType tv : this.quant)
            sb.append('∀').append(tv);
        return sb.append('.').append(this.base).toString();
    }
}

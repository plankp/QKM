package lang.qkm.sem;

import java.math.BigInteger;
import java.util.*;
import lang.qkm.type.*;

public final class TypeState {

    private BigInteger counter = BigInteger.ZERO;

    public VarType freshType() {
        final BigInteger k = this.counter.add(BigInteger.ONE);
        this.counter = k;
        return VarType.of("'" + k);
    }

    public VarType freshType(String name) {
        return VarType.of(name);
    }

    public VarType freshPoly(String name) {
        return VarType.poly(name);
    }

    public Type inst(PolyType p) {
        final Map<VarType, VarType> map = new HashMap<>();
        for (final VarType t : p.quants)
            map.put(t, this.freshType());

        return p.body.replace(map);
    }

    public PolyType gen(Type t, List<VarType> quants) {
        if (quants.isEmpty())
            return new PolyType(List.of(), t.expand());

        final Iterator<VarType> it = quants.iterator();
        final Map<VarType, VarType> m = new HashMap<>();
        BigInteger id = null;
        for (;;) {
            final String name = id == null ? "t" : "t" + id;
            m.put(it.next(), this.freshPoly(name));

            if (!it.hasNext())
                return new PolyType(
                        Collections.unmodifiableList(new ArrayList<>(m.values())),
                        t.replace(m));

            id = id == null ? BigInteger.ONE : id.add(BigInteger.ONE);
        }
    }
}

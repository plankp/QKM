package lang.qkm.sem;

import java.math.BigInteger;
import java.util.*;
import lang.qkm.type.*;

public final class TypeState {

    private BigInteger counter = BigInteger.ZERO;

    public TyVar freshType() {
        final BigInteger k = this.counter.add(BigInteger.ONE);
        this.counter = k;
        return TyVar.unifiable("'" + k);
    }

    public TyVar freshType(String name) {
        return TyVar.unifiable(name);
    }

    public TyVar freshPoly(String name) {
        return TyVar.grounded(name);
    }

    public Type inst(Type p) {
        if (!(p instanceof TyPoly))
            // it's already instantiated
            return p;

        final Map<TyVar, TyVar> map = new HashMap<>();
        while (p instanceof TyPoly) {
            final TyPoly f = (TyPoly) p;
            map.put(f.arg, this.freshType());
            p = f.body;
        }

        return p.eval(map);
    }

    public static Type gen(Type t, Iterable<TyVar> quants) {
        final Iterator<TyVar> it = quants.iterator();
        if (!it.hasNext())
            // nothing to generalize, just expand the type
            return t.eval(Map.of());

        final LinkedList<TyVar> args = new LinkedList<>();
        final Map<TyVar, TyVar> m = new HashMap<>();
        BigInteger id = null;
        for (;;) {
            final TyVar arg = TyVar.grounded(id == null ? "t" : "t" + id);
            args.push(arg);
            m.put(it.next(), arg);

            if (!it.hasNext())
                break;

            id = id == null ? BigInteger.ONE : id.add(BigInteger.ONE);
        }

        t = t.eval(m);
        while (!args.isEmpty())
            t = new TyPoly(args.pop(), t);
        return t;
    }
}

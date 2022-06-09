package lang.qkm.type;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.*;
import lang.qkm.match.CtorSet;

public final class TyInt implements Type, CtorSet {

    public final int bits;

    public TyInt(int bits) {
        if (bits < 1)
            throw new RuntimeException("iN type needs at least one bit");
        this.bits = bits;
    }

    public BigInteger signed(BigInteger value) {
        value = this.unsigned(value);
        if (value.bitLength() < this.bits)
            return value;

        // sign bit is set, need make it negative
        return value.subtract(BigInteger.ONE.shiftLeft(this.bits));
    }

    public BigInteger unsigned(BigInteger value) {
        return BigInteger.ONE
                .shiftLeft(this.bits)
                .subtract(BigInteger.ONE)
                .and(value);
    }

    @Override
    public Type unwrap() {
        return this;
    }

    @Override
    public TyApp unapply() {
        return null;
    }

    @Override
    public Stream<TyVar> fv() {
        return Stream.empty();
    }

    @Override
    public void unify(Type other) {
        other = other.unwrap();

        if (this.equals(other))
            return;
        if (other instanceof TyVar) {
            ((TyVar) other).set(this);
            return;
        }

        throw new RuntimeException("Cannot unify " + this + " and " + other);
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(this.bits);
    }

    @Override
    public boolean equals(Object k) {
        if (k == this)
            return true;
        if (!(k instanceof TyInt))
            return false;

        final TyInt ty = (TyInt) k;
        return this.bits == ty.bits;
    }

    @Override
    public Type eval(Map<TyVar, ? extends Type> env) {
        return this;
    }

    @Override
    public CtorSet getCtorSet() {
        return this;
    }

    @Override
    public String toString() {
        return "i" + this.bits;
    }

    @Override
    public Optional<Boolean> sameSize(int sz) {
        // bits need to be capped at around 32 to 63 to make sure the shift
        // doesn't overflow.
        return Optional.of(this.bits < 60 && sz == (1L << this.bits));
    }

    @Override
    public boolean spannedBy(Collection<?> c) {
        // literally iterate through all possible *signed* integer values and
        // see if the supplied collection contains all of them.
        final BigInteger max = BigInteger.ONE.shiftLeft(this.bits - 1);

        BigInteger i = max.negate();
        for (;;) {
            if (!c.contains(i))
                return false;
            i = i.add(BigInteger.ONE);
            if (max.equals(i))
                return true;
        }
    }

    @Override
    public List<? extends Type> getArgs(Object id) {
        return List.of();
    }
}

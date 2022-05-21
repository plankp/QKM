package lang.qkm.type;

import java.math.BigInteger;
import java.util.*;

public final class IntType implements ClosedType {

    public final int bits;

    public IntType(int bits) {
        if (bits < 1)
            throw new IllegalArgumentException("Integer must have at least one bit");

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
    public List<Type> getArgs(Object id) {
        // all int values do not take arguments
        return List.of();
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(this.bits);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof IntType))
            return false;

        final IntType ty = (IntType) obj;
        return this.bits == ty.bits;
    }
}

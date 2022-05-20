package lang.qkm.type;

import java.math.BigInteger;
import java.util.*;
import lang.qkm.util.Range;

public final class IntType implements Type, Range<BigInteger> {

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
    public BigInteger size() {
        return BigInteger.ONE.shiftLeft(this.bits);
    }

    @Override
    public boolean contains(Object o) {
        if (!(o instanceof BigInteger))
            return false;

        return o.equals(IntType.this.signed((BigInteger) o));
    }

    @Override
    public Iterator<BigInteger> iterator() {
        return new Iterator<>() {

            private final BigInteger lastPlusOne = BigInteger.ONE.shiftLeft(bits - 1);
            private BigInteger i = lastPlusOne.negate().subtract(BigInteger.ONE);

            @Override
            public boolean hasNext() {
                return this.i.compareTo(this.lastPlusOne) < 0;
            }

            @Override
            public BigInteger next() {
                if (!this.hasNext())
                    throw new NoSuchElementException();

                final BigInteger k = this.i.add(BigInteger.ONE);
                this.i = k;
                return k;
            }
        };
    }

    @Override
    public String toString() {
        return "i" + this.bits;
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

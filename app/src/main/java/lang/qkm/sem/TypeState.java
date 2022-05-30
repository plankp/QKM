package lang.qkm.sem;

import java.math.BigInteger;
import lang.qkm.type.*;

public final class TypeState {

    private BigInteger counter = BigInteger.ZERO;

    public VarType freshType() {
        final BigInteger k = this.counter.add(BigInteger.ONE);
        this.counter = k;
        return new VarType("'" + k);
    }
}

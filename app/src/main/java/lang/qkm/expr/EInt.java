package lang.qkm.expr;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.*;
import lang.qkm.type.TyInt;

public final class EInt implements Expr {

    public final BigInteger value;
    public final TyInt type;

    public EInt(BigInteger value, TyInt type) {
        this.value = type.signed(value);
        this.type = type;
    }

    @Override
    public Stream<EVar> fv() {
        return Stream.empty();
    }

    @Override
    public String toString() {
        return this.value.toString() + this.type;
    }
}

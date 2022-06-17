package lang.qkm.expr;

import java.util.*;
import java.util.stream.*;

public final class ETup implements Expr {

    public final List<? extends Expr> elements;

    public ETup(List<? extends Expr> elements) {
        this.elements = elements;
    }

    @Override
    public <R> R accept(Visitor<R> v) {
        return v.visitETup(this);
    }

    @Override
    public Stream<EVar> fv() {
        return this.elements.stream().flatMap(Expr::fv);
    }

    @Override
    public boolean isAtom() {
        return this.elements.isEmpty();
    }

    @Override
    public String toString() {
        if (this.elements.isEmpty())
            return "'()";

        return this.elements.stream()
                .map(Object::toString)
                .collect(Collectors.joining(" ", "'(", ")"));
    }
}

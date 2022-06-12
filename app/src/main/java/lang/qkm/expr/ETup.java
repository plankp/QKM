package lang.qkm.expr;

import java.util.*;
import java.util.stream.*;

public final class ETup implements Expr {

    public final List<Expr> elements;

    public ETup(List<Expr> elements) {
        this.elements = elements;
    }

    @Override
    public Stream<EVar> fv() {
        return this.elements.stream().flatMap(Expr::fv);
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

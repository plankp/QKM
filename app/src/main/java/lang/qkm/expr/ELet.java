package lang.qkm.expr;

import java.util.*;
import java.util.stream.*;

public final class ELet implements Expr {

    public final EVar bind;
    public final Expr value;
    public final Expr body;

    public ELet(EVar bind, Expr value, Expr body) {
        this.bind = bind;
        this.value = value;
        this.body = body;
    }

    @Override
    public <R> R accept(Visitor<R> v) {
        return v.visitELet(this);
    }

    @Override
    public Stream<EVar> fv() {
        return Stream.concat(this.value.fv(), this.body.fv()
                .filter(v -> !this.bind.equals(v)));
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append("(let ((")
                .append(this.bind).append(' ').append(this.value)
                .append(")) ").append(this.body).append(')').toString();
    }
}

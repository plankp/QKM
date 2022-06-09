package lang.qkm.type;

import java.util.*;
import java.util.stream.*;

public final class TyPoly implements Type {

    public final TyVar arg;
    public final Type body;

    public TyPoly(TyVar arg, Type body) {
        this.arg = arg;
        this.body = body;
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
        return this.body.fv().filter(k -> k != this.arg);
    }

    @Override
    public void unify(Type other) {
        other = other.unwrap();
        if (other == this)
            return;

        throw new RuntimeException("Cannot unify " + this + " and " + other);
    }

    @Override
    public Type eval(Map<TyVar, ? extends Type> env) {
        if (env.containsKey(this.arg)) {
            env = new HashMap<>(env);
            env.remove(this.arg);
        }

        return new TyPoly(this.arg, this.body.eval(env));
    }

    @Override
    public String toString() {
        if (!(this.body instanceof TyPoly))
            return "(" + this.arg + ". " + this.body + ")";

        final StringBuilder sb = new StringBuilder();
        sb.append('(');

        TyPoly acc = this;
        for (;;) {
            sb.append(acc.arg);
            if (!(acc.body instanceof TyPoly))
                return sb.append(". ").append(acc.body).append(')')
                        .toString();

            acc = (TyPoly) acc.body;
            sb.append(' ');
        }
    }
}

package lang.qkm.type;

import java.util.*;
import java.util.stream.*;
import lang.qkm.match.CtorSet;

public final class TyTup implements Type, CtorSet {

    public final List<? extends Type> elements;

    public TyTup(List<? extends Type> elements) {
        this.elements = elements;
    }

    @Override
    public Type unwrap() {
        return this;
    }

    @Override
    public TyApp unapply() {
        if (this.elements.isEmpty())
            return null;

        // eta expand the constructor into an application of a polytype.
        final List<TyVar> exp = this.elements.stream()
                .map(p -> new TyVar())
                .collect(Collectors.toList());

        Type base = new TyTup(Collections.unmodifiableList(exp));
        for (int i = exp.size(); i-- > 0; )
            base = new TyPoly(exp.get(i), base);
        for (final Type arg : this.elements)
            base = new TyApp(base, arg);
        return (TyApp) base;
    }

    @Override
    public Stream<TyVar> fv() {
        return this.elements.stream().flatMap(Type::fv);
    }

    @Override
    public void unify(Type other) {
        other = other.unwrap();

        if (other == this)
            return;
        if (other instanceof TyVar) {
            ((TyVar) other).set(this);
            return;
        }
        if (other instanceof TyApp) {
            other.unify(this);
            return;
        }

        error: {
            if (!(other instanceof TyTup))
                break error;

            final TyTup tup = (TyTup) other;
            if (this.elements.size() != tup.elements.size())
                break error;

            final Iterator<? extends Type> it1 = this.elements.iterator();
            final Iterator<? extends Type> it2 = tup.elements.iterator();
            while (it1.hasNext() && it2.hasNext())
                it1.next().unify(it2.next());
            if (it1.hasNext() == it2.hasNext())
                return;
        }

        throw new RuntimeException("Cannot unify " + this + " and " + other);
    }

    @Override
    public Type eval(Map<TyVar, ? extends Type> env) {
        return new TyTup(this.elements.stream()
                .map(t -> t.eval(env))
                .collect(Collectors.toList()));
    }

    @Override
    public CtorSet getCtorSet() {
        return this;
    }

    @Override
    public String toString() {
        return this.elements.stream()
                .map(Object::toString)
                .collect(Collectors.joining(", ", "(", ")"));
    }

    @Override
    public Optional<Boolean> sameSize(int sz) {
        return Optional.of(sz == 1);
    }

    @Override
    public boolean spannedBy(Collection<?> c) {
        return c.contains(TyTup.class);
    }

    @Override
    public List<? extends Type> getArgs(Object id) {
        return this.elements;
    }
}

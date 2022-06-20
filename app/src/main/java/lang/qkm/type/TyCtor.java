package lang.qkm.type;

import java.util.*;
import java.util.stream.*;
import lang.qkm.match.CtorSet;

public final class TyCtor implements Type, CtorSet {

    public static final class Template {

        public final String name;
        public final List<TyVar> quants;
        public final Map<String, List<Type>> cases;

        public Template(String name, List<TyVar> quants, Map<String, List<Type>> cases) {
            this.name = name;
            this.quants = quants;
            this.cases = cases;
        }
    }

    public final Template template;
    public final List<? extends Type> args;

    public TyCtor(Template template, List<? extends Type> args) {
        this.template = template;
        this.args = args;
    }

    @Override
    public Type unwrap() {
        return this;
    }

    @Override
    public TyApp unapply() {
        if (this.args.isEmpty())
            return null;

        // eta expand the constructor into an application of a polytype.
        final List<TyVar> exp = this.args.stream()
                .map(p -> new TyVar())
                .collect(Collectors.toList());

        Type base = new TyCtor(this.template, Collections.unmodifiableList(exp));
        for (int i = exp.size(); i-- > 0; )
            base = new TyPoly(exp.get(i), base);
        for (final Type arg : this.args)
            base = new TyApp(base, arg);
        return (TyApp) base;
    }

    @Override
    public Stream<TyVar> fv() {
        return this.args.stream().flatMap(Type::fv);
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
            if (!(other instanceof TyCtor))
                break error;

            final TyCtor ctor = (TyCtor) other;
            if (this.template != ctor.template)
                break error;
            if (this.args.size() != ctor.args.size())
                break error;

            final Iterator<? extends Type> it1 = this.args.iterator();
            final Iterator<? extends Type> it2 = ctor.args.iterator();
            while (it1.hasNext() && it2.hasNext())
                it1.next().unify(it2.next());
            if (it1.hasNext() == it2.hasNext())
                return;
        }

        throw new RuntimeException("Cannot unify " + this + " and " + other);
    }

    @Override
    public Type eval(Map<TyVar, ? extends Type> env) {
        return new TyCtor(this.template, this.args.stream()
                .map(t -> t.eval(env))
                .collect(Collectors.toList()));
    }

    @Override
    public CtorSet getCtorSet() {
        return this;
    }

    @Override
    public String toString() {
        if (this.args.isEmpty())
            return this.template.name;

        return this.args.stream()
                .map(Object::toString)
                .collect(Collectors.joining(" ", "(" + this.template.name + " ", ")"));
    }

    @Override
    public Optional<Boolean> sameSize(int sz) {
        final int refsz = this.template.cases.size();
        if (0 <= refsz && refsz < Integer.MAX_VALUE)
            return Optional.of(sz == refsz);

        // size might be capped, so use spans instead.
        return Optional.empty();
    }

    @Override
    public Object missingCase(Collection<?> c) {
        for (final String k : this.template.cases.keySet())
            if (!c.contains(k))
                return k;

        return null;
    }

    @Override
    public List<? extends Type> getArgs(Object id) {
        final List<Type> t = this.template.cases.get(id);
        if (t.isEmpty() || this.template.quants.isEmpty())
            return t;

        final Map<TyVar, Type> m = new HashMap<>();
        final Iterator<TyVar> q = this.template.quants.iterator();
        final Iterator<? extends Type> r = this.args.iterator();
        while (q.hasNext() && r.hasNext())
            m.put(q.next(), r.next());

        return t.stream()
                .map(v -> v.eval(m))
                .collect(Collectors.toList());
    }
}


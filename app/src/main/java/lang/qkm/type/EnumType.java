package lang.qkm.type;

import java.util.*;
import java.util.stream.*;

public final class EnumType implements Type {

    public static final class Template {

        public final String name;
        public final List<VarType> quants;
        public final Map<String, List<Type>> cases;

        public Template(String name, List<VarType> quants, Map<String, List<Type>> cases) {
            this.name = name;
            this.quants = quants;
            this.cases = cases;
        }
    }

    public final Template template;
    public final List<? extends Type> args;

    public EnumType(Template template, List<? extends Type> args) {
        this.template = template;
        this.args = args;
    }

    @Override
    public Type get() {
        return this;
    }

    @Override
    public Type expand() {
        return new EnumType(this.template, this.args.stream()
                .map(Type::expand)
                .collect(Collectors.toList()));
    }

    @Override
    public Stream<VarType> fv() {
        return this.args.stream().flatMap(Type::fv);
    }

    @Override
    public Type replace(Map<VarType, ? extends Type> map) {
        return new EnumType(this.template, this.args.stream()
                .map(t -> t.replace(map))
                .collect(Collectors.toList()));
    }

    @Override
    public void unify(Type other) {
        other = other.get();

        if (other == this)
            return;
        if (other instanceof VarType) {
            ((VarType) other).set(this);
            return;
        }

        error: {
            if (!(other instanceof EnumType))
                break error;

            final EnumType ty = (EnumType) other;
            if (this.template != ty.template)
                break error;

            final Iterator<? extends Type> it1 = this.args.iterator();
            final Iterator<? extends Type> it2 = ty.args.iterator();
            while (it1.hasNext() && it2.hasNext())
                it1.next().unify(it2.next());
            if (it1.hasNext() == it2.hasNext())
                return;
        }

        throw new RuntimeException("Cannot unify " + this + " and " + other);
    }

    @Override
    public String toString() {
        if (this.args.isEmpty())
            return this.template.name;

        final StringBuilder sb = new StringBuilder(this.template.name);
        for (final Type arg : this.args)
            sb.append(' ').append(arg);
        return sb.toString();
    }
}

package lang.qkm.type;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.*;

public final class EnumType implements ClosedType {

    public static final class Template {

        public final String name;
        public final List<VarType> quant;
        public final Map<String, Optional<Type>> cases;

        public Template(String name, List<VarType> quant, Map<String, Optional<Type>> cases) {
            this.name = name;
            this.quant = Collections.unmodifiableList(quant);
            this.cases = Collections.unmodifiableMap(cases);
        }

        @Override
        public String toString() {
            return "enum " + this.name + this.quant + " " + this.cases;
        }
    }

    public final Template body;
    public final List<? extends Type> args;

    public EnumType(Template body, List<? extends Type> args) {
        this.body = body;
        this.args = Collections.unmodifiableList(args);
    }

    @Override
    public String toString() {
        return "enum " + this.body.name + this.args;
    }

    @Override
    public Stream<VarType> collectVars() {
        return this.args.stream().flatMap(Type::collectVars);
    }

    @Override
    public Type replace(Map<VarType, Type> m) {
        boolean changed = false;
        final ArrayList<Type> list = new ArrayList<>(this.args);
        final ListIterator<Type> it = list.listIterator();
        while (it.hasNext()) {
            final Type t = it.next();
            final Type r = t.replace(m);
            changed |= t != r;
            it.set(r);
        }

        return !changed
                ? this
                : new EnumType(this.body, list);
    }

    @Override
    public Type expand(Map<BigInteger, Type> m) {
        boolean changed = false;
        final ArrayList<Type> list = new ArrayList<>(this.args);
        final ListIterator<Type> it = list.listIterator();
        while (it.hasNext()) {
            final Type t = it.next();
            final Type r = t.expand(m);
            changed |= t != r;
            it.set(r);
        }

        return !changed
                ? this
                : new EnumType(this.body, list);
    }

    @Override
    public Optional<Boolean> sameSize(int sz) {
        final int refsz = this.body.cases.size();
        if (0 <= refsz && refsz < Integer.MAX_VALUE)
            return Optional.of(sz == refsz);

        // size might be capped, so use spans instead.
        return Optional.empty();
    }

    @Override
    public boolean spannedBy(Collection<?> c) {
        return c.containsAll(this.body.cases.keySet());
    }

    @Override
    public List<Type> getArgs(Object id) {
        // here we assume id is valid
        final Optional<Type> t = this.body.cases.get(id);
        if (t.isEmpty())
            return List.of();
        if (this.body.quant.isEmpty())
            return List.of(t.get());

        final Map<VarType, Type> m = new HashMap<>();
        final Iterator<VarType> q = this.body.quant.iterator();
        final Iterator<? extends Type> r = this.args.iterator();
        while (q.hasNext() && r.hasNext())
            m.put(q.next(), r.next());
        return List.of(t.get().replace(m));
    }
}

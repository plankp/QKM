package lang.qkm.eval;

import java.util.*;
import java.util.stream.*;
import lang.qkm.expr.*;
import lang.qkm.match.*;

public class ASTWalker implements Evaluator, Expr.Visitor<ASTWalker.Computation> {

    public static abstract class Computation {

        public abstract Value force();

        private Computation() { /* restricted subclassing */ }
    }

    public static final class Suspended extends Computation {

        public final Map<EVar, Value> env;
        public final Expr expr;

        public Suspended(final Map<EVar, Value> env, Expr expr) {
            this.env = env;
            this.expr = expr;
        }

        @Override
        public Value force() {
            Suspended thunk = this;
            for (;;) {
                final ASTWalker exec = new ASTWalker();
                exec.env = thunk.env;
                final Computation k = thunk.expr.accept(exec);
                if (k instanceof Value)
                    return (Value) k;

                thunk = (Suspended) k;
            }
        }
    }

    public static abstract class Value extends Computation {

        @Override
        public final Value force() {
            return this;
        }

        public Value unwrap() {
            return this;
        }

        public abstract boolean unpack(Match m, Map<EVar, Value> env);
    }

    public static final class VBool extends Value {

        public final boolean value;

        public VBool(boolean value) {
            this.value = value;
        }

        @Override
        public boolean unpack(Match m, Map<EVar, Value> env) {
            // match must be either a complete match or a decons pattern
            if (m instanceof MatchAll) {
                final MatchAll k = (MatchAll) m;
                if (k.capture != null)
                    env.put(new EVar(k.capture), this);
                return true;
            }

            if (m instanceof MatchBool)
                return this.value == ((MatchBool) m).value;

            return false;
        }

        @Override
        public String toString() {
            return this.value ? "true" : "false";
        }
    }

    public static final class VString extends Value {

        public final String value;

        public VString(String value) {
            this.value = value;
        }

        @Override
        public boolean unpack(Match m, Map<EVar, Value> env) {
            // match must be either a complete match or a decons pattern
            if (m instanceof MatchAll) {
                final MatchAll k = (MatchAll) m;
                if (k.capture != null)
                    env.put(new EVar(k.capture), this);
                return true;
            }

            if (m instanceof MatchString)
                return this.value.equals(((MatchString) m).value);

            return false;
        }

        @Override
        public String toString() {
            return this.value;
        }
    }

    public static final class VInt extends Value {

        public final EInt value;

        public VInt(EInt value) {
            this.value = value;
        }

        @Override
        public boolean unpack(Match m, Map<EVar, Value> env) {
            // match must be either a complete match or a decons pattern
            if (m instanceof MatchAll) {
                final MatchAll k = (MatchAll) m;
                if (k.capture != null)
                    env.put(new EVar(k.capture), this);
                return true;
            }

            if (m instanceof MatchInt)
                return this.value.value.equals(((MatchInt) m).value);

            return false;
        }

        @Override
        public String toString() {
            return this.value.value.toString();
        }
    }

    public static final class VTup extends Value {

        public final List<Value> elements;

        public VTup(List<Value> elements) {
            this.elements = elements;
        }

        @Override
        public boolean unpack(Match m, Map<EVar, Value> env) {
            // match must be either a complete match or a decons pattern
            if (m instanceof MatchAll) {
                final MatchAll k = (MatchAll) m;
                if (k.capture != null)
                    env.put(new EVar(k.capture), this);
                return true;
            }

            if (m instanceof MatchTup) {
                final MatchTup n = (MatchTup) m;
                final Iterator<Value> it1 = this.elements.iterator();
                final Iterator<Match> it2 = n.elements.iterator();
                while (it1.hasNext() && it2.hasNext())
                    if (!it1.next().unpack(it2.next(), env))
                        return false;

                return it1.hasNext() == it2.hasNext();
            }

            return false;
        }

        @Override
        public String toString() {
            if (this.elements.isEmpty())
                return "()";

            return this.elements.stream()
                    .map(Object::toString)
                    .collect(Collectors.joining(", ", "(", ")"));
        }
    }

    public static final class VCtor extends Value {

        public final String id;
        public final List<Value> args;

        public VCtor(String id, List<Value> args) {
            this.id = id;
            this.args = args;
        }

        @Override
        public boolean unpack(Match m, Map<EVar, Value> env) {
            // match must be either a complete match or a decons pattern
            if (m instanceof MatchAll) {
                final MatchAll k = (MatchAll) m;
                if (k.capture != null)
                    env.put(new EVar(k.capture), this);
                return true;
            }

            if (m instanceof MatchCtor) {
                final MatchCtor n = (MatchCtor) m;
                if (!n.ctor.equals(this.id))
                    return false;

                final Iterator<Value> it1 = this.args.iterator();
                final Iterator<Match> it2 = n.args.iterator();
                while (it1.hasNext() && it2.hasNext())
                    if (!it1.next().unpack(it2.next(), env))
                        return false;

                return it1.hasNext() == it2.hasNext();
            }

            return false;
        }

        @Override
        public String toString() {
            if (this.args.isEmpty())
                return this.id;

            return this.args.stream()
                    .map(k -> k instanceof VCtor ? "(" + k + ")" : k.toString())
                    .collect(Collectors.joining(" ", this.id + " ", ""));
        }
    }

    public static final class VLam extends Value {

        public final Map<EVar, Value> env;
        public final ELam f;

        public VLam(Map<EVar, Value> env, ELam f) {
            this.env = env;
            this.f = f;
        }

        @Override
        public boolean unpack(Match m, Map<EVar, Value> env) {
            // only complete matches can match against lambdas
            if (!(m instanceof MatchAll))
                return false;

            final MatchAll k = (MatchAll) m;
            if (k.capture != null)
                env.put(new EVar(k.capture), this);
            return true;
        }

        @Override
        public String toString() {
            return "<fun>";
        }
    }

    public static final class VBox extends Value {

        public Value boxed;

        @Override
        public Value unwrap() {
            if (this.boxed == null)
                throw new IllegalStateException("Unwrapping unbounded value");

            return this.boxed;
        }

        @Override
        public boolean unpack(Match m, Map<EVar, Value> env) {
            if (this.boxed == null)
                throw new IllegalStateException("Unpacking unbounded value");

            return this.boxed.unpack(m, env);
        }

        @Override
        public String toString() {
            return this.boxed == null ? "<undefined>" : this.boxed.toString();
        }
    }

    private Map<EVar, Value> env = new HashMap<>();

    @Override
    public void define(Map<EVar, Expr> defs) {
        // defines are always recursive, but what makes things bit more tricky
        // is the fact that we allow recursive data constructors...

        for (final EVar b : defs.keySet())
            this.env.put(b, new VBox());

        for (final Map.Entry<EVar, Expr> pair : defs.entrySet()) {
            final VBox box = (VBox) this.env.get(pair.getKey());
            if (box.boxed != null)
                throw new IllegalStateException("Invalid binding initialization");

            box.boxed = pair.getValue().accept(this).force();
        }
    }

    @Override
    public void eval(Expr e) {
        System.out.println(e.accept(this).force());
    }

    @Override
    public Value visitEBool(EBool e) {
        return new VBool(e.value);
    }

    @Override
    public Value visitECtor(ECtor e) {
        if (e.args.isEmpty())
            return new VCtor(e.id, List.of());

        return new VCtor(e.id, e.args.stream()
                .map(k -> k.accept(this).force())
                .collect(Collectors.toList()));
    }

    @Override
    public Value visitEInt(EInt e) {
        return new VInt(e);
    }

    @Override
    public Value visitEString(EString e) {
        return new VString(e.value);
    }

    @Override
    public Value visitETup(ETup e) {
        if (e.elements.isEmpty())
            return new VTup(List.of());

        return new VTup(e.elements.stream()
                .map(k -> k.accept(this).force())
                .collect(Collectors.toList()));
    }

    @Override
    public Suspended visitEMatch(EMatch e) {
        final Value value = e.scrutinee.accept(this).force();

        for (final Map.Entry<Match, Expr> pair : e.cases) {
            final Match m = pair.getKey();
            final Map<EVar, Value> cap = new HashMap<>(this.env);
            if (value.unpack(m, cap))
                return new Suspended(cap, pair.getValue());
        }

        throw new RuntimeException("Match failure!");
    }

    @Override
    public Value visitEVar(EVar e) {
        final Value v = this.env.get(e);
        if (v == null)
            throw new RuntimeException("Undeclared variable " + e);

        return v;
    }

    @Override
    public Value visitELam(ELam e) {
        return new VLam(new HashMap<>(this.env), e);
    }

    @Override
    public Suspended visitEApp(EApp e) {
        final VLam f = (VLam) e.f.accept(this).force().unwrap();
        final Value arg = e.arg.accept(this).force();

        f.env.put(f.f.arg, arg);
        return new Suspended(f.env, f.f.body);
    }

    @Override
    public Suspended visitELetrec(ELetrec e) {
        final Map<EVar, Value> old = this.env;
        this.env = new HashMap<>(old);
        try {
            this.define(e.binds);
            return new Suspended(this.env, e.body);
        } finally {
            this.env = old;
        }
    }

    @Override
    public Value visitEErr(EErr e) {
        throw new RuntimeException(e.value.accept(this).force() + "");
    }
}

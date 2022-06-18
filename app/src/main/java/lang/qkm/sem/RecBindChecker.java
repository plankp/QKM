package lang.qkm.sem;

import java.util.*;
import java.util.stream.*;
import lang.qkm.expr.*;

public final class RecBindChecker {

    // based on https://v2.ocaml.org/manual/letrecvalues.html

    public void check(Map<EVar, Expr> defs) {
        final Set<EVar> k = new HashSet<>(defs.keySet());
        for (final Expr init : defs.values()) {
            if (!new StaticallyConstructive(k).check(init)
                    || new ImmediatelyLinked(k).check(init))
                throw new RuntimeException("Illegal recursive binding initializer");
        }
    }

    private static final class StaticallyConstructive extends ExprBaseVisitor<Object> {

        private Set<EVar> recBinds;

        public StaticallyConstructive(Set<EVar> recBinds) {
            this.recBinds = recBinds;
        }

        public boolean check(Expr e) {
            if (e.fv().noneMatch(this.recBinds::contains))
                return true;
            return e.accept(this) != null;
        }

        @Override
        public Object visitEVar(EVar e) {
            return this;
        }

        @Override
        public Object visitELam(ELam e) {
            return this;
        }

        @Override
        public Object visitETup(ETup e) {
            for (final Expr element : e.elements)
                if (!this.check(element))
                    return false;
            return this;
        }

        @Override
        public Object visitECtor(ECtor e) {
            for (final Expr arg : e.args)
                if (!this.check(arg))
                    return false;
            return this;
        }

        @Override
        public Object visitELet(ELet e) {
            if (!this.check(e.value))
                return false;

            final boolean newBinding = this.recBinds.add(e.bind);
            try {
                return this.check(e.body);
            } finally {
                if (newBinding)
                    this.recBinds.remove(e.bind);
            }
        }

        @Override
        public Object visitELetrec(ELetrec e) {
            for (final Expr value : e.binds.values())
                if (!this.check(value))
                    return false;

            final Set<EVar> old = this.recBinds;
            this.recBinds = new HashSet<>(old);
            try {
                this.recBinds.addAll(e.binds.keySet());
                return this.check(e.body);
            } finally {
                this.recBinds = old;
            }
        }
    }

    private static final class ImmediatelyLinked extends ExprBaseVisitor<EVar> {

        private Set<EVar> recBinds;

        public ImmediatelyLinked(Set<EVar> recBinds) {
            this.recBinds = recBinds;
        }

        public boolean check(Expr e) {
            return e.accept(this) != null;
        }

        @Override
        public EVar visitEVar(EVar e) {
            return this.recBinds.contains(e) ? e : null;
        }

        @Override
        public EVar visitELet(ELet e) {
            final boolean newBinding = this.recBinds.add(e.bind);
            final EVar link;
            try {
                link = e.body.accept(this);
            } finally {
                if (newBinding)
                    this.recBinds.remove(e.bind);
            }

            if (link == null || !link.equals(e.bind))
                return link;

            // let xname1 = expr1 in expr0      (expr0 is linked to xname1)
            // need to check if expr1 is linked to name (the outer context).
            return e.value.accept(this);
        }

        @Override
        public EVar visitELetrec(ELetrec e) {
            final Set<EVar> old = this.recBinds;
            this.recBinds = new HashSet<>();
            final EVar link;
            try {
                link = e.body.accept(this);
            } finally {
                this.recBinds = old;
            }

            if (link == null)
                return null;

            final Expr init = e.binds.get(link);
            return init == null ? link : init.accept(this);
        }
    }
}

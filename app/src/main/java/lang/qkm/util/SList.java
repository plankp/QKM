package lang.qkm.util;

import java.util.*;
import java.util.stream.*;

public abstract class SList<E> {

    public static final class Cons<E> extends SList<E> {

        public final E data;
        SList<E> next;

        private Cons(E data) {
            this.data = data;
        }

        public Cons(E data, SList<E> next) {
            if (next == null)
                throw new IllegalArgumentException("NULL CONS CELL");

            this.data = data;
            this.next = next;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public E head() {
            return this.data;
        }

        @Override
        public SList<E> tail() {
            final SList<E> xs = this.next;
            return xs == null ? new Nil<>() : xs;
        }

        @Override
        public String toString() {
            return "Cons(" + this.data + ")";
        }
    }

    public static final class Nil<E> extends SList<E> {

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public E head() {
            throw new NoSuchElementException("Cannot #head on Nil");
        }

        @Override
        public SList<E> tail() {
            throw new NoSuchElementException("Cannot #tail on Nil");
        }

        @Override
        public String toString() {
            return "Nil";
        }
    }

    public static <E> SList<E> empty() {
        return new Nil<>();
    }

    public static <E> SList<E> of(E data) {
        return new Cons<>(data, new Nil<>());
    }

    public abstract boolean isEmpty();

    public final boolean nonEmpty() {
        return !this.isEmpty();
    }

    public abstract E head();
    public abstract SList<E> tail();

    public final SList<E> prepend(E data) {
        return new Cons<>(data, this);
    }

    public final SList<E> prependAll(Iterator<? extends E> it) {
        if (!it.hasNext())
            return this;

        final Cons<E> newHead = new Cons<>(it.next());
        Cons<E> acc = newHead;
        while (it.hasNext()) {
            final Cons<E> next = new Cons<>(it.next());
            acc.next = next;
            acc = next;
        }
        acc.next = this;
        return newHead;
    }

    public final SList<E> prependAll(Collection<? extends E> data) {
        return this.prependAll(data.iterator());
    }
}

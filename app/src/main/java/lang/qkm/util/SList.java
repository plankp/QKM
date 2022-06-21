package lang.qkm.util;

import java.util.*;
import java.util.stream.*;

public abstract class SList<E> implements Iterable<E> {

    public static final class Builder<E> {

        private Cons<E> head;
        private Cons<E> last;

        public void addLast(E data) {
            final Cons<E> node = new Cons<>(data);
            if (this.head == null)
                this.head = node;
            else
                this.last.next = node;
            this.last = node;
        }

        public SList<E> build() {
            if (this.head == null)
                return new Nil<>();

            final Cons<E> head = this.head;
            final Cons<E> last = this.last;
            this.head = null;
            this.last = null;
            last.next = new Nil<>();
            return head;
        }
    }

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

        @Override
        public Iterator<E> iterator() {
            return new Iterator<>() {

                private SList<E> node = Cons.this;

                @Override
                public boolean hasNext() {
                    return this.node.nonEmpty();
                }

                @Override
                public E next() {
                    if (this.node.isEmpty())
                        throw new NoSuchElementException();

                    final E data = node.head();
                    node = node.tail();
                    return data;
                }
            };
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

        @Override
        public Iterator<E> iterator() {
            return Collections.emptyIterator();
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

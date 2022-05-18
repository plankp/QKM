package lang.qkm.match;

import java.util.*;
import java.util.stream.*;
import java.util.function.*;
import lang.qkm.type.*;
import lang.qkm.util.SList;

public interface Match {

    public static boolean covers(List<SList<Match>> ps, SList<Match> qs, SList<Type> ts) {
        // based on http://moscova.inria.fr/~maranget/papers/warn/index.html
        tailcall:
        for (;;) {
            if (ps.isEmpty())
                return false;
            if (qs.isEmpty())
                return true;

            final Match headQ = qs.head();
            final Type headTy = ts.head();
            if (headQ instanceof MatchComplete) {
                // the idea is to replace headQ with every possible
                // constructor combination, try again, and make sure all cases
                // are covered.

                if (headTy instanceof EnumType) {
                    final Iterator<String> ctors = ((EnumType) headTy).cases
                            .keySet()
                            .iterator();
                    if (!ctors.hasNext())
                        // this happens when you use a enum type that is
                        // defined without values... let's just say this
                        // pattern cannot be matched.
                        return false;

                    for (;;) {
                        final MatchCtor ctor = new MatchCtor(ctors.next(), new MatchComplete());
                        qs = qs.tail().prepend(ctor);

                        if (!ctors.hasNext())
                            continue tailcall;
                        if (!covers(ps, qs, ts))
                            return false;
                    }
                }

                if (headTy instanceof TupleType) {
                    final List<Match> seq = ((TupleType) headTy).elements
                            .stream()
                            .map(x -> new MatchComplete())
                            .collect(Collectors.toList());
                    qs = qs.tail().prepend(new MatchTuple(seq));
                    continue tailcall;
                }

                // this type cannot be deconstructed, so the only thing
                // that works is irrefutable matches.
                final List<SList<Match>> nextPs = new LinkedList<>();
                for (final SList<Match> row : ps)
                    if (!row.isEmpty() && row.head() instanceof MatchComplete)
                        nextPs.add(row.tail());

                ps = nextPs;
                qs = qs.tail();
                ts = ts.tail();
                continue tailcall;
            }

            if (headQ instanceof MatchTuple) {
                final Thunk<List<Match>> seq = new Thunk<>(() -> {
                    final TupleType ty = (TupleType) headTy;
                    if (ty.elements.isEmpty())
                        return List.of();
                    return ty.elements.stream()
                            .map(x -> new MatchComplete())
                            .collect(Collectors.toList());
                });

                final List<SList<Match>> nextPs = new LinkedList<>();
                for (final SList<Match> row : ps) {
                    if (row.isEmpty())
                        continue;

                    final Match headRow = row.head();
                    final List<Match> expansion;
                    if (headRow instanceof MatchComplete)
                        expansion = seq.get();
                    else if (headRow instanceof MatchTuple)
                        expansion = ((MatchTuple) headRow).elements;
                    else
                        continue;

                    nextPs.add(row.tail().prependAll(expansion));
                }

                ps = nextPs;
                qs = qs.tail().prependAll(((MatchTuple) headQ).elements);
                ts = ts.tail().prependAll(((TupleType) headTy).elements);
                continue tailcall;
            }

            if (headQ instanceof MatchCtor) {
                final MatchCtor ctor = (MatchCtor) headQ;
                final List<SList<Match>> nextPs = new LinkedList<>();
                for (final SList<Match> row : ps) {
                    if (row.isEmpty())
                        continue;

                    final Match headRow = row.head();
                    final Match expansion;
                    if (headRow instanceof MatchComplete)
                        expansion = new MatchComplete();
                    else if (headRow instanceof MatchCtor && ((MatchCtor) headRow).id.equals(ctor.id))
                        expansion = ((MatchCtor) headRow).arg;
                    else
                        continue;

                    nextPs.add(row.tail().prepend(expansion));
                }

                ps = nextPs;
                qs = qs.tail().prepend(ctor.arg);
                ts = ts.tail().prepend(((EnumType) headTy).cases.get(ctor.id));
                continue tailcall;
            }

            throw new AssertionError("UNREACHABLE!!");
        }
    }
}

final class Thunk<T> implements Supplier<T> {

    private final Supplier<T> supplier;

    private boolean computed;
    private T value;

    public Thunk(Supplier<T> supplier) {
        this.supplier = supplier;
    }

    @Override
    public T get() {
        if (this.computed)
            return this.value;

        final T v = this.supplier.get();
        this.value = v;
        this.computed = true;
        return v;
    }
}

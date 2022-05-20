package lang.qkm.match;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.*;
import java.util.function.*;
import lang.qkm.type.*;
import lang.qkm.util.Range;
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

                if (headTy instanceof TupleType) {
                    final List<Match> seq = ((TupleType) headTy).elements
                            .stream()
                            .map(x -> new MatchComplete())
                            .collect(Collectors.toList());
                    qs = qs.tail().prepend(new MatchTuple(seq));
                    continue tailcall;
                }

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

                if (headTy instanceof Range) {
                    // we break the pattern up into groups based on the
                    // constructor. note that irrefutable patterns are added
                    // to each group as well as tracked separately.

                    final Range<?> range = (Range<?>) headTy;
                    final Map<Object, List<SList<Match>>> groups = new HashMap<>();
                    for (final SList<Match> row : ps) {
                        if (row.isEmpty())
                            continue;

                        final Match headRow = row.head();
                        if (headRow instanceof MatchComplete) {
                            groups.computeIfAbsent(null, k -> new LinkedList<>());
                            for (final List<SList<Match>> refutable : groups.values())
                                refutable.add(row.tail());
                        } else if (headRow instanceof MatchAtom) {
                            groups.computeIfAbsent(((MatchAtom) headRow).value, k -> {
                                return new LinkedList<>(groups.getOrDefault(null, List.of()));
                            }).add(row.tail());
                        }
                    }

                    // check if the column spans
                    final List<SList<Match>> irrefutable = groups.remove(null);

                    final int sz = groups.size();
                    final boolean spans;
                    if (sz >= Integer.MAX_VALUE)
                        spans = range.containsAll(groups.keySet());
                    else {
                        // this fast case relies on the fact that the
                        // partition keys are normalized.
                        final BigInteger expected = range.size();
                        spans = expected.bitLength() < 60 && sz == expected.longValue();
                    }

                    if (irrefutable == null && !spans)
                        return false;

                    qs = qs.tail();
                    ts = ts.tail();

                    if (!spans)
                        groups.put(null, irrefutable);

                    final Iterator<List<SList<Match>>> ctors = groups.values().iterator();
                    for (;;) {
                        ps = ctors.next();
                        if (!ctors.hasNext())
                            continue tailcall;
                        if (!covers(ps, qs, ts))
                            return false;
                    }
                }

                // this type either cannot be deconstructed, or it's something
                // like a string where there are infinite number of possible
                // patterns. the only thing that works is irrefutable matches.
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
                final MatchTuple tupl = (MatchTuple) headQ;
                final List<SList<Match>> nextPs = new LinkedList<>();
                for (final SList<Match> row : ps) {
                    if (row.isEmpty())
                        continue;

                    final Match headRow = row.head();
                    final List<Match> expansion;
                    if (headRow instanceof MatchComplete)
                        expansion = tupl.elements;
                    else if (headRow instanceof MatchTuple)
                        expansion = ((MatchTuple) headRow).elements;
                    else
                        continue;

                    nextPs.add(row.tail().prependAll(expansion));
                }

                ps = nextPs;
                qs = qs.tail().prependAll(tupl.elements);
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

            if (headQ instanceof MatchAtom) {
                final List<SList<Match>> nextPs = new LinkedList<>();
                for (final SList<Match> row : ps) {
                    if (row.isEmpty())
                        continue;

                    final Match headRow = row.head();
                    if (!(headRow instanceof MatchComplete || headQ.equals(headRow)))
                        continue;

                    nextPs.add(row.tail());
                }

                ps = nextPs;
                qs = qs.tail();
                ts = ts.tail();
                continue tailcall;
            }

            throw new AssertionError("UNREACHABLE!!");
        }
    }
}

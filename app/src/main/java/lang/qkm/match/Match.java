package lang.qkm.match;

import java.math.BigInteger;
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

                if (headTy instanceof ClosedType) {
                    // the idea is to instead of actually replacing headQ
                    // with every possible constructor, we break all rows into
                    // groups based on the constructor.

                    final ClosedType range = (ClosedType) headTy;
                    final Map<Object, List<SList<Match>>> groups = new HashMap<>();
                    for (final SList<Match> row : ps) {
                        if (row.isEmpty())
                            continue;

                        final Match headRow = row.head();
                        if (headRow instanceof MatchComplete) {
                            groups.computeIfAbsent(null, k -> new LinkedList<>());
                            for (final Map.Entry<Object, List<SList<Match>>> pair : groups.entrySet()) {
                                final Object ctor = pair.getKey();
                                SList<Match> acc = row.tail();
                                if (ctor != null)
                                    for (final Type t : range.getArgs(ctor))
                                        acc = acc.prepend(new MatchComplete());
                                pair.getValue().add(acc);
                            }
                        } else if (headRow instanceof MatchNode) {
                            final MatchNode node = (MatchNode) headRow;
                            groups.computeIfAbsent(node.id, k -> {
                                final LinkedList<SList<Match>> initial = new LinkedList<>();
                                final List<Type> prepends = range.getArgs(node.id);
                                for (SList<Match> acc : groups.getOrDefault(null, List.of())) {
                                    for (final Type t : prepends)
                                        acc = acc.prepend(new MatchComplete());
                                    initial.add(acc);
                                }
                                return initial;
                            }).add(row.tail().prependAll(node.args));
                        }
                    }

                    final List<SList<Match>> irrefutable = groups.remove(null);

                    // check if the partitioned groups span.
                    final int sz = groups.size();
                    final boolean spans;
                    final Optional<Boolean> fastPath;
                    if (sz < Integer.MAX_VALUE
                            && (fastPath = range.sameSize(sz)).isPresent())
                        spans = fastPath.get();
                    else
                        spans = range.spannedBy(groups.keySet());

                    if (!spans) {
                        if (irrefutable == null)
                            return false;

                        // this means the irrefutable patterns contribute
                        // to more matches, and should be considered.
                        groups.put(null, irrefutable);
                    }

                    final SList<Match> tailQs = qs.tail();
                    final SList<Type> tailTs = ts.tail();
                    final Iterator<Map.Entry<Object, List<SList<Match>>>> ctors = groups.entrySet().iterator();
                    for (;;) {
                        final Map.Entry<Object, List<SList<Match>>> pair = ctors.next();
                        ps = pair.getValue();
                        qs = tailQs;
                        ts = tailTs;
                        final Object ctor = pair.getKey();
                        if (ctor != null) {
                            final List<Type> expansion = range.getArgs(ctor);
                            ts = ts.prependAll(expansion);
                            for (final Type t : expansion)
                                qs = qs.prepend(new MatchComplete());
                        }

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

            if (headQ instanceof MatchNode) {
                final MatchNode node = (MatchNode) headQ;
                final List<SList<Match>> nextPs = new LinkedList<>();
                for (final SList<Match> row : ps) {
                    if (row.isEmpty())
                        continue;

                    final Match headRow = row.head();
                    if (headRow instanceof MatchComplete)
                        nextPs.add(row.tail().prependAll(node.args));
                    else if (headRow instanceof MatchNode) {
                        final MatchNode m = (MatchNode) headRow;
                        if (node.id.equals(m.id))
                            nextPs.add(row.tail().prependAll(m.args));
                    }
                }

                ps = nextPs;
                qs = qs.tail().prependAll(node.args);
                ts = ts.tail().prependAll(((ClosedType) headTy).getArgs(node.id));
                continue tailcall;
            }

            throw new AssertionError("UNREACHABLE!!");
        }
    }
}

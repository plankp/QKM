package lang.qkm.match;

import java.util.*;
import java.util.stream.*;
import java.util.function.*;
import lang.qkm.sem.Typed;
import lang.qkm.type.*;
import lang.qkm.util.*;

public interface Match {

    public interface Visitor<R> {

        public R visitMatchAll(MatchAll m);
        public R visitMatchBool(MatchBool m);
        public R visitMatchCtor(MatchCtor m);
        public R visitMatchInt(MatchInt m);
        public R visitMatchString(MatchString m);
        public R visitMatchTup(MatchTup m);
    }

    public <R> R accept(Visitor<R> v);

    public Stream<String> getCaptures();

    public Object getCtor();

    public List<Match> getArgs();

    public default Match toWildcard(Supplier<? extends Match> gen) {
        return this;
    }

    public static boolean covers(List<SList<Match>> ps, SList<Typed<Match>> qs) {
        // based on http://moscova.inria.fr/~maranget/papers/warn/index.html
        tailcall:
        for (;;) {
            if (ps.isEmpty())
                return false;
            if (qs.isEmpty())
                return true;

            final Typed<Match> headQ = qs.head();
            final CtorSet range = headQ.type.getCtorSet();
            if (headQ.value instanceof MatchAll) {
                if (range == null) {
                    // this type either cannot be deconstructed, or it's something
                    // like a string where there are infinite number of possible
                    // patterns. the only thing that works is irrefutable matches.
                    final List<SList<Match>> nextPs = new LinkedList<>();
                    for (final SList<Match> row : ps)
                        if (!row.isEmpty() && row.head() instanceof MatchAll)
                            nextPs.add(row.tail());

                    ps = nextPs;
                    qs = qs.tail();
                    continue tailcall;
                }

                // the idea is to instead of actually replacing headQ
                // with every possible constructor, we break all rows into
                // groups based on the constructor.

                final Map<Object, List<SList<Match>>> groups = new HashMap<>();
                for (final SList<Match> row : ps) {
                    if (row.isEmpty())
                        continue;

                    final Match headRow = row.head();
                    if (headRow instanceof MatchAll) {
                        groups.computeIfAbsent(null, k -> new LinkedList<>());
                        for (final Map.Entry<Object, List<SList<Match>>> pair : groups.entrySet()) {
                            final Object ctor = pair.getKey();
                            SList<Match> acc = row.tail();
                            if (ctor != null)
                                acc = acc.prependAll(range.getArgs(ctor).stream()
                                        .map(t -> new MatchAll())
                                        .iterator());
                            pair.getValue().add(acc);
                        }
                    } else {
                        groups.computeIfAbsent(headRow.getCtor(), k -> {
                            final LinkedList<SList<Match>> initial = new LinkedList<>();
                            for (SList<Match> acc : groups.getOrDefault(null, List.of()))
                                initial.add(acc.prependAll(headRow.getArgs().stream()
                                    .map(t -> new MatchAll())
                                    .iterator()));
                            return initial;
                        }).add(row.tail().prependAll(headRow.getArgs()));
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

                final SList<Typed<Match>> tailQs = qs.tail();
                final Iterator<Map.Entry<Object, List<SList<Match>>>> ctors = groups.entrySet().iterator();
                for (;;) {
                    final Map.Entry<Object, List<SList<Match>>> pair = ctors.next();
                    ps = pair.getValue();
                    qs = tailQs;
                    final Object ctor = pair.getKey();
                    if (ctor != null)
                        qs = qs.prependAll(range.getArgs(ctor).stream()
                                .map(t -> new Typed<Match>(new MatchAll(), t))
                                .iterator());

                    if (!ctors.hasNext())
                        continue tailcall;
                    if (!covers(ps, qs))
                        return false;
                }
            }

            final List<SList<Match>> nextPs = new LinkedList<>();
            for (final SList<Match> row : ps) {
                if (row.isEmpty())
                    continue;

                final Match headRow = row.head();
                if (headRow instanceof MatchAll)
                    nextPs.add(row.tail().prependAll(headQ.value.getArgs()));
                else if (headQ.value.getCtor().equals(headRow.getCtor()))
                    nextPs.add(row.tail().prependAll(headRow.getArgs()));
            }

            ps = nextPs;
            qs = qs.tail().prependAll(new Zipper<>(
                    headQ.value.getArgs().iterator(),
                    range.getArgs(headQ.value.getCtor()).iterator(),
                    Typed::new));

            continue tailcall;
        }
    }
}

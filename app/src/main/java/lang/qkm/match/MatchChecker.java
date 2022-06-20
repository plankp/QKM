package lang.qkm.match;

import java.util.*;
import java.util.stream.*;
import lang.qkm.sem.Typed;
import lang.qkm.type.*;
import lang.qkm.util.SList;
import lang.qkm.util.Zipper;

public final class MatchChecker {

    // based on http://moscova.inria.fr/~maranget/papers/warn/index.html

    public boolean useful(List<SList<Match>> ps, SList<Typed<Match>> qs) {
        for (;;) {
            if (ps.isEmpty())
                return true;
            if (qs.isEmpty())
                return false;

            final Typed<Match> q = qs.head();
            final CtorSet range = q.type.getCtorSet();
            if (q.value instanceof MatchAll) {
                if (range == null || !range.isComplete()) {
                    ps = this.defaulted(ps);
                    qs = qs.tail();
                    continue;
                }

                final Set<? extends Object> ctors = this.firstCtors(ps);

                final boolean spans;
                final Optional<Boolean> fastPath;
                if (ctors.size() < Integer.MAX_VALUE
                        && (fastPath = range.sameSize(ctors.size())).isPresent())
                    spans = fastPath.get();
                else
                    spans = range.spannedBy(ctors);

                // regardless of if all possible constructors appear at least
                // once, we need to specialize against it.
                for (final Object ctor : ctors) {
                    final List<? extends Type> argTys = range.getArgs(ctor);
                    final List<? extends Match> argMs = argTys.stream()
                            .map(k -> new MatchAll())
                            .collect(Collectors.toList());

                    final List<SList<Match>> subPs = this.specialized(ps, ctor, argMs);
                    final SList<Typed<Match>> subQs = qs.tail().prependAll(new Zipper<>(
                            argMs.iterator(),
                            argTys.iterator(),
                            Typed::new));
                    if (this.useful(subPs, subQs))
                        return true;
                }

                if (!spans) {
                    // not all constructors are specified. we check the
                    // default matrix to see if there is a wildcard to handle
                    // the missing constructors.
                    ps = this.defaulted(ps);
                    qs = qs.tail();
                    continue;
                }

                return false;
            } else {
                ps = this.specialized(ps, q.value.getCtor(), q.value.getArgs());
                qs = qs.tail().prependAll(new Zipper<>(
                        q.value.getArgs().iterator(),
                        range.getArgs(q.value.getCtor()).iterator(),
                        Typed::new));
                continue;
            }
        }
    }

    public Set<? extends Object> firstCtors(List<SList<Match>> cases) {
        return cases.stream()
                .map(SList::head)
                .map(Match::getCtor)
                .filter(k -> k != null)
                .collect(Collectors.toSet());
    }

    public List<SList<Match>> specialized(List<SList<Match>> cases, Object ctor, List<? extends Match> args) {
        final List<SList<Match>> result = new ArrayList<>(cases.size());
        for (final SList<Match> row : cases) {
            final Match m = row.head();
            final SList<Match> ms = row.tail();
            if (m instanceof MatchAll)
                result.add(ms.prependAll(args));
            else if (m.getCtor().equals(ctor))
                result.add(ms.prependAll(m.getArgs()));
        }

        return result;
    }

    public List<SList<Match>> defaulted(List<SList<Match>> cases) {
        final List<SList<Match>> result = new ArrayList<>(cases.size());
        for (final SList<Match> row : cases) {
            final Match m = row.head();
            final SList<Match> ms = row.tail();
            if (m instanceof MatchAll)
                result.add(ms);
        }

        return result;
    }
}

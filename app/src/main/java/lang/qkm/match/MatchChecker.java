package lang.qkm.match;

import java.util.*;
import java.util.stream.*;
import lang.qkm.sem.Typed;
import lang.qkm.type.*;
import lang.qkm.util.SList;
import lang.qkm.util.Zipper;

public final class MatchChecker {

    // based on http://moscova.inria.fr/~maranget/papers/warn/index.html

    public static boolean covers(List<SList<Match>> ps, SList<Typed<Match>> qs) {
        for (;;) {
            if (ps.isEmpty())
                return false;
            if (qs.isEmpty())
                return true;

            final Typed<Match> q = qs.head();
            final CtorSet range = q.type.getCtorSet();
            if (q.value instanceof MatchAll) {
                Set<? extends Object> ctors = null;
                boolean spans = false;

                if (range != null && range.isComplete()) {
                    ctors = firstCtors(ps);

                    final Optional<Boolean> fastPath;
                    if (ctors.size() < Integer.MAX_VALUE
                            && (fastPath = range.sameSize(ctors.size())).isPresent())
                        spans = fastPath.get();
                    else
                        spans = range.missingCase(ctors) == null;
                }

                if (!spans) {
                    // if it's not a complete signature, computing the default
                    // matrix is enough
                    ps = defaulted(ps);
                    qs = qs.tail();
                    continue;
                }

                // only specialize against all possible constructors if all
                // appear at least once
                final Iterator<? extends Object> it = ctors.iterator();
                if (!it.hasNext())
                    return true;

                for (;;) {
                    final Object ctor = it.next();
                    final List<? extends Type> argTys = range.getArgs(ctor);
                    final List<? extends Match> argMs = argTys.stream()
                            .map(k -> new MatchAll())
                            .collect(Collectors.toList());

                    final List<SList<Match>> subPs = specialized(ps, ctor, argMs);
                    final SList<Typed<Match>> subQs = qs.tail().prependAll(new Zipper<>(
                            argMs.iterator(),
                            argTys.iterator(),
                            Typed::new));

                    if (!it.hasNext()) {
                        // setup variables for tailcall
                        ps = subPs;
                        qs = subQs;
                        break;
                    }

                    if (!covers(subPs, subQs))
                        return false;
                }
            } else if (q.value instanceof MatchOr) {
                final Iterator<Match> it = ((MatchOr) q.value).submatches.iterator();
                for (;;) {
                    final Typed<Match> head = new Typed<>(it.next(), q.type);
                    final SList<Typed<Match>> seq = qs.tail().prepend(head);
                    if (!it.hasNext()) {
                        // setup variables for tailcall
                        qs = seq;
                        break;
                    }

                    if (!covers(ps, seq))
                        return false;
                }
            } else {
                ps = specialized(ps, q.value.getCtor(), q.value.getArgs());
                qs = qs.tail().prependAll(new Zipper<>(
                        q.value.getArgs().iterator(),
                        range.getArgs(q.value.getCtor()).iterator(),
                        Typed::new));
            }
        }
    }

    public static SList<String> missingMatch(List<SList<Match>> ps, SList<Type> qs) {
        if (ps.isEmpty()) {
            if (qs.isEmpty())
                return SList.empty();

            SList<String> list = SList.empty();
            for (SList<?> k = qs; k.nonEmpty(); k = k.tail())
                list = list.prepend("_");
            return list;
        }
        if (qs.isEmpty())
            return null;

        final Type q = qs.head();
        final CtorSet range = q.getCtorSet();
        if (range == null) {
            final SList<String> result = missingMatch(defaulted(ps), qs.tail());
            return result == null ? null : result.prepend("_");
        }

        Set<? extends Object> ctors = null;
        boolean spans = false;

        if (range.isComplete()) {
            ctors = firstCtors(ps);

            final Optional<Boolean> fastPath;
            if (ctors.size() < Integer.MAX_VALUE
                    && (fastPath = range.sameSize(ctors.size())).isPresent())
                spans = fastPath.get();
            else
                spans = range.missingCase(ctors) == null;
        }

        if (!spans) {
            final SList<String> result = missingMatch(defaulted(ps), qs.tail());
            if (result == null)
                return null;

            final Object missing = range.missingCase(firstCtors(ps));
            final List<?> args = range.getArgs(missing);
            if (args.isEmpty())
                return result.prepend(missing.toString());

            final StringBuilder sb = new StringBuilder();
            sb.append(missing);
            for (final Object arg : args)
                sb.append(" _");
            return result.prepend(sb.toString());
        }

        // regardless of if all possible constructors appear at least
        // once, we need to specialize against it.
        for (final Object ctor : ctors) {
            final List<? extends Type> argTys = range.getArgs(ctor);
            final List<? extends Match> argMs = argTys.stream()
                    .map(k -> new MatchAll())
                    .collect(Collectors.toList());

            final List<SList<Match>> subPs = specialized(ps, ctor, argMs);
            final SList<Type> subQs = qs.tail().prependAll(argTys);
            SList<String> result = missingMatch(subPs, subQs);
            if (result == null)
                // this one is exhaustive, try another one
                continue;

            final StringBuilder sb = new StringBuilder();
            if (ctor == TyTup.class) {
                for (final Type arg : argTys) {
                    sb.append(", ").append(result.head());
                    result = result.tail();
                }
                sb.replace(0, 2, "(").append(')');
            } else {
                sb.append(ctor);
                for (final Type arg : argTys) {
                    sb.append(" (").append(result.head()).append(')');
                    result = result.tail();
                }
            }

            return result.prepend(sb.toString());
        }

        // reaching here means it was exhaustive
        return null;
    }

    public static Set<? extends Object> firstCtors(List<SList<Match>> cases) {
        final Set<Object> ctors = new HashSet<>();
        for (final SList<Match> k : cases)
            firstCtors(SList.of(k.head()), ctors);

        return ctors;
    }

    private static void firstCtors(SList<Match> column, Set<Object> ctors) {
        for (final Match m : column) {
            if (m instanceof MatchAll)
                continue;
            if (m instanceof MatchOr)
                firstCtors(((MatchOr) m).submatches, ctors);
            else
                ctors.add(m.getCtor());
        }
    }

    public static List<SList<Match>> specialized(List<SList<Match>> cases, Object ctor, List<? extends Match> args) {
        final List<SList<Match>> result = new ArrayList<>(cases.size());
        for (final SList<Match> row : cases)
            specialized(SList.of(row.head()), row.tail(), ctor, args, result);

        return result;
    }

    private static void specialized(SList<Match> column, SList<Match> ms, Object ctor, List<? extends Match> args, List<SList<Match>> result) {
        for (final Match m : column) {
            if (m instanceof MatchAll)
                result.add(ms.prependAll(args));
            else if (m instanceof MatchOr)
                specialized(((MatchOr) m).submatches, ms, ctor, args, result);
            else if (m.getCtor().equals(ctor))
                result.add(ms.prependAll(m.getArgs()));
        }
    }

    public static List<SList<Match>> defaulted(List<SList<Match>> cases) {
        final List<SList<Match>> result = new ArrayList<>(cases.size());
        for (final SList<Match> row : cases)
            if (hasWildcard(SList.of(row.head())))
                result.add(row.tail());

        return result;
    }

    public static boolean hasWildcard(SList<Match> column) {
        for (final Match m : column) {
            if (m instanceof MatchAll)
                return true;
            if (m instanceof MatchOr && hasWildcard(((MatchOr) m).submatches))
                return true;
        }
        return false;
    }
}

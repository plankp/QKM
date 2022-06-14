package lang.qkm.match;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.*;
import lang.qkm.expr.*;
import lang.qkm.type.*;

public final class MatchCompiler {

    private final Map<Expr, Map<String, Map.Entry<Expr, Type>>> bindings = new HashMap<>();

    private BigInteger id = BigInteger.ZERO;

    public MatchAll wildcard(Type t) {
        return new MatchAll("`" + (this.id = this.id.add(BigInteger.ONE)), t);
    }

    public Expr compile(Expr scrutinee, Type type, List<Map.Entry<Match, Expr>> cases) {
        return this.compile(List.of(Map.entry(scrutinee, type)), cases.stream()
                .map(p -> Map.entry(List.of(p.getKey()), p.getValue()))
                .collect(Collectors.toList()));
    }

    private Expr compile(List<Map.Entry<Expr, Type>> input, List<Map.Entry<List<Match>, Expr>> cases) {
        if (cases.isEmpty())
            return new EErr(new EString("Match failure!"));

        int column = -1;
        boolean allWildcard = true;
        for (final Match m : cases.get(0).getKey()) {
            column++;
            if (!(allWildcard &= m instanceof MatchAll))
                break;
        }

        if (allWildcard) {
            final Map.Entry<List<Match>, Expr> row = cases.get(0);
            final Expr e = row.getValue();
            final Map<String, Map.Entry<Expr, Type>> captures = this.bindings.getOrDefault(e, Map.of());

            // conservatively emit a match to make sure the variables are
            // bound correctly.

            final List<Expr> scrutinee = new ArrayList<>(input.size() + captures.size());
            for (final Map.Entry<Expr, Type> p : input)
                scrutinee.add(p.getKey());

            final List<Match> match = new ArrayList<>(input.size() + captures.size());
            match.addAll(row.getKey());

            for (final Map.Entry<String, Map.Entry<Expr, Type>> capture : captures.entrySet()) {
                final String name = capture.getKey();
                final Map.Entry<Expr, Type> value = capture.getValue();
                scrutinee.add(value.getKey());
                match.add(new MatchAll(capture.getKey(), value.getValue()));
            }

            if (scrutinee.size() == 1)
                return new EMatch(scrutinee.get(0),
                                  List.of(Map.entry(match.get(0), e)));

            final MatchTup m = new MatchTup(new TyTup(match.stream()
                    .map(Match::getType)
                    .collect(Collectors.toList())), match);
            return new EMatch(new ETup(scrutinee), List.of(Map.entry(m, e)));
        }

        final Map.Entry<Expr, Type> scrutinee = input.get(column);
        final CtorSet range = scrutinee.getValue().getCtorSet();

        final Map<Object, Match> ctors = new HashMap<>();
        for (final Map.Entry<List<Match>, Expr> k : cases) {
            final Match m = k.getKey().get(column);
            if (!(m instanceof MatchAll))
                ctors.computeIfAbsent(m.getCtor(), _ign -> m.toWildcard(this::wildcard));
        }

        final List<Map.Entry<Match, Expr>> newCases = new ArrayList<>(ctors.size() + 1);
        for (final Match ctor : ctors.values()) {
            final List<Map.Entry<Expr, Type>> newInput = new ArrayList<>(input.size() - 1 + ctor.getArgs().size());
            newInput.addAll(input.subList(0, column));
            for (final Match exp : ctor.getArgs()) {
                final MatchAll node = (MatchAll) exp;
                newInput.add(Map.entry(new EVar(node.capture), node.getType()));
            }
            newInput.addAll(input.subList(column + 1, input.size()));

            newCases.add(Map.entry(ctor, this.compile(newInput, this.specialize(cases, column, ctor, scrutinee))));
        }

        // if we are already spanning, then there's no point of emitting a
        // extra wildcard case.
        final int sz = ctors.size();
        final boolean spans;
        final Optional<Boolean> fastPath;
        if (sz < Integer.MAX_VALUE
                && (fastPath = range.sameSize(sz)).isPresent())
            spans = fastPath.get();
        else
            spans = range.spannedBy(ctors.keySet());

        if (!spans) {
            final MatchAll guard = this.wildcard(scrutinee.getValue());
            final List<Map.Entry<Expr, Type>> newInput = new ArrayList<>(input.size() - 1);
            newInput.addAll(input.subList(0, column));
            newInput.addAll(input.subList(column + 1, input.size()));

            newCases.add(Map.entry(guard, this.compile(newInput, this.defaulted(cases, column, scrutinee))));
        }

        return new EMatch(scrutinee.getKey(), newCases);
    }

    private List<Map.Entry<List<Match>, Expr>> specialize(List<Map.Entry<List<Match>, Expr>> cases, int column, Match node, Map.Entry<Expr, Type> scrutinee) {
        final List<Map.Entry<List<Match>, Expr>> result = new ArrayList<>(cases.size());
        final List<Match> args = node.getArgs();

        for (final Map.Entry<List<Match>, Expr> pair : cases) {
            final List<Match> oldRow = pair.getKey();
            final Match m = oldRow.get(column);
            if (m instanceof MatchAll) {
                final MatchAll wildcard = (MatchAll) m;
                if (wildcard.capture != null)
                    this.bindings.computeIfAbsent(pair.getValue(), k -> new HashMap<>())
                            .put(wildcard.capture, scrutinee);

                final List<Match> newRow = new ArrayList<>(oldRow.size() - 1 + args.size());
                newRow.addAll(oldRow.subList(0, column));
                for (final Match exp : args)
                    newRow.add(new MatchAll(exp.getType()));
                newRow.addAll(oldRow.subList(column + 1, oldRow.size()));
                result.add(Map.entry(newRow, pair.getValue()));
            } else if (m.getCtor().equals(node.getCtor())) {
                final List<Match> newRow = new ArrayList<>(oldRow.size() - 1 + args.size());
                newRow.addAll(oldRow.subList(0, column));
                newRow.addAll(m.getArgs());
                newRow.addAll(oldRow.subList(column + 1, oldRow.size()));
                result.add(Map.entry(newRow, pair.getValue()));
            }
        }
        return result;
    }

    private List<Map.Entry<List<Match>, Expr>> defaulted(List<Map.Entry<List<Match>, Expr>> cases, int column, Map.Entry<Expr, Type> scrutinee) {
        final List<Map.Entry<List<Match>, Expr>> result = new ArrayList<>(cases.size());

        for (final Map.Entry<List<Match>, Expr> pair : cases) {
            final List<Match> oldRow = pair.getKey();
            final Match m = oldRow.get(column);
            if (!(m instanceof MatchAll))
                continue;

            final MatchAll wildcard = (MatchAll) m;
            if (wildcard.capture != null)
                this.bindings.computeIfAbsent(pair.getValue(), k -> new HashMap<>())
                        .put(wildcard.capture, scrutinee);

            if (column == 0)
                result.add(Map.entry(oldRow.subList(1, oldRow.size()), pair.getValue()));
            else if (column == oldRow.size() - 1)
                result.add(Map.entry(oldRow.subList(0, column), pair.getValue()));
            else {
                final List<Match> newRow = new ArrayList<>(oldRow.size() - 1);
                newRow.addAll(oldRow.subList(0, column));
                newRow.addAll(oldRow.subList(column + 1, oldRow.size()));
                result.add(Map.entry(newRow, pair.getValue()));
            }
        }
        return result;
    }
}

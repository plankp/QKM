package lang.qkm.match;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.*;
import lang.qkm.expr.*;
import lang.qkm.type.*;

public final class MatchCompiler {

    private final Map<Expr, Map<String, Expr>> bindings = new HashMap<>();

    private BigInteger id = BigInteger.ZERO;

    public MatchAll wildcard() {
        return new MatchAll("`p" + (this.id = this.id.add(BigInteger.ONE)));
    }

    public Expr compile(Expr scrutinee, List<Map.Entry<Match, Expr>> cases) {
        return this.compile(List.of(scrutinee), cases.stream()
                .map(p -> Map.entry(List.of(p.getKey()), p.getValue()))
                .collect(Collectors.toList()));
    }

    private Expr compile(List<Expr> input, List<Map.Entry<List<Match>, Expr>> cases) {
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
            Expr action = row.getValue();
            final Map<String, Expr> captures = this.bindings.computeIfAbsent(action, k -> new HashMap<>());

            // iterate the columns to make sure we are capturing if necessary
            final Iterator<Expr> itInput = input.iterator();
            final Iterator<Match> itMatch = row.getKey().iterator();
            while (itInput.hasNext() && itMatch.hasNext()) {
                final Expr scrutinee = itInput.next();
                final MatchAll match = (MatchAll) itMatch.next();
                if (match.capture != null)
                    captures.put(match.capture, scrutinee);
            }

            for (final Map.Entry<String, Expr> pair : captures.entrySet())
                action = new ELet(new EVar(pair.getKey()), pair.getValue(), action);

            return action;
        }

        final Expr scrutinee = input.get(column);

        final Map<Object, Match> ctors = new HashMap<>();
        for (final Map.Entry<List<Match>, Expr> k : cases) {
            final Match m = k.getKey().get(column);
            if (!(m instanceof MatchAll))
                ctors.computeIfAbsent(m.getCtor(), _ign -> m.toWildcard(this::wildcard));
        }

        final List<Map.Entry<Match, Expr>> newCases = new ArrayList<>(ctors.size() + 1);
        for (final Match ctor : ctors.values()) {
            final List<Expr> newInput = new ArrayList<>(input.size() - 1 + ctor.getArgs().size());
            newInput.addAll(input.subList(0, column));
            for (final Match exp : ctor.getArgs()) {
                final MatchAll node = (MatchAll) exp;
                newInput.add(new EVar(node.capture));
            }
            newInput.addAll(input.subList(column + 1, input.size()));

            newCases.add(Map.entry(ctor, this.compile(newInput, this.specialize(cases, column, ctor, scrutinee))));
        }

        final MatchAll guard = this.wildcard();
        final List<Expr> newInput = new ArrayList<>(input.size() - 1);
        newInput.addAll(input.subList(0, column));
        newInput.addAll(input.subList(column + 1, input.size()));

        newCases.add(Map.entry(guard, this.compile(newInput, this.defaulted(cases, column, scrutinee))));

        return new EMatch(scrutinee, newCases);
    }

    private List<Map.Entry<List<Match>, Expr>> specialize(List<Map.Entry<List<Match>, Expr>> cases, int column, Match node, Expr scrutinee) {
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
                    newRow.add(new MatchAll());
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

    private List<Map.Entry<List<Match>, Expr>> defaulted(List<Map.Entry<List<Match>, Expr>> cases, int column, Expr scrutinee) {
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

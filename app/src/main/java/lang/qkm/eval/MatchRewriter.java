package lang.qkm.eval;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.*;
import lang.qkm.expr.*;
import lang.qkm.match.*;

public final class MatchRewriter implements ExprRewriter {

    private BigInteger id = BigInteger.ZERO;
    private boolean compiling;

    @Override
    public Expr rewrite(Expr e) {
        this.compiling = true;
        e = e.accept(this);
        this.compiling = false;
        e = e.accept(this);

        return e;
    }

    public String newName() {
        return "`j" + (this.id = this.id.add(BigInteger.ONE));
    }

    @Override
    public Expr visitEMatch(EMatch e) {
        if (!this.compiling) {
            final Expr k = ExprRewriter.super.visitEMatch(e);
            if (!(k instanceof EMatch))
                return k;

            // try to remove the default case like the following:
            //   match k with (...) -> e1 | _ -> (!! Match failure).
            // clearly, core constructs like tuple only need one case.

            e = (EMatch) k;
            final Match m = e.cases.get(0).getKey();
            if (m instanceof MatchTup)
                // must be a unpack, only first case is needed
                return new EMatch(e.scrutinee, List.of(e.cases.get(0)));

            if (m instanceof MatchBool)
                // must either be true or false, so only keep the first two cases.
                // since the match compiler always inserts a default care,
                // there will always be at least two cases.
                return new EMatch(e.scrutinee, List.of(e.cases.get(0), e.cases.get(1)));

            return e;
        }

        // because or patterns (and potentially other patterns) are expanded,
        // we explicitly introduce join points. consider the following:
        //   match scrutinee with p1 -> e1 | p2 -> e2 | ...
        // it then becomes
        //   let j1 = \capture1. e1 in
        //   let j2 = \capture2. e2 in ...
        //   match scrutinee with p1 -> j1 capture1 | p2 -> j2 capture2
        //
        // (and let the later pass re-inline them or whatever)

        final Expr scrutinee = e.scrutinee.accept(this);
        final ArrayDeque<Map.Entry<EVar, Expr>> joinPoints = new ArrayDeque<>();

        final List<Map.Entry<Match, Expr>> cases = new ArrayList<>(e.cases.size());
        for (final Map.Entry<Match, Expr> k : e.cases) {
            final Match m = k.getKey();
            Expr action = k.getValue().accept(this);

            final EVar node = new EVar(this.newName());
            final ArrayDeque<String> args = m.getCaptures()
                    .distinct()
                    .collect(Collectors.toCollection(ArrayDeque::new));
            if (args.isEmpty())
                action = new ELam(node, action);
            else {
                final Iterator<String> it = args.descendingIterator();
                while (it.hasNext())
                    action = new ELam(new EVar(it.next()), action);
            }
            joinPoints.push(Map.entry(node, action));

            Expr jump = node;
            if (args.isEmpty())
                jump = new EApp(jump, new ETup(List.of()));
            else
                for (final String arg : args)
                    jump = new EApp(jump, new EVar(arg));
            cases.add(Map.entry(m, jump));
        }

        Expr result = new MatchCompiler().compile(scrutinee, cases);
        while (!joinPoints.isEmpty()) {
            final Map.Entry<EVar, Expr> info = joinPoints.pop();
            result = new ELet(info.getKey(), info.getValue(), result);
        }
        return result;
    }
}

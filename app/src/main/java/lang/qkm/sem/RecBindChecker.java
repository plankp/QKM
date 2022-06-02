package lang.qkm.sem;

import java.util.*;
import java.util.stream.*;
import lang.qkm.QKMBaseVisitor;
import static lang.qkm.QKMParser.*;

public final class RecBindChecker extends QKMBaseVisitor<Stream<String>> {

    // implement a reduced checker where we only allow function use.
    //
    // there are more cases that are safe such as constructors. example is
    // cyclic lists in ocaml:
    // let rec x = 10::x

    @Override
    public Stream<String> visitDefBind(DefBindContext ctx) {
        final Set<String> names = new HashSet<>();
        final Set<String> fvs = new HashSet<>();
        for (final BindingContext b : ctx.b) {
            names.add(b.n.getText());
            this.visit(b.e).forEach(fvs::add);
        }

        if (Collections.disjoint(names, fvs))
            return fvs.stream();

        throw new RuntimeException("Illegal use of recursive binding");
    }

    @Override
    public Stream<String> visitExprLetrec(ExprLetrecContext ctx) {
        final Set<String> names = new HashSet<>();
        final Set<String> fvs = new HashSet<>();
        for (final BindingContext b : ctx.b) {
            names.add(b.n.getText());
            this.visit(b.e).forEach(fvs::add);
        }

        if (Collections.disjoint(names, fvs))
            return Stream.concat(
                    fvs.stream(),
                    this.visit(ctx.e).filter(v -> !names.contains(v)));

        throw new RuntimeException("Illegal use of recursive binding");
    }

    @Override
    public Stream<String> visitExprApply(ExprApplyContext ctx) {
        return Stream.concat(Stream.of(ctx.f), ctx.args.stream())
                .flatMap(this::visit);
    }

    @Override
    public Stream<String> visitExprIdent(ExprIdentContext ctx) {
        return Stream.of(ctx.n.getText());
    }

    @Override
    public Stream<String> visitExprCtor(ExprCtorContext ctx) {
        return Stream.empty();
    }

    @Override
    public Stream<String> visitExprTrue(ExprTrueContext ctx) {
        return Stream.empty();
    }

    @Override
    public Stream<String> visitExprFalse(ExprFalseContext ctx) {
        return Stream.empty();
    }

    @Override
    public Stream<String> visitExprChar(ExprCharContext ctx) {
        return Stream.empty();
    }

    @Override
    public Stream<String> visitExprText(ExprTextContext ctx) {
        return Stream.empty();
    }

    @Override
    public Stream<String> visitExprLambda(ExprLambdaContext ctx) {
        // let rec x = fun k -> e
        // e can definitely use x.
        return Stream.empty();
    }

    @Override
    public Stream<String> visitExprGroup(ExprGroupContext ctx) {
        return ctx.es.stream().flatMap(this::visit);
    }

    @Override
    public Stream<String> visitExprMatch(ExprMatchContext ctx) {
        return Stream.concat(Stream.of(ctx.v), ctx.k.stream())
                .flatMap(this::visit);
    }

    @Override
    public Stream<String> visitMatchCase(MatchCaseContext ctx) {
        final Set<String> k = this.visit(ctx.p).collect(Collectors.toSet());
        return this.visit(ctx.e).filter(v -> !k.contains(v));
    }

    @Override
    public Stream<String> visitPatIgnore(PatIgnoreContext ctx) {
        return Stream.empty();
    }

    @Override
    public Stream<String> visitPatBind(PatBindContext ctx) {
        return Stream.of(ctx.n.getText());
    }

    @Override
    public Stream<String> visitPatTrue(PatTrueContext ctx) {
        return Stream.empty();
    }

    @Override
    public Stream<String> visitPatChar(PatCharContext ctx) {
        return Stream.empty();
    }

    @Override
    public Stream<String> visitPatText(PatTextContext ctx) {
        return Stream.empty();
    }

    @Override
    public Stream<String> visitPatGroup(PatGroupContext ctx) {
        return ctx.ps.stream().flatMap(this::visit);
    }
}

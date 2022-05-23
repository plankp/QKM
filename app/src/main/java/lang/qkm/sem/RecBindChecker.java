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
    public Stream<String> visitDefRecBind(DefRecBindContext ctx) {
        final String name = ctx.n.getText();
        final Set<String> s = this.visit(ctx.e).collect(Collectors.toSet());
        if (s.contains(name))
            throw new RuntimeException("Illegal use of recursive binding " + name);

        return s.stream();
    }

    @Override
    public Stream<String> visitExprApply(ExprApplyContext ctx) {
        return Stream.concat(this.visit(ctx.f), this.visit(ctx.arg));
    }

    @Override
    public Stream<String> visitExprCons(ExprConsContext ctx) {
        return this.visit(ctx.arg);
    }

    @Override
    public Stream<String> visitExprIdent(ExprIdentContext ctx) {
        return Stream.of(ctx.n.getText());
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
        return Stream.concat(Stream.of(ctx.i), ctx.r.stream())
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
    public Stream<String> visitPatDecons(PatDeconsContext ctx) {
        return ctx.arg == null ? Stream.empty() : this.visit(ctx.arg);
    }

    @Override
    public Stream<String> visitPatBind(PatBindContext ctx) {
        return Stream.of(ctx.n.getText());
    }

    @Override
    public Stream<String> visitPatGroup(PatGroupContext ctx) {
        return ctx.ps.stream().flatMap(this::visit);
    }
}

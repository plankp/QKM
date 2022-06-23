package lang.qkm.eval;

import java.util.*;
import lang.qkm.expr.*;
import lang.qkm.match.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ANFConverterTest {

    @Test
    public void testFlattenNestedLet() {
        /* // test the following
        let c =
          let b = let a = "7" in ((a, ()), "1")
          in ((b, ()), "2")
        in ((c, ()), "3")
        */

        final ETup unit = new ETup(List.of());
        final ELet bindA = new ELet(
                new EVar("a"), new EString("7"),
                new ETup(List.of(new ETup(List.of(new EVar("a"), unit)), new EString("1"))));
        final ELet bindB = new ELet(
                new EVar("b"), bindA,
                new ETup(List.of(new ETup(List.of(new EVar("b"), unit)), new EString("2"))));
        final ELet bindC = new ELet(
                new EVar("c"), bindB,
                new ETup(List.of(new ETup(List.of(new EVar("c"), unit)), new EString("3"))));

        final String expected =
                "(let ((`v1 7)) " +
                "(let ((`v2 (`v1, ()))) " +
                "(let ((`v3 (`v2, 1))) " +
                "(let ((`v4 (`v3, ()))) " +
                "(let ((`v5 (`v4, 2))) " +
                "(let ((`v6 (`v5, ()))) " +
                "(`v6, 3)))))))";
        assertEquals(expected, new ANFConverter().rewrite(bindC).toString());
    }

    @Test
    public void testFlattenNestedMatch() {
        /* // test the following
        (match (match true with
                 | true  -> (match (true, false) with (_, k) -> k)
                 | false -> false) with
           | _ -> "1", "9")
        */

        final EMatch matchUnpack = new EMatch(
                new ETup(List.of(new EBool(true), new EBool(false))),
                List.of(Map.entry(new MatchTup(List.of(new MatchAll(), new MatchAll("k"))), new EVar("k"))));
        final EMatch matchNegate = new EMatch(new EBool(true), List.of(
                Map.entry(new MatchBool(true), matchUnpack),
                Map.entry(new MatchBool(false), new EBool(true))));
        final EMatch matchDiscard = new EMatch(matchNegate,
                List.of(Map.entry(new MatchAll(), new EString("1"))));
        final ETup pack = new ETup(List.of(matchDiscard, new EString("9")));

        final String expected =
                "(let ((`v6 (\\`v3. " +
                "(let ((`v5 (\\`v4. (`v4, 9)))) " +
                "(match `v3 (_ (`v5 1))))))) " +
                "(match true " +
                "(true (let ((`v1 (true, false))) (match `v1 ((_, `v2) (`v6 `v2))))) " +
                "(false (`v6 true))))";
        assertEquals(expected, new ANFConverter().rewrite(pack).toString());
    }
}

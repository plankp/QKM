package lang.qkm.type;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public final class TyCtorTest {

    @Test
    public void testUnifyWithTyApp() {
        final TyVar q = TyVar.grounded("q");
        final TyCtor.Template template = new TyCtor.Template(
                "Foo", List.of(q), Map.of("#foo", List.of()));

        // #foo bool ~ t1 t2
        final TyVar t1 = TyVar.unifiable("t1");
        final TyVar t2 = TyVar.unifiable("t2");
        final TyCtor k = new TyCtor(template, List.of(TyBool.INSTANCE));
        final TyApp s = new TyApp(t1, t2);
        k.unify(s);

        // t2 should be bool
        t2.unify(TyBool.INSTANCE);

        // t1 should be (t. Foo t)
        final TyCtor v = new TyCtor(template, List.of(TyString.INSTANCE));
        final Type exp = new TyApp(t1, TyString.INSTANCE).eval(Map.of());
        assertTrue(exp instanceof TyCtor);
        v.unify(exp);
    }
}

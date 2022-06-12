package lang.qkm.expr;

import java.util.*;
import java.util.stream.*;

public interface Expr {

    public interface Visitor<R> {

        public R visitEBool(EBool e);
        public R visitECtor(ECtor e);
        public R visitEInt(EInt e);
        public R visitEString(EString e);
        public R visitETup(ETup e);
        public R visitEMatch(EMatch e);
        public R visitEVar(EVar e);
        public R visitELam(ELam e);
        public R visitEApp(EApp e);
        public R visitELetrec(ELetrec e);
    }

    public <R> R accept(Visitor<R> v);

    public Stream<EVar> fv();
}

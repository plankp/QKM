package lang.qkm.eval;

import java.util.*;
import lang.qkm.expr.*;

public interface Evaluator {

    public void define(Map<EVar, Expr> defs);

    public void eval(Expr e);
}

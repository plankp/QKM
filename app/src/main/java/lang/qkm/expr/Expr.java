package lang.qkm.expr;

import java.util.*;
import java.util.stream.*;

public interface Expr {

    public Stream<EVar> fv();
}

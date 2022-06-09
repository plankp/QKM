package lang.qkm.type;

import java.util.*;
import java.util.stream.*;
import lang.qkm.match.CtorSet;

public interface Type {

    public Type unwrap();

    public TyApp unapply();

    public Stream<TyVar> fv();

    public void unify(Type other);

    public Type eval(Map<TyVar, ? extends Type> env);

    public default CtorSet getCtorSet() {
        return null;
    }
}

package lang.qkm.type;

import java.util.*;
import java.util.stream.*;
import lang.qkm.match.CtorSet;

public interface Type {

    public Type get();

    public Type expand();

    public Stream<VarType> fv();

    public Type replace(Map<VarType, ? extends Type> map);

    public void unify(Type other);

    public default CtorSet getCtorSet() {
        return null;
    }
}

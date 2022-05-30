package lang.qkm.type;

import java.util.*;
import java.util.stream.*;

public interface Type {

    public Type get();

    public Type expand();

    public Stream<VarType> fv();

    public Type replace(Map<VarType, ? extends Type> map);

    public void unify(Type other);
}

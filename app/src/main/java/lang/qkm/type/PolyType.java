package lang.qkm.type;

import java.util.*;
import java.util.stream.*;

public final class PolyType {

    public final List<VarType> quants;
    public final Type body;

    public PolyType(List<VarType> quants, Type body) {
        this.quants = quants;
        this.body = body;
    }

    @Override
    public String toString() {
        if (this.quants.isEmpty())
            return this.body.toString();

        return this.quants.stream()
                .map(Object::toString)
                .collect(Collectors.joining(" ", "", ". " + this.body));
    }

    public Stream<VarType> fv() {
        final Set<VarType> set = new HashSet<>(this.quants);
        return this.body.fv().filter(tv -> !set.contains(tv));
    }
}

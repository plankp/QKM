package lang.qkm.match;

import lang.qkm.type.Type;

public final class MatchComplete implements Match {

    public final Type type;

    public MatchComplete(Type type) {
        this.type = type;
    }

    @Override
    public Type getType() {
        return this.type;
    }

    @Override
    public String toString() {
        return "_";
    }
}

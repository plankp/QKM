package lang.qkm.match;

public final class MatchComplete implements Match {

    @Override
    public String toString() {
        return "_";
    }

    @Override
    public int hashCode() {
        return MatchComplete.class.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof MatchComplete;
    }
}

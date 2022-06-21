package lang.qkm.match;

import java.util.*;
import java.util.stream.*;
import java.util.function.*;
import lang.qkm.sem.Typed;
import lang.qkm.type.*;
import lang.qkm.util.*;

public interface Match {

    public interface Visitor<R> {

        public R visitMatchAll(MatchAll m);
        public R visitMatchBool(MatchBool m);
        public R visitMatchCtor(MatchCtor m);
        public R visitMatchInt(MatchInt m);
        public R visitMatchString(MatchString m);
        public R visitMatchTup(MatchTup m);
        public R visitMatchOr(MatchOr m);
    }

    public <R> R accept(Visitor<R> v);

    public Stream<String> getCaptures();

    public Object getCtor();

    public List<Match> getArgs();

    public default Match toWildcard(Supplier<? extends Match> gen) {
        return this;
    }

    public default Match simplify() {
        return this;
    }
}

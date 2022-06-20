package lang.qkm.match;

import java.util.*;
import lang.qkm.type.Type;

public interface CtorSet {

    /**
     * For fast spans check for types like iN where checking if two sets have
     * the same dimension is almost always going to be faster than iterating
     * through all possible constructors.
     *
     * by default returns empty which means this fast path is not applicable
     * and the spans method should be used instead.
     *
     * @param sz size of the other set, watch out for sets that are larger
     *           than Integer.MAX_VALUE: they should not use this method.
     *
     * @return if the size is the same or not, or empty
     */
    public default Optional<Boolean> sameSize(int sz) {
        return Optional.empty();
    }

    /**
     * Checks if the supplied collection contains all possible constructors of
     * this type, thereby spanning. If it spans, then return null. Otherwise,
     * return one example of such missing case.
     *
     * @return one example of a missing (valid) constructor or null if spans.
     */
    public Object missingCase(Collection<?> c);

    /**
     * @param id a valid constructor
     *
     * @return types of the argument
     */
    public List<? extends Type> getArgs(Object id);

    /**
     * By default assumes the set is complete (like booleans) where there are
     * finite number of poossible constructors.
     *
     * @return if it is a complete set (e.g. booleans) or not (e.g. strings)
     */
    public default boolean isComplete() {
        return true;
    }
}

package lang.qkm.type;

import java.math.BigInteger;
import java.util.*;

public interface ClosedType extends Type {

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
     * this type (thereby spanning).
     *
     * @param c supplied collection
     *
     * @return if it spans
     */
    public boolean spannedBy(Collection<?> c);

    /**
     * @param id constructor
     *
     * @return types of the argument
     */
    public List<Type> getArgs(Object id);
}
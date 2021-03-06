package ch.unibas.dmi.dbis.cs108pet.common;

import ch.unibas.dmi.dbis.cs108pet.data.Requirement;

import java.util.Comparator;

/**
 * A class which contains several {@link Comparator}s which are used across the software.
 * <p>
 * Since instances of this class are not needed, there is no public constructor for it.
 *
 * @author loris.sauter
 */
public class SortingUtils {
  
  /**
   * A {@link Comparator} for {@link Boolean}s which sorts booleans with value {@code true} first.
   */
  public static final Comparator<Boolean> TRUE_FIRST_COMPARATOR = (b1, b2) -> {
    if (b1 == b2) {
      return 0;
    } else if (b1) {
      return -1;
    } else {
      return 1;
    }
  };
  /**
   * A {@link Comparator} for {@link Boolean}s which sorts booleans with value {@code false} first.
   * This is exactly the {@link Comparator} resulting by invoking {@code {@link SortingUtils#FALSE_FIRST_COMPARATOR}.reversed()}
   */
  public static final Comparator<Boolean> FALSE_FIRST_COMPARATOR = TRUE_FIRST_COMPARATOR.reversed();
  
  /**
   * The {@link Comparator} used to compare two requirement types.
   * Its sortinig is as follows:
   * <ol>
   * <li>{@link Requirement.Type#REGULAR}</li>
   * <li>{@link Requirement.Type#MALUS}</li>
   * <li>{@link Requirement.Type#BONUS}</li>
   * </ol>
   */
  public static final Comparator<Requirement.Type> REQUIREMENT_TYPE_COMPARATOR = Comparator.comparingInt(Requirement.Type::ordinal);
  
  
  /**
   * Utility class, no instance needed.
   */
  private SortingUtils() {
    // No constructor needed
  }
  
}

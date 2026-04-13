package eb.framework1;

import java.util.Random;

/**
 * Shared random-selection utilities used by generators and editors.
 *
 * <p>Each method requires an explicit {@link Random} instance so that callers
 * control seeding (useful for reproducible tests).
 */
public final class RandomUtils {

    private RandomUtils() { /* utility class */ }

    /**
     * Returns one element chosen at random from the given strings.
     *
     * @param random  random-number source
     * @param options one or more candidates; must not be empty
     * @return a randomly selected element
     * @throws IllegalArgumentException if {@code options} is empty
     */
    public static String pick(Random random, String... options) {
        if (options.length == 0) throw new IllegalArgumentException("options must not be empty");
        return options[random.nextInt(options.length)];
    }

    /**
     * Returns {@code "M"} or {@code "F"} at random.
     *
     * @param random random-number source
     * @return {@code "M"} or {@code "F"}
     */
    public static String randomGender(Random random) {
        return random.nextBoolean() ? "M" : "F";
    }

    /**
     * Picks a random value from the array that differs from {@code exclude}.
     *
     * <p>If the array has only one element, that element is returned regardless
     * of whether it matches {@code exclude}.
     *
     * @param random  random-number source
     * @param options candidates; must not be empty
     * @param exclude value to avoid
     * @return a randomly selected element that is not equal to {@code exclude}
     */
    public static String pickDifferent(Random random, String[] options, String exclude) {
        if (options.length <= 1) return options[0];
        String result;
        do {
            result = options[random.nextInt(options.length)];
        } while (result.equals(exclude));
        return result;
    }
}

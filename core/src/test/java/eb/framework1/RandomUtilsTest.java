package eb.framework1;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.Random;

/**
 * Tests for {@link RandomUtils}.
 */
public class RandomUtilsTest {

    @Test
    public void pick_returnsSingleOption() {
        Random r = new Random(42);
        assertEquals("only", RandomUtils.pick(r, "only"));
    }

    @Test
    public void pick_returnsElementFromPool() {
        Random r = new Random(42);
        String[] pool = {"a", "b", "c"};
        String result = RandomUtils.pick(r, pool);
        assertTrue("a".equals(result) || "b".equals(result) || "c".equals(result));
    }

    @Test(expected = IllegalArgumentException.class)
    public void pick_throwsOnEmptyArray() {
        RandomUtils.pick(new Random(), new String[0]);
    }

    @Test
    public void randomGender_returnsMOrF() {
        Random r = new Random(42);
        for (int i = 0; i < 50; i++) {
            String g = RandomUtils.randomGender(r);
            assertTrue("M".equals(g) || "F".equals(g));
        }
    }

    @Test
    public void pickDifferent_avoidsExcluded() {
        Random r = new Random(42);
        String[] options = {"a", "b", "c"};
        for (int i = 0; i < 50; i++) {
            assertNotEquals("b", RandomUtils.pickDifferent(r, options, "b"));
        }
    }

    @Test
    public void pickDifferent_singleElementReturnsIt() {
        Random r = new Random(42);
        assertEquals("only", RandomUtils.pickDifferent(r, new String[]{"only"}, "only"));
    }
}

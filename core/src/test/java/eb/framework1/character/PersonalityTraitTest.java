package eb.framework1.character;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for the {@link PersonalityTrait} enum.
 */
public class PersonalityTraitTest {

    @Test
    public void testClampWithinRange() {
        assertEquals(0, PersonalityTrait.clamp(0));
        assertEquals(-3, PersonalityTrait.clamp(-3));
        assertEquals(3, PersonalityTrait.clamp(3));
        assertEquals(2, PersonalityTrait.clamp(2));
        assertEquals(-1, PersonalityTrait.clamp(-1));
    }

    @Test
    public void testClampOutsideRange() {
        assertEquals(-3, PersonalityTrait.clamp(-5));
        assertEquals(-3, PersonalityTrait.clamp(-100));
        assertEquals(3, PersonalityTrait.clamp(5));
        assertEquals(3, PersonalityTrait.clamp(100));
    }

    @Test
    public void testLabelForValue() {
        assertEquals("strongly dislikes", PersonalityTrait.labelForValue(-3));
        assertEquals("dislikes", PersonalityTrait.labelForValue(-2));
        assertEquals("slightly dislikes", PersonalityTrait.labelForValue(-1));
        assertEquals("neutral", PersonalityTrait.labelForValue(0));
        assertEquals("slightly likes", PersonalityTrait.labelForValue(1));
        assertEquals("likes", PersonalityTrait.labelForValue(2));
        assertEquals("strongly likes", PersonalityTrait.labelForValue(3));
    }

    @Test
    public void testLabelForValueClamped() {
        // Values outside range should still produce a label
        assertEquals("strongly dislikes", PersonalityTrait.labelForValue(-5));
        assertEquals("strongly likes", PersonalityTrait.labelForValue(5));
    }

    @Test
    public void testDescribe() {
        assertEquals("strongly likes sports",
                PersonalityTrait.SPORTS.describe(3));
        assertEquals("dislikes cooking",
                PersonalityTrait.COOKING.describe(-2));
        assertEquals("neutral flirting",
                PersonalityTrait.FLIRTING.describe(0));
    }

    @Test
    public void testAllTraitsHaveCategories() {
        for (PersonalityTrait trait : PersonalityTrait.values()) {
            assertNotNull("Trait " + trait + " must have a category", trait.getCategory());
            assertNotNull("Trait " + trait + " must have a display name", trait.getDisplayName());
            assertFalse("Trait " + trait + " display name should not be empty",
                    trait.getDisplayName().isEmpty());
            assertNotNull("Trait " + trait + " must have a description", trait.getDescription());
            assertFalse("Trait " + trait + " description should not be empty",
                    trait.getDescription().isEmpty());
        }
    }

    @Test
    public void testTraitCount() {
        // We should have 16 traits across 3 categories
        assertEquals(16, PersonalityTrait.values().length);
    }

    @Test
    public void testMinMaxConstants() {
        assertEquals(-3, PersonalityTrait.MIN_VALUE);
        assertEquals(3, PersonalityTrait.MAX_VALUE);
    }
}

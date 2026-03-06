package eb.framework1.character;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for {@link SkillCategory}, {@link SkillCategoryDefinition}, and the
 * {@link NpcSkill#getSkillCategory()} accessor.
 *
 * <p>All tests are pure-Java with no libGDX runtime dependency.
 * {@link eb.framework1.save.GameDataManager} is NOT exercised here because it
 * requires a libGDX {@code Gdx.files} environment; the JSON parsing path is
 * tested indirectly via the {@link SkillCategoryDefinition} POJO instead.
 */
public class SkillCategoryTest {

    // =========================================================================
    // SkillCategory enum — basic values
    // =========================================================================

    @Test
    public void enumHasThreeValues() {
        assertEquals(3, SkillCategory.values().length);
    }

    @Test
    public void workCategory_codeAndName() {
        assertEquals("work",  SkillCategory.WORK.getCode());
        assertEquals("Work",  SkillCategory.WORK.getDisplayName());
    }

    @Test
    public void hobbiesCategory_codeAndName() {
        assertEquals("hobbies",  SkillCategory.HOBBIES.getCode());
        assertEquals("Hobbies",  SkillCategory.HOBBIES.getDisplayName());
    }

    @Test
    public void generalCategory_codeAndName() {
        assertEquals("general",  SkillCategory.GENERAL.getCode());
        assertEquals("General",  SkillCategory.GENERAL.getDisplayName());
    }

    // =========================================================================
    // SkillCategory.fromCode
    // =========================================================================

    @Test
    public void fromCode_work_returnsWORK() {
        assertSame(SkillCategory.WORK, SkillCategory.fromCode("work"));
    }

    @Test
    public void fromCode_hobbies_returnsHOBBIES() {
        assertSame(SkillCategory.HOBBIES, SkillCategory.fromCode("hobbies"));
    }

    @Test
    public void fromCode_general_returnsGENERAL() {
        assertSame(SkillCategory.GENERAL, SkillCategory.fromCode("general"));
    }

    @Test
    public void fromCode_upperCase_matchesCaseInsensitively() {
        assertSame(SkillCategory.WORK,    SkillCategory.fromCode("WORK"));
        assertSame(SkillCategory.HOBBIES, SkillCategory.fromCode("HOBBIES"));
        assertSame(SkillCategory.GENERAL, SkillCategory.fromCode("GENERAL"));
    }

    @Test
    public void fromCode_mixedCase_matchesCaseInsensitively() {
        assertSame(SkillCategory.WORK, SkillCategory.fromCode("Work"));
    }

    @Test
    public void fromCode_unknown_returnsNull() {
        assertNull(SkillCategory.fromCode("unknown"));
        assertNull(SkillCategory.fromCode(""));
    }

    @Test
    public void fromCode_null_returnsNull() {
        assertNull(SkillCategory.fromCode(null));
    }

    // =========================================================================
    // SkillCategoryDefinition POJO
    // =========================================================================

    @Test
    public void definition_storesCodeAndName() {
        SkillCategoryDefinition def = new SkillCategoryDefinition("work", "Work");
        assertEquals("work", def.getCode());
        assertEquals("Work", def.getName());
    }

    @Test
    public void definition_setters_work() {
        SkillCategoryDefinition def = new SkillCategoryDefinition();
        def.setCode("hobbies");
        def.setName("Hobbies");
        assertEquals("hobbies", def.getCode());
        assertEquals("Hobbies", def.getName());
    }

    @Test
    public void definition_toString_containsCodeAndName() {
        SkillCategoryDefinition def = new SkillCategoryDefinition("general", "General");
        String str = def.toString();
        assertTrue(str.contains("general"));
        assertTrue(str.contains("General"));
    }

    // =========================================================================
    // NpcSkill.getSkillCategory — all current skills are WORK
    // =========================================================================

    @Test
    public void allCurrentNpcSkills_haveWorkCategory() {
        for (NpcSkill skill : NpcSkill.values()) {
            assertNotNull("skill category must not be null for " + skill, skill.getSkillCategory());
            assertSame(
                    "NpcSkill." + skill + " should be WORK but was " + skill.getSkillCategory(),
                    SkillCategory.WORK,
                    skill.getSkillCategory());
        }
    }

    @Test
    public void shopClerk_isWorkCategory() {
        assertSame(SkillCategory.WORK, NpcSkill.SHOP_CLERK.getSkillCategory());
    }

    @Test
    public void freelancer_isWorkCategory() {
        assertSame(SkillCategory.WORK, NpcSkill.FREELANCER.getSkillCategory());
    }

    @Test
    public void homemaker_isWorkCategory() {
        assertSame(SkillCategory.WORK, NpcSkill.HOMEMAKER.getSkillCategory());
    }

    // =========================================================================
    // JSON codes match SkillCategory enum codes
    // =========================================================================

    @Test
    public void jsonCodes_matchEnumCodes() {
        // The three JSON entries declared in category_en.json (verified here
        // via the enum constants, which mirror the JSON values).
        String[] expectedCodes = {"work", "hobbies", "general"};
        for (String code : expectedCodes) {
            assertNotNull("fromCode('" + code + "') should return a known constant",
                    SkillCategory.fromCode(code));
        }
    }
}

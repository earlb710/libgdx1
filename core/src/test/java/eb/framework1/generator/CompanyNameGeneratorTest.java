package eb.framework1.generator;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link CompanyNameGenerator}.
 *
 * All tests are pure-Java with no libGDX runtime: data is constructed inline
 * and a seeded {@link Random} is injected for deterministic results.
 */
public class CompanyNameGeneratorTest {

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static CompanyNameGenerator.NameTemplate tmpl(String template, String type) {
        return new CompanyNameGenerator.NameTemplate(template, type);
    }

    private static CompanyNameGenerator.CompanyType type(String id, String name,
                                                          String... buildings) {
        return new CompanyNameGenerator.CompanyType(id, name, Arrays.asList(buildings));
    }

    private static List<CompanyNameGenerator.NameTemplate> sampleTemplates() {
        return Arrays.asList(
            tmpl("$surname & Associates",  "G"),
            tmpl("$surname Holdings",      "G"),
            tmpl("Global Corp.",           "G"),
            tmpl("$surname's Diner",       "food"),
            tmpl("$surname Grill",         "food"),
            tmpl("$surname Pharmacy",      "retail"),
            tmpl("$surname Superstore",    "retail"),
            tmpl("$surname Technologies",  "technology"),
            tmpl("$surname Bank",          "financial")
        );
    }

    private static List<CompanyNameGenerator.CompanyType> sampleTypes() {
        return Arrays.asList(
            type("G",          "Generic"),
            type("food",       "Food & Beverage", "fast_food_restaurant", "coffee_shop"),
            type("retail",     "Retail",          "convenience_store", "supermarket"),
            type("technology", "Technology",      "data_center", "office_building_large"),
            type("financial",  "Financial",       "bank_branch")
        );
    }

    private static List<String> sampleSurnames() {
        return Arrays.asList("Smith", "Jones", "Brown");
    }

    private CompanyNameGenerator gen(long seed) {
        return new CompanyNameGenerator(
                sampleTemplates(), sampleTypes(), sampleSurnames(), new Random(seed));
    }

    // -------------------------------------------------------------------------
    // Construction / counts
    // -------------------------------------------------------------------------

    @Test
    public void testTemplateCounts() {
        CompanyNameGenerator g = gen(1);
        assertEquals(9, g.templateCount());
        assertEquals(3, g.genericTemplateCount());
        assertEquals(2, g.templateCountForType("food"));
        assertEquals(2, g.templateCountForType("retail"));
        assertEquals(1, g.templateCountForType("technology"));
        assertEquals(0, g.templateCountForType("unknown_type"));
    }

    @Test
    public void testTypeCount() {
        assertEquals(5, gen(1).typeCount());
    }

    // -------------------------------------------------------------------------
    // generate() – any template
    // -------------------------------------------------------------------------

    @Test
    public void testGenerateReturnsNonNull() {
        assertNotNull(gen(1).generate());
    }

    @Test
    public void testGenerateReturnsNonEmpty() {
        assertFalse(gen(1).generate().isEmpty());
    }

    @Test
    public void testGenerateVarietyAcrossSeeds() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            seen.add(new CompanyNameGenerator(
                    sampleTemplates(), sampleTypes(), sampleSurnames(),
                    new Random(i)).generate());
        }
        assertTrue("Should see multiple distinct names", seen.size() >= 3);
    }

    // -------------------------------------------------------------------------
    // generate(typeId) – type-filtered
    // -------------------------------------------------------------------------

    @Test
    public void testGenerateByTypeReturnsOnlyTypedTemplates() {
        // Food templates: "$surname's Diner", "$surname Grill" — generics must NOT appear.
        Set<String> validFood = new HashSet<>();
        for (String s : sampleSurnames()) {
            validFood.add(s + "'s Diner");
            validFood.add(s + " Grill");
        }

        for (int i = 0; i < 100; i++) {
            String name = new CompanyNameGenerator(
                    sampleTemplates(), sampleTypes(), sampleSurnames(),
                    new Random(i)).generate("food");
            assertTrue("Expected food-specific name, got generic: " + name,
                    validFood.contains(name));
        }
    }

    @Test
    public void testGenerateByType_neverReturnsGenericWhenTypedTemplatesExist() {
        Set<String> genericNames = new HashSet<>();
        for (String s : sampleSurnames()) {
            genericNames.add(s + " & Associates");
            genericNames.add(s + " Holdings");
        }
        genericNames.add("Global Corp.");

        // Retail has 2 typed templates; run many times and confirm no generic is returned.
        for (int i = 0; i < 200; i++) {
            String name = new CompanyNameGenerator(
                    sampleTemplates(), sampleTypes(), sampleSurnames(),
                    new Random(i)).generate("retail");
            assertFalse("Generic name returned for retail type: " + name,
                    genericNames.contains(name));
        }
    }

    @Test
    public void testGenerateUnknownTypeReturnsGenericOrAll() {
        // Should not throw; should return something
        String result = gen(1).generate("nonexistent_type");
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    public void testGenerateNullTypeReturnsGenericOrAll() {
        String result = gen(1).generate(null);
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    public void testGenerateGenericTypeReturnsFromGenerics() {
        Set<String> generics = new HashSet<>();
        for (String s : sampleSurnames()) {
            generics.add(s + " & Associates");
            generics.add(s + " Holdings");
        }
        generics.add("Global Corp.");

        for (int i = 0; i < 60; i++) {
            String name = new CompanyNameGenerator(
                    sampleTemplates(), sampleTypes(), sampleSurnames(),
                    new Random(i)).generate("G");
            assertTrue("Expected generic name, got: " + name, generics.contains(name));
        }
    }

    // -------------------------------------------------------------------------
    // generateForBuilding(buildingId)
    // -------------------------------------------------------------------------

    @Test
    public void testGenerateForBuildingFoodBuilding() {
        // fast_food_restaurant → type "food"
        String result = gen(1).generateForBuilding("fast_food_restaurant");
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    public void testGenerateForBuildingRetailBuilding() {
        String result = gen(1).generateForBuilding("supermarket");
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    public void testGenerateForBuildingUnknownBuildingFallsBack() {
        // Unknown building → generic / all
        String result = gen(1).generateForBuilding("unknown_building_xyz");
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    public void testGenerateForBuildingNullFallsBack() {
        String result = gen(1).generateForBuilding(null);
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    // -------------------------------------------------------------------------
    // getTypeForBuilding
    // -------------------------------------------------------------------------

    @Test
    public void testGetTypeForBuildingKnown() {
        assertEquals("food",      gen(1).getTypeForBuilding("fast_food_restaurant"));
        assertEquals("food",      gen(1).getTypeForBuilding("coffee_shop"));
        assertEquals("retail",    gen(1).getTypeForBuilding("convenience_store"));
        assertEquals("financial", gen(1).getTypeForBuilding("bank_branch"));
    }

    @Test
    public void testGetTypeForBuildingUnknown() {
        assertNull(gen(1).getTypeForBuilding("some_unknown_building"));
    }

    @Test
    public void testGetTypeForBuildingNull() {
        assertNull(gen(1).getTypeForBuilding(null));
    }

    // -------------------------------------------------------------------------
    // $surname placeholder resolution
    // -------------------------------------------------------------------------

    @Test
    public void testSurnameReplacedInTemplate() {
        Set<String> validSurnames = new HashSet<>(sampleSurnames());
        for (int i = 0; i < 50; i++) {
            String name = new CompanyNameGenerator(
                    sampleTemplates(), sampleTypes(), sampleSurnames(),
                    new Random(i)).generate("G");
            if (name.contains(" & Associates") || name.contains(" Holdings")) {
                String prefix = name.split(" ")[0];
                assertTrue("Surname not in list: " + prefix, validSurnames.contains(prefix));
            }
        }
    }

    @Test
    public void testTemplateWithoutTokenReturnedVerbatim() {
        List<CompanyNameGenerator.NameTemplate> fixed = Arrays.asList(
                tmpl("Acme Corp.", "G"));
        CompanyNameGenerator g = new CompanyNameGenerator(
                fixed, sampleTypes(), sampleSurnames(), new Random(1));
        assertEquals("Acme Corp.", g.generate());
    }

    @Test
    public void testMultipleSurnameTokensBothReplaced() {
        List<CompanyNameGenerator.NameTemplate> dual = Arrays.asList(
                tmpl("$surname & $surname Law", "office"));
        List<CompanyNameGenerator.CompanyType> types = Arrays.asList(
                type("office", "Office", "office_building_small"));
        Set<String> validSurnames = new HashSet<>(sampleSurnames());

        for (int i = 0; i < 30; i++) {
            String name = new CompanyNameGenerator(
                    dual, types, sampleSurnames(), new Random(i)).generate("office");
            assertFalse("$surname token should not remain in: " + name,
                    name.contains("$surname"));
            String[] parts = name.split(" ");
            // parts[0] and parts[2] should be surnames
            assertTrue("First word should be a surname: " + parts[0],
                    validSurnames.contains(parts[0]));
            assertTrue("Third word should be a surname: " + parts[2],
                    validSurnames.contains(parts[2]));
        }
    }

    @Test
    public void testSurnameTokenWithEmptySurnameListReplacedWithEmpty() {
        List<CompanyNameGenerator.NameTemplate> templates = Arrays.asList(
                tmpl("$surname Corp.", "G"));
        CompanyNameGenerator g = new CompanyNameGenerator(
                templates, sampleTypes(), new ArrayList<String>(), new Random(1));
        String result = g.generate();
        assertFalse("$surname token should be resolved", result.contains("$surname"));
    }

    // -------------------------------------------------------------------------
    // Edge cases / null-safety
    // -------------------------------------------------------------------------

    @Test
    public void testNullListsDoNotThrow() {
        CompanyNameGenerator g = new CompanyNameGenerator(null, null, null);
        assertEquals("Unknown Company", g.generate());
        assertEquals("Unknown Company", g.generate("food"));
        assertEquals("Unknown Company", g.generateForBuilding("fast_food_restaurant"));
    }

    @Test
    public void testEmptyTemplateListReturnsUnknownCompany() {
        CompanyNameGenerator g = new CompanyNameGenerator(
                new ArrayList<CompanyNameGenerator.NameTemplate>(), sampleTypes(), sampleSurnames());
        assertEquals("Unknown Company", g.generate());
    }

    @Test
    public void testNameTemplateNullsNormalised() {
        CompanyNameGenerator.NameTemplate t = new CompanyNameGenerator.NameTemplate(null, null);
        assertEquals("", t.template);
        assertEquals(CompanyNameGenerator.TYPE_GENERIC, t.typeId);
    }

    @Test
    public void testCompanyTypeNullsNormalised() {
        CompanyNameGenerator.CompanyType ct = new CompanyNameGenerator.CompanyType(null, null, null);
        assertEquals("", ct.id);
        assertEquals("", ct.name);
        assertTrue(ct.buildings.isEmpty());
    }
}

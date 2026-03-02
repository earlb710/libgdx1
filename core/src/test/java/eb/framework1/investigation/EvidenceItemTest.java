package eb.framework1.investigation;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

/**
 * Tests for {@link EvidenceModifier}, {@link EvidenceItem}, and the
 * {@link EvidenceItem}-related API on {@link CaseFile}.
 */
public class EvidenceItemTest {

    // -------------------------------------------------------------------------
    // EvidenceModifier — basic enum sanity
    // -------------------------------------------------------------------------

    @Test
    public void evidenceModifier_allValuesHaveDisplayName() {
        for (EvidenceModifier m : EvidenceModifier.values()) {
            assertNotNull(m.getDisplayName());
            assertFalse(m.getDisplayName().isEmpty());
        }
    }

    @Test
    public void evidenceModifier_allValuesHaveDescription() {
        for (EvidenceModifier m : EvidenceModifier.values()) {
            assertNotNull(m.getDescription());
            assertFalse(m.getDescription().isEmpty());
        }
    }

    @Test
    public void evidenceModifier_expectedValuesExist() {
        assertNotNull(EvidenceModifier.FINGERPRINTS);
        assertNotNull(EvidenceModifier.BLOOD);
        assertNotNull(EvidenceModifier.DNA);
        assertNotNull(EvidenceModifier.FIBER);
        assertNotNull(EvidenceModifier.HAIR);
        assertNotNull(EvidenceModifier.TOOL_MARKS);
        assertNotNull(EvidenceModifier.SOIL);
        assertNotNull(EvidenceModifier.GLASS_FRAGMENTS);
    }

    // -------------------------------------------------------------------------
    // EvidenceItem.Builder
    // -------------------------------------------------------------------------

    @Test
    public void builder_basicProperties() {
        EvidenceItem item = new EvidenceItem.Builder("Test Item")
                .description("A test object")
                .possibleModifier(EvidenceModifier.FINGERPRINTS)
                .build();
        assertEquals("Test Item", item.getName());
        assertEquals("A test object", item.getDescription());
        assertEquals(1, item.getPossibleModifiers().size());
        assertTrue(item.getPossibleModifiers().contains(EvidenceModifier.FINGERPRINTS));
        assertTrue(item.getSubmittedModifiers().isEmpty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void builder_rejectsNullName() {
        new EvidenceItem.Builder(null).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void builder_rejectsBlankName() {
        new EvidenceItem.Builder("  ").build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void builder_rejectsNullModifier() {
        new EvidenceItem.Builder("Item").possibleModifier(null).build();
    }

    @Test
    public void builder_duplicateModifierSilentlyIgnored() {
        EvidenceItem item = new EvidenceItem.Builder("Item")
                .possibleModifier(EvidenceModifier.BLOOD)
                .possibleModifier(EvidenceModifier.BLOOD)
                .build();
        assertEquals(1, item.getPossibleModifiers().size());
    }

    @Test
    public void builder_defaultDescription_isEmpty() {
        EvidenceItem item = new EvidenceItem.Builder("Thing").build();
        assertEquals("", item.getDescription());
    }

    // -------------------------------------------------------------------------
    // EvidenceItem — submit for analysis
    // -------------------------------------------------------------------------

    @Test
    public void submitForAnalysis_marksModifier() {
        EvidenceItem glass = EvidenceItem.createByName("Drinking Glass");
        assertNotNull(glass);
        assertFalse(glass.isSubmittedForAnalysis(EvidenceModifier.FINGERPRINTS));
        glass.submitForAnalysis(EvidenceModifier.FINGERPRINTS);
        assertTrue(glass.isSubmittedForAnalysis(EvidenceModifier.FINGERPRINTS));
    }

    @Test
    public void submitForAnalysis_multipleModifiers() {
        EvidenceItem glass = EvidenceItem.createByName("Drinking Glass");
        assertNotNull(glass);
        glass.submitForAnalysis(EvidenceModifier.FINGERPRINTS);
        glass.submitForAnalysis(EvidenceModifier.DNA);
        assertEquals(2, glass.getSubmittedModifiers().size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void submitForAnalysis_inapplicableModifier_throws() {
        EvidenceItem glass = EvidenceItem.createByName("Drinking Glass");
        assertNotNull(glass);
        // SOIL is not in DRINKING_GLASS's possible modifiers
        glass.submitForAnalysis(EvidenceModifier.SOIL);
    }

    @Test(expected = IllegalStateException.class)
    public void submitForAnalysis_alreadySubmitted_throws() {
        EvidenceItem glass = EvidenceItem.createByName("Drinking Glass");
        assertNotNull(glass);
        glass.submitForAnalysis(EvidenceModifier.FINGERPRINTS);
        glass.submitForAnalysis(EvidenceModifier.FINGERPRINTS); // duplicate
    }

    @Test(expected = IllegalArgumentException.class)
    public void submitForAnalysis_nullModifier_throws() {
        EvidenceItem item = new EvidenceItem.Builder("Custom")
                .possibleModifier(EvidenceModifier.BLOOD).build();
        item.submitForAnalysis(null);
    }

    @Test
    public void getSubmittedModifiers_isUnmodifiable() {
        EvidenceItem item = new EvidenceItem.Builder("Custom")
                .possibleModifier(EvidenceModifier.BLOOD).build();
        item.submitForAnalysis(EvidenceModifier.BLOOD);
        try {
            item.getSubmittedModifiers().clear();
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }

    @Test
    public void getPossibleModifiers_isUnmodifiable() {
        EvidenceItem item = new EvidenceItem.Builder("Custom")
                .possibleModifier(EvidenceModifier.BLOOD).build();
        try {
            item.getPossibleModifiers().clear();
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }

    // -------------------------------------------------------------------------
    // EvidenceItem catalogue
    // -------------------------------------------------------------------------

    @Test
    public void catalogue_containsExpectedItems() {
        List<EvidenceItem> catalogue = EvidenceItem.getCatalogue();
        List<String> names = new java.util.ArrayList<>();
        for (EvidenceItem item : catalogue) names.add(item.getName());
        assertTrue(names.contains("Drinking Glass"));
        assertTrue(names.contains("Kitchen Knife"));
        assertTrue(names.contains("Cloth"));
        assertTrue(names.contains("Bullet Casing"));
        assertTrue(names.contains("Cigarette"));
        assertTrue(names.contains("Hair Sample"));
        assertTrue(names.contains("Shoe Print"));
        assertTrue(names.contains("Document"));
    }

    @Test
    public void catalogue_isUnmodifiable() {
        try {
            EvidenceItem.getCatalogue().clear();
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }

    @Test
    public void drinkingGlass_possibleModifiers() {
        EvidenceItem glass = EvidenceItem.DRINKING_GLASS;
        assertTrue(glass.getPossibleModifiers().contains(EvidenceModifier.FINGERPRINTS));
        assertTrue(glass.getPossibleModifiers().contains(EvidenceModifier.DNA));
        assertFalse(glass.getPossibleModifiers().contains(EvidenceModifier.BLOOD));
        assertFalse(glass.getPossibleModifiers().contains(EvidenceModifier.SOIL));
    }

    @Test
    public void kitchenKnife_possibleModifiers() {
        EvidenceItem knife = EvidenceItem.KITCHEN_KNIFE;
        assertTrue(knife.getPossibleModifiers().contains(EvidenceModifier.FINGERPRINTS));
        assertTrue(knife.getPossibleModifiers().contains(EvidenceModifier.BLOOD));
        assertTrue(knife.getPossibleModifiers().contains(EvidenceModifier.DNA));
    }

    @Test
    public void bulletCasing_possibleModifiers() {
        EvidenceItem casing = EvidenceItem.BULLET_CASING;
        assertTrue(casing.getPossibleModifiers().contains(EvidenceModifier.FINGERPRINTS));
        assertTrue(casing.getPossibleModifiers().contains(EvidenceModifier.TOOL_MARKS));
        assertFalse(casing.getPossibleModifiers().contains(EvidenceModifier.BLOOD));
    }

    @Test
    public void hairSample_possibleModifiers() {
        EvidenceItem hair = EvidenceItem.HAIR_SAMPLE;
        assertTrue(hair.getPossibleModifiers().contains(EvidenceModifier.DNA));
        assertTrue(hair.getPossibleModifiers().contains(EvidenceModifier.HAIR));
    }

    @Test
    public void shoePrint_possibleModifiers() {
        EvidenceItem shoe = EvidenceItem.SHOE_PRINT;
        assertTrue(shoe.getPossibleModifiers().contains(EvidenceModifier.TOOL_MARKS));
        assertTrue(shoe.getPossibleModifiers().contains(EvidenceModifier.SOIL));
    }

    // -------------------------------------------------------------------------
    // EvidenceItem.createByName
    // -------------------------------------------------------------------------

    @Test
    public void createByName_returnsIndependentCopy() {
        EvidenceItem copy1 = EvidenceItem.createByName("Drinking Glass");
        EvidenceItem copy2 = EvidenceItem.createByName("Drinking Glass");
        assertNotNull(copy1);
        assertNotNull(copy2);
        assertNotSame(copy1, copy2);

        // Submitting on one copy must not affect the other
        copy1.submitForAnalysis(EvidenceModifier.FINGERPRINTS);
        assertFalse(copy2.isSubmittedForAnalysis(EvidenceModifier.FINGERPRINTS));
    }

    @Test
    public void createByName_unknownName_returnsNull() {
        assertNull(EvidenceItem.createByName("Unknown Item"));
    }

    @Test
    public void createByName_null_returnsNull() {
        assertNull(EvidenceItem.createByName(null));
    }

    @Test
    public void createByName_copiedItemPreservesPossibleModifiers() {
        EvidenceItem original = EvidenceItem.DRINKING_GLASS;
        EvidenceItem copy = EvidenceItem.createByName("Drinking Glass");
        assertNotNull(copy);
        assertEquals(original.getPossibleModifiers().size(),
                copy.getPossibleModifiers().size());
        for (EvidenceModifier m : original.getPossibleModifiers()) {
            assertTrue(copy.getPossibleModifiers().contains(m));
        }
    }

    // -------------------------------------------------------------------------
    // CaseFile — addEvidenceItem / getEvidenceItems
    // -------------------------------------------------------------------------

    @Test
    public void caseFile_evidenceItemsEmptyByDefault() {
        CaseFile cf = new CaseFile("Robbery", "desc", "2050-01-02 10:00");
        assertTrue(cf.getEvidenceItems().isEmpty());
    }

    @Test
    public void caseFile_addEvidenceItem() {
        CaseFile cf = new CaseFile("Robbery", "desc", "2050-01-02 10:00");
        EvidenceItem glass = EvidenceItem.createByName("Drinking Glass");
        cf.addEvidenceItem(glass);
        assertEquals(1, cf.getEvidenceItems().size());
        assertSame(glass, cf.getEvidenceItems().get(0));
    }

    @Test(expected = IllegalArgumentException.class)
    public void caseFile_addNullEvidenceItem_throws() {
        CaseFile cf = new CaseFile("Test", "desc", "2050-01-02 10:00");
        cf.addEvidenceItem(null);
    }

    @Test
    public void caseFile_evidenceItemsListIsUnmodifiable() {
        CaseFile cf = new CaseFile("Test", "desc", "2050-01-02 10:00");
        cf.addEvidenceItem(EvidenceItem.createByName("Drinking Glass"));
        try {
            cf.getEvidenceItems().clear();
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }

    @Test
    public void caseFile_getSubmittedEvidenceItems_onlyReturnsSubmitted() {
        CaseFile cf = new CaseFile("Test", "desc", "2050-01-02 10:00");
        EvidenceItem glass = EvidenceItem.createByName("Drinking Glass");
        EvidenceItem knife = EvidenceItem.createByName("Kitchen Knife");

        cf.addEvidenceItem(glass);
        cf.addEvidenceItem(knife);

        // No submissions yet
        assertTrue(cf.getSubmittedEvidenceItems().isEmpty());

        // Submit one analysis on glass
        glass.submitForAnalysis(EvidenceModifier.FINGERPRINTS);
        List<EvidenceItem> submitted = cf.getSubmittedEvidenceItems();
        assertEquals(1, submitted.size());
        assertSame(glass, submitted.get(0));
    }

    @Test
    public void caseFile_fullWorkflow_glassToFingerprints() {
        // Scenario: player finds a glass, adds it to the case, sends it for fingerprints
        CaseFile cf = new CaseFile("Bar Fight", "Assault at the Blue Moon bar", "2050-03-14 22:00");

        EvidenceItem glass = EvidenceItem.createByName("Drinking Glass");
        assertNotNull(glass);
        assertFalse(glass.isSubmittedForAnalysis(EvidenceModifier.FINGERPRINTS));

        cf.addEvidenceItem(glass);
        assertEquals(1, cf.getEvidenceItems().size());

        glass.submitForAnalysis(EvidenceModifier.FINGERPRINTS);
        assertTrue(glass.isSubmittedForAnalysis(EvidenceModifier.FINGERPRINTS));

        List<EvidenceItem> submitted = cf.getSubmittedEvidenceItems();
        assertEquals(1, submitted.size());
        assertEquals("Drinking Glass", submitted.get(0).getName());
    }

    @Test
    public void caseFile_existingStringEvidenceUnaffected() {
        CaseFile cf = new CaseFile("Test", "desc", "2050-01-02 10:00");
        cf.addEvidence("Bloody knife");
        cf.addEvidenceItem(EvidenceItem.createByName("Cigarette"));

        assertEquals(1, cf.getEvidence().size());
        assertEquals(1, cf.getEvidenceItems().size());
    }
}

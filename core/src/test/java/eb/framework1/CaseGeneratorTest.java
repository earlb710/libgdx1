package eb.framework1;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Tests for {@link CaseType}, {@link DiscoveryMethod}, {@link CaseLead},
 * the new {@link CaseFile} fields, and {@link CaseGenerator}.
 */
public class CaseGeneratorTest {

    // -------------------------------------------------------------------------
    // CaseType enum
    // -------------------------------------------------------------------------

    @Test
    public void caseType_allValuesHaveDisplayName() {
        for (CaseType t : CaseType.values()) {
            assertNotNull(t.getDisplayName());
            assertFalse(t.getDisplayName().isEmpty());
        }
    }

    @Test
    public void caseType_allValuesHaveDescription() {
        for (CaseType t : CaseType.values()) {
            assertNotNull(t.getDescription());
            assertFalse(t.getDescription().isEmpty());
        }
    }

    @Test
    public void caseType_expectedValuesExist() {
        assertNotNull(CaseType.MISSING_PERSON);
        assertNotNull(CaseType.INFIDELITY);
        assertNotNull(CaseType.THEFT);
        assertNotNull(CaseType.FRAUD);
        assertNotNull(CaseType.BLACKMAIL);
        assertNotNull(CaseType.MURDER);
        assertNotNull(CaseType.STALKING);
        assertNotNull(CaseType.CORPORATE_ESPIONAGE);
        assertEquals(8, CaseType.values().length);
    }

    @Test
    public void caseType_allValuesHaveDifficultyRangeInBounds() {
        for (CaseType t : CaseType.values()) {
            int min = t.getMinDifficulty();
            int max = t.getMaxDifficulty();
            assertTrue("minDifficulty must be >= 1 for " + t, min >= 1);
            assertTrue("maxDifficulty must be <= 10 for " + t, max <= 10);
            assertTrue("minDifficulty must be <= maxDifficulty for " + t, min <= max);
        }
    }

    @Test
    public void caseType_specificDifficultyRanges() {
        assertEquals(1, CaseType.MISSING_PERSON.getMinDifficulty());
        assertEquals(5, CaseType.MISSING_PERSON.getMaxDifficulty());

        assertEquals(1, CaseType.INFIDELITY.getMinDifficulty());
        assertEquals(4, CaseType.INFIDELITY.getMaxDifficulty());

        assertEquals(1, CaseType.THEFT.getMinDifficulty());
        assertEquals(5, CaseType.THEFT.getMaxDifficulty());

        assertEquals(4, CaseType.FRAUD.getMinDifficulty());
        assertEquals(9, CaseType.FRAUD.getMaxDifficulty());

        assertEquals(3, CaseType.BLACKMAIL.getMinDifficulty());
        assertEquals(7, CaseType.BLACKMAIL.getMaxDifficulty());

        assertEquals(5, CaseType.MURDER.getMinDifficulty());
        assertEquals(10, CaseType.MURDER.getMaxDifficulty());

        assertEquals(2, CaseType.STALKING.getMinDifficulty());
        assertEquals(6, CaseType.STALKING.getMaxDifficulty());

        assertEquals(5, CaseType.CORPORATE_ESPIONAGE.getMinDifficulty());
        assertEquals(10, CaseType.CORPORATE_ESPIONAGE.getMaxDifficulty());
    }

    // -------------------------------------------------------------------------
    // DiscoveryMethod enum
    // -------------------------------------------------------------------------

    @Test
    public void discoveryMethod_allValuesHaveDisplayName() {
        for (DiscoveryMethod m : DiscoveryMethod.values()) {
            assertNotNull(m.getDisplayName());
            assertFalse(m.getDisplayName().isEmpty());
        }
    }

    @Test
    public void discoveryMethod_allValuesHaveDescription() {
        for (DiscoveryMethod m : DiscoveryMethod.values()) {
            assertNotNull(m.getDescription());
            assertFalse(m.getDescription().isEmpty());
        }
    }

    @Test
    public void discoveryMethod_expectedValuesExist() {
        assertNotNull(DiscoveryMethod.INTERVIEW);
        assertNotNull(DiscoveryMethod.SURVEILLANCE);
        assertNotNull(DiscoveryMethod.FORENSICS);
        assertNotNull(DiscoveryMethod.DOCUMENTS);
        assertNotNull(DiscoveryMethod.PHYSICAL_SEARCH);
        assertNotNull(DiscoveryMethod.BACKGROUND_CHECK);
        assertEquals(6, DiscoveryMethod.values().length);
    }

    // -------------------------------------------------------------------------
    // CaseLead
    // -------------------------------------------------------------------------

    @Test
    public void caseLead_defaultNotDiscovered() {
        CaseLead lead = new CaseLead("lead-1", "Full secret", "Vague hint",
                DiscoveryMethod.INTERVIEW);
        assertEquals("lead-1", lead.getId());
        assertEquals("Full secret", lead.getDescription());
        assertEquals("Vague hint", lead.getHint());
        assertEquals(DiscoveryMethod.INTERVIEW, lead.getDiscoveryMethod());
        assertFalse(lead.isDiscovered());
    }

    @Test
    public void caseLead_discover_setsFlag() {
        CaseLead lead = new CaseLead("lead-1", "Secret", "Hint",
                DiscoveryMethod.SURVEILLANCE);
        lead.discover();
        assertTrue(lead.isDiscovered());
    }

    @Test
    public void caseLead_discover_idempotent() {
        CaseLead lead = new CaseLead("lead-1", "Secret", "Hint",
                DiscoveryMethod.FORENSICS);
        lead.discover();
        lead.discover(); // second call must not throw
        assertTrue(lead.isDiscovered());
    }

    @Test(expected = IllegalArgumentException.class)
    public void caseLead_nullId_throws() {
        new CaseLead(null, "secret", "hint", DiscoveryMethod.INTERVIEW);
    }

    @Test(expected = IllegalArgumentException.class)
    public void caseLead_blankId_throws() {
        new CaseLead("  ", "secret", "hint", DiscoveryMethod.INTERVIEW);
    }

    @Test(expected = IllegalArgumentException.class)
    public void caseLead_nullHint_throws() {
        new CaseLead("lead-1", "secret", null, DiscoveryMethod.INTERVIEW);
    }

    @Test(expected = IllegalArgumentException.class)
    public void caseLead_blankHint_throws() {
        new CaseLead("lead-1", "secret", "  ", DiscoveryMethod.INTERVIEW);
    }

    @Test(expected = IllegalArgumentException.class)
    public void caseLead_nullMethod_throws() {
        new CaseLead("lead-1", "secret", "hint", null);
    }

    @Test
    public void caseLead_nullDescription_defaultsToEmpty() {
        CaseLead lead = new CaseLead("lead-1", null, "hint", DiscoveryMethod.DOCUMENTS);
        assertEquals("", lead.getDescription());
    }

    @Test
    public void caseLead_toString_containsId() {
        CaseLead lead = new CaseLead("lead-42", "s", "h", DiscoveryMethod.BACKGROUND_CHECK);
        assertTrue(lead.toString().contains("lead-42"));
    }

    // -------------------------------------------------------------------------
    // CaseFile — new generator fields
    // -------------------------------------------------------------------------

    @Test
    public void caseFile_generatorFields_defaultEmpty() {
        CaseFile cf = new CaseFile("Test", "desc", "2050-01-02 10:00");
        assertNull(cf.getCaseType());
        assertEquals("", cf.getClientName());
        assertEquals("", cf.getSubjectName());
        assertEquals("", cf.getObjective());
        assertTrue(cf.getLeads().isEmpty());
    }

    @Test
    public void caseFile_setAndGetGeneratorFields() {
        CaseFile cf = new CaseFile("Test", "desc", "2050-01-02 10:00");
        cf.setCaseType(CaseType.THEFT);
        cf.setClientName("Alice Smith");
        cf.setSubjectName("Bob Jones");
        cf.setObjective("Find the stolen watch.");

        assertEquals(CaseType.THEFT, cf.getCaseType());
        assertEquals("Alice Smith", cf.getClientName());
        assertEquals("Bob Jones", cf.getSubjectName());
        assertEquals("Find the stolen watch.", cf.getObjective());
    }

    @Test
    public void caseFile_setGeneratorFieldsNullSafe() {
        CaseFile cf = new CaseFile("Test", "desc", "2050-01-02 10:00");
        cf.setClientName(null);
        cf.setSubjectName(null);
        cf.setObjective(null);
        assertEquals("", cf.getClientName());
        assertEquals("", cf.getSubjectName());
        assertEquals("", cf.getObjective());
    }

    @Test
    public void caseFile_addLead_and_getLeads() {
        CaseFile cf = new CaseFile("Test", "desc", "2050-01-02 10:00");
        CaseLead lead = new CaseLead("lead-1", "secret", "hint",
                DiscoveryMethod.INTERVIEW);
        cf.addLead(lead);
        assertEquals(1, cf.getLeads().size());
        assertSame(lead, cf.getLeads().get(0));
    }

    @Test(expected = IllegalArgumentException.class)
    public void caseFile_addNullLead_throws() {
        CaseFile cf = new CaseFile("Test", "desc", "2050-01-02 10:00");
        cf.addLead(null);
    }

    @Test
    public void caseFile_leadsListIsUnmodifiable() {
        CaseFile cf = new CaseFile("Test", "desc", "2050-01-02 10:00");
        cf.addLead(new CaseLead("lead-1", "s", "h", DiscoveryMethod.SURVEILLANCE));
        try {
            cf.getLeads().clear();
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }

    @Test
    public void caseFile_getDiscoveredLeads_filtersCorrectly() {
        CaseFile cf = new CaseFile("Test", "desc", "2050-01-02 10:00");
        CaseLead lead1 = new CaseLead("lead-1", "s1", "h1", DiscoveryMethod.INTERVIEW);
        CaseLead lead2 = new CaseLead("lead-2", "s2", "h2", DiscoveryMethod.FORENSICS);
        CaseLead lead3 = new CaseLead("lead-3", "s3", "h3", DiscoveryMethod.DOCUMENTS);
        cf.addLead(lead1);
        cf.addLead(lead2);
        cf.addLead(lead3);

        assertTrue(cf.getDiscoveredLeads().isEmpty());
        assertEquals(3, cf.getUndiscoveredLeads().size());

        lead1.discover();
        lead3.discover();

        List<CaseLead> discovered = cf.getDiscoveredLeads();
        assertEquals(2, discovered.size());
        assertTrue(discovered.contains(lead1));
        assertTrue(discovered.contains(lead3));

        List<CaseLead> undiscovered = cf.getUndiscoveredLeads();
        assertEquals(1, undiscovered.size());
        assertTrue(undiscovered.contains(lead2));
    }

    // -------------------------------------------------------------------------
    // CaseGenerator — construction validation
    // -------------------------------------------------------------------------

    @Test(expected = IllegalArgumentException.class)
    public void caseGenerator_nullNameGen_throws() {
        new CaseGenerator(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void caseGenerator_nullType_throws() {
        CaseGenerator gen = makeGenerator(42L);
        gen.generate(null, "2050-01-01 09:00");
    }

    // -------------------------------------------------------------------------
    // CaseGenerator — generated case structure
    // -------------------------------------------------------------------------

    @Test
    public void generate_returnsOpenCase() {
        CaseGenerator gen = makeGenerator(1L);
        CaseFile cf = gen.generate("2050-05-01 09:00");
        assertEquals(CaseFile.Status.OPEN, cf.getStatus());
        assertTrue(cf.isOpen());
    }

    @Test
    public void generate_setsDateOpened() {
        CaseGenerator gen = makeGenerator(2L);
        CaseFile cf = gen.generate("2050-05-02 10:00");
        assertEquals("2050-05-02 10:00", cf.getDateOpened());
    }

    @Test
    public void generate_populatesCaseType() {
        CaseGenerator gen = makeGenerator(3L);
        CaseFile cf = gen.generate("2050-05-03 11:00");
        assertNotNull(cf.getCaseType());
    }

    @Test
    public void generate_populatesClientName() {
        CaseGenerator gen = makeGenerator(4L);
        CaseFile cf = gen.generate("2050-05-04 09:00");
        assertFalse(cf.getClientName().isEmpty());
    }

    @Test
    public void generate_populatesSubjectName() {
        CaseGenerator gen = makeGenerator(5L);
        CaseFile cf = gen.generate("2050-05-05 09:00");
        assertFalse(cf.getSubjectName().isEmpty());
    }

    @Test
    public void generate_populatesObjective() {
        CaseGenerator gen = makeGenerator(6L);
        CaseFile cf = gen.generate("2050-05-06 09:00");
        assertFalse(cf.getObjective().isEmpty());
    }

    @Test
    public void generate_populatesDescription() {
        CaseGenerator gen = makeGenerator(7L);
        CaseFile cf = gen.generate("2050-05-07 09:00");
        assertFalse(cf.getDescription().isEmpty());
    }

    @Test
    public void generate_nameIncludesCaseType() {
        CaseGenerator gen = makeGenerator(8L);
        CaseFile cf = gen.generate(CaseType.MISSING_PERSON, "2050-05-08 09:00");
        assertTrue(cf.getName().contains(CaseType.MISSING_PERSON.getDisplayName()));
    }

    @Test
    public void generate_nameIncludesSubjectName() {
        CaseGenerator gen = makeGenerator(9L);
        CaseFile cf = gen.generate(CaseType.THEFT, "2050-05-09 09:00");
        assertTrue(cf.getName().contains(cf.getSubjectName()));
    }

    @Test
    public void generate_atLeastOneLeadProduced() {
        for (CaseType type : CaseType.values()) {
            CaseGenerator gen = makeGenerator(type.ordinal());
            CaseFile cf = gen.generate(type, "2050-05-10 09:00");
            assertFalse("Expected leads for type " + type, cf.getLeads().isEmpty());
        }
    }

    @Test
    public void generate_allLeadsUndiscoveredInitially() {
        CaseGenerator gen = makeGenerator(10L);
        CaseFile cf = gen.generate("2050-05-11 09:00");
        for (CaseLead lead : cf.getLeads()) {
            assertFalse("Lead should not be discovered initially: " + lead, lead.isDiscovered());
        }
        assertEquals(cf.getLeads().size(), cf.getUndiscoveredLeads().size());
        assertTrue(cf.getDiscoveredLeads().isEmpty());
    }

    @Test
    public void generate_eachLeadHasNonBlankHint() {
        for (CaseType type : CaseType.values()) {
            CaseGenerator gen = makeGenerator(type.ordinal() + 100);
            CaseFile cf = gen.generate(type, "2050-05-12 09:00");
            for (CaseLead lead : cf.getLeads()) {
                assertFalse("Lead hint is blank for type " + type, lead.getHint().isEmpty());
            }
        }
    }

    @Test
    public void generate_eachLeadHasDiscoveryMethod() {
        for (CaseType type : CaseType.values()) {
            CaseGenerator gen = makeGenerator(type.ordinal() + 200);
            CaseFile cf = gen.generate(type, "2050-05-13 09:00");
            for (CaseLead lead : cf.getLeads()) {
                assertNotNull("Lead method is null for type " + type,
                        lead.getDiscoveryMethod());
            }
        }
    }

    @Test
    public void generate_specificType_respectsType() {
        CaseGenerator gen = makeGenerator(11L);
        CaseFile cf = gen.generate(CaseType.FRAUD, "2050-05-14 09:00");
        assertEquals(CaseType.FRAUD, cf.getCaseType());
    }

    @Test
    public void generate_twoCallsProduceDifferentCases() {
        CaseGenerator gen = makeGenerator(12L);
        CaseFile cf1 = gen.generate("2050-06-01 09:00");
        CaseFile cf2 = gen.generate("2050-06-01 09:00");
        // Names or types will differ given different random draws
        assertNotEquals(cf1.getId(), cf2.getId());
    }

    @Test
    public void generate_descriptionMentionsSubjectOrClient() {
        for (CaseType type : CaseType.values()) {
            CaseGenerator gen = makeGenerator(type.ordinal() + 300);
            CaseFile cf = gen.generate(type, "2050-05-15 09:00");
            boolean mentionsOne = cf.getDescription().contains(cf.getClientName())
                    || cf.getDescription().contains(cf.getSubjectName());
            assertTrue("Description should mention client or subject for type " + type,
                    mentionsOne);
        }
    }

    @Test
    public void generate_objectiveMentionsSubject() {
        for (CaseType type : CaseType.values()) {
            CaseGenerator gen = makeGenerator(type.ordinal() + 400);
            CaseFile cf = gen.generate(type, "2050-05-16 09:00");
            assertTrue("Objective should mention subject for type " + type,
                    cf.getObjective().contains(cf.getSubjectName()));
        }
    }

    @Test
    public void generate_nullDateOpened_doesNotThrow() {
        CaseGenerator gen = makeGenerator(13L);
        CaseFile cf = gen.generate(CaseType.STALKING, null);
        assertNotNull(cf);
        assertEquals("", cf.getDateOpened());
    }

    @Test
    public void generate_existingCaseFileFieldsIntact() {
        // Verify that generating a case does not break the existing CaseFile API
        CaseGenerator gen = makeGenerator(14L);
        CaseFile cf = gen.generate(CaseType.MURDER, "2050-05-17 09:00");
        assertTrue(cf.getClues().isEmpty());
        assertTrue(cf.getEvidence().isEmpty());
        assertTrue(cf.getNotes().isEmpty());
        assertTrue(cf.getEvidenceItems().isEmpty());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Builds a CaseGenerator backed by seeded name lists and a seeded Random. */
    private static CaseGenerator makeGenerator(long seed) {
        List<PersonNameGenerator.NameEntry> firstNames = Arrays.asList(
                new PersonNameGenerator.NameEntry("Alice", "F"),
                new PersonNameGenerator.NameEntry("Bob",   "M"),
                new PersonNameGenerator.NameEntry("Carol", "F"),
                new PersonNameGenerator.NameEntry("Dave",  "M"),
                new PersonNameGenerator.NameEntry("Eve",   "F"),
                new PersonNameGenerator.NameEntry("Frank", "M")
        );
        List<String> surnames = Arrays.asList(
                "Smith", "Jones", "Williams", "Taylor", "Brown"
        );
        PersonNameGenerator nameGen = new PersonNameGenerator(firstNames, surnames,
                new Random(seed));
        return new CaseGenerator(nameGen, new Random(seed));
    }
}

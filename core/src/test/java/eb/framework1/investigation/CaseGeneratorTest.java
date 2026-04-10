package eb.framework1.investigation;

import eb.framework1.generator.*;
import eb.framework1.phone.*;


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
        assertNull("storyRoot should be null for manually created cases", cf.getStoryRoot());
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
    public void generate_complexityIsInRange() {
        for (CaseType type : CaseType.values()) {
            CaseGenerator gen = makeGenerator(type.ordinal() + 500);
            CaseFile cf = gen.generate(type, "2050-05-18 09:00");
            int c = cf.getComplexity();
            assertTrue("complexity must be >= 1 for " + type, c >= 1);
            assertTrue("complexity must be <= 3 for " + type, c <= 3);
        }
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
    // CaseGenerator — story tree generation
    // -------------------------------------------------------------------------

    @Test
    public void generate_storyRootIsNotNull() {
        CaseGenerator gen = makeGenerator(15L);
        CaseFile cf = gen.generate("2050-06-01 09:00");
        assertNotNull("Generated case must have a story root", cf.getStoryRoot());
    }

    @Test
    public void generate_storyRootTypeIsRoot() {
        CaseGenerator gen = makeGenerator(16L);
        CaseFile cf = gen.generate("2050-06-01 09:00");
        assertEquals(CaseStoryNode.NodeType.ROOT, cf.getStoryRoot().getNodeType());
    }

    @Test
    public void generate_storyPhaseCountMatchesComplexity() {
        // Test many seeds to cover all three complexity values across all types.
        for (CaseType type : CaseType.values()) {
            for (int seed = 0; seed < 30; seed++) {
                CaseGenerator gen = makeGenerator(seed + 600L);
                CaseFile cf = gen.generate(type, "2050-06-01 09:00");
                int phases = cf.getStoryRoot().getChildren().size();
                assertEquals("Phase count must equal complexity for type "
                        + type + " seed " + seed, cf.getComplexity(), phases);
            }
        }
    }

    @Test
    public void generate_storyPhaseNodesAreAllPlotTwist() {
        for (CaseType type : CaseType.values()) {
            CaseGenerator gen = makeGenerator(type.ordinal() + 700L);
            CaseFile cf = gen.generate(type, "2050-06-01 09:00");
            for (CaseStoryNode phase : cf.getStoryRoot().getChildren()) {
                assertEquals("Phase child must be PLOT_TWIST for type " + type,
                        CaseStoryNode.NodeType.PLOT_TWIST, phase.getNodeType());
            }
        }
    }

    @Test
    public void generate_storyMajorProgressNodesUnderEachPhase() {
        for (CaseType type : CaseType.values()) {
            CaseGenerator gen = makeGenerator(type.ordinal() + 800L);
            CaseFile cf = gen.generate(type, "2050-06-01 09:00");
            for (CaseStoryNode phase : cf.getStoryRoot().getChildren()) {
                assertFalse("Each phase must have major-progress children", phase.getChildren().isEmpty());
                for (CaseStoryNode major : phase.getChildren()) {
                    assertEquals("Children of a phase must be MAJOR_PROGRESS for type " + type,
                            CaseStoryNode.NodeType.MAJOR_PROGRESS, major.getNodeType());
                }
            }
        }
    }

    @Test
    public void generate_storyLeafNodesAreAllActions() {
        for (CaseType type : CaseType.values()) {
            CaseGenerator gen = makeGenerator(type.ordinal() + 900L);
            CaseFile cf = gen.generate(type, "2050-06-01 09:00");
            assertAllLeavesAreActions(cf.getStoryRoot(), type.name());
        }
    }

    @Test
    public void generate_storyNoneCompleteInitially() {
        CaseGenerator gen = makeGenerator(19L);
        CaseFile cf = gen.generate("2050-06-01 09:00");
        assertFalse("Newly generated story tree must not be fully complete",
                cf.getStoryRoot().isFullyComplete());
    }

    @Test
    public void generate_storySecondPhaseLockedInitially() {
        // If complexity >= 2 the second phase must not be available until first is complete.
        for (int seed = 0; seed < 50; seed++) {
            CaseGenerator gen = makeGenerator(seed + 1000L);
            CaseFile cf = gen.generate("2050-06-01 09:00");
            CaseStoryNode root = cf.getStoryRoot();
            if (root.getChildren().size() >= 2) {
                assertFalse("Second phase must be locked until first phase is complete (seed " + seed + ")",
                        root.isChildAvailable(1));
                return; // found a multi-phase case — test passed
            }
        }
        // If all 50 seeds produced complexity-1 cases the assertion is vacuously true;
        // generate_storyPhaseCountMatchesComplexity covers the multi-phase path.
    }

    @Test
    public void generate_storySubjectNameAppearedInNodeText() {
        CaseGenerator gen = makeGenerator(20L);
        CaseFile cf = gen.generate(CaseType.MISSING_PERSON, "2050-06-01 09:00");
        String subject = cf.getSubjectName();
        assertTrue("Subject name should appear somewhere in the story tree text",
                anyNodeContains(cf.getStoryRoot(), subject));
    }

    // -------------------------------------------------------------------------
    // Meeting dialogue generation
    // -------------------------------------------------------------------------

    @Test
    public void generate_meetingDialogueIsNotEmpty() {
        CaseGenerator gen = makeGenerator(42L);
        CaseFile cf = gen.generate("2050-06-01 09:00");
        assertNotNull("meetingDialogue must not be null", cf.getMeetingDialogue());
        assertFalse("meetingDialogue must not be empty", cf.getMeetingDialogue().isEmpty());
    }

    @Test
    public void generate_meetingDialogueContainsFourBaseQuestions() {
        for (CaseType type : CaseType.values()) {
            CaseGenerator gen = makeGenerator(type.ordinal() + 200L);
            CaseFile cf = gen.generate(type, "2050-06-01 09:00");
            List<MeetingQA> dialogue = cf.getMeetingDialogue();
            // At minimum there are 4 base questions
            assertTrue("At least 4 Q&As expected for " + type, dialogue.size() >= 4);
            assertEquals("First question must be about the task",
                    "What exactly do you need me to do?", dialogue.get(0).getQuestion());
            assertEquals("Second question must be about the subject",
                    "Tell me more about the subject.", dialogue.get(1).getQuestion());
        }
    }

    @Test
    public void generate_meetingDialogueAnswersAreNonEmpty() {
        for (CaseType type : CaseType.values()) {
            CaseGenerator gen = makeGenerator(type.ordinal() + 300L);
            CaseFile cf = gen.generate(type, "2050-06-01 09:00");
            for (MeetingQA qa : cf.getMeetingDialogue()) {
                assertNotNull("Answer must not be null for type " + type, qa.getAnswer());
                assertFalse("Answer must not be empty for type " + type + " Q: " + qa.getQuestion(),
                        qa.getAnswer().trim().isEmpty());
            }
        }
    }

    @Test
    public void generate_meetingDialogueHasPhaseEntriesForComplexCase() {
        // With complexity >= 2 the dialogue should contain more than 4 entries
        // (base 4 + one per PLOT_TWIST phase). Run enough seeds to find at least one.
        for (int seed = 0; seed < 100; seed++) {
            CaseGenerator gen = makeGenerator(seed + 400L);
            CaseFile cf = gen.generate("2050-06-01 09:00");
            if (cf.getComplexity() >= 2) {
                assertTrue("Complex case (complexity " + cf.getComplexity()
                        + ") should have more than 4 Q&A entries (got " + cf.getMeetingDialogue().size() + ")",
                        cf.getMeetingDialogue().size() > 4);
                return; // Test passed
            }
        }
        // If no complexity>=2 case found in 100 seeds the count test is vacuously satisfied;
        // generate_storyPhaseCountMatchesComplexity already covers multi-phase trees.
    }

    @Test
    public void generate_meetingDialoguePhaseQuestionsReferencePhaseTitle() {
        for (CaseType type : CaseType.values()) {
            CaseGenerator gen = makeGenerator(type.ordinal() + 500L);
            CaseFile cf = gen.generate(type, "2050-06-01 09:00");
            List<MeetingQA> dialogue = cf.getMeetingDialogue();
            // Each Q&A beyond the first 4 corresponds to a PLOT_TWIST phase
            int phaseCount = cf.getStoryRoot().getChildren().size();
            assertEquals("Dialogue size must be 4 + phaseCount for " + type,
                    4 + phaseCount, dialogue.size());
            // Check that phase questions reference the phase title
            for (int i = 0; i < phaseCount; i++) {
                String phaseTitle = cf.getStoryRoot().getChildren().get(i).getTitle();
                String question = dialogue.get(4 + i).getQuestion();
                assertTrue("Phase question for " + type + " phase " + i
                        + " should reference the phase title '" + phaseTitle + "'",
                        question.contains(phaseTitle));
            }
        }
    }

    @Test
    public void generate_meetingDialogueObjectiveAppearsInFirstAnswer() {
        CaseGenerator gen = makeGenerator(123L);
        CaseFile cf = gen.generate(CaseType.THEFT, "2050-06-01 09:00");
        String firstAnswer = cf.getMeetingDialogue().get(0).getAnswer();
        // The first answer should be (or contain) the case objective
        String objective = cf.getObjective();
        assertEquals("First Q&A answer must equal the case objective", objective, firstAnswer);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Recursively asserts that every leaf in the tree has type ACTION. */
    private static void assertAllLeavesAreActions(CaseStoryNode node, String context) {
        if (node.getChildren().isEmpty()) {
            assertEquals("Leaf node must be ACTION for " + context
                    + ", got " + node.getNodeType(), CaseStoryNode.NodeType.ACTION, node.getNodeType());
        } else {
            for (CaseStoryNode child : node.getChildren()) {
                assertAllLeavesAreActions(child, context);
            }
        }
    }

    /** Returns true if any node's title or description contains the given text. */
    private static boolean anyNodeContains(CaseStoryNode node, String text) {
        if (node.getTitle().contains(text) || node.getDescription().contains(text)) return true;
        for (CaseStoryNode child : node.getChildren()) {
            if (anyNodeContains(child, text)) return true;
        }
        return false;
    }

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

    // -------------------------------------------------------------------------
    // Murder case — death-time-determination lead
    // -------------------------------------------------------------------------

    @Test
    public void murderCase_hasFiveLeads() {
        CaseGenerator gen = makeGenerator(200L);
        CaseFile cf = gen.generate(CaseType.MURDER, "2050-01-05 09:00");
        assertEquals("Murder case should have 5 leads", 5, cf.getLeads().size());
    }

    @Test
    public void murderCase_hasTimeOfDeathLead() {
        CaseGenerator gen = makeGenerator(201L);
        CaseFile cf = gen.generate(CaseType.MURDER, "2050-01-05 09:00");
        boolean found = false;
        for (CaseLead lead : cf.getLeads()) {
            if (lead.getHint().contains("time of death")
                    && lead.getDiscoveryMethod() == DiscoveryMethod.FORENSICS) {
                found = true;
                break;
            }
        }
        assertTrue("Murder leads must include a FORENSICS lead about determining time of death",
                found);
    }
}

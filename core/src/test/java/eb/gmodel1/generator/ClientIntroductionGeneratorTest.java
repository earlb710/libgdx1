package eb.gmodel1.generator;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link ClientIntroductionGenerator}.
 *
 * <p>All tests are pure-Java with no libGDX dependency.
 */
public class ClientIntroductionGeneratorTest {

    // =========================================================================
    // buildIntroduction — non-null and non-empty
    // =========================================================================

    @Test
    public void buildIntroduction_returnsNonNull() {
        String intro = ClientIntroductionGenerator.buildIntroduction(
                "Alice Smith", "F", 7, 8, 5, "I need you to find my missing husband.");
        assertNotNull(intro);
        assertFalse(intro.trim().isEmpty());
    }

    @Test
    public void buildIntroduction_containsClientName() {
        String intro = ClientIntroductionGenerator.buildIntroduction(
                "Bob Jones", "M", 5, 5, 5, "Someone is blackmailing me.");
        assertTrue("Introduction should contain the client's name",
                intro.contains("Bob Jones"));
    }

    @Test
    public void buildIntroduction_nullNameHandled() {
        // null name should not throw — defaults to "Unknown"
        String intro = ClientIntroductionGenerator.buildIntroduction(
                null, "M", 5, 5, 5, "I have a problem.");
        assertNotNull(intro);
        assertFalse(intro.trim().isEmpty());
    }

    @Test
    public void buildIntroduction_blankNameHandled() {
        String intro = ClientIntroductionGenerator.buildIntroduction(
                "   ", "F", 5, 5, 5, "My sister is missing.");
        assertNotNull(intro);
    }

    // =========================================================================
    // buildIntroduction — caseContext included when non-empty
    // =========================================================================

    @Test
    public void buildIntroduction_caseContextIncluded() {
        String context = "I believe my business partner is stealing from the company.";
        String intro = ClientIntroductionGenerator.buildIntroduction(
                "Carol Brown", "F", 5, 5, 5, context);
        assertTrue("Introduction should reference the case context",
                intro.contains("business partner"));
    }

    @Test
    public void buildIntroduction_emptyCaseContext_fallsBackToGenericLine() {
        String intro = ClientIntroductionGenerator.buildIntroduction(
                "Dave Williams", "M", 5, 5, 5, "");
        assertNotNull(intro);
        assertFalse(intro.trim().isEmpty());
    }

    @Test
    public void buildIntroduction_nullCaseContext_handledGracefully() {
        String intro = ClientIntroductionGenerator.buildIntroduction(
                "Eve Taylor", "F", 5, 5, 5, null);
        assertNotNull(intro);
    }

    // =========================================================================
    // buildIntroduction — anxious style (nervousness >= 7)
    // =========================================================================

    @Test
    public void buildIntroduction_anxious_openingContainsHesitation() {
        String intro = ClientIntroductionGenerator.buildIntroduction(
                "Frank Moore", "M", 4, 4, 9, "Something is very wrong.");
        // Anxious opening starts with "I…" or similar
        assertTrue("Anxious intro should start with an ellipsis/hesitation marker",
                intro.contains("…") || intro.startsWith("I…"));
    }

    // =========================================================================
    // buildIntroduction — polished style (charisma >= 7)
    // =========================================================================

    @Test
    public void buildIntroduction_polished_openingIsFormalLanguage() {
        String intro = ClientIntroductionGenerator.buildIntroduction(
                "Grace Hall", "F", 9, 4, 3, "I have a delicate matter to discuss.");
        assertTrue("Polished intro should use formal language",
                intro.contains("Good day") || intro.contains("appreciate"));
    }

    // =========================================================================
    // buildIntroduction — male vs. female pronoun variation
    // =========================================================================

    @Test
    public void buildIntroduction_femaleHighEmpathyAnxious_usesShePronoun() {
        String intro = ClientIntroductionGenerator.buildIntroduction(
                "Helen Wright", "F", 4, 9, 9, "My brother is missing.");
        // When empathy >= 7 AND nervousness >= 7, the pronoun block uses "she has"
        assertTrue("Female high-empathy+anxious intro should contain 'she has'",
                intro.contains("she has"));
    }

    @Test
    public void buildIntroduction_maleHighEmpathyAnxious_usesHesPronoun() {
        String intro = ClientIntroductionGenerator.buildIntroduction(
                "Ian Clark", "M", 4, 9, 9, "My sister is missing.");
        assertTrue("Male high-empathy+anxious intro should contain 'he has'",
                intro.contains("he has"));
    }

    // =========================================================================
    // buildDetectiveReply — basics
    // =========================================================================

    @Test
    public void buildDetectiveReply_returnsNonNull() {
        assertNotNull(ClientIntroductionGenerator.buildDetectiveReply("Alice Smith"));
    }

    @Test
    public void buildDetectiveReply_containsFirstName() {
        String reply = ClientIntroductionGenerator.buildDetectiveReply("Alice Smith");
        assertTrue("Reply should use the client's first name",
                reply.contains("Alice"));
    }

    @Test
    public void buildDetectiveReply_nullNameHandled() {
        // Should not throw; falls back to "there"
        String reply = ClientIntroductionGenerator.buildDetectiveReply(null);
        assertNotNull(reply);
        assertTrue("Reply should contain 'there' when name is null",
                reply.contains("there"));
    }

    @Test
    public void buildDetectiveReply_emptyNameHandled() {
        String reply = ClientIntroductionGenerator.buildDetectiveReply("");
        assertNotNull(reply);
    }

    @Test
    public void buildDetectiveReply_singleWordName_usedAsIs() {
        String reply = ClientIntroductionGenerator.buildDetectiveReply("Monique");
        assertTrue(reply.contains("Monique"));
    }

    // =========================================================================
    // buildQuestions — structure
    // =========================================================================

    @Test
    public void buildQuestions_returnsNonNullList() {
        assertNotNull(ClientIntroductionGenerator.buildQuestions());
    }

    @Test
    public void buildQuestions_returnsAtLeastFourQuestions() {
        List<String> qs = ClientIntroductionGenerator.buildQuestions();
        assertTrue("Should have at least 4 follow-up questions", qs.size() >= 4);
    }

    @Test
    public void buildQuestions_noNullOrBlankEntries() {
        for (String q : ClientIntroductionGenerator.buildQuestions()) {
            assertNotNull(q);
            assertFalse("Question must not be blank", q.trim().isEmpty());
        }
    }

    @Test(expected = UnsupportedOperationException.class)
    public void buildQuestions_listIsUnmodifiable() {
        ClientIntroductionGenerator.buildQuestions().add("Extra question");
    }

    // =========================================================================
    // firstName helper
    // =========================================================================

    @Test
    public void firstName_fullName_returnsFirstToken() {
        assertEquals("Alice", ClientIntroductionGenerator.firstName("Alice Smith"));
    }

    @Test
    public void firstName_singleName_returnsSelf() {
        assertEquals("Alice", ClientIntroductionGenerator.firstName("Alice"));
    }

    @Test
    public void firstName_nullName_returnsFallback() {
        assertEquals("there", ClientIntroductionGenerator.firstName(null));
    }

    @Test
    public void firstName_emptyName_returnsFallback() {
        assertEquals("there", ClientIntroductionGenerator.firstName(""));
    }
}

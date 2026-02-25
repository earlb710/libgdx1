package eb.framework1;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests for the phone contact feature:
 * {@link Profile#markContactPhoned}, {@link Profile#isContactPhoned},
 * and the contact list derivation logic that {@code MainScreen.buildPhoneContacts}
 * mirrors.
 */
public class PhoneContactTest {

    // -------------------------------------------------------------------------
    // Profile phoned-contact tracking
    // -------------------------------------------------------------------------

    @Test
    public void newProfileHasNoPhonedContacts() {
        Profile p = new Profile("Alice", "Female", "Normal");
        assertFalse(p.isContactPhoned("case-1", "John Smith"));
    }

    @Test
    public void markContactPhonedPersists() {
        Profile p = new Profile("Alice", "Female", "Normal");
        p.markContactPhoned("case-1", "John Smith");
        assertTrue(p.isContactPhoned("case-1", "John Smith"));
    }

    @Test
    public void phonedStatusIsContactSpecific() {
        Profile p = new Profile("Alice", "Female", "Normal");
        p.markContactPhoned("case-1", "John Smith");
        // Different name → not phoned
        assertFalse(p.isContactPhoned("case-1", "Jane Doe"));
        // Different case → not phoned
        assertFalse(p.isContactPhoned("case-2", "John Smith"));
    }

    @Test
    public void markContactPhonedIgnoresNulls() {
        Profile p = new Profile("Alice", "Female", "Normal");
        // Should not throw
        p.markContactPhoned(null, "John Smith");
        p.markContactPhoned("case-1", null);
        assertFalse(p.isContactPhoned(null, "John Smith"));
        assertFalse(p.isContactPhoned("case-1", null));
    }

    @Test
    public void markSameContactTwiceIsIdempotent() {
        Profile p = new Profile("Alice", "Female", "Normal");
        p.markContactPhoned("case-1", "John Smith");
        p.markContactPhoned("case-1", "John Smith"); // second call should not throw or duplicate
        assertTrue(p.isContactPhoned("case-1", "John Smith"));
    }

    // -------------------------------------------------------------------------
    // Contact list derivation (mirrors MainScreen.buildPhoneContacts logic)
    // -------------------------------------------------------------------------

    /**
     * Simulates the contact-list building logic from {@code MainScreen}
     * without requiring a running game instance.
     */
    private static List<PhoneContact> buildContactsFromProfile(Profile profile) {
        List<PhoneContact> result = new ArrayList<>();
        for (CaseFile cf : profile.getCaseFiles()) {
            if (!cf.isOpen()) continue;
            String caseId = cf.getId();
            java.util.Set<String> seen = new java.util.LinkedHashSet<>();
            String client = cf.getClientName();
            if (client != null && !client.trim().isEmpty() && seen.add(client.trim())) {
                result.add(new PhoneContact(client.trim(), caseId, true,
                        profile.isContactPhoned(caseId, client.trim())));
            }
            String subject = cf.getSubjectName();
            if (subject != null && !subject.trim().isEmpty() && seen.add(subject.trim())) {
                result.add(new PhoneContact(subject.trim(), caseId, true,
                        profile.isContactPhoned(caseId, subject.trim())));
            }
        }
        return result;
    }

    @Test
    public void noOpenCasesProducesEmptyContactList() {
        Profile p = new Profile("Bob", "Male", "Normal");
        assertTrue(buildContactsFromProfile(p).isEmpty());
    }

    @Test
    public void openCaseContactsAreIncluded() {
        Profile p = new Profile("Bob", "Male", "Normal");
        CaseFile cf = new CaseFile("Case1", "desc", "2050-01-02 10:00");
        cf.setClientName("Alice Client");
        cf.setSubjectName("Bob Subject");
        p.addCaseFile(cf);

        List<PhoneContact> contacts = buildContactsFromProfile(p);
        assertEquals(2, contacts.size());
        assertEquals("Alice Client", contacts.get(0).name);
        assertEquals("Bob Subject",  contacts.get(1).name);
        assertTrue("All contacts from open case should have star", contacts.get(0).caseOpen);
        assertTrue("All contacts from open case should have star", contacts.get(1).caseOpen);
    }

    @Test
    public void closedCaseContactsAreExcluded() {
        Profile p = new Profile("Carol", "Female", "Normal");
        CaseFile cf = new CaseFile("Case1", "desc", "2050-01-02 10:00");
        cf.setClientName("Dead Contact");
        cf.close("2050-02-01 10:00");
        p.addCaseFile(cf);

        List<PhoneContact> contacts = buildContactsFromProfile(p);
        assertTrue("Closed-case contacts must not appear in the phone", contacts.isEmpty());
    }

    @Test
    public void phonedContactsAreMarked() {
        Profile p = new Profile("Dave", "Male", "Normal");
        CaseFile cf = new CaseFile("Case1", "desc", "2050-01-02 10:00");
        cf.setClientName("Alice Client");
        cf.setSubjectName("Bob Subject");
        p.addCaseFile(cf);

        p.markContactPhoned(cf.getId(), "Alice Client");

        List<PhoneContact> contacts = buildContactsFromProfile(p);
        assertTrue("Alice should be marked phoned",    contacts.get(0).phoned);
        assertFalse("Bob should not be marked phoned", contacts.get(1).phoned);
    }

    @Test
    public void blankNameContactsAreSkipped() {
        Profile p = new Profile("Eve", "Female", "Normal");
        CaseFile cf = new CaseFile("Case1", "desc", "2050-01-02 10:00");
        cf.setClientName("  ");    // blank
        cf.setSubjectName("Real Name");
        p.addCaseFile(cf);

        List<PhoneContact> contacts = buildContactsFromProfile(p);
        assertEquals(1, contacts.size());
        assertEquals("Real Name", contacts.get(0).name);
    }

    @Test
    public void multipleOpenCasesAccumulateContacts() {
        Profile p = new Profile("Frank", "Male", "Normal");

        CaseFile cf1 = new CaseFile("Case1", "desc", "2050-01-02 10:00");
        cf1.setClientName("Alice");
        cf1.setSubjectName("Bob");
        p.addCaseFile(cf1);

        CaseFile cf2 = new CaseFile("Case2", "desc", "2050-01-03 10:00");
        cf2.setClientName("Charlie");
        cf2.setSubjectName("Diana");
        p.addCaseFile(cf2);

        List<PhoneContact> contacts = buildContactsFromProfile(p);
        assertEquals(4, contacts.size());
    }
}

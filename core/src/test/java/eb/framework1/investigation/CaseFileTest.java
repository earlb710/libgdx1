package eb.framework1.investigation;

import eb.framework1.character.*;


import org.junit.Test;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

/**
 * Tests for {@link CaseFile} and the case-file management in {@link Profile}.
 */
public class CaseFileTest {

    // -------------------------------------------------------------------------
    // CaseFile basics
    // -------------------------------------------------------------------------

    @Test
    public void newCaseIsOpen() {
        CaseFile cf = new CaseFile("Robbery", "A bank robbery downtown", "2050-01-02 10:00");
        assertEquals("Robbery", cf.getName());
        assertEquals("A bank robbery downtown", cf.getDescription());
        assertEquals("2050-01-02 10:00", cf.getDateOpened());
        assertEquals(CaseFile.Status.OPEN, cf.getStatus());
        assertTrue(cf.isOpen());
        assertNull(cf.getDateClosed());
        assertNotNull(cf.getId());
        assertTrue(cf.getClues().isEmpty());
        assertTrue(cf.getEvidence().isEmpty());
        assertTrue(cf.getNotes().isEmpty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullNameThrows() {
        new CaseFile(null, "desc", "2050-01-02 10:00");
    }

    @Test(expected = IllegalArgumentException.class)
    public void emptyNameThrows() {
        new CaseFile("  ", "desc", "2050-01-02 10:00");
    }

    @Test
    public void addClue() {
        CaseFile cf = new CaseFile("Test", "desc", "2050-01-02 10:00");
        cf.addClue("Fingerprint on window");
        cf.addClue("Witness saw a red car");
        assertEquals(2, cf.getClues().size());
        assertEquals("Fingerprint on window", cf.getClues().get(0));
    }

    @Test
    public void addNullOrBlankClueIgnored() {
        CaseFile cf = new CaseFile("Test", "desc", "2050-01-02 10:00");
        cf.addClue(null);
        cf.addClue("  ");
        assertEquals(0, cf.getClues().size());
    }

    @Test
    public void addEvidence() {
        CaseFile cf = new CaseFile("Test", "desc", "2050-01-02 10:00");
        cf.addEvidence("Bloody knife");
        cf.addEvidence("Security footage");
        assertEquals(2, cf.getEvidence().size());
        assertEquals("Bloody knife", cf.getEvidence().get(0));
    }

    @Test
    public void addNullOrBlankEvidenceIgnored() {
        CaseFile cf = new CaseFile("Test", "desc", "2050-01-02 10:00");
        cf.addEvidence(null);
        cf.addEvidence("  ");
        assertEquals(0, cf.getEvidence().size());
    }

    @Test
    public void addNote() {
        CaseFile cf = new CaseFile("Test", "desc", "2050-01-02 10:00");
        cf.addNote("Check the alley behind the bar");
        cf.addNote("Talk to the bartender again");
        assertEquals(2, cf.getNotes().size());
        assertEquals("Check the alley behind the bar", cf.getNotes().get(0));
    }

    @Test
    public void addNullOrBlankNoteIgnored() {
        CaseFile cf = new CaseFile("Test", "desc", "2050-01-02 10:00");
        cf.addNote(null);
        cf.addNote("  ");
        assertEquals(0, cf.getNotes().size());
    }

    @Test
    public void closeCase() {
        CaseFile cf = new CaseFile("Test", "desc", "2050-01-02 10:00");
        cf.close("2050-02-10 15:00");
        assertEquals(CaseFile.Status.CLOSED, cf.getStatus());
        assertFalse(cf.isOpen());
        assertEquals("2050-02-10 15:00", cf.getDateClosed());
    }

    @Test
    public void markCold() {
        CaseFile cf = new CaseFile("Test", "desc", "2050-01-02 10:00");
        cf.markCold();
        assertEquals(CaseFile.Status.COLD, cf.getStatus());
        assertFalse(cf.isOpen());
    }

    @Test
    public void reopenCase() {
        CaseFile cf = new CaseFile("Test", "desc", "2050-01-02 10:00");
        cf.close("2050-02-10 15:00");
        cf.reopen();
        assertTrue(cf.isOpen());
        assertNull(cf.getDateClosed());
    }

    @Test
    public void fullConstructor() {
        List<String> clues = Arrays.asList("clue1", "clue2");
        List<String> evidence = Arrays.asList("knife", "photo");
        List<String> notes = Arrays.asList("note1", "note2", "note3");
        CaseFile cf = new CaseFile("id1", "Name", "Desc", CaseFile.Status.CLOSED,
                "2050-01-01 00:00", "2050-06-01 12:00", clues, evidence, notes);
        assertEquals("id1", cf.getId());
        assertEquals("Name", cf.getName());
        assertEquals("Desc", cf.getDescription());
        assertEquals(CaseFile.Status.CLOSED, cf.getStatus());
        assertEquals("2050-01-01 00:00", cf.getDateOpened());
        assertEquals("2050-06-01 12:00", cf.getDateClosed());
        assertEquals(2, cf.getClues().size());
        assertEquals(2, cf.getEvidence().size());
        assertEquals("knife", cf.getEvidence().get(0));
        assertEquals(3, cf.getNotes().size());
        assertEquals("note1", cf.getNotes().get(0));
    }

    @Test
    public void fullConstructorDefaults() {
        CaseFile cf = new CaseFile(null, "Name", null, null, null, null, null, null, null);
        assertNotNull(cf.getId());
        assertEquals("", cf.getDescription());
        assertEquals(CaseFile.Status.OPEN, cf.getStatus());
        assertEquals("", cf.getDateOpened());
        assertTrue(cf.getClues().isEmpty());
        assertTrue(cf.getEvidence().isEmpty());
        assertTrue(cf.getNotes().isEmpty());
    }

    @Test
    public void toStringIncludesName() {
        CaseFile cf = new CaseFile("Test", "desc", "2050-01-02 10:00");
        assertTrue(cf.toString().contains("Test"));
    }

    // -------------------------------------------------------------------------
    // Profile case-file management
    // -------------------------------------------------------------------------

    @Test
    public void profileStartsWithNoCases() {
        Profile p = new Profile("Alice", "Female", "Normal");
        assertTrue(p.getCaseFiles().isEmpty());
        assertTrue(p.getOpenCases().isEmpty());
        assertNull(p.getActiveCaseFile());
    }

    @Test
    public void addCaseFileSetsActive() {
        Profile p = new Profile("Bob", "Male", "Normal");
        CaseFile cf = new CaseFile("Case1", "desc", "2050-01-02 10:00");
        p.addCaseFile(cf);
        assertEquals(1, p.getCaseFiles().size());
        assertEquals(cf, p.getActiveCaseFile());
    }

    @Test
    public void getOpenCasesFilters() {
        Profile p = new Profile("Charlie", "Male", "Normal");
        CaseFile open1 = new CaseFile("Open1", "d", "2050-01-02 10:00");
        CaseFile open2 = new CaseFile("Open2", "d", "2050-01-03 10:00");
        CaseFile closed = new CaseFile("Closed", "d", "2050-01-04 10:00");
        closed.close("2050-02-01 10:00");

        p.addCaseFile(open1);
        p.addCaseFile(open2);
        p.addCaseFile(closed);

        assertEquals(3, p.getCaseFiles().size());
        assertEquals(2, p.getOpenCases().size());
    }

    @Test
    public void setActiveCaseFile() {
        Profile p = new Profile("Dana", "Female", "Normal");
        CaseFile cf1 = new CaseFile("Case1", "d", "2050-01-02 10:00");
        CaseFile cf2 = new CaseFile("Case2", "d", "2050-01-03 10:00");
        p.addCaseFile(cf1);
        p.addCaseFile(cf2);
        // Last added becomes active
        assertEquals(cf2, p.getActiveCaseFile());
        p.setActiveCaseFile(cf1);
        assertEquals(cf1, p.getActiveCaseFile());
    }

    @Test(expected = IllegalArgumentException.class)
    public void addNullCaseThrows() {
        Profile p = new Profile("Eve", "Female", "Normal");
        p.addCaseFile(null);
    }

    @Test
    public void setActiveCaseFileToNullAllowed() {
        Profile p = new Profile("Frank", "Male", "Normal");
        CaseFile cf = new CaseFile("Case1", "d", "2050-01-02 10:00");
        p.addCaseFile(cf);
        p.setActiveCaseFile(null);
        assertNull(p.getActiveCaseFile());
    }

    @Test(expected = IllegalArgumentException.class)
    public void setActiveCaseFileNotInListThrows() {
        Profile p = new Profile("Grace", "Female", "Normal");
        CaseFile cf = new CaseFile("NotAdded", "d", "2050-01-02 10:00");
        p.setActiveCaseFile(cf);
    }

    // -------------------------------------------------------------------------
    // Complexity
    // -------------------------------------------------------------------------

    @Test
    public void complexityDefaultsToOne() {
        CaseFile cf = new CaseFile("Test", "desc", "2050-01-02 10:00");
        assertEquals(1, cf.getComplexity());
    }

    @Test
    public void complexityCanBeSetToValidValues() {
        CaseFile cf = new CaseFile("Test", "desc", "2050-01-02 10:00");
        cf.setComplexity(1);
        assertEquals(1, cf.getComplexity());
        cf.setComplexity(2);
        assertEquals(2, cf.getComplexity());
        cf.setComplexity(3);
        assertEquals(3, cf.getComplexity());
    }

    @Test(expected = IllegalArgumentException.class)
    public void complexityBelowOneThrows() {
        CaseFile cf = new CaseFile("Test", "desc", "2050-01-02 10:00");
        cf.setComplexity(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void complexityAboveThreeThrows() {
        CaseFile cf = new CaseFile("Test", "desc", "2050-01-02 10:00");
        cf.setComplexity(4);
    }

    // -------------------------------------------------------------------------
    // Known facts with source
    // -------------------------------------------------------------------------

    @Test
    public void newCaseHasNoKnownFacts() {
        CaseFile cf = new CaseFile("Test", "desc", "2050-01-02 10:00");
        assertTrue(cf.getKnownFacts().isEmpty());
    }

    @Test
    public void addKnownFactObject() {
        CaseFile cf = new CaseFile("Test", "desc", "2050-01-02 10:00");
        KnownFact fact = new KnownFact("Client reported theft", FactSource.CASE);
        cf.addKnownFact(fact);
        assertEquals(1, cf.getKnownFacts().size());
        assertEquals("Client reported theft", cf.getKnownFacts().get(0).getText());
        assertEquals(FactSource.CASE, cf.getKnownFacts().get(0).getSource());
    }

    @Test
    public void addKnownFactConvenience() {
        CaseFile cf = new CaseFile("Test", "desc", "2050-01-02 10:00");
        cf.addKnownFact("DNA analysis confirms match", FactSource.POLICE);
        assertEquals(1, cf.getKnownFacts().size());
        assertEquals("DNA analysis confirms match", cf.getKnownFacts().get(0).getText());
        assertEquals(FactSource.POLICE, cf.getKnownFacts().get(0).getSource());
    }

    @Test
    public void getKnownFactsBySource() {
        CaseFile cf = new CaseFile("Test", "desc", "2050-01-02 10:00");
        cf.addKnownFact("Client hired detective", FactSource.CASE);
        cf.addKnownFact("Scene evidence collected", FactSource.POLICE);
        cf.addKnownFact("DNA match confirmed", FactSource.POLICE);
        cf.addKnownFact("Witness located by investigator", FactSource.DISCOVERED);

        List<KnownFact> caseFacts = cf.getKnownFactsBySource(FactSource.CASE);
        assertEquals(1, caseFacts.size());
        assertEquals("Client hired detective", caseFacts.get(0).getText());

        List<KnownFact> policeFacts = cf.getKnownFactsBySource(FactSource.POLICE);
        assertEquals(2, policeFacts.size());
        assertEquals("Scene evidence collected", policeFacts.get(0).getText());
        assertEquals("DNA match confirmed", policeFacts.get(1).getText());

        List<KnownFact> discoveredFacts = cf.getKnownFactsBySource(FactSource.DISCOVERED);
        assertEquals(1, discoveredFacts.size());
        assertEquals("Witness located by investigator", discoveredFacts.get(0).getText());
    }

    @Test(expected = IllegalArgumentException.class)
    public void addNullKnownFactThrows() {
        CaseFile cf = new CaseFile("Test", "desc", "2050-01-02 10:00");
        cf.addKnownFact((KnownFact) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void knownFactNullTextThrows() {
        new KnownFact(null, FactSource.CASE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void knownFactBlankTextThrows() {
        new KnownFact("  ", FactSource.CASE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void knownFactNullSourceThrows() {
        new KnownFact("fact text", null);
    }

    @Test
    public void knownFactEquality() {
        KnownFact a = new KnownFact("fact", FactSource.POLICE);
        KnownFact b = new KnownFact("fact", FactSource.POLICE);
        KnownFact c = new KnownFact("fact", FactSource.CASE);
        assertEquals(a, b);
        assertNotEquals(a, c);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void knownFactToString() {
        KnownFact f = new KnownFact("DNA match", FactSource.POLICE);
        assertTrue(f.toString().contains("POLICE"));
        assertTrue(f.toString().contains("DNA match"));
    }

    @Test
    public void forensicResultsArePoliceSource() {
        CaseFile cf = new CaseFile("Murder", "desc", "2050-01-02 10:00");
        // Forensic lab results should be categorised under POLICE
        cf.addKnownFact("DNA analysis of blood sample confirms match with suspect", FactSource.POLICE);
        cf.addKnownFact("Toxicology report reveals sedative in victim's system", FactSource.POLICE);
        cf.addKnownFact("Digital forensics recovered deleted messages", FactSource.POLICE);
        cf.addKnownFact("Financial records show suspicious payment", FactSource.POLICE);

        List<KnownFact> policeFacts = cf.getKnownFactsBySource(FactSource.POLICE);
        assertEquals(4, policeFacts.size());
        // All forensic results end up under known fact.police
        for (KnownFact fact : policeFacts) {
            assertEquals(FactSource.POLICE, fact.getSource());
        }
    }
}

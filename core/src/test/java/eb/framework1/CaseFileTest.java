package eb.framework1;

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
        CaseFile cf = new CaseFile("id1", "Name", "Desc", CaseFile.Status.CLOSED,
                "2050-01-01 00:00", "2050-06-01 12:00", clues, evidence);
        assertEquals("id1", cf.getId());
        assertEquals("Name", cf.getName());
        assertEquals("Desc", cf.getDescription());
        assertEquals(CaseFile.Status.CLOSED, cf.getStatus());
        assertEquals("2050-01-01 00:00", cf.getDateOpened());
        assertEquals("2050-06-01 12:00", cf.getDateClosed());
        assertEquals(2, cf.getClues().size());
        assertEquals(2, cf.getEvidence().size());
        assertEquals("knife", cf.getEvidence().get(0));
    }

    @Test
    public void fullConstructorDefaults() {
        CaseFile cf = new CaseFile(null, "Name", null, null, null, null, null, null);
        assertNotNull(cf.getId());
        assertEquals("", cf.getDescription());
        assertEquals(CaseFile.Status.OPEN, cf.getStatus());
        assertEquals("", cf.getDateOpened());
        assertTrue(cf.getClues().isEmpty());
        assertTrue(cf.getEvidence().isEmpty());
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
}

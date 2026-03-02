package eb.gmodel1.investigation;

import eb.gmodel1.character.*;
import eb.gmodel1.city.*;
import eb.gmodel1.save.*;


import org.junit.Test;
import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tests for story-tree and case-file save/load serialisation round-trips.
 *
 * <p>These tests verify that {@link SaveGameManager#toData(GameSave)} and
 * {@link SaveGameManager#fromData(SaveGameManager.SaveData)} correctly preserve
 * all {@link CaseFile} fields — including the {@link CaseStoryNode} story tree
 * and player-progress flags — across a full serialisation cycle.
 *
 * <p>All tests are pure-Java; no libGDX runtime is required.
 */
public class StoryTreeSaveLoadTest {

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Profile makeProfile() {
        Map<String, Integer> attrs = new HashMap<>();
        attrs.put(CharacterAttribute.INTELLIGENCE.name(), 3);
        return new Profile("TestDet", "Male", "Normal", "man1", attrs, 2050, 42L);
    }

    /**
     * Builds a minimal two-level tree:
     * <pre>
     * ROOT
     *   PLOT_TWIST "Phase 1"
     *     ACTION "A1" (completed)
     *     ACTION "A2" (not completed)
     * </pre>
     */
    private CaseStoryNode makeSimpleTree() {
        CaseStoryNode root  = new CaseStoryNode("root",  "Root",    "", CaseStoryNode.NodeType.ROOT);
        CaseStoryNode phase = new CaseStoryNode("p1",    "Phase 1", "", CaseStoryNode.NodeType.PLOT_TWIST);
        CaseStoryNode a1    = new CaseStoryNode("a1",    "Action 1","desc1", CaseStoryNode.NodeType.ACTION);
        CaseStoryNode a2    = new CaseStoryNode("a2",    "Action 2","desc2", CaseStoryNode.NodeType.ACTION);
        a1.complete();
        phase.addChild(a1);
        phase.addChild(a2);
        root.addChild(phase);
        return root;
    }

    /**
     * Builds a full four-level tree as created by {@link CaseGenerator}:
     * ROOT → PLOT_TWIST → MAJOR_PROGRESS → MINOR_PROGRESS → ACTION (leaf).
     */
    private CaseStoryNode makeDeepTree(boolean completeFirstAction) {
        CaseStoryNode root   = new CaseStoryNode("r",   "Root",   "", CaseStoryNode.NodeType.ROOT);
        CaseStoryNode twist  = new CaseStoryNode("t1",  "Twist",  "", CaseStoryNode.NodeType.PLOT_TWIST);
        CaseStoryNode major  = new CaseStoryNode("m1",  "Major",  "", CaseStoryNode.NodeType.MAJOR_PROGRESS);
        CaseStoryNode minor  = new CaseStoryNode("mn1", "Minor",  "", CaseStoryNode.NodeType.MINOR_PROGRESS);
        CaseStoryNode action = new CaseStoryNode("act1","Act 1",  "do something", CaseStoryNode.NodeType.ACTION);
        if (completeFirstAction) action.complete();
        minor.addChild(action);
        major.addChild(minor);
        twist.addChild(major);
        root.addChild(twist);
        return root;
    }

    /** Round-trips a CaseFile through toData/fromData and returns the restored file. */
    private CaseFile roundTrip(CaseFile original) {
        Profile  p   = makeProfile();
        p.addCaseFile(original);
        CityMap  map = new CityMap(12345L);
        GameSave save = GameSave.from(p, map, 0, 0, 0, 0);
        SaveGameManager.SaveData data     = SaveGameManager.toData(save);
        GameSave                 restored = SaveGameManager.fromData(data);
        List<CaseFile> cases = restored.getCaseFiles();
        assertEquals("Exactly one case file should survive round-trip", 1, cases.size());
        return cases.get(0);
    }

    // -------------------------------------------------------------------------
    // GameSave.from() captures case files
    // -------------------------------------------------------------------------

    @Test
    public void gameSaveFrom_capturesCaseFiles() {
        Profile  p   = makeProfile();
        CaseFile cf  = new CaseFile("Case1", "desc", "2050-01-02 10:00");
        p.addCaseFile(cf);
        CityMap  map = new CityMap(12345L);
        GameSave s   = GameSave.from(p, map, 0, 0, 0, 0);
        assertEquals(1, s.getCaseFiles().size());
        assertEquals("Case1", s.getCaseFiles().get(0).getName());
    }

    @Test
    public void gameSaveFrom_capturesActiveCaseId() {
        Profile  p   = makeProfile();
        CaseFile cf  = new CaseFile("Active", "d", "2050-01-02 10:00");
        p.addCaseFile(cf);
        CityMap  map = new CityMap(12345L);
        GameSave s   = GameSave.from(p, map, 0, 0, 0, 0);
        assertEquals(cf.getId(), s.getActiveCaseId());
    }

    @Test
    public void gameSaveFrom_noActiveCaseId_whenNoCases() {
        Profile  p   = makeProfile();
        CityMap  map = new CityMap(12345L);
        GameSave s   = GameSave.from(p, map, 0, 0, 0, 0);
        assertNull(s.getActiveCaseId());
    }

    @Test
    public void applyToProfile_restoresCaseFiles() {
        Profile  p   = makeProfile();
        CaseFile cf  = new CaseFile("Restored", "d", "2050-01-02 10:00");
        p.addCaseFile(cf);
        CityMap  map = new CityMap(12345L);
        GameSave s   = GameSave.from(p, map, 0, 0, 0, 0);

        Profile p2 = makeProfile();
        s.applyToProfile(p2);
        assertEquals(1, p2.getCaseFiles().size());
        assertEquals("Restored", p2.getCaseFiles().get(0).getName());
    }

    @Test
    public void applyToProfile_restoresActiveCaseFile() {
        Profile  p   = makeProfile();
        CaseFile cf1 = new CaseFile("Case1", "d", "2050-01-02 10:00");
        CaseFile cf2 = new CaseFile("Case2", "d", "2050-01-03 10:00");
        p.addCaseFile(cf1);
        p.addCaseFile(cf2);
        p.setActiveCaseFile(cf1);
        CityMap  map = new CityMap(12345L);
        GameSave s   = GameSave.from(p, map, 0, 0, 0, 0);

        Profile p2 = makeProfile();
        s.applyToProfile(p2);
        assertNotNull("Active case must be restored", p2.getActiveCaseFile());
        assertEquals("Active case name must match", "Case1",
                p2.getActiveCaseFile().getName());
    }

    // -------------------------------------------------------------------------
    // CaseFile fields survive toData / fromData
    // -------------------------------------------------------------------------

    @Test
    public void caseFile_basicFields_surviveRoundTrip() {
        CaseFile cf = new CaseFile("id-42", "My Case", "Detailed description",
                CaseFile.Status.OPEN, "2050-02-01 08:00", null,
                java.util.Arrays.asList("clue1", "clue2"),
                java.util.Arrays.asList("evidence A"),
                java.util.Arrays.asList("note X"));
        CaseFile r = roundTrip(cf);
        assertEquals("id-42",                  r.getId());
        assertEquals("My Case",                r.getName());
        assertEquals("Detailed description",   r.getDescription());
        assertEquals(CaseFile.Status.OPEN,     r.getStatus());
        assertEquals("2050-02-01 08:00",       r.getDateOpened());
        assertNull(r.getDateClosed());
        assertEquals(2, r.getClues().size());
        assertEquals("clue1", r.getClues().get(0));
        assertEquals(1, r.getEvidence().size());
        assertEquals("evidence A", r.getEvidence().get(0));
        assertEquals(1, r.getNotes().size());
        assertEquals("note X", r.getNotes().get(0));
    }

    @Test
    public void caseFile_closedStatus_surviveRoundTrip() {
        CaseFile cf = new CaseFile("id-99", "Closed", "d", CaseFile.Status.CLOSED,
                "2050-01-01 00:00", "2050-03-15 12:00",
                null, null, null);
        CaseFile r = roundTrip(cf);
        assertEquals(CaseFile.Status.CLOSED, r.getStatus());
        assertEquals("2050-03-15 12:00", r.getDateClosed());
    }

    @Test
    public void caseFile_generatorFields_surviveRoundTrip() {
        CaseFile cf = new CaseFile("gen-1", "Fraud Case", "d", CaseFile.Status.OPEN,
                "2050-01-01 09:00", null, null, null, null);
        cf.setCaseType(CaseType.FRAUD);
        cf.setClientName("Alice Smith");
        cf.setSubjectName("Bob Jones");
        cf.setObjective("Uncover financial fraud");
        cf.setComplexity(2);
        CaseFile r = roundTrip(cf);
        assertEquals(CaseType.FRAUD,          r.getCaseType());
        assertEquals("Alice Smith",            r.getClientName());
        assertEquals("Bob Jones",              r.getSubjectName());
        assertEquals("Uncover financial fraud",r.getObjective());
        assertEquals(2,                        r.getComplexity());
    }

    @Test
    public void caseFile_leads_surviveRoundTrip() {
        CaseFile cf = new CaseFile("lead-test", "Case with leads", "d",
                CaseFile.Status.OPEN, "2050-01-01 09:00", null, null, null, null);
        CaseLead l1 = new CaseLead("l1", "Secret info", "Check the bar",
                DiscoveryMethod.SURVEILLANCE);
        CaseLead l2 = new CaseLead("l2", "Financial record", "Look at accounts",
                DiscoveryMethod.DOCUMENTS);
        l2.discover();
        cf.addLead(l1);
        cf.addLead(l2);
        CaseFile r = roundTrip(cf);
        assertEquals(2, r.getLeads().size());
        CaseLead rl1 = r.getLeads().get(0);
        CaseLead rl2 = r.getLeads().get(1);
        assertEquals("l1", rl1.getId());
        assertEquals("Check the bar", rl1.getHint());
        assertEquals(DiscoveryMethod.SURVEILLANCE, rl1.getDiscoveryMethod());
        assertFalse(rl1.isDiscovered());
        assertTrue("Discovered lead must remain discovered after round-trip",
                rl2.isDiscovered());
    }

    @Test
    public void caseFile_evidenceItems_surviveRoundTrip() {
        CaseFile cf = new CaseFile("ev-test", "Evidence Case", "d",
                CaseFile.Status.OPEN, "2050-01-01 09:00", null, null, null, null);
        EvidenceItem glass = EvidenceItem.createByName("Drinking Glass");
        assertNotNull("Drinking Glass must be in catalogue", glass);
        glass.submitForAnalysis(EvidenceModifier.FINGERPRINTS);
        cf.addEvidenceItem(glass);
        CaseFile r = roundTrip(cf);
        assertEquals(1, r.getEvidenceItems().size());
        EvidenceItem ri = r.getEvidenceItems().get(0);
        assertEquals("Drinking Glass", ri.getName());
        assertTrue("Submitted analysis must survive round-trip",
                ri.isSubmittedForAnalysis(EvidenceModifier.FINGERPRINTS));
        assertFalse(ri.isSubmittedForAnalysis(EvidenceModifier.DNA));
    }

    // -------------------------------------------------------------------------
    // Story tree survives round-trip
    // -------------------------------------------------------------------------

    @Test
    public void storyTree_null_forLegacyCases() {
        CaseFile cf = new CaseFile("legacy", "Legacy", "d", CaseFile.Status.OPEN,
                "2050-01-01", null, null, null, null);
        assertNull(cf.getStoryRoot());
        CaseFile r = roundTrip(cf);
        assertNull("Legacy case must have null storyRoot after round-trip", r.getStoryRoot());
    }

    @Test
    public void storyTree_rootIdAndType_surviveRoundTrip() {
        CaseFile cf = new CaseFile("tree-1", "Tree Case", "d", CaseFile.Status.OPEN,
                "2050-01-01", null, null, null, null);
        cf.setStoryRoot(makeSimpleTree());
        CaseFile r = roundTrip(cf);
        assertNotNull("Story root must not be null after round-trip", r.getStoryRoot());
        assertEquals("root", r.getStoryRoot().getId());
        assertEquals(CaseStoryNode.NodeType.ROOT, r.getStoryRoot().getNodeType());
    }

    @Test
    public void storyTree_childCount_surviveRoundTrip() {
        CaseFile cf = new CaseFile("tree-2", "Tree Case", "d", CaseFile.Status.OPEN,
                "2050-01-01", null, null, null, null);
        cf.setStoryRoot(makeSimpleTree());
        CaseFile r = roundTrip(cf);
        // ROOT has 1 PLOT_TWIST child
        assertEquals(1, r.getStoryRoot().getChildren().size());
        CaseStoryNode phase = r.getStoryRoot().getChildren().get(0);
        assertEquals("p1", phase.getId());
        // PLOT_TWIST has 2 ACTION children
        assertEquals(2, phase.getChildren().size());
    }

    @Test
    public void storyTree_completedFlag_surviveRoundTrip() {
        CaseFile cf = new CaseFile("tree-3", "Tree Case", "d", CaseFile.Status.OPEN,
                "2050-01-01", null, null, null, null);
        cf.setStoryRoot(makeSimpleTree());
        CaseFile r = roundTrip(cf);
        CaseStoryNode phase = r.getStoryRoot().getChildren().get(0);
        CaseStoryNode a1 = phase.getChildren().get(0);
        CaseStoryNode a2 = phase.getChildren().get(1);
        assertTrue("Action a1 was completed before save — must be completed after load",
                a1.isCompleted());
        assertFalse("Action a2 was not completed before save — must not be completed after load",
                a2.isCompleted());
    }

    @Test
    public void storyTree_isFullyComplete_afterAllActionsRestored() {
        CaseStoryNode root  = new CaseStoryNode("r",  "Root",    "", CaseStoryNode.NodeType.ROOT);
        CaseStoryNode phase = new CaseStoryNode("p1", "Phase",   "", CaseStoryNode.NodeType.PLOT_TWIST);
        CaseStoryNode a1    = new CaseStoryNode("a1", "Action1", "", CaseStoryNode.NodeType.ACTION);
        CaseStoryNode a2    = new CaseStoryNode("a2", "Action2", "", CaseStoryNode.NodeType.ACTION);
        a1.complete();
        a2.complete();
        phase.addChild(a1);
        phase.addChild(a2);
        root.addChild(phase);

        CaseFile cf = new CaseFile("fc", "Fully complete", "d", CaseFile.Status.OPEN,
                "2050-01-01", null, null, null, null);
        cf.setStoryRoot(root);

        CaseFile r = roundTrip(cf);
        assertTrue("Fully-complete tree must still report isFullyComplete() after load",
                r.getStoryRoot().isFullyComplete());
    }

    @Test
    public void storyTree_partialProgress_preservesSequentialUnlock() {
        // a1 completed, a2 not completed → a2 should still be available (a1 done)
        CaseFile cf = new CaseFile("seq", "Sequential", "d", CaseFile.Status.OPEN,
                "2050-01-01", null, null, null, null);
        cf.setStoryRoot(makeSimpleTree());
        CaseFile r = roundTrip(cf);
        CaseStoryNode phase = r.getStoryRoot().getChildren().get(0);
        assertTrue("a1 is complete, so a2 (index 1) must be available",
                phase.isChildAvailable(1));
        // a1 is done and a2 is not, so getNextActiveChild should return a2
        CaseStoryNode next = phase.getNextActiveChild();
        assertNotNull("getNextActiveChild must return a2 (the only incomplete child)", next);
        assertEquals("a2", next.getId());
    }

    @Test
    public void storyTree_deepTree_allLevelsPreserved() {
        CaseFile cf = new CaseFile("deep", "Deep Tree", "d", CaseFile.Status.OPEN,
                "2050-01-01", null, null, null, null);
        cf.setStoryRoot(makeDeepTree(true)); // first action completed
        CaseFile r = roundTrip(cf);

        CaseStoryNode rRoot = r.getStoryRoot();
        assertEquals(CaseStoryNode.NodeType.ROOT, rRoot.getNodeType());

        CaseStoryNode twist = rRoot.getChildren().get(0);
        assertEquals(CaseStoryNode.NodeType.PLOT_TWIST, twist.getNodeType());

        CaseStoryNode major = twist.getChildren().get(0);
        assertEquals(CaseStoryNode.NodeType.MAJOR_PROGRESS, major.getNodeType());

        CaseStoryNode minor = major.getChildren().get(0);
        assertEquals(CaseStoryNode.NodeType.MINOR_PROGRESS, minor.getNodeType());

        CaseStoryNode action = minor.getChildren().get(0);
        assertEquals(CaseStoryNode.NodeType.ACTION, action.getNodeType());
        assertEquals("act1", action.getId());
        assertTrue("Action completed before save must be completed after load",
                action.isCompleted());
    }

    @Test
    public void storyTree_nodeDescriptions_surviveRoundTrip() {
        CaseFile cf = new CaseFile("desc-t", "Desc Test", "d", CaseFile.Status.OPEN,
                "2050-01-01", null, null, null, null);
        cf.setStoryRoot(makeSimpleTree());
        CaseFile r = roundTrip(cf);
        CaseStoryNode phase = r.getStoryRoot().getChildren().get(0);
        CaseStoryNode a1 = phase.getChildren().get(0);
        assertEquals("desc1", a1.getDescription());
    }

    @Test
    public void storyTree_nodeTitles_surviveRoundTrip() {
        CaseFile cf = new CaseFile("title-t", "Title Test", "d", CaseFile.Status.OPEN,
                "2050-01-01", null, null, null, null);
        cf.setStoryRoot(makeSimpleTree());
        CaseFile r = roundTrip(cf);
        CaseStoryNode phase = r.getStoryRoot().getChildren().get(0);
        assertEquals("Phase 1", phase.getTitle());
        CaseStoryNode a2 = phase.getChildren().get(1);
        assertEquals("Action 2", a2.getTitle());
    }

    // -------------------------------------------------------------------------
    // Multiple case files survive round-trip
    // -------------------------------------------------------------------------

    @Test
    public void multipleCaseFiles_surviveRoundTrip() {
        Profile  p   = makeProfile();
        CaseFile cf1 = new CaseFile("c1", "Case One",   "d1", CaseFile.Status.OPEN,
                "2050-01-01", null, null, null, null);
        CaseFile cf2 = new CaseFile("c2", "Case Two",   "d2", CaseFile.Status.CLOSED,
                "2050-01-02", "2050-03-01", null, null, null);
        cf1.setStoryRoot(makeSimpleTree());
        p.addCaseFile(cf1);
        p.addCaseFile(cf2);
        p.setActiveCaseFile(cf2);

        CityMap  map  = new CityMap(12345L);
        GameSave save = GameSave.from(p, map, 0, 0, 0, 0);
        SaveGameManager.SaveData data     = SaveGameManager.toData(save);
        GameSave                 restored = SaveGameManager.fromData(data);

        assertEquals(2, restored.getCaseFiles().size());
        assertEquals("c2", restored.getActiveCaseId());
        CaseFile r1 = restored.getCaseFiles().get(0);
        CaseFile r2 = restored.getCaseFiles().get(1);
        assertEquals("Case One",  r1.getName());
        assertEquals("Case Two",  r2.getName());
        assertNotNull("Case One story root must be preserved", r1.getStoryRoot());
        assertNull("Case Two (no story root) must still have null root", r2.getStoryRoot());
    }

    // -------------------------------------------------------------------------
    // Profile round-trip including cases
    // -------------------------------------------------------------------------

    @Test
    public void applyToProfile_restoresStoryTree() {
        Profile  p   = makeProfile();
        CaseFile cf  = new CaseFile("ct", "Tree Case", "d", CaseFile.Status.OPEN,
                "2050-01-01", null, null, null, null);
        cf.setStoryRoot(makeSimpleTree());
        p.addCaseFile(cf);

        CityMap  map  = new CityMap(12345L);
        GameSave save = GameSave.from(p, map, 0, 0, 0, 0);

        Profile p2 = makeProfile();
        save.applyToProfile(p2);

        assertEquals(1, p2.getCaseFiles().size());
        CaseStoryNode restoredRoot = p2.getCaseFiles().get(0).getStoryRoot();
        assertNotNull("Story root must be present after applyToProfile", restoredRoot);
        CaseStoryNode phase = restoredRoot.getChildren().get(0);
        assertTrue("Completed action must still be completed after applyToProfile",
                phase.getChildren().get(0).isCompleted());
        assertFalse("Incomplete action must still be incomplete after applyToProfile",
                phase.getChildren().get(1).isCompleted());
    }
}

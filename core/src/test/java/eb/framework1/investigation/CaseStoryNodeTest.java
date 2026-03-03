package eb.framework1.investigation;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for {@link CaseStoryNode} — construction, completion, sequential-unlock
 * logic, and the progress-query helpers.
 */
public class CaseStoryNodeTest {

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------

    @Test
    public void newNodeStoresFields() {
        CaseStoryNode n = new CaseStoryNode("n1", "My Title", "Some desc",
                CaseStoryNode.NodeType.ACTION);
        assertEquals("n1", n.getId());
        assertEquals("My Title", n.getTitle());
        assertEquals("Some desc", n.getDescription());
        assertEquals(CaseStoryNode.NodeType.ACTION, n.getNodeType());
        assertTrue(n.getChildren().isEmpty());
    }

    @Test
    public void newNodeIsNotComplete() {
        CaseStoryNode n = new CaseStoryNode("n1", "Title", "Desc",
                CaseStoryNode.NodeType.ACTION);
        assertFalse(n.isCompleted());
        assertFalse(n.isFullyComplete());
    }

    @Test
    public void nullDescriptionDefaultsToEmpty() {
        CaseStoryNode n = new CaseStoryNode("n1", "Title", null,
                CaseStoryNode.NodeType.MINOR_PROGRESS);
        assertEquals("", n.getDescription());
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullIdThrows() {
        new CaseStoryNode(null, "Title", "Desc", CaseStoryNode.NodeType.ACTION);
    }

    @Test(expected = IllegalArgumentException.class)
    public void blankIdThrows() {
        new CaseStoryNode("  ", "Title", "Desc", CaseStoryNode.NodeType.ACTION);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullTitleThrows() {
        new CaseStoryNode("n1", null, "Desc", CaseStoryNode.NodeType.ACTION);
    }

    @Test(expected = IllegalArgumentException.class)
    public void blankTitleThrows() {
        new CaseStoryNode("n1", "   ", "Desc", CaseStoryNode.NodeType.ACTION);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullNodeTypeThrows() {
        new CaseStoryNode("n1", "Title", "Desc", null);
    }

    // -------------------------------------------------------------------------
    // NodeType enum
    // -------------------------------------------------------------------------

    @Test
    public void allNodeTypesExist() {
        assertNotNull(CaseStoryNode.NodeType.ROOT);
        assertNotNull(CaseStoryNode.NodeType.PLOT_TWIST);
        assertNotNull(CaseStoryNode.NodeType.MAJOR_PROGRESS);
        assertNotNull(CaseStoryNode.NodeType.MINOR_PROGRESS);
        assertNotNull(CaseStoryNode.NodeType.ACTION);
        assertEquals(5, CaseStoryNode.NodeType.values().length);
    }

    // -------------------------------------------------------------------------
    // addChild
    // -------------------------------------------------------------------------

    @Test
    public void addChildAppendsInOrder() {
        CaseStoryNode branch = branch("b");
        CaseStoryNode a1 = action("a1");
        CaseStoryNode a2 = action("a2");
        branch.addChild(a1);
        branch.addChild(a2);
        assertEquals(2, branch.getChildren().size());
        assertSame(a1, branch.getChildren().get(0));
        assertSame(a2, branch.getChildren().get(1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void addNullChildThrows() {
        branch("b").addChild(null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void childrenListIsUnmodifiable() {
        CaseStoryNode b = branch("b");
        b.addChild(action("a1"));
        b.getChildren().clear();
    }

    // -------------------------------------------------------------------------
    // complete() and isCompleted()
    // -------------------------------------------------------------------------

    @Test
    public void completeLeafSetsFlag() {
        CaseStoryNode leaf = action("l");
        leaf.complete();
        assertTrue(leaf.isCompleted());
    }

    @Test
    public void completeIsIdempotent() {
        CaseStoryNode leaf = action("l");
        leaf.complete();
        leaf.complete();
        assertTrue(leaf.isCompleted());
    }

    // -------------------------------------------------------------------------
    // isFullyComplete
    // -------------------------------------------------------------------------

    @Test
    public void leafFullyCompleteAfterComplete() {
        CaseStoryNode leaf = action("l");
        assertFalse(leaf.isFullyComplete());
        leaf.complete();
        assertTrue(leaf.isFullyComplete());
    }

    @Test
    public void branchRequiresAllChildrenComplete() {
        CaseStoryNode branch = branch("b");
        CaseStoryNode a1 = action("a1");
        CaseStoryNode a2 = action("a2");
        branch.addChild(a1);
        branch.addChild(a2);

        assertFalse(branch.isFullyComplete());
        a1.complete();
        assertFalse("Branch should not be done when only one child is complete",
                branch.isFullyComplete());
        a2.complete();
        assertTrue(branch.isFullyComplete());
    }

    @Test
    public void deepBranchFullyCompleteWhenAllLeavesComplete() {
        // minor → 2 actions
        CaseStoryNode minor = new CaseStoryNode("n", "Minor", "", CaseStoryNode.NodeType.MINOR_PROGRESS);
        CaseStoryNode a1 = action("a1");
        CaseStoryNode a2 = action("a2");
        minor.addChild(a1);
        minor.addChild(a2);

        // major → 1 minor
        CaseStoryNode major = new CaseStoryNode("m", "Major", "", CaseStoryNode.NodeType.MAJOR_PROGRESS);
        major.addChild(minor);

        assertFalse(major.isFullyComplete());
        a1.complete();
        assertFalse(major.isFullyComplete());
        a2.complete();
        assertTrue(minor.isFullyComplete());
        assertTrue(major.isFullyComplete());
    }

    // -------------------------------------------------------------------------
    // isChildAvailable — sequential-unlock rule
    // -------------------------------------------------------------------------

    @Test
    public void firstChildIsAlwaysAvailable() {
        CaseStoryNode branch = branch("b");
        branch.addChild(action("a1"));
        branch.addChild(action("a2"));
        assertTrue(branch.isChildAvailable(0));
    }

    @Test
    public void secondChildNotAvailableUntilFirstComplete() {
        CaseStoryNode branch = branch("b");
        CaseStoryNode a1 = action("a1");
        CaseStoryNode a2 = action("a2");
        branch.addChild(a1);
        branch.addChild(a2);

        assertFalse(branch.isChildAvailable(1));
        a1.complete();
        assertTrue(branch.isChildAvailable(1));
    }

    @Test
    public void thirdChildNotAvailableUntilFirstTwoComplete() {
        CaseStoryNode branch = branch("b");
        CaseStoryNode a1 = action("a1");
        CaseStoryNode a2 = action("a2");
        CaseStoryNode a3 = action("a3");
        branch.addChild(a1);
        branch.addChild(a2);
        branch.addChild(a3);

        assertFalse(branch.isChildAvailable(2));
        a1.complete();
        assertFalse(branch.isChildAvailable(2));
        a2.complete();
        assertTrue(branch.isChildAvailable(2));
    }

    @Test
    public void negativeIndexReturnsFalse() {
        CaseStoryNode branch = branch("b");
        branch.addChild(action("a1"));
        assertFalse(branch.isChildAvailable(-1));
    }

    @Test
    public void outOfBoundsIndexReturnsFalse() {
        CaseStoryNode branch = branch("b");
        branch.addChild(action("a1"));
        assertFalse(branch.isChildAvailable(1));
        assertFalse(branch.isChildAvailable(99));
    }

    @Test
    public void noChildrenAlwaysReturnsFalse() {
        CaseStoryNode branch = branch("b");
        assertFalse(branch.isChildAvailable(0));
    }

    // -------------------------------------------------------------------------
    // getNextActiveChild
    // -------------------------------------------------------------------------

    @Test
    public void nextActiveChildReturnsFirstIncomplete() {
        CaseStoryNode branch = branch("b");
        CaseStoryNode a1 = action("a1");
        CaseStoryNode a2 = action("a2");
        branch.addChild(a1);
        branch.addChild(a2);

        assertSame(a1, branch.getNextActiveChild());
        a1.complete();
        assertSame(a2, branch.getNextActiveChild());
        a2.complete();
        assertNull(branch.getNextActiveChild());
    }

    @Test
    public void nextActiveChildReturnsNullWhenNoChildren() {
        assertNull(branch("b").getNextActiveChild());
    }

    @Test
    public void nextActiveChildReturnsNullWhenAllComplete() {
        CaseStoryNode branch = branch("b");
        CaseStoryNode a1 = action("a1");
        branch.addChild(a1);
        a1.complete();
        assertNull(branch.getNextActiveChild());
    }

    // -------------------------------------------------------------------------
    // getNextAvailableAction
    // -------------------------------------------------------------------------

    @Test
    public void nextAvailableActionOnIncompleteLeaf() {
        CaseStoryNode leaf = action("a");
        assertSame("Incomplete leaf should return itself", leaf, leaf.getNextAvailableAction());
    }

    @Test
    public void nextAvailableActionOnCompletedLeaf() {
        CaseStoryNode leaf = action("a");
        leaf.complete();
        assertNull("Completed leaf should return null", leaf.getNextAvailableAction());
    }

    @Test
    public void nextAvailableActionTraversesOneLevelDown() {
        CaseStoryNode branch = branch("b");
        CaseStoryNode a1 = action("a1");
        CaseStoryNode a2 = action("a2");
        branch.addChild(a1);
        branch.addChild(a2);
        assertSame("a1 is first available", a1, branch.getNextAvailableAction());
        a1.complete();
        assertSame("a2 becomes next after a1 done", a2, branch.getNextAvailableAction());
        a2.complete();
        assertNull("null when all done", branch.getNextAvailableAction());
    }

    @Test
    public void nextAvailableActionTraversesDeepTree() {
        CaseStoryNode root  = new CaseStoryNode("r",  "Root",  "", CaseStoryNode.NodeType.ROOT);
        CaseStoryNode twist = new CaseStoryNode("t",  "Twist", "", CaseStoryNode.NodeType.PLOT_TWIST);
        CaseStoryNode major = new CaseStoryNode("m",  "Major", "", CaseStoryNode.NodeType.MAJOR_PROGRESS);
        CaseStoryNode minor = new CaseStoryNode("mn", "Minor", "", CaseStoryNode.NodeType.MINOR_PROGRESS);
        CaseStoryNode a1    = action("a1");
        CaseStoryNode a2    = action("a2");
        minor.addChild(a1);
        minor.addChild(a2);
        major.addChild(minor);
        twist.addChild(major);
        root.addChild(twist);

        assertSame("first action before any completions", a1, root.getNextAvailableAction());
        a1.complete();
        assertSame("second action after first done", a2, root.getNextAvailableAction());
        a2.complete();
        assertNull("null when all done", root.getNextAvailableAction());
    }

    @Test
    public void nextAvailableActionRespectsSequentialUnlock() {
        CaseStoryNode branch = branch("b");
        CaseStoryNode a1 = action("a1");
        CaseStoryNode a2 = action("a2");
        branch.addChild(a1);
        branch.addChild(a2);
        assertSame("a1 is the only available action initially", a1, branch.getNextAvailableAction());
    }

    @Test
    public void nextAvailableActionOnBranchWithNoChildren() {
        assertNull("Empty branch returns null", branch("b").getNextAvailableAction());
    }

    @Test
    public void nextAvailableActionOnFullyCompleteBranch() {
        CaseStoryNode branch = branch("b");
        CaseStoryNode a1 = action("a1");
        branch.addChild(a1);
        a1.complete();
        assertNull("Fully complete branch returns null", branch.getNextAvailableAction());
    }

    // -------------------------------------------------------------------------
    // toString
    // -------------------------------------------------------------------------

    @Test
    public void toStringContainsId() {
        CaseStoryNode n = new CaseStoryNode("my-unique-42", "Title", "Desc",
                CaseStoryNode.NodeType.PLOT_TWIST);
        assertTrue(n.toString().contains("my-unique-42"));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static CaseStoryNode action(String id) {
        return new CaseStoryNode(id, "Action " + id, "Desc", CaseStoryNode.NodeType.ACTION);
    }

    private static CaseStoryNode branch(String id) {
        return new CaseStoryNode(id, "Branch " + id, "Desc",
                CaseStoryNode.NodeType.MAJOR_PROGRESS);
    }
}

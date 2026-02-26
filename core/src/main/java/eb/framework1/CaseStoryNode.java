package eb.framework1;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A single node in the story-progression tree attached to a {@link CaseFile}.
 *
 * <p>The tree is structured as four named levels beneath a single invisible
 * {@link NodeType#ROOT} container:
 * <ol>
 *   <li>{@link NodeType#PLOT_TWIST} — a major investigative phase (one node per
 *       complexity level; the first is always the initial investigation, later
 *       ones represent plot twists such as a missing-person case becoming a
 *       murder investigation).</li>
 *   <li>{@link NodeType#MAJOR_PROGRESS} — a significant milestone within a
 *       phase (e.g. "Gather Primary Evidence").</li>
 *   <li>{@link NodeType#MINOR_PROGRESS} — a focused sub-task required to
 *       achieve a milestone (e.g. "Witness Interviews").</li>
 *   <li>{@link NodeType#ACTION} <em>(leaf)</em> — a concrete puzzle, discovery,
 *       or action the player must perform (e.g. "Photograph the scene").</li>
 * </ol>
 *
 * <p><strong>Sequential-unlock rule:</strong> among a node's children, child at
 * position {@code i} becomes available only after every child at positions
 * {@code 0..i-1} is fully complete.  Use {@link #isChildAvailable(int)} to
 * check accessibility and {@link #getNextActiveChild()} to find the current
 * task.
 *
 * <p>Leaf ({@link NodeType#ACTION}) nodes are completed by calling
 * {@link #complete()}.  Branch nodes are considered fully complete when every
 * descendant action is complete — see {@link #isFullyComplete()}.
 *
 * <p>Story trees are built and attached to a {@link CaseFile} by
 * {@link CaseGenerator}; the root node is accessible via
 * {@link CaseFile#getStoryRoot()}.
 */
public class CaseStoryNode {

    /** Structural role of this node in the story-progression tree. */
    public enum NodeType {
        /** Invisible root container — one per case. */
        ROOT,
        /** Level-1: a major investigative phase or plot twist. */
        PLOT_TWIST,
        /** Level-2: a significant milestone within a phase. */
        MAJOR_PROGRESS,
        /** Level-3: a sub-task contributing to a milestone. */
        MINOR_PROGRESS,
        /** Leaf: a concrete action the player must perform. */
        ACTION
    }

    private final String   id;
    private final String   title;
    private final String   description;
    private final NodeType nodeType;
    private final List<CaseStoryNode> children;
    /** Completion flag — meaningful only for {@link NodeType#ACTION} leaves. */
    private boolean completed;

    /**
     * Creates a new, incomplete story node.
     *
     * @param id          unique identifier within the case (must not be blank)
     * @param title       short label for display (must not be blank)
     * @param description longer explanation; {@code null} is silently treated
     *                    as an empty string
     * @param nodeType    structural role of this node (must not be {@code null})
     * @throws IllegalArgumentException if {@code id}, {@code title}, or
     *         {@code nodeType} is invalid
     */
    public CaseStoryNode(String id, String title, String description,
                         NodeType nodeType) {
        String trimmedId    = id    != null ? id.trim()    : "";
        String trimmedTitle = title != null ? title.trim() : "";
        if (trimmedId.isEmpty()) {
            throw new IllegalArgumentException("CaseStoryNode id must not be blank");
        }
        if (trimmedTitle.isEmpty()) {
            throw new IllegalArgumentException("CaseStoryNode title must not be blank");
        }
        if (nodeType == null) {
            throw new IllegalArgumentException("CaseStoryNode nodeType must not be null");
        }
        this.id          = trimmedId;
        this.title       = trimmedTitle;
        this.description = description != null ? description.trim() : "";
        this.nodeType    = nodeType;
        this.children    = new ArrayList<>();
        this.completed   = false;
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    /** Unique identifier for this node within its case. */
    public String getId() { return id; }

    /** Short label shown in the UI (e.g. "Gather Primary Evidence"). */
    public String getTitle() { return title; }

    /** Longer description of what this node represents (may be empty). */
    public String getDescription() { return description; }

    /** The structural role of this node in the tree. */
    public NodeType getNodeType() { return nodeType; }

    /**
     * Returns an unmodifiable, ordered view of this node's children.
     * Children are presented and unlocked in order.
     */
    public List<CaseStoryNode> getChildren() {
        return Collections.unmodifiableList(children);
    }

    /**
     * Returns {@code true} if this node has been individually marked as
     * complete via {@link #complete()}.
     *
     * <p>For branch nodes prefer {@link #isFullyComplete()}, which checks the
     * entire sub-tree.
     */
    public boolean isCompleted() { return completed; }

    // -------------------------------------------------------------------------
    // Mutation
    // -------------------------------------------------------------------------

    /**
     * Appends {@code child} as the last child of this node.  Children are
     * unlocked in the order they are added.
     *
     * @param child the node to append; must not be {@code null}
     * @throws IllegalArgumentException if {@code child} is {@code null}
     */
    public void addChild(CaseStoryNode child) {
        if (child == null) {
            throw new IllegalArgumentException("CaseStoryNode child must not be null");
        }
        children.add(child);
    }

    /**
     * Marks this {@link NodeType#ACTION} leaf as complete.  Calling this more
     * than once is a no-op.  Calling it on a branch node has no structural
     * effect — the branch is still incomplete until all of its descendants are
     * done.
     */
    public void complete() {
        this.completed = true;
    }

    // -------------------------------------------------------------------------
    // Progress queries
    // -------------------------------------------------------------------------

    /**
     * Returns {@code true} when the entire sub-tree rooted at this node is
     * done.
     * <ul>
     *   <li>Leaf ({@link NodeType#ACTION}) nodes: equivalent to
     *       {@link #isCompleted()}.</li>
     *   <li>Branch nodes: every child must return {@code true} from
     *       {@code isFullyComplete()}.</li>
     * </ul>
     */
    public boolean isFullyComplete() {
        if (children.isEmpty()) return completed;
        return children.stream().allMatch(CaseStoryNode::isFullyComplete);
    }

    /**
     * Returns the first child that is not yet fully complete, provided that
     * every child before it in the list is already fully complete (sequential-
     * unlock rule).  Returns {@code null} when all children are complete or
     * there are no children.
     *
     * <p>Use this to find the child the player should work on next.
     */
    public CaseStoryNode getNextActiveChild() {
        for (CaseStoryNode child : children) {
            if (!child.isFullyComplete()) return child;
        }
        return null;
    }

    /**
     * Recursively finds the first available, non-completed
     * {@link NodeType#ACTION} leaf reachable from this node via the
     * sequential-unlock rule.
     *
     * <p>For an {@link NodeType#ACTION} leaf, returns itself if not yet
     * completed, or {@code null} if already completed.  For branch nodes,
     * delegates to {@link #getNextActiveChild()} and recurses, so the
     * sequential-unlock constraint is honoured at every level.
     *
     * @return the next action the player should perform, or {@code null} if
     *         all actions in this sub-tree are already complete
     */
    public CaseStoryNode getNextAvailableAction() {
        if (nodeType == NodeType.ACTION) {
            return completed ? null : this;
        }
        CaseStoryNode next = getNextActiveChild();
        if (next == null) return null;
        return next.getNextAvailableAction();
    }

    /**
     * Returns {@code true} if the child at zero-based {@code index} is
     * currently accessible — every child before it must be fully complete.
     *
     * @param index zero-based index into {@link #getChildren()}
     * @return {@code false} if the index is out of range or a preceding child
     *         is incomplete
     */
    public boolean isChildAvailable(int index) {
        if (index < 0 || index >= children.size()) return false;
        for (int i = 0; i < index; i++) {
            if (!children.get(i).isFullyComplete()) return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "CaseStoryNode{id='" + id + "', type=" + nodeType
                + ", children=" + children.size()
                + ", fullyComplete=" + isFullyComplete() + '}';
    }
}

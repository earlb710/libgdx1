package eb.admin.ui;

import eb.admin.model.CategoryData;
import eb.admin.model.CategoryEntry;
import eb.framework1.investigation.CaseGenerator;
import eb.framework1.investigation.CaseType;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Admin panel for step-by-step case generation testing.
 *
 * <p>This panel mirrors the stages of the core {@code CaseGenerator} and lets
 * the admin manually walk through each step, inspect the generated data, and
 * tweak values before moving on.  It does <em>not</em> depend on the core
 * module — all generation logic is reproduced locally using the code tables
 * loaded from {@code category_en.json} via {@link CategoryData}.
 *
 * <h3>Steps</h3>
 * <ol>
 *   <li><b>Case Type</b> — choose a type from the case_types table</li>
 *   <li><b>NPC Characters</b> — enter client/subject names, generate
 *       case-specific NPCs with roles and relationships</li>
 *   <li><b>Description &amp; Objective</b> — view generated narrative text</li>
 *   <li><b>Leads</b> — add hidden leads with discovery methods</li>
 *   <li><b>Story Tree</b> — build and inspect the four-level story tree</li>
 * </ol>
 */
public class CaseEditorPanel extends JPanel {

    private final JLabel statusLabel;
    private final Random random = new Random();

    // Step 1 – Case Type
    private final JComboBox<String> caseTypeCombo = new JComboBox<>();
    private final JLabel caseTypeDesc = new JLabel(" ");
    private final JSpinner complexitySpinner =
            new JSpinner(new SpinnerNumberModel(1, 1, 3, 1));

    // Step 2 – NPC Characters (includes Client & Subject names)
    private final JTextField clientNameField  = new JTextField(20);
    private final JTextField subjectNameField = new JTextField(20);
    private final JTextField victimNameField  = new JTextField(20);
    private final JLabel     victimNameLabel  = new JLabel("  Victim Name:");
    private final JComboBox<String> clientGenderCombo  = new JComboBox<>(new String[]{"M", "F"});
    private final JComboBox<String> subjectGenderCombo = new JComboBox<>(new String[]{"M", "F"});
    private final DefaultTableModel npcModel =
            new DefaultTableModel(new String[]{
                    "Role", "Name", "Gender", "Age", "Occupation",
                    "Cooperativeness", "Honesty", "Nervousness",
                    "Dead", "Death Date/Time", "Variance (min)"}, 0) {
                @Override
                public Class<?> getColumnClass(int col) {
                    if (col == 8) return Boolean.class;   // Dead checkbox
                    if (col == 9) return Long.class;      // Death epoch millis
                    if (col == 3 || col == 5 || col == 6 || col == 7 || col == 10) return Integer.class;
                    return String.class;
                }
            };
    private final DefaultTableModel relationshipModel =
            new DefaultTableModel(new String[]{
                    "From", "To", "Type", "Opinion"}, 0);

    // Step 3 – Description & Objective
    private final JTextArea descriptionArea = new JTextArea(4, 60);
    private final JTextArea objectiveArea   = new JTextArea(2, 60);

    // Step 4 – Leads
    private final DefaultTableModel leadsModel =
            new DefaultTableModel(new String[]{"ID", "Hint", "Discovery Method", "Description"}, 0);

    // Step 5 – Facts
    // Columns: 0=ID, 1=Category, 2=Fact, 3=Status, 4=Date(epoch),
    //          5=Char1, 6=Char2, 7=Rel Type, 8=Item ID, 9=Evidence ID, 10=Importance
    private final DefaultTableModel factsModel =
            new DefaultTableModel(new String[]{
                    "ID", "Category", "Fact", "Status",
                    "Date (epoch)", "Char1", "Char2", "Rel Type",
                    "Item ID", "Evidence ID", "Importance"}, 0) {
                @Override public boolean isCellEditable(int row, int col) { return col != 0; }
                @Override public Class<?> getColumnClass(int col) {
                    if (col == 4)  return Long.class;
                    if (col == 10) return Integer.class;
                    return String.class;
                }
            };

    // Step 6 – Story Tree
    private final DefaultMutableTreeNode storyRoot = new DefaultMutableTreeNode("Story Root");
    private final DefaultTreeModel       treeModel = new DefaultTreeModel(storyRoot);
    private final JTree                  storyTree = new JTree(treeModel);
    private final JTextArea              nodeDetailArea = new JTextArea();

    // Summary
    private final JTextArea summaryArea = new JTextArea(12, 60);

    /** Cached copy of the loaded category data — set via {@link #loadData(CategoryData)}. */
    private CategoryData categoryData;

    public CaseEditorPanel(JLabel statusLabel) {
        this.statusLabel = statusLabel;
        setLayout(new BorderLayout(0, 4));
        add(buildSteps(), BorderLayout.CENTER);
    }

    // -------------------------------------------------------------------------
    // Data loading
    // -------------------------------------------------------------------------

    /**
     * Loads category data so the panel can populate combo boxes from the
     * code tables.  Called by {@link CategoryEditorScreen} after the JSON is
     * loaded.
     */
    public void loadData(CategoryData data) {
        this.categoryData = data;
        refreshCaseTypeCombo();
        refreshDiscoveryMethodColumn();
    }

    /** Clears all fields and resets the panel to its initial state. */
    public void clearAll() {
        caseTypeCombo.removeAllItems();
        caseTypeDesc.setText(" ");
        complexitySpinner.setValue(1);
        clientNameField.setText("");
        subjectNameField.setText("");
        victimNameField.setText("");
        npcModel.setRowCount(0);
        relationshipModel.setRowCount(0);
        descriptionArea.setText("");
        objectiveArea.setText("");
        leadsModel.setRowCount(0);
        factsModel.setRowCount(0);
        storyRoot.removeAllChildren();
        treeModel.reload();
        summaryArea.setText("");
        categoryData = null;
    }

    // -------------------------------------------------------------------------
    // UI construction
    // -------------------------------------------------------------------------

    private JTabbedPane buildSteps() {
        JTabbedPane steps = new JTabbedPane();
        steps.addTab("1. Case Type",              buildCaseTypeStep());
        steps.addTab("2. NPC Characters",         buildNpcStep());
        steps.addTab("3. Description & Objective", buildDescriptionStep());
        steps.addTab("4. Leads",                  buildLeadsStep());
        steps.addTab("5. Facts",                  buildFactsStep());
        steps.addTab("6. Story Tree",             buildStoryTreeStep());
        steps.addTab("Summary",                   buildSummaryStep());
        return steps;
    }

    // -- Step 1 ---------------------------------------------------------------

    private JPanel buildCaseTypeStep() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0;
        form.add(new JLabel("Case Type:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1;
        form.add(caseTypeCombo, gbc);

        caseTypeCombo.addActionListener(e -> updateCaseTypeDescription());

        gbc.gridx = 0; gbc.gridy = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        form.add(new JLabel("Description:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1;
        caseTypeDesc.setFont(caseTypeDesc.getFont().deriveFont(Font.ITALIC));
        form.add(caseTypeDesc, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        form.add(new JLabel("Complexity (1–3):"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.NONE;
        form.add(complexitySpinner, gbc);

        JButton randomBtn = new JButton("Random Type");
        randomBtn.addActionListener(e -> pickRandomCaseType());
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.CENTER;
        form.add(randomBtn, gbc);

        panel.add(form, BorderLayout.NORTH);

        JLabel info = new JLabel(
                "<html><i>Select a case type and complexity, then move to the next tab.</i></html>");
        panel.add(info, BorderLayout.SOUTH);
        return panel;
    }

    // -- Step 2: NPC Characters (includes Client & Subject name fields) ------

    private JPanel buildNpcStep() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // --- Client & Subject name fields (top) ---
        JPanel nameForm = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0;
        nameForm.add(new JLabel("Client Name:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1;
        nameForm.add(clientNameField, gbc);
        gbc.gridx = 2; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        nameForm.add(new JLabel(" Gender:"), gbc);
        gbc.gridx = 3;
        nameForm.add(clientGenderCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        nameForm.add(new JLabel("Subject Name:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1;
        nameForm.add(subjectNameField, gbc);
        gbc.gridx = 2; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        nameForm.add(new JLabel(" Gender:"), gbc);
        gbc.gridx = 3;
        nameForm.add(subjectGenderCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        nameForm.add(victimNameLabel, gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1;
        nameForm.add(victimNameField, gbc);
        // Victim name only shown for Murder — visibility toggled in updateVictimNameVisibility()
        victimNameLabel.setVisible(false);
        victimNameField.setVisible(false);

        // --- NPC table ---
        JTable npcTable = new JTable(npcModel);
        npcTable.setRowHeight(26);
        npcTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        npcTable.getTableHeader().setReorderingAllowed(false);
        npcTable.getColumnModel().getColumn(0).setPreferredWidth(120);  // Role
        npcTable.getColumnModel().getColumn(1).setPreferredWidth(160);  // Name
        npcTable.getColumnModel().getColumn(2).setPreferredWidth(60);   // Gender
        npcTable.getColumnModel().getColumn(3).setPreferredWidth(50);   // Age
        npcTable.getColumnModel().getColumn(4).setPreferredWidth(140);  // Occupation
        npcTable.getColumnModel().getColumn(5).setPreferredWidth(80);   // Cooperativeness
        npcTable.getColumnModel().getColumn(6).setPreferredWidth(60);   // Honesty
        npcTable.getColumnModel().getColumn(7).setPreferredWidth(70);   // Nervousness
        npcTable.getColumnModel().getColumn(8).setPreferredWidth(40);   // Dead
        npcTable.getColumnModel().getColumn(9).setPreferredWidth(120);  // Death Date/Time
        npcTable.getColumnModel().getColumn(10).setPreferredWidth(80);  // Variance

        // Render Death Date/Time column as formatted "YYYY-MM-DD HH:mm" instead of raw long
        npcTable.getColumnModel().getColumn(9).setCellRenderer(
                new javax.swing.table.DefaultTableCellRenderer() {
                    @Override
                    public void setValue(Object value) {
                        if (value instanceof Number) {
                            long millis = ((Number) value).longValue();
                            if (millis == 0L) {
                                setText("");
                            } else {
                                java.util.Calendar cal = java.util.Calendar.getInstance(
                                        java.util.TimeZone.getTimeZone("UTC"));
                                cal.setTimeInMillis(millis);
                                setText(String.format("%04d-%02d-%02d %02d:%02d",
                                        cal.get(java.util.Calendar.YEAR),
                                        cal.get(java.util.Calendar.MONTH) + 1,
                                        cal.get(java.util.Calendar.DAY_OF_MONTH),
                                        cal.get(java.util.Calendar.HOUR_OF_DAY),
                                        cal.get(java.util.Calendar.MINUTE)));
                            }
                        } else {
                            super.setValue(value);
                        }
                    }
                });

        // --- Relationship table ---
        JTable relTable = new JTable(relationshipModel);
        relTable.setRowHeight(26);
        relTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        relTable.getTableHeader().setReorderingAllowed(false);
        relTable.getColumnModel().getColumn(0).setPreferredWidth(160);  // From
        relTable.getColumnModel().getColumn(1).setPreferredWidth(160);  // To
        relTable.getColumnModel().getColumn(2).setPreferredWidth(140);  // Type
        relTable.getColumnModel().getColumn(3).setPreferredWidth(80);   // Opinion

        // Split pane with NPC table above, relationships below
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                new JScrollPane(npcTable), new JScrollPane(relTable));
        split.setDividerLocation(250);
        split.setResizeWeight(0.6);

        // --- Buttons ---
        JButton genNpcsBtn  = new JButton("Generate NPCs");
        JButton addNpcBtn   = new JButton("Add NPC");
        JButton delNpcBtn   = new JButton("Delete NPC");
        JButton genRelsBtn  = new JButton("Generate Relationships");
        JButton addRelBtn   = new JButton("Add Relationship");
        JButton delRelBtn   = new JButton("Delete Relationship");

        genNpcsBtn.addActionListener(e -> generateNpcs());
        addNpcBtn.addActionListener(e -> {
            npcModel.addRow(new Object[]{"", "", "M", 30, "", 5, 5, 5, false, 0L, 0});
        });
        delNpcBtn.addActionListener(e -> {
            int row = npcTable.getSelectedRow();
            if (row >= 0) npcModel.removeRow(row);
        });
        genRelsBtn.addActionListener(e -> generateRelationships());
        addRelBtn.addActionListener(e -> {
            relationshipModel.addRow(new Object[]{"", "", "", 0});
        });
        delRelBtn.addActionListener(e -> {
            int row = relTable.getSelectedRow();
            if (row >= 0) relationshipModel.removeRow(row);
        });

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        buttons.add(genNpcsBtn);
        buttons.add(addNpcBtn);
        buttons.add(delNpcBtn);
        buttons.add(Box.createHorizontalStrut(18));
        buttons.add(genRelsBtn);
        buttons.add(addRelBtn);
        buttons.add(delRelBtn);

        panel.add(nameForm, BorderLayout.NORTH);
        panel.add(split,    BorderLayout.CENTER);
        panel.add(buttons,  BorderLayout.SOUTH);
        return panel;
    }

    // -- Step 3 ---------------------------------------------------------------

    private JPanel buildDescriptionStep() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        objectiveArea.setLineWrap(true);
        objectiveArea.setWrapStyleWord(true);

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0; gbc.fill = GridBagConstraints.NONE;
        form.add(new JLabel("Description:"), gbc);
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 1; gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1;
        form.add(new JScrollPane(descriptionArea), gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0; gbc.weighty = 0;
        gbc.fill = GridBagConstraints.NONE;
        form.add(new JLabel("Objective:"), gbc);
        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 1; gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 0.5;
        form.add(new JScrollPane(objectiveArea), gbc);

        JButton generateBtn = new JButton("Generate Description & Objective");
        generateBtn.addActionListener(e -> generateDescriptionAndObjective());
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        btnPanel.add(generateBtn);

        panel.add(form, BorderLayout.CENTER);
        panel.add(btnPanel, BorderLayout.SOUTH);
        return panel;
    }

    // -- Step 4 ---------------------------------------------------------------

    private JPanel buildLeadsStep() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JTable leadsTable = new JTable(leadsModel);
        leadsTable.setRowHeight(26);
        leadsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        leadsTable.getTableHeader().setReorderingAllowed(false);
        leadsTable.getColumnModel().getColumn(0).setPreferredWidth(80);
        leadsTable.getColumnModel().getColumn(1).setPreferredWidth(250);
        leadsTable.getColumnModel().getColumn(2).setPreferredWidth(140);
        leadsTable.getColumnModel().getColumn(3).setPreferredWidth(300);

        JScrollPane scroll = new JScrollPane(leadsTable);

        JButton addBtn     = new JButton("Add Lead");
        JButton deleteBtn  = new JButton("Delete Lead");
        JButton genLeadsBtn = new JButton("Generate Leads");

        addBtn.addActionListener(e -> {
            int nextId = leadsModel.getRowCount() + 1;
            leadsModel.addRow(new Object[]{"lead-" + nextId, "", "", ""});
        });

        deleteBtn.addActionListener(e -> {
            int row = leadsTable.getSelectedRow();
            if (row >= 0) leadsModel.removeRow(row);
        });

        genLeadsBtn.addActionListener(e -> generateLeads());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        buttons.add(addBtn);
        buttons.add(deleteBtn);
        buttons.add(Box.createHorizontalStrut(12));
        buttons.add(genLeadsBtn);

        panel.add(scroll,  BorderLayout.CENTER);
        panel.add(buttons, BorderLayout.SOUTH);
        return panel;
    }

    // -- Step 5 ---------------------------------------------------------------

    private JPanel buildFactsStep() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JTable factsTable = new JTable(factsModel);
        factsTable.setRowHeight(26);
        factsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        factsTable.getTableHeader().setReorderingAllowed(false);
        factsTable.getColumnModel().getColumn(0).setPreferredWidth(70);
        factsTable.getColumnModel().getColumn(1).setPreferredWidth(100);
        factsTable.getColumnModel().getColumn(2).setPreferredWidth(300);
        factsTable.getColumnModel().getColumn(3).setPreferredWidth(80);
        factsTable.getColumnModel().getColumn(4).setPreferredWidth(110);
        factsTable.getColumnModel().getColumn(5).setPreferredWidth(90);
        factsTable.getColumnModel().getColumn(6).setPreferredWidth(90);
        factsTable.getColumnModel().getColumn(7).setPreferredWidth(100);
        factsTable.getColumnModel().getColumn(8).setPreferredWidth(110);
        factsTable.getColumnModel().getColumn(9).setPreferredWidth(120);
        factsTable.getColumnModel().getColumn(10).setPreferredWidth(80);

        // Status column — restricted combo: KNOWN or HIDDEN
        JComboBox<String> statusCombo = new JComboBox<>(new String[]{"KNOWN", "HIDDEN"});
        factsTable.getColumnModel().getColumn(3)
                .setCellEditor(new DefaultCellEditor(statusCombo));

        JScrollPane scroll = new JScrollPane(factsTable);

        JButton addBtn    = new JButton("Add Fact");
        JButton deleteBtn = new JButton("Delete Fact");
        JButton genBtn    = new JButton("Generate Facts");

        addBtn.addActionListener(e -> {
            int nextId = factsModel.getRowCount() + 1;
            factsModel.addRow(new Object[]{
                    "fact-" + nextId, "DATE", "", "HIDDEN",
                    0L, "", "", "", "", "", 0});
        });

        deleteBtn.addActionListener(e -> {
            int row = factsTable.getSelectedRow();
            if (row >= 0) factsModel.removeRow(row);
        });

        genBtn.addActionListener(e -> generateFacts());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        buttons.add(addBtn);
        buttons.add(deleteBtn);
        buttons.add(Box.createHorizontalStrut(12));
        buttons.add(genBtn);

        panel.add(scroll,   BorderLayout.CENTER);
        panel.add(buttons,  BorderLayout.SOUTH);
        return panel;
    }

    // -- Step 6 ---------------------------------------------------------------

    private JPanel buildStoryTreeStep() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        storyTree.setRootVisible(true);
        storyTree.setShowsRootHandles(true);
        JScrollPane treeScroll = new JScrollPane(storyTree);

        // Right-hand detail area
        nodeDetailArea.setLineWrap(true);
        nodeDetailArea.setWrapStyleWord(true);
        nodeDetailArea.setEditable(false);
        nodeDetailArea.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        nodeDetailArea.setText("Click a node to see its description.");
        JScrollPane detailScroll = new JScrollPane(nodeDetailArea);
        detailScroll.setBorder(BorderFactory.createTitledBorder("Node Description"));

        // Wire selection listener
        storyTree.addTreeSelectionListener(e -> {
            TreePath path = storyTree.getSelectionPath();
            if (path == null) {
                nodeDetailArea.setText("");
                return;
            }
            DefaultMutableTreeNode node =
                    (DefaultMutableTreeNode) path.getLastPathComponent();
            String label = node.getUserObject() != null ? node.getUserObject().toString() : "";
            nodeDetailArea.setText(buildNodeDescription(label));
            nodeDetailArea.setCaretPosition(0);
        });

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treeScroll, detailScroll);
        split.setDividerLocation(340);
        split.setResizeWeight(0.45);

        JButton genTreeBtn    = new JButton("Generate Story Tree");
        JButton addChildBtn   = new JButton("Add Child");
        JButton deleteNodeBtn = new JButton("Delete Node");
        JButton expandBtn     = new JButton("Expand All");

        genTreeBtn.addActionListener(e -> generateStoryTree());

        addChildBtn.addActionListener(e -> {
            TreePath path = storyTree.getSelectionPath();
            DefaultMutableTreeNode parent = (path != null)
                    ? (DefaultMutableTreeNode) path.getLastPathComponent()
                    : storyRoot;
            DefaultMutableTreeNode child = new DefaultMutableTreeNode("New Node");
            treeModel.insertNodeInto(child, parent, parent.getChildCount());
            storyTree.expandPath(new TreePath(parent.getPath()));
        });

        deleteNodeBtn.addActionListener(e -> {
            TreePath path = storyTree.getSelectionPath();
            if (path != null) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                if (node != storyRoot) {
                    treeModel.removeNodeFromParent(node);
                }
            }
        });

        expandBtn.addActionListener(e -> expandAll(storyTree));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        buttons.add(genTreeBtn);
        buttons.add(addChildBtn);
        buttons.add(deleteNodeBtn);
        buttons.add(Box.createHorizontalStrut(12));
        buttons.add(expandBtn);

        panel.add(split,   BorderLayout.CENTER);
        panel.add(buttons, BorderLayout.SOUTH);
        return panel;
    }

    /**
     * Generates a human-readable description for a story-tree node based on its label.
     * Node labels follow the pattern "[TYPE] Title", where TYPE is one of:
     * ACTION, MINOR, MAJOR, PLOT_TWIST.
     */
    private String buildNodeDescription(String label) {
        if (label == null || label.isEmpty()) return "";

        // Strip the leading [TYPE] tag
        String tag   = "";
        String title = label;
        if (label.startsWith("[") && label.contains("] ")) {
            int close = label.indexOf(']');
            tag   = label.substring(1, close).trim();
            title = label.substring(close + 2).trim();
        }

        String subject = subjectNameField.getText().trim();
        if (subject.isEmpty()) subject = "the subject";
        String victim  = victimNameField.getText().trim();
        if (victim.isEmpty()) victim = "the victim";
        String client  = clientNameField.getText().trim();
        if (client.isEmpty()) client = "the client";

        switch (tag) {
            case "ACTION":
                return buildActionDescription(title, subject, victim, client);
            case "MINOR":
                return "Minor objective: " + title + ".\n\n"
                        + "This step groups a set of related actions the investigator must "
                        + "complete to advance the current major objective. "
                        + "Complete all actions beneath this node to unlock the next minor step.";
            case "MAJOR":
                return "Major objective: " + title + ".\n\n"
                        + "This is one of the primary goals for the current investigation phase. "
                        + "Progress here drives the overall case forward and may reveal new leads.";
            case "PLOT_TWIST":
                return "Phase: " + title + ".\n\n"
                        + "This phase represents a key turning point in the investigation. "
                        + "Completing it unlocks the next chapter and may change the direction "
                        + "of the case based on what has been discovered.";
            default:
                return title;
        }
    }

    /** Generates a detailed action description for each known action title. */
    private String buildActionDescription(String title, String subject, String victim, String client) {
        if ("Photograph the scene".equals(title)) {
            return "Action: Photograph the scene.\n\n"
                    + "The investigator documents the location with photographs. "
                    + "Key items to capture include entry and exit points, signs of disturbance, "
                    + "any objects out of place, and the general layout of the area. "
                    + "Photos will be compared against the official report and may "
                    + "contradict the coroner's findings.\n\n"
                    + "Possible Results:\n"
                    + "  [PERCEPTION: 2]      Notices general layout and obvious disturbances — "
                    + "confirms the scene matches the official report.\n"
                    + "  [PERCEPTION: 4]      Spots a detail out of place (moved furniture, disturbed "
                    + "dust, partial footprint) — adds a minor clue to the case file.\n"
                    + "  [PERCEPTION: 6]      Identifies a hidden or deliberately concealed item "
                    + "(covered blood stain, tampered lock) — unlocks a new lead.\n"
                    + "  [INTELLIGENCE: 4]    Cross-references photos against the coroner's "
                    + "measurements — finds a discrepancy in the official timeline.";

        } else if ("Collect physical evidence".equals(title)) {
            return "Action: Collect physical evidence.\n\n"
                    + "Physical items at the scene are catalogued and preserved. "
                    + "This includes anything that could place " + subject
                    + " at the location, establish a timeline, or link a third party "
                    + "to the incident. Proper chain of custody must be maintained.\n\n"
                    + "Possible Results:\n"
                    + "  [INTELLIGENCE: 2]    Recovers surface-level items (discarded items, "
                    + "obvious debris) — standard evidence log entry.\n"
                    + "  [INTELLIGENCE: 4]    Recovers trace evidence (fibres, hair, partial "
                    + "fingerprint) — provides a forensic lead linking " + subject + " or a third party.\n"
                    + "  [INTELLIGENCE: 6]    Uncovers concealed or micro-scale evidence "
                    + "(DNA under fingernails, hidden compartment) — strong physical link.\n"
                    + "  [PERCEPTION: 5]      Notices that a specific item is missing from where "
                    + "it should be — suggests deliberate removal or staging.";

        } else if (title.startsWith("Interview a contact of")) {
            return "Action: Interview a contact of " + subject + ".\n\n"
                    + "The investigator approaches someone in " + subject
                    + "'s social or professional circle. "
                    + "The goal is to establish " + subject
                    + "'s movements, relationships, and possible motive. "
                    + "The contact's cooperativeness and honesty will affect what is revealed.\n\n"
                    + "Possible Results:\n"
                    + "  [CHARISMA: 2]        Contact speaks briefly — confirms or denies a known "
                    + "alibi for " + subject + ".\n"
                    + "  [CHARISMA: 4]        Contact opens up — reveals a detail about " + subject
                    + "'s recent behaviour or state of mind.\n"
                    + "  [CHARISMA: 6]        Contact confides something sensitive — exposes a "
                    + "hidden relationship or secret relevant to the case.\n"
                    + "  [INTIMIDATION: 4]    Investigator applies pressure — contact divulges "
                    + "information they were reluctant to share.\n"
                    + "  [EMPATHY: 3]         Reads the contact's emotional state — determines "
                    + "whether fear, guilt, or loyalty is shaping their answers.";

        } else if ("Review documents or records".equals(title)) {
            return "Action: Review documents or records.\n\n"
                    + "Relevant paperwork — financial statements, phone records, "
                    + "employment files, or official reports — is obtained and analysed. "
                    + "Discrepancies between the documented record and witness accounts "
                    + "may surface hidden connections between " + subject
                    + " and " + victim + ".\n\n"
                    + "Possible Results:\n"
                    + "  [INTELLIGENCE: 2]    Locates the requested documents and confirms "
                    + "basic facts (dates, names, addresses).\n"
                    + "  [INTELLIGENCE: 4]    Spots an anomaly in the records (unusual "
                    + "payment, altered entry, missing period) — raises a question about "
                    + subject + "'s activities.\n"
                    + "  [INTELLIGENCE: 6]    Traces a financial or communication chain "
                    + "connecting " + subject + " and " + victim
                    + " — provides strong circumstantial evidence.\n"
                    + "  [MEMORY: 3]          Recalls a related fact from earlier in the case — "
                    + "cross-links two previously unconnected details.\n"
                    + "  [STEALTH: 4]         Recognises forged or tampered documents — "
                    + "proves deliberate cover-up.";

        } else {
            // Generic fallback for manually added or custom actions
            return "Action: " + title + ".\n\n"
                    + "The investigator carries out this action as part of the current "
                    + "minor objective. Document findings carefully — any detail could "
                    + "become relevant as the case develops.\n\n"
                    + "Possible Results:\n"
                    + "  [PERCEPTION: 2]      Basic observation — confirms surface-level details.\n"
                    + "  [INTELLIGENCE: 4]    Deeper analysis — uncovers a hidden connection "
                    + "or inconsistency.\n"
                    + "  [INTUITION: 3]       A hunch proves correct — points toward an "
                    + "unexpected angle in the investigation.";
        }
    }

    // -- Summary --------------------------------------------------------------

    private JPanel buildSummaryStep() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        summaryArea.setEditable(false);
        summaryArea.setLineWrap(true);
        summaryArea.setWrapStyleWord(true);
        summaryArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        JButton refreshBtn = new JButton("Refresh Summary");
        refreshBtn.addActionListener(e -> refreshSummary());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        buttons.add(refreshBtn);

        panel.add(new JScrollPane(summaryArea), BorderLayout.CENTER);
        panel.add(buttons, BorderLayout.SOUTH);
        return panel;
    }

    // -------------------------------------------------------------------------
    // Combo / column helpers
    // -------------------------------------------------------------------------

    private void refreshCaseTypeCombo() {
        caseTypeCombo.removeAllItems();
        if (categoryData == null) return;
        for (CategoryEntry e : categoryData.getCase_types()) {
            String name = e.getName() != null ? e.getName() : e.getCode();
            caseTypeCombo.addItem(name);
        }
        updateCaseTypeDescription();
    }

    private void refreshDiscoveryMethodColumn() {
        // Discovery method names are used when generating leads; no column
        // editor is needed because leads are generated programmatically.
    }

    private void updateCaseTypeDescription() {
        if (categoryData == null || caseTypeCombo.getSelectedIndex() < 0) {
            caseTypeDesc.setText(" ");
            updateVictimNameVisibility();
            return;
        }
        int idx = caseTypeCombo.getSelectedIndex();
        List<CategoryEntry> types = categoryData.getCase_types();
        if (idx >= 0 && idx < types.size()) {
            String desc = types.get(idx).getDescription();
            caseTypeDesc.setText(desc != null ? desc : " ");
        }
        updateVictimNameVisibility();
    }

    /** Shows the victim name field only when the selected case type is Murder. */
    private void updateVictimNameVisibility() {
        String caseType = (String) caseTypeCombo.getSelectedItem();
        boolean isMurder = "Murder".equals(caseType);
        victimNameLabel.setVisible(isMurder);
        victimNameField.setVisible(isMurder);
    }

    private void pickRandomCaseType() {
        if (caseTypeCombo.getItemCount() > 0) {
            int idx = random.nextInt(caseTypeCombo.getItemCount());
            caseTypeCombo.setSelectedIndex(idx);
            complexitySpinner.setValue(1 + random.nextInt(3));
            statusLabel.setText("Randomly selected case type: " + caseTypeCombo.getSelectedItem());
        }
    }

    // -------------------------------------------------------------------------
    // NPC generation helpers
    // -------------------------------------------------------------------------

    /** Typical first names per gender, used by the admin panel for quick NPC generation. */
    private static final String[] MALE_NAMES = {
        "James", "Robert", "William", "Thomas", "Michael", "David", "Richard",
        "Daniel", "Edward", "George", "Henry", "Samuel", "Arthur", "Frank",
        "Peter", "Joseph", "Patrick", "Marcus", "Leon", "Vincent"
    };
    private static final String[] FEMALE_NAMES = {
        "Mary", "Elizabeth", "Sarah", "Catherine", "Margaret", "Alice", "Helen",
        "Dorothy", "Grace", "Victoria", "Claire", "Emma", "Sophie", "Hannah",
        "Olivia", "Laura", "Diana", "Angela", "Rose", "Julia"
    };
    private static final String[] SURNAMES = {
        "Smith", "Johnson", "Brown", "Williams", "Jones", "Davis", "Miller",
        "Wilson", "Taylor", "Anderson", "Thomas", "Jackson", "White", "Harris",
        "Clark", "Lewis", "Hall", "Walker", "Young", "King", "Wright", "Green",
        "Baker", "Adams", "Nelson", "Carter", "Mitchell", "Roberts", "Turner",
        "Phillips", "Campbell", "Parker", "Evans", "Collins", "Stewart", "Morris"
    };

    /** Relationship type labels used in the relationship table. */
    private static final String[] RELATIONSHIP_TYPES = {
        "Family", "Friend", "Colleague", "Acquaintance", "Rival",
        "Employer", "Employee", "Neighbour", "Partner", "Ex-Partner",
        "Business Associate"
    };

    /**
     * Returns case-type-specific NPC roles.  The first two roles are always
     * "Client" and "Subject/Victim"; additional roles depend on the case type.
     */
    private String[] rolesForCaseType(String caseType) {
        switch (caseType) {
            case "Missing Person":
                return new String[]{"Client", "Subject (Missing)", "Witness",
                        "Last-Known Contact", "Neighbour"};
            case "Infidelity":
                return new String[]{"Client", "Subject (Partner)", "Other Party",
                        "Mutual Friend", "Witness"};
            case "Theft":
                return new String[]{"Client (Victim)", "Subject (Suspect)",
                        "Witness", "Insurance Adjuster", "Fence/Dealer"};
            case "Fraud":
                return new String[]{"Client (Victim)", "Subject (Perpetrator)",
                        "Accountant", "Business Associate", "Insider Witness"};
            case "Blackmail":
                return new String[]{"Client (Victim)", "Subject (Blackmailer)",
                        "Witness", "Intermediary", "Confidant"};
            case "Murder":
                return new String[]{"Client", "Subject (Suspect)", "Victim",
                        "Key Witness", "Victim's Associate", "Police Contact"};
            case "Stalking":
                return new String[]{"Client (Victim)", "Subject (Stalker)",
                        "Neighbour Witness", "Ex-Partner", "Friend of Client"};
            case "Corporate Espionage":
                return new String[]{"Client (Employer)", "Subject (Leak)",
                        "Rival Contact", "Trusted Colleague", "IT Specialist"};
            default:
                return new String[]{"Client", "Subject", "Witness"};
        }
    }

    /** Typical occupations per NPC role, to add flavour. */
    private String occupationForRole(String role) {
        if (role.startsWith("Client"))           return "—";
        if (role.startsWith("Subject"))          return "—";
        if (role.contains("Witness"))            return pick("Shop Owner", "Bartender", "Office Worker", "Retired Teacher");
        if (role.contains("Neighbour"))          return pick("Retired", "Freelancer", "Nurse", "Electrician");
        if (role.contains("Accountant"))         return "Accountant";
        if (role.contains("Insurance"))          return "Insurance Assessor";
        if (role.contains("Fence") || role.contains("Dealer")) return "Second-Hand Dealer";
        if (role.contains("Police"))             return "Police Detective";
        if (role.contains("IT"))                 return "IT Security Analyst";
        if (role.contains("Friend"))             return pick("Teacher", "Architect", "Journalist", "Consultant");
        if (role.contains("Associate"))          return pick("Manager", "Solicitor", "Accountant", "Director");
        if (role.contains("Other Party"))        return pick("Personal Trainer", "Colleague", "Artist", "Manager");
        if (role.contains("Contact"))            return pick("Receptionist", "Driver", "Office Manager", "Barista");
        if (role.contains("Intermediary"))       return pick("Courier", "Solicitor", "Assistant", "Broker");
        if (role.contains("Confidant"))          return pick("Therapist", "Old Friend", "Sibling", "Clergy");
        if (role.contains("Insider"))            return pick("Junior Accountant", "Office Clerk", "Administrator");
        if (role.contains("Rival"))              return pick("Business Analyst", "Sales Director", "Consultant");
        if (role.contains("Colleague"))          return pick("Software Developer", "Office Manager", "HR Officer");
        if (role.contains("Ex-Partner"))         return pick("Teacher", "Nurse", "Graphic Designer", "Writer");
        return pick("Office Worker", "Shop Assistant", "Driver", "Freelancer");
    }

    private String pick(String... options) {
        return options[random.nextInt(options.length)];
    }

    private String randomName(String gender) {
        String first = "F".equals(gender)
                ? FEMALE_NAMES[random.nextInt(FEMALE_NAMES.length)]
                : MALE_NAMES[random.nextInt(MALE_NAMES.length)];
        String last = SURNAMES[random.nextInt(SURNAMES.length)];
        return first + " " + last;
    }

    private void generateNpcs() {
        String caseType = (String) caseTypeCombo.getSelectedItem();
        if (caseType == null || caseType.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select a case type first.",
                    "Missing Data", JOptionPane.WARNING_MESSAGE);
            return;
        }

        npcModel.setRowCount(0);
        relationshipModel.setRowCount(0);

        String[] roles = rolesForCaseType(caseType);
        String client  = clientNameField.getText().trim();
        String subject = subjectNameField.getText().trim();

        String victim = victimNameField.getText().trim();

        for (int i = 0; i < roles.length; i++) {
            String role   = roles[i];
            String gender = random.nextBoolean() ? "M" : "F";
            String name;
            // Use the names from the name fields for the first two NPCs
            if (i == 0 && !client.isEmpty()) {
                name = client;
            } else if (i == 1 && !subject.isEmpty()) {
                name = subject;
            } else if (role.equals("Victim") && !victim.isEmpty()) {
                name = victim;
            } else {
                name = randomName(gender);
            }
            // Populate name fields with generated names so step 3 can use them
            if (i == 0 && client.isEmpty()) {
                clientNameField.setText(name);
            } else if (i == 1 && subject.isEmpty()) {
                subjectNameField.setText(name);
            } else if (role.equals("Victim") && victim.isEmpty()) {
                victimNameField.setText(name);
            }
            int age  = 22 + random.nextInt(43); // 22–64
            String occupation = occupationForRole(role);
            int cooperativeness = 2 + random.nextInt(8); // 2–9
            int honesty         = 2 + random.nextInt(8);
            int nervousness     = 1 + random.nextInt(9); // 1–9

            // Special personality tweaks per role
            if (role.startsWith("Client")) {
                cooperativeness = 6 + random.nextInt(4); // 6–9 (cooperative)
                honesty         = 5 + random.nextInt(5); // 5–9
            } else if (role.startsWith("Subject")) {
                cooperativeness = 1 + random.nextInt(5); // 1–5 (less cooperative)
                nervousness     = 4 + random.nextInt(6); // 4–9 (more nervous)
            }

            // Death state — only Victim roles in Murder cases are dead
            boolean dead            = false;
            long    deathDateTime   = 0L;
            int     deathVariance   = 0;
            if (role.equals("Victim") && "Murder".equals(caseType)) {
                dead = true;
                // Generate a plausible death date/time: 1–14 days ago, random hour
                int daysAgo = 1 + random.nextInt(14);
                int hour    = random.nextInt(24);
                int minute  = random.nextInt(60);
                java.util.Calendar cal = java.util.Calendar.getInstance(
                        java.util.TimeZone.getTimeZone("UTC"));
                cal.set(2050, 0, 1, 0, 0, 0);  // 2050-01-01 baseline
                cal.set(java.util.Calendar.MILLISECOND, 0);
                cal.add(java.util.Calendar.DAY_OF_YEAR, -daysAgo);
                cal.set(java.util.Calendar.HOUR_OF_DAY, hour);
                cal.set(java.util.Calendar.MINUTE, minute);
                deathDateTime = cal.getTimeInMillis();
                // Variance: 0 = precise, up to 180 min, or -1 = unknown (body missing)
                int roll = random.nextInt(10);
                if (roll < 3) {
                    deathVariance = 0;            // 30% chance: precise
                } else if (roll < 9) {
                    deathVariance = 15 + random.nextInt(166); // 60%: 15–180 min
                } else {
                    deathVariance = -1;           // 10% chance: unknown (body missing)
                    deathDateTime = 0L;
                }
            }

            npcModel.addRow(new Object[]{
                    role, name, gender, age, occupation,
                    cooperativeness, honesty, nervousness,
                    dead, deathDateTime, deathVariance
            });
        }
        statusLabel.setText("Generated " + roles.length + " NPCs for " + caseType + " case.");
    }

    private void generateRelationships() {
        if (npcModel.getRowCount() < 2) {
            JOptionPane.showMessageDialog(this, "Generate NPCs first.",
                    "Missing Data", JOptionPane.WARNING_MESSAGE);
            return;
        }

        relationshipModel.setRowCount(0);

        // Collect NPC names
        List<String> names = new ArrayList<>();
        for (int i = 0; i < npcModel.getRowCount(); i++) {
            names.add((String) npcModel.getValueAt(i, 1));
        }
        // Collect roles for context
        List<String> roles = new ArrayList<>();
        for (int i = 0; i < npcModel.getRowCount(); i++) {
            roles.add((String) npcModel.getValueAt(i, 0));
        }

        // Always create client ↔ subject relationship
        String relType = inferRelationshipType(roles.get(0), roles.get(1),
                (String) caseTypeCombo.getSelectedItem());
        int opinion1 = -20 + random.nextInt(41); // -20 to +20
        relationshipModel.addRow(new Object[]{
                names.get(0), names.get(1), relType, opinion1});
        // Reverse relationship
        int opinion2 = -20 + random.nextInt(41);
        relationshipModel.addRow(new Object[]{
                names.get(1), names.get(0), relType, opinion2});

        // Create relationships between additional NPCs and client or subject
        for (int i = 2; i < names.size(); i++) {
            // Link to either client (index 0) or subject (index 1)
            int target = (i % 2 == 0) ? 0 : 1;
            String type = inferRelationshipType(roles.get(i), roles.get(target),
                    (String) caseTypeCombo.getSelectedItem());
            int op = -10 + random.nextInt(51); // -10 to +40
            relationshipModel.addRow(new Object[]{
                    names.get(i), names.get(target), type, op});
        }

        // Add one or two inter-NPC relationships for depth
        if (names.size() >= 4) {
            String type = RELATIONSHIP_TYPES[random.nextInt(RELATIONSHIP_TYPES.length)];
            int op = -20 + random.nextInt(61); // -20 to +40
            int a = 2 + random.nextInt(names.size() - 2);
            int b = 2 + random.nextInt(names.size() - 2);
            if (a != b) {
                relationshipModel.addRow(new Object[]{
                        names.get(a), names.get(b), type, op});
            }
        }

        statusLabel.setText("Generated " + relationshipModel.getRowCount() + " relationships.");
    }

    /**
     * Infers a sensible relationship type label based on the roles of the two
     * NPCs and the case type.
     */
    private String inferRelationshipType(String role1, String role2, String caseType) {
        // Client–Subject relationship depends on case type
        if ((role1.startsWith("Client") && role2.startsWith("Subject"))
                || (role2.startsWith("Client") && role1.startsWith("Subject"))) {
            switch (caseType != null ? caseType : "") {
                case "Missing Person":  return "Family";
                case "Infidelity":      return "Partner";
                case "Theft":           return "Acquaintance";
                case "Fraud":           return "Business Associate";
                case "Blackmail":       return "Acquaintance";
                case "Murder":          return "Acquaintance";
                case "Stalking":        return "Ex-Partner";
                case "Corporate Espionage": return "Employer";
                default: return "Acquaintance";
            }
        }
        // Witness/neighbour roles
        if (role1.contains("Witness") || role2.contains("Witness"))   return "Acquaintance";
        if (role1.contains("Neighbour") || role2.contains("Neighbour")) return "Neighbour";
        if (role1.contains("Friend") || role2.contains("Friend"))     return "Friend";
        if (role1.contains("Colleague") || role2.contains("Colleague")) return "Colleague";
        if (role1.contains("Partner") || role2.contains("Partner"))    return "Partner";
        if (role1.contains("Associate") || role2.contains("Associate")) return "Business Associate";
        if (role1.contains("Police") || role2.contains("Police"))     return "Acquaintance";
        return RELATIONSHIP_TYPES[random.nextInt(RELATIONSHIP_TYPES.length)];
    }

    // -------------------------------------------------------------------------
    // Generation helpers
    // -------------------------------------------------------------------------

    private void generateDescriptionAndObjective() {
        String caseTypeName = (String) caseTypeCombo.getSelectedItem();
        String client       = clientNameField.getText().trim();
        String subject      = subjectNameField.getText().trim();

        if (caseTypeName == null || caseTypeName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select a case type first.",
                    "Missing Data", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (client.isEmpty())  client  = "the client";
        if (subject.isEmpty()) subject = "the subject";

        CaseType type          = caseTypeFromDisplayName(caseTypeName);
        String clientGender    = (String) clientGenderCombo.getSelectedItem();
        String subjectGender   = (String) subjectGenderCombo.getSelectedItem();
        String victim          = victimNameField.getText().trim();
        if (type == CaseType.MURDER && victim.isEmpty()) victim = "the victim";

        String desc = CaseGenerator.capitalizeSentences(
                CaseGenerator.buildDescription(type, client, subject, victim,
                        clientGender, subjectGender));
        String obj  = CaseGenerator.buildObjective(type, client, subject, victim);

        descriptionArea.setText(desc);
        objectiveArea.setText(obj);
        statusLabel.setText("Generated description and objective for " + caseTypeName + " case.");
    }

    /**
     * Maps a case-type display name (e.g. "Missing Person") to the core
     * {@link CaseType} enum.  Returns {@code null} if no match is found.
     */
    private static CaseType caseTypeFromDisplayName(String displayName) {
        if (displayName == null) return null;
        for (CaseType ct : CaseType.values()) {
            if (ct.getDisplayName().equals(displayName)) return ct;
        }
        return null;
    }

    private void generateLeads() {
        if (categoryData == null) {
            JOptionPane.showMessageDialog(this, "No category data loaded.",
                    "Missing Data", JOptionPane.WARNING_MESSAGE);
            return;
        }
        List<CategoryEntry> methods = categoryData.getDiscovery_methods();
        if (methods.isEmpty()) return;

        leadsModel.setRowCount(0);
        int complexity = (int) complexitySpinner.getValue();
        int leadCount = 2 + complexity; // 3–5 leads based on complexity

        String subject = subjectNameField.getText().trim();
        if (subject.isEmpty()) subject = "the subject";

        for (int i = 1; i <= leadCount; i++) {
            CategoryEntry method = methods.get(random.nextInt(methods.size()));
            String methodName = method.getName() != null ? method.getName() : method.getCode();
            String hint = buildLeadHint(i, methodName, subject);
            String desc = buildLeadDescription(i, methodName, subject);
            leadsModel.addRow(new Object[]{"lead-" + i, hint, methodName, desc});
        }
        statusLabel.setText("Generated " + leadCount + " leads.");
    }

    private void generateFacts() {
        factsModel.setRowCount(0);

        String subject = subjectNameField.getText().trim();
        if (subject.isEmpty()) subject = "the subject";
        String victim  = victimNameField.getText().trim();
        if (victim.isEmpty())  victim  = "the victim";
        String client  = clientNameField.getText().trim();
        if (client.isEmpty())  client  = "the client";

        int factId = 1;

        // --- DATE facts ---------------------------------------------------
        long deathEpoch = 0L;
        String deathDateText = null;
        for (int r = 0; r < npcModel.getRowCount(); r++) {
            Object dead = npcModel.getValueAt(r, 8);
            if (Boolean.TRUE.equals(dead)) {
                Object epoch = npcModel.getValueAt(r, 9);
                if (epoch instanceof Long && (Long) epoch > 0L) {
                    deathEpoch = (Long) epoch;
                    java.text.SimpleDateFormat sdf =
                            new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm");
                    deathDateText = sdf.format(new java.util.Date(deathEpoch));
                }
                break;
            }
        }
        if (deathDateText != null) {
            // col: ID, Cat, Fact, Status, Date(epoch), Char1, Char2, RelType, ItemID, EvidID, Importance
            factsModel.addRow(new Object[]{
                    "fact-" + factId++, "DATE",
                    victim + " was found dead on " + deathDateText + ".",
                    "KNOWN", deathEpoch, "", "", "", "", "", 5});
        } else {
            factsModel.addRow(new Object[]{
                    "fact-" + factId++, "DATE",
                    "Exact date and time of " + victim + "'s death is unknown.",
                    "HIDDEN", 0L, "", "", "", "", "", 5});
        }
        // Last-seen date: 2 days before now as a rough epoch placeholder
        long lastSeenEpoch = System.currentTimeMillis() - 2L * 24 * 60 * 60 * 1000;
        factsModel.addRow(new Object[]{
                "fact-" + factId++, "DATE",
                subject + " was last seen two days before the incident.",
                "HIDDEN", lastSeenEpoch, "", "", "", "", "", 3});
        // Report date: current day morning as placeholder
        factsModel.addRow(new Object[]{
                "fact-" + factId++, "DATE",
                client + " reported the case on the morning after the incident.",
                "KNOWN", System.currentTimeMillis(), "", "", "", "", "", 0});

        // --- RELATIONSHIP facts -------------------------------------------
        factsModel.addRow(new Object[]{
                "fact-" + factId++, "RELATIONSHIP",
                client + " knew " + victim + " personally.",
                "KNOWN", 0L, client, victim, "PERSONAL", "", "", 0});
        factsModel.addRow(new Object[]{
                "fact-" + factId++, "RELATIONSHIP",
                subject + " and " + victim + " had a prior dispute.",
                "HIDDEN", 0L, subject, victim, "DISPUTE", "", "", 4});
        for (int r = 0; r < relationshipModel.getRowCount(); r++) {
            String from    = String.valueOf(relationshipModel.getValueAt(r, 0));
            String to      = String.valueOf(relationshipModel.getValueAt(r, 1));
            String relType = String.valueOf(relationshipModel.getValueAt(r, 2));
            String status  = "HIDDEN";
            if ("Client".equalsIgnoreCase(from) || "Client".equalsIgnoreCase(to)) status = "KNOWN";
            factsModel.addRow(new Object[]{
                    "fact-" + factId++, "RELATIONSHIP",
                    from + " → " + to + " (" + relType + ").",
                    status, 0L, from, to, relType, "", "", 0});
        }

        // --- ITEM facts ---------------------------------------------------
        String[][] knownItemPairs  = {{"Document", "DOCUMENT"}, {"Mobile Phone", "MOBILE_PHONE"}, {"Envelope", "ENVELOPE"}};
        String[][] hiddenItemPairs = {{"Kitchen Knife", "KITCHEN_KNIFE"}, {"Bullet Casing", "BULLET_CASING"},
                                       {"Firearm", "FIREARM"}, {"Syringe", "SYRINGE"},
                                       {"Burned Material", "BURNED_MATERIAL"}, {"Letter", "LETTER"}};
        String[] knownItemPair  = knownItemPairs[random.nextInt(knownItemPairs.length)];
        String[] hiddenItemPair = hiddenItemPairs[random.nextInt(hiddenItemPairs.length)];
        factsModel.addRow(new Object[]{
                "fact-" + factId++, "ITEM",
                "A " + knownItemPair[0].toLowerCase() + " belonging to " + victim
                        + " was recovered at the scene.",
                "KNOWN", 0L, "", "", "", knownItemPair[1], "", 0});
        factsModel.addRow(new Object[]{
                "fact-" + factId++, "ITEM",
                "A " + hiddenItemPair[0].toLowerCase() + " linked to " + subject
                        + " is hidden at an unknown location.",
                "HIDDEN", 0L, "", "", "", hiddenItemPair[1], "", 3});

        // --- EVIDENCE facts -----------------------------------------------
        String[] knownEvidenceIds  = {"FINGERPRINTS", "BLOOD", "HAIR"};
        String[] hiddenEvidenceIds = {"DNA", "BALLISTICS", "TOXICOLOGY", "DIGITAL_DATA", "GUNSHOT_RESIDUE"};
        String knownEvId  = knownEvidenceIds[random.nextInt(knownEvidenceIds.length)];
        String hiddenEvId = hiddenEvidenceIds[random.nextInt(hiddenEvidenceIds.length)];
        factsModel.addRow(new Object[]{
                "fact-" + factId++, "EVIDENCE",
                knownEvId + " evidence was collected from the scene.",
                "KNOWN", 0L, "", "", "", "", knownEvId, 0});
        factsModel.addRow(new Object[]{
                "fact-" + factId++, "EVIDENCE",
                hiddenEvId + " trace links " + subject + " to the incident — awaiting lab confirmation.",
                "HIDDEN", 0L, "", "", "", "", hiddenEvId, 4});

        statusLabel.setText("Generated " + factsModel.getRowCount() + " facts.");
    }

    private String buildLeadHint(int index, String method, String subject) {
        switch (method) {
            case "Interview":
                return "Someone close to " + subject + " might know something.";
            case "Surveillance":
                return subject + " has a regular pattern that could reveal something.";
            case "Forensics":
                return "Physical evidence from the scene needs lab analysis.";
            case "Documents":
                return "There may be a paper trail worth following.";
            case "Physical Search":
                return "A location connected to " + subject + " should be searched.";
            case "Background Check":
                return subject + "'s past may hold important clues.";
            default:
                return "Lead #" + index + " requires further investigation.";
        }
    }

    private String buildLeadDescription(int index, String method, String subject) {
        switch (method) {
            case "Interview":
                return subject + "'s close associate can reveal key information about their recent activities.";
            case "Surveillance":
                return subject + " visits a specific location on a regular schedule that is significant to the case.";
            case "Forensics":
                return "Lab results will link physical evidence to " + subject + " or an accomplice.";
            case "Documents":
                return "Financial records or correspondence will prove " + subject + "'s involvement.";
            case "Physical Search":
                return "A hidden item at " + subject + "'s known location contains critical evidence.";
            case "Background Check":
                return subject + " has a prior history that is directly relevant to this investigation.";
            default:
                return "This lead will uncover an important fact about " + subject + ".";
        }
    }

    private void generateStoryTree() {
        storyRoot.removeAllChildren();
        storyRoot.setUserObject("Story Root");

        int complexity = (int) complexitySpinner.getValue();
        String caseType = (String) caseTypeCombo.getSelectedItem();
        if (caseType == null) caseType = "Investigation";

        String subject = subjectNameField.getText().trim();
        if (subject.isEmpty()) subject = "the subject";

        for (int phase = 1; phase <= complexity; phase++) {
            String phaseTitle = (phase == 1)
                    ? "Initial Investigation"
                    : "Plot Twist " + (phase - 1);
            DefaultMutableTreeNode phaseNode = new DefaultMutableTreeNode(
                    "[PLOT_TWIST] " + phaseTitle);

            for (int major = 1; major <= 2; major++) {
                String majorTitle = buildMajorTitle(phase, major, caseType);
                DefaultMutableTreeNode majorNode = new DefaultMutableTreeNode(
                        "[MAJOR] " + majorTitle);

                for (int minor = 1; minor <= 2; minor++) {
                    String minorTitle = buildMinorTitle(major, minor);
                    DefaultMutableTreeNode minorNode = new DefaultMutableTreeNode(
                            "[MINOR] " + minorTitle);

                    for (int action = 1; action <= 2; action++) {
                        String actionTitle = buildActionTitle(minor, action, subject);
                        minorNode.add(new DefaultMutableTreeNode(
                                "[ACTION] " + actionTitle));
                    }
                    majorNode.add(minorNode);
                }
                phaseNode.add(majorNode);
            }
            storyRoot.add(phaseNode);
        }

        treeModel.reload();
        expandAll(storyTree);
        statusLabel.setText("Generated story tree with " + complexity + " phase(s).");
    }

    private String buildMajorTitle(int phase, int major, String caseType) {
        if (phase == 1) {
            return (major == 1) ? "Gather Primary Evidence" : "Identify Key Persons";
        } else {
            return (major == 1) ? "Investigate New Lead" : "Confirm Revised Theory";
        }
    }

    private String buildMinorTitle(int major, int minor) {
        if (major == 1) {
            return (minor == 1) ? "Scene Investigation" : "Witness Contact";
        } else {
            return (minor == 1) ? "Background Research" : "Surveillance Operation";
        }
    }

    private String buildActionTitle(int minor, int action, String subject) {
        if (minor == 1) {
            return (action == 1)
                    ? "Photograph the scene"
                    : "Collect physical evidence";
        } else {
            return (action == 1)
                    ? "Interview a contact of " + subject
                    : "Review documents or records";
        }
    }

    // -------------------------------------------------------------------------
    // Summary
    // -------------------------------------------------------------------------

    private void refreshSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== CASE SUMMARY ===\n\n");

        sb.append("Case Type:    ").append(nullSafe(caseTypeCombo.getSelectedItem())).append('\n');
        sb.append("Complexity:   ").append(complexitySpinner.getValue()).append('\n');
        sb.append("Client:       ").append(nullSafe(clientNameField.getText())).append('\n');
        sb.append("Subject:      ").append(nullSafe(subjectNameField.getText())).append('\n');
        sb.append("Victim:       ").append(nullSafe(victimNameField.getText())).append('\n');
        sb.append('\n');

        sb.append("--- Description ---\n");
        sb.append(nullSafe(descriptionArea.getText())).append("\n\n");

        sb.append("--- Objective ---\n");
        sb.append(nullSafe(objectiveArea.getText())).append("\n\n");

        sb.append("--- NPC Characters (").append(npcModel.getRowCount()).append(") ---\n");
        for (int i = 0; i < npcModel.getRowCount(); i++) {
            sb.append("  [").append(npcModel.getValueAt(i, 0)).append("] ")
              .append(npcModel.getValueAt(i, 1))
              .append(" (").append(npcModel.getValueAt(i, 2))
              .append(", age ").append(npcModel.getValueAt(i, 3))
              .append(", ").append(npcModel.getValueAt(i, 4)).append(")")
              .append("  coop=").append(npcModel.getValueAt(i, 5))
              .append(" hon=").append(npcModel.getValueAt(i, 6))
              .append(" nerv=").append(npcModel.getValueAt(i, 7));
            Object deadVal = npcModel.getValueAt(i, 8);
            if (Boolean.TRUE.equals(deadVal)) {
                sb.append("  [DEAD]");
                Object deathDt = npcModel.getValueAt(i, 9);
                Object variance = npcModel.getValueAt(i, 10);
                int var = (variance instanceof Number) ? ((Number) variance).intValue() : 0;
                if (var == -1) {
                    sb.append(" death=UNKNOWN (body missing)");
                } else if (deathDt instanceof Number && ((Number) deathDt).longValue() != 0L) {
                    long millis = ((Number) deathDt).longValue();
                    java.util.Calendar cal = java.util.Calendar.getInstance(
                            java.util.TimeZone.getTimeZone("UTC"));
                    cal.setTimeInMillis(millis);
                    String formatted = String.format("%04d-%02d-%02d %02d:%02d",
                            cal.get(java.util.Calendar.YEAR),
                            cal.get(java.util.Calendar.MONTH) + 1,
                            cal.get(java.util.Calendar.DAY_OF_MONTH),
                            cal.get(java.util.Calendar.HOUR_OF_DAY),
                            cal.get(java.util.Calendar.MINUTE));
                    sb.append(" death=").append(formatted);
                    if (var > 0) sb.append(" ±").append(var).append("min");
                }
            }
            sb.append('\n');
        }
        sb.append('\n');

        sb.append("--- Relationships (").append(relationshipModel.getRowCount()).append(") ---\n");
        for (int i = 0; i < relationshipModel.getRowCount(); i++) {
            sb.append("  ").append(relationshipModel.getValueAt(i, 0))
              .append(" → ").append(relationshipModel.getValueAt(i, 1))
              .append("  [").append(relationshipModel.getValueAt(i, 2)).append("]")
              .append("  opinion=").append(relationshipModel.getValueAt(i, 3))
              .append('\n');
        }
        sb.append('\n');

        sb.append("--- Leads (").append(leadsModel.getRowCount()).append(") ---\n");
        for (int i = 0; i < leadsModel.getRowCount(); i++) {
            sb.append("  ").append(leadsModel.getValueAt(i, 0))
              .append(" [").append(leadsModel.getValueAt(i, 2)).append("] ")
              .append(leadsModel.getValueAt(i, 1)).append('\n');
        }
        sb.append('\n');

        sb.append("--- Story Tree ---\n");
        appendTreeText(sb, storyRoot, 0);

        summaryArea.setText(sb.toString());
        summaryArea.setCaretPosition(0);
        statusLabel.setText("Summary refreshed.");
    }

    private void appendTreeText(StringBuilder sb, DefaultMutableTreeNode node, int depth) {
        if (depth > 0) {
            for (int i = 0; i < depth; i++) sb.append("  ");
            sb.append(node.getUserObject()).append('\n');
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            appendTreeText(sb, (DefaultMutableTreeNode) node.getChildAt(i), depth + 1);
        }
    }

    // -------------------------------------------------------------------------
    // Utility
    // -------------------------------------------------------------------------

    private static String nullSafe(Object o) {
        return o != null ? o.toString() : "";
    }

    private static void expandAll(JTree tree) {
        for (int i = 0; i < tree.getRowCount(); i++) {
            tree.expandRow(i);
        }
    }
}

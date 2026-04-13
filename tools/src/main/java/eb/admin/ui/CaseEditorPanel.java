package eb.admin.ui;

import eb.admin.model.CategoryData;
import eb.admin.model.CategoryEntry;
import eb.framework1.investigation.ActionType;
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
 *   <li><b>Case Type</b> — choose a type, motive, and complexity from the code tables</li>
 *   <li><b>NPC Characters</b> — enter client/subject names, generate
 *       case-specific NPCs with roles and relationships</li>
 *   <li><b>Description &amp; Objective</b> — view generated narrative text</li>
 *   <li><b>Story Tree</b> — build and inspect the five-level story tree (generates facts and leads)</li>
 *   <li><b>Leads</b> — review or manually add hidden leads</li>
 *   <li><b>Facts</b> — review or manually add case facts</li>
 * </ol>
 */
public class CaseEditorPanel extends JPanel {

    private final JLabel statusLabel;
    private final Random random = new Random();

    // Step 1 – Case Type
    private final JComboBox<String> caseTypeCombo = new JComboBox<>();
    private final JLabel caseTypeDesc = new JLabel(" ");
    private final JComboBox<String> motiveCombo = new JComboBox<>();
    private final JLabel motiveDesc = new JLabel(" ");
    private final JSpinner complexitySpinner =
            new JSpinner(new SpinnerNumberModel(1, 1, 3, 1));

    // Step 2 – NPC Characters (includes Client & Subject names)
    private final JTextField clientNameField  = new JTextField(20);
    private final JTextField subjectNameField = new JTextField(20);
    private final JTextField victimNameField  = new JTextField(20);
    private final JLabel     victimNameLabel  = new JLabel("  Victim Name:");
    private final JComboBox<String> clientGenderCombo  = new JComboBox<>(new String[]{"M", "F"});
    private final JComboBox<String> subjectGenderCombo = new JComboBox<>(new String[]{"M", "F"});
    // NPC table columns:
    // 0=Role, 1=Name, 2=Gender, 3=Age, 4=Occupation,
    // 5=Cooperativeness, 6=Honesty, 7=Nervousness,
    // 8=Dead, 9=Death Date/Time, 10=Variance (min),
    // 11=Hair Color, 12=Beard Style, 13=Opportunity, 14=Access, 15=Has Motive,
    // 16=Phone Number, 17=Phone Discovered, 18=Default Location
    private final DefaultTableModel npcModel =
            new DefaultTableModel(new String[]{
                    "Role", "Name", "Gender", "Age", "Occupation",
                    "Cooperativeness", "Honesty", "Nervousness",
                    "Dead", "Death Date/Time", "Variance (min)",
                    "Hair Color", "Beard Style",
                    "Opportunity", "Access", "Has Motive",
                    "Phone Number", "Phone Discovered", "Default Location"}, 0) {
                @Override
                public Class<?> getColumnClass(int col) {
                    if (col == 8) return Boolean.class;   // Dead checkbox
                    if (col == 15) return Boolean.class;   // Has Motive checkbox
                    if (col == 17) return Boolean.class;   // Phone Discovered checkbox
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

    // Step 7 – Interviews
    // Columns: 0=NPC Name, 1=Role, 2=Topic, 3=Question, 4=Answer, 5=Truthful,
    //          6=About NPC, 7=Req Attribute, 8=Req Value, 9=Alt Answer
    private final DefaultTableModel interviewModel =
            new DefaultTableModel(new String[]{
                    "NPC Name", "Role", "Topic", "Question", "Answer",
                    "Truthful", "About NPC",
                    "Req Attribute", "Req Value", "Alt Answer"}, 0) {
                @Override public Class<?> getColumnClass(int col) {
                    if (col == 5) return Boolean.class;  // Truthful checkbox
                    if (col == 8) return Integer.class;   // Req Value
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
        refreshMotiveCombo();
        refreshDiscoveryMethodColumn();
    }

    /** Clears all fields and resets the panel to its initial state. */
    public void clearAll() {
        caseTypeCombo.removeAllItems();
        caseTypeDesc.setText(" ");
        motiveCombo.removeAllItems();
        motiveDesc.setText(" ");
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
        interviewModel.setRowCount(0);
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
        steps.addTab("4. Story Tree",             buildStoryTreeStep());
        steps.addTab("5. Leads",                  buildLeadsStep());
        steps.addTab("6. Facts",                  buildFactsStep());
        steps.addTab("7. Interviews",             buildInterviewsStep());
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
        form.add(new JLabel("Motive:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1;
        form.add(motiveCombo, gbc);

        motiveCombo.addActionListener(e -> updateMotiveDescription());

        gbc.gridx = 0; gbc.gridy = 3; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        form.add(new JLabel("Motive Detail:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1;
        motiveDesc.setFont(motiveDesc.getFont().deriveFont(Font.ITALIC));
        form.add(motiveDesc, gbc);

        gbc.gridx = 0; gbc.gridy = 4; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        form.add(new JLabel("Complexity (1–3):"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.NONE;
        form.add(complexitySpinner, gbc);

        JButton randomBtn = new JButton("Random Type");
        randomBtn.addActionListener(e -> pickRandomCaseType());
        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.CENTER;
        form.add(randomBtn, gbc);

        panel.add(form, BorderLayout.NORTH);

        JLabel info = new JLabel(
                "<html><i>Select a case type, motive, and complexity, then move to the next tab.</i></html>");
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
        npcTable.getColumnModel().getColumn(11).setPreferredWidth(80);  // Hair Color
        npcTable.getColumnModel().getColumn(12).setPreferredWidth(90);  // Beard Style
        npcTable.getColumnModel().getColumn(13).setPreferredWidth(140); // Opportunity
        npcTable.getColumnModel().getColumn(14).setPreferredWidth(160); // Access
        npcTable.getColumnModel().getColumn(15).setPreferredWidth(70);  // Has Motive
        npcTable.getColumnModel().getColumn(16).setPreferredWidth(100); // Phone Number
        npcTable.getColumnModel().getColumn(17).setPreferredWidth(100); // Phone Discovered
        npcTable.getColumnModel().getColumn(18).setPreferredWidth(120); // Default Location

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
            npcModel.addRow(new Object[]{"", "", "M", 30, "", 5, 5, 5, false, 0L, 0, "", "", "", "", false, "", false, ""});
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

        // Status column — restricted combo: KNOWN, HIDDEN, or UNKNOWN
        JComboBox<String> statusCombo = new JComboBox<>(new String[]{"KNOWN", "HIDDEN", "UNKNOWN"});
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

    // -- Step 7: Interviews ---------------------------------------------------

    private JPanel buildInterviewsStep() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JTable interviewTable = new JTable(interviewModel);
        interviewTable.setRowHeight(26);
        interviewTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        interviewTable.getTableHeader().setReorderingAllowed(false);
        interviewTable.getColumnModel().getColumn(0).setPreferredWidth(120); // NPC Name
        interviewTable.getColumnModel().getColumn(1).setPreferredWidth(120); // Role
        interviewTable.getColumnModel().getColumn(2).setPreferredWidth(100); // Topic
        interviewTable.getColumnModel().getColumn(3).setPreferredWidth(250); // Question
        interviewTable.getColumnModel().getColumn(4).setPreferredWidth(300); // Answer
        interviewTable.getColumnModel().getColumn(5).setPreferredWidth(60);  // Truthful
        interviewTable.getColumnModel().getColumn(6).setPreferredWidth(100); // About NPC
        interviewTable.getColumnModel().getColumn(7).setPreferredWidth(100); // Req Attribute
        interviewTable.getColumnModel().getColumn(8).setPreferredWidth(60);  // Req Value
        interviewTable.getColumnModel().getColumn(9).setPreferredWidth(300); // Alt Answer

        // Req Attribute column — combo editor with character attributes
        JComboBox<String> attrCombo = new JComboBox<>(new String[]{
                "", "Perception", "Empathy", "Intuition", "Charisma",
                "Intimidation", "Intelligence", "Memory", "Stealth"});
        interviewTable.getColumnModel().getColumn(7)
                .setCellEditor(new DefaultCellEditor(attrCombo));

        // Topic column — combo editor
        JComboBox<String> topicCombo = new JComboBox<>();
        for (eb.framework1.investigation.InterviewTopic t : eb.framework1.investigation.InterviewTopic.values()) {
            topicCombo.addItem(t.getDisplayName());
        }
        interviewTable.getColumnModel().getColumn(2)
                .setCellEditor(new DefaultCellEditor(topicCombo));

        JScrollPane scroll = new JScrollPane(interviewTable);

        // Detail area for viewing full answer text
        JTextArea interviewDetailArea = new JTextArea(4, 60);
        interviewDetailArea.setLineWrap(true);
        interviewDetailArea.setWrapStyleWord(true);
        interviewDetailArea.setEditable(false);
        interviewDetailArea.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        interviewDetailArea.setText("Select a row to see the full question and answer.");
        JScrollPane detailScroll = new JScrollPane(interviewDetailArea);
        detailScroll.setBorder(BorderFactory.createTitledBorder("Interview Detail"));

        interviewTable.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            int row = interviewTable.getSelectedRow();
            if (row < 0) {
                interviewDetailArea.setText("");
                return;
            }
            String npc      = String.valueOf(interviewModel.getValueAt(row, 0));
            String role     = String.valueOf(interviewModel.getValueAt(row, 1));
            String topic    = String.valueOf(interviewModel.getValueAt(row, 2));
            String question = String.valueOf(interviewModel.getValueAt(row, 3));
            String answer   = String.valueOf(interviewModel.getValueAt(row, 4));
            Object truthObj = interviewModel.getValueAt(row, 5);
            boolean truthful = Boolean.TRUE.equals(truthObj);
            String aboutNpc = String.valueOf(interviewModel.getValueAt(row, 6));
            String reqAttr  = String.valueOf(interviewModel.getValueAt(row, 7));
            Object reqValObj = interviewModel.getValueAt(row, 8);
            int reqVal = reqValObj instanceof Integer ? (Integer) reqValObj : 0;
            String altAnswer = String.valueOf(interviewModel.getValueAt(row, 9));

            StringBuilder sb = new StringBuilder();
            sb.append("NPC: ").append(npc).append(" (").append(role).append(")\n");
            sb.append("Topic: ").append(topic);
            if (!aboutNpc.isEmpty()) sb.append("  —  About: ").append(aboutNpc);
            sb.append("\n\n");
            sb.append("Q: ").append(question).append("\n\n");
            sb.append("A: ").append(answer).append("\n\n");
            sb.append(truthful ? "✔ This answer is TRUTHFUL." : "✘ This answer is DECEPTIVE.");

            if (reqAttr != null && !reqAttr.isEmpty() && !"null".equals(reqAttr) && reqVal > 0) {
                sb.append("\n\n── Attribute Requirement ──");
                sb.append("\nRequires: ").append(reqAttr).append(" ≥ ").append(reqVal);
                sb.append("\n\nIf NOT met, player sees:");
                sb.append("\n").append(altAnswer != null && !"null".equals(altAnswer) ? altAnswer : "(no alternate)");
            }

            interviewDetailArea.setText(sb.toString());
            interviewDetailArea.setCaretPosition(0);
        });

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                scroll, detailScroll);
        split.setDividerLocation(300);
        split.setResizeWeight(0.7);

        JButton genBtn    = new JButton("Generate Interviews");
        JButton addBtn    = new JButton("Add Response");
        JButton deleteBtn = new JButton("Delete Response");

        genBtn.addActionListener(e -> generateInterviews());
        addBtn.addActionListener(e -> {
            interviewModel.addRow(new Object[]{
                    "", "", "Alibi", "", "", Boolean.TRUE, "",
                    "", Integer.valueOf(0), ""});
        });
        deleteBtn.addActionListener(e -> {
            int row = interviewTable.getSelectedRow();
            if (row >= 0) interviewModel.removeRow(row);
        });

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        buttons.add(genBtn);
        buttons.add(addBtn);
        buttons.add(deleteBtn);

        panel.add(split,   BorderLayout.CENTER);
        panel.add(buttons, BorderLayout.SOUTH);
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
     * ACTION, RESULT, MINOR, MAJOR, PLOT_TWIST, LEAD, LEAD:RED_HERRING, INTERVIEW.
     */
    private String buildNodeDescription(String label) {
        if (label == null || label.isEmpty()) return "";

        // Strip the leading [TYPE] or [TYPE:name] tag
        String tag   = "";
        String title = label;
        if (label.startsWith("[") && label.contains("]")) {
            int close = label.indexOf(']');
            tag   = label.substring(1, close).trim();
            // title is text after "] " if present, otherwise empty
            title = (close + 1 < label.length()) ? label.substring(close + 1).trim() : "";
        }

        String subject = subjectNameField.getText().trim();
        if (subject.isEmpty()) subject = "the subject";
        String victim  = victimNameField.getText().trim();
        if (victim.isEmpty()) victim = "the victim";
        String client  = clientNameField.getText().trim();
        if (client.isEmpty()) client = "the client";

        // Handle [ACTION:action name] format
        if (tag.startsWith("ACTION:")) {
            String actionName = tag.substring("ACTION:".length()).trim();
            return buildActionDescription(actionName, subject, victim, client);
        }

        switch (tag) {
            case "ACTION":
                return buildActionDescription(title, subject, victim, client);
            case "RESULT":
                return buildResultNodeDescription(title);
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
            case "KNOWN_FACTS":
                return "Known Facts & Leads\n\n"
                        + "These are the facts and leads available to the investigator at the "
                        + "start of the case. Known facts come from the initial briefing and "
                        + "public information. Leads point to avenues of inquiry — but beware, "
                        + "some may be red herrings that waste time without advancing the case.";
            case "UNKNOWN_FACTS":
                return "Unknown Facts (To Be Solved)\n\n"
                        + "These are the critical unknowns that the investigator must uncover "
                        + "through the storyline. They represent what actually happened — the "
                        + "method, motive, and means of the crime. Solving these facts is the "
                        + "core objective of the investigation.";
            case "FACT":
                return "Known fact: " + title + "\n\n"
                        + "This fact is available from the start of the investigation. "
                        + "It may provide context or constrain the investigator's theories.";
            case "FACT:UNKNOWN":
                return "Unknown fact (to be solved): " + title + "\n\n"
                        + "This fact represents a critical unknown in the case. The investigator "
                        + "does not know this at the start — it must be uncovered by following "
                        + "leads, gathering evidence, and advancing the storyline. Discovering "
                        + "this fact moves the investigation closer to resolution.";
            case "LEAD":
                return "Lead: " + title + "\n\n"
                        + "This lead is available from the start. Following it may uncover "
                        + "new hidden facts or advance the investigation.";
            case "LEAD:RED_HERRING":
                return "Lead (RED HERRING): " + title + "\n\n"
                        + "⚠ This lead is a red herring! Following it will consume time and "
                        + "resources without producing useful results. The investigator won't "
                        + "know this in advance — it appears identical to a genuine lead.";
            case "INTERVIEW":
                return "Interview: " + title + "\n\n"
                        + "This NPC can be questioned on this topic. Conducting the interview "
                        + "may reveal new facts or corroborate existing leads. Some responses "
                        + "are attribute-gated and require a sufficiently skilled investigator "
                        + "to obtain the full answer.";
            default:
                return title;
        }
    }

    /** Generates a description for a RESULT node, resolving its req/ok/fail references. */
    private String buildResultNodeDescription(String rawTitle) {
        // Parse pipe-separated segments beyond the description
        // Format: "description | req:ATTR:N | action:title | ok:fact:id | fail:fact:id"
        int firstPipe = rawTitle.indexOf(" | ");
        String description = (firstPipe >= 0) ? rawTitle.substring(0, firstPipe).trim() : rawTitle.trim();

        String reqAttr     = null;
        int    reqThresh   = 0;
        String actionTitle = null;
        String okFactRef   = null;
        String okLeadRef   = null;
        String failFactRef = null;
        String failLeadRef = null;

        if (firstPipe >= 0) {
            String[] segments = rawTitle.substring(firstPipe + 3).split(" \\| ");
            for (String seg : segments) {
                seg = seg.trim();
                if (seg.startsWith("req:")) {
                    String[] parts = seg.substring(4).split(":");
                    if (parts.length == 2) {
                        reqAttr = parts[0];
                        try { reqThresh = Integer.parseInt(parts[1]); } catch (NumberFormatException ignored) {}
                    }
                } else if (seg.startsWith("action:")) {
                    actionTitle = seg.substring(7).trim();
                } else if (seg.startsWith("ok:fact:")) {
                    okFactRef = seg.substring(8).trim();
                } else if (seg.startsWith("ok:lead:")) {
                    okLeadRef = seg.substring(8).trim();
                } else if (seg.startsWith("fail:fact:")) {
                    failFactRef = seg.substring(10).trim();
                } else if (seg.startsWith("fail:lead:")) {
                    failLeadRef = seg.substring(10).trim();
                }
                // Legacy single-pipe format fallbacks
                else if (seg.startsWith("fact:")) {
                    okFactRef = seg.substring(5).trim();
                } else if (seg.startsWith("lead:")) {
                    okLeadRef = seg.substring(5).trim();
                }
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Result: ").append(description).append(".\n\n");

        // Requirement and how it was applied
        if (reqAttr != null) {
            sb.append("Requirement: [").append(reqAttr).append(": ").append(reqThresh).append("]\n")
              .append("The investigator must have ").append(reqAttr).append(" ≥ ").append(reqThresh)
              .append(" to achieve the primary outcome.\n\n");
        }

        // Success (ok) branch
        sb.append("✔ SUCCESS OUTCOME");
        if (reqAttr != null) sb.append("  (").append(reqAttr).append(" ≥ ").append(reqThresh).append(")");
        sb.append(":\n");
        if (reqAttr != null) {
            sb.append("  ").append(buildAttributeSuccessNarrative(reqAttr, actionTitle)).append("\n");
        }
        if (okFactRef != null) {
            sb.append(resolveFactRef(okFactRef, "Reveals hidden fact", "becomes KNOWN."));
        } else if (okLeadRef != null) {
            sb.append(resolveLeadRef(okLeadRef, "Creates new lead"));
        } else {
            sb.append("  (Generate the story tree after generating facts to see the specific fact revealed.)\n");
        }

        // Failure (fail) branch
        sb.append("\n✘ FAILURE OUTCOME");
        if (reqAttr != null) sb.append("  (").append(reqAttr).append(" < ").append(reqThresh).append(")");
        sb.append(":\n");
        if (reqAttr != null) {
            sb.append("  ").append(buildAttributeFailureNarrative(reqAttr, actionTitle)).append("\n");
        }
        if (failFactRef != null) {
            sb.append(resolveFactRef(failFactRef, "Reveals a minor hidden fact", "becomes KNOWN (lower importance)."));
        } else if (failLeadRef != null) {
            sb.append(resolveLeadRef(failLeadRef, "Creates a new lead instead"));
        } else {
            sb.append("  (No else outcome assigned — generate the story tree after generating facts/leads.)\n");
        }

        return sb.toString();
    }

    /**
     * Returns a narrative sentence describing how the investigator successfully applied
     * the given attribute to accomplish the action.
     */
    private String buildAttributeSuccessNarrative(String attr, String actionTitle) {
        ActionType actionType = ActionType.classify(actionTitle);
        boolean isInterview = actionType == ActionType.INTERVIEW;
        boolean isEvidence  = actionType == ActionType.EVIDENCE;
        boolean isDocument  = actionType == ActionType.DOCUMENT;
        boolean isPhoto     = actionType == ActionType.PHOTOGRAPH;

        switch (attr) {
            case "INTIMIDATION":
                if (isInterview)
                    return "With a stern look you make it clear that cooperation is not optional. "
                         + "Under your unblinking gaze they reluctantly answer.";
                if (isEvidence)
                    return "Your commanding presence clears the area. You examine the evidence undisturbed.";
                if (isDocument)
                    return "Your authoritative demand leaves the clerk no room to argue — "
                         + "the files appear on the counter without delay.";
                return "Your forceful demeanour gets results — they comply without further argument.";

            case "CHARISMA":
                if (isInterview)
                    return "With a warm smile and well-chosen words you earn their trust. "
                         + "They open up more than they intended.";
                if (isDocument)
                    return "A friendly rapport with the clerk gets you access to records that aren't publicly available.";
                if (isEvidence)
                    return "You charm the officer guarding the perimeter into letting you through. "
                         + "Inside you find exactly the sample you needed.";
                if (isPhoto)
                    return "A quick conversation with a bystander gets you access to a better vantage point "
                         + "for the perfect angle.";
                return "Your easy manner wins cooperation and they hand over what you need.";

            case "PERCEPTION":
                if (isEvidence)
                    return "Your sharp eye catches something others missed — a detail that changes everything.";
                if (isInterview)
                    return "You pick up on a micro-expression that reveals more than their words let on.";
                if (isDocument)
                    return "Scanning the pages, a single mismatched date jumps out at you — "
                         + "the discrepancy is unmistakable.";
                if (isPhoto)
                    return "You notice an odd reflection in the window and angle your camera to capture it. "
                         + "The resulting image reveals a critical detail.";
                return "A careful scan of the surroundings reveals a detail that wasn't in any report.";

            case "INTELLIGENCE":
                if (isDocument)
                    return "You rapidly cross-reference dates and figures, spotting the anomaly buried in the paperwork.";
                if (isInterview)
                    return "You ask the exact right question — one they didn't expect — and watch their composure crack.";
                if (isEvidence)
                    return "You reconstruct the sequence of events from the physical evidence alone, "
                         + "identifying the one item that doesn't belong.";
                return "You piece together the scattered facts and the pattern becomes clear.";

            case "EMPATHY":
                if (isInterview)
                    return "Reading the tension in their body language, you find the right moment to gently press. "
                         + "They share something they haven't told anyone else.";
                if (isEvidence)
                    return "Sensing something personal about the way items are arranged, you look deeper — "
                         + "there's a hidden keepsake that tells a story the scene report missed.";
                if (isDocument)
                    return "Between the dry lines of the report you sense the fear of the person who wrote it. "
                         + "You re-read the passage and find the detail they tried to bury.";
                return "Your sensitivity to the emotional undercurrents in the room guides you to the truth.";

            case "MEMORY":
                if (isDocument)
                    return "You recall a detail from an earlier briefing that maps perfectly onto this record — "
                         + "the connection is unmistakable.";
                if (isInterview)
                    return "Something they say triggers a memory. You name the detail and watch their expression change.";
                if (isEvidence)
                    return "You remember a serial number from the case file. Checking the item, "
                         + "the match confirms it was moved from the original location.";
                if (isPhoto)
                    return "Comparing this angle to a photo you saw earlier, "
                         + "you spot the object that has been moved since the initial sweep.";
                return "You recall something from earlier in the case that makes this evidence click into place.";

            case "STEALTH":
                if (isEvidence)
                    return "Moving quietly and unobserved, you collect what you need before anyone notices you were there.";
                if (isInterview)
                    return "You observe them from a distance before approaching, giving you an advantage when you do speak.";
                if (isPhoto)
                    return "From a concealed position you photograph the scene without disturbing it — "
                         + "the raw, untouched state tells its own story.";
                if (isDocument)
                    return "You slip into the records room unnoticed and locate the file "
                         + "before anyone can conveniently misplace it.";
                return "Your unobtrusive presence lets you gather information without anyone realising you were watching.";

            default:
                return "Applying your skill at the critical moment, you achieve the outcome you were after.";
        }
    }

    /**
     * Returns a narrative sentence describing what happens when the investigator lacks
     * the required attribute and the action fails.
     */
    private String buildAttributeFailureNarrative(String attr, String actionTitle) {
        ActionType actionType = ActionType.classify(actionTitle);
        boolean isInterview = actionType == ActionType.INTERVIEW;
        boolean isEvidence  = actionType == ActionType.EVIDENCE;
        boolean isDocument  = actionType == ActionType.DOCUMENT;
        boolean isPhoto     = actionType == ActionType.PHOTOGRAPH;

        switch (attr) {
            case "INTIMIDATION":
                if (isInterview)
                    return "You ask them to stand aside, but they don't seem willing. "
                         + "You do notice it's the same person you saw talking to the subject earlier — "
                         + "that connection may be worth pursuing.";
                if (isEvidence)
                    return "Someone challenges your right to be here. Without sufficient authority "
                         + "you're forced to back down, but you catch a glimpse of something before you leave.";
                if (isDocument)
                    return "The records clerk refuses to hand over the restricted file. "
                         + "You notice they glance nervously at a particular drawer — that itself is a clue.";
                return "Your attempt to assert authority falls short. They hold their ground — "
                     + "but their reaction itself tells you something.";

            case "CHARISMA":
                if (isInterview)
                    return "Your approach falls flat — they seem unimpressed and shut the conversation down. "
                         + "However, on your way out you overhear something that might be worth a follow-up.";
                if (isDocument)
                    return "The clerk turns you away without the access you need. "
                         + "You'll have to find another way in, or come back with better credentials.";
                if (isEvidence)
                    return "The officer on guard isn't swayed by your request. "
                         + "You're turned away from the perimeter, but from outside you spot a secondary entrance "
                         + "that may be worth investigating later.";
                if (isPhoto)
                    return "A bystander blocks your best angle and won't budge. "
                         + "The shots you manage are mediocre, but one catches an odd detail in the background.";
                return "They aren't won over by your approach and give you nothing useful — "
                     + "but their reluctance itself may be telling.";

            case "PERCEPTION":
                if (isEvidence)
                    return "You scan the area carefully but nothing immediately stands out. "
                         + "Whatever was here may have already been disturbed or removed.";
                if (isInterview)
                    return "You miss the subtle cue in their expression. "
                         + "They answer smoothly — too smoothly — but you can't pin down what's off.";
                if (isDocument)
                    return "The numbers blur together and the anomaly hides in plain sight. "
                         + "You'll need to come back with fresh eyes or a different approach.";
                if (isPhoto)
                    return "You photograph the scene but miss the critical angle. "
                         + "Later review of your shots shows nothing the initial report didn't already cover.";
                return "The detail you were looking for doesn't reveal itself this time. "
                     + "It may still be there — consider returning with fresh eyes.";

            case "INTELLIGENCE":
                if (isDocument)
                    return "The volume and complexity of the paperwork overwhelms you for now. "
                         + "You'll need more context before the pattern emerges.";
                if (isInterview)
                    return "You ask the wrong question and they steer the conversation away. "
                         + "You leave with less than you came with.";
                if (isEvidence)
                    return "The evidence doesn't connect to anything you already know. "
                         + "The link is there, but without more context you can't see it yet.";
                return "The pieces don't connect yet. There's still a gap somewhere in your reasoning.";

            case "EMPATHY":
                if (isInterview)
                    return "You try to connect but they remain guarded throughout. "
                         + "Their defensiveness itself might be telling — why would they be so closed off?";
                if (isEvidence)
                    return "You walk the scene looking for anything personal or out of place, "
                         + "but the sterile environment yields nothing to your instinct this time.";
                if (isDocument)
                    return "You read through the statements but nothing strikes an emotional chord. "
                         + "The writer was careful — or perhaps genuinely uninvolved.";
                return "You misread the emotional temperature in the room. "
                     + "Your approach creates distance instead of trust.";

            case "MEMORY":
                if (isDocument)
                    return "The relevant detail is just out of reach. You know you've seen something like this before, "
                         + "but you can't recall where. Time to review your notes.";
                if (isInterview)
                    return "They mention something that should ring a bell, but the connection escapes you in the moment. "
                         + "Write it down — it may make sense later.";
                if (isEvidence)
                    return "You feel like you've seen this type of item before, but the case reference escapes you. "
                         + "A trip back to the case file might jog your memory.";
                if (isPhoto)
                    return "You can't recall what the scene looked like in the earlier photos, "
                         + "so you miss what changed. Re-check the originals when you get back.";
                return "The critical detail doesn't surface when you need it. "
                     + "Go back through your earlier findings before proceeding.";

            case "STEALTH":
                if (isEvidence)
                    return "Your presence is noticed earlier than expected. "
                         + "You're forced to abandon the search, but you managed to pocket one small piece of evidence.";
                if (isInterview)
                    return "They spot you watching before you're ready. "
                         + "The element of surprise is lost, and they're now on guard.";
                if (isPhoto)
                    return "Someone sees your camera and calls you out. "
                         + "You're asked to leave, but not before you notice which area they were most anxious to protect.";
                if (isDocument)
                    return "The records clerk spots you in the restricted section. "
                         + "You're escorted out, but you glimpsed a folder label that may narrow your next search.";
                return "You're seen when you should have remained undetected. "
                     + "The opportunity is lost for now — but they don't know exactly what you know.";

            default:
                return "Without the required skill you fall short this time. "
                     + "There may still be another way to achieve the same result.";
        }
    }

    private String resolveFactRef(String factId, String prefix, String suffix) {
        for (int r = 0; r < factsModel.getRowCount(); r++) {
            Object id = factsModel.getValueAt(r, 0);
            if (factId.equals(String.valueOf(id))) {
                String cat  = String.valueOf(factsModel.getValueAt(r, 1));
                String text = String.valueOf(factsModel.getValueAt(r, 2));
                Object imp  = factsModel.getValueAt(r, 10);
                int importance = (imp instanceof Number) ? ((Number) imp).intValue() : 0;
                return "  " + prefix + " [" + factId + "] (" + cat + ", importance=" + importance + "): "
                        + suffix + "\n  \"" + text + "\"\n";
            }
        }
        return "  " + prefix + " [" + factId + "] — generate facts first to see details.\n";
    }

    private String resolveLeadRef(String leadId, String prefix) {
        for (int r = 0; r < leadsModel.getRowCount(); r++) {
            Object id = leadsModel.getValueAt(r, 0);
            if (leadId.equals(String.valueOf(id))) {
                String hint   = String.valueOf(leadsModel.getValueAt(r, 1));
                String method = String.valueOf(leadsModel.getValueAt(r, 2));
                return "  " + prefix + " [" + leadId + "] via " + method + ":\n  \"" + hint + "\"\n";
            }
        }
        return "  " + prefix + " [" + leadId + "] — generate leads first to see details.\n";
    }

    /** Generates a detailed action description for each known action title. */
    private String buildActionDescription(String title, String subject, String victim, String client) {
        if ("Photograph the scene".equals(title)) {
            return "Action: Photograph the scene.\n\n"
                    + "The investigator documents the location with photographs. "
                    + "Key items to capture include entry and exit points, signs of disturbance, "
                    + "any objects out of place, and the general layout of the area. "
                    + "Photos will be compared against the official report and may "
                    + "contradict the coroner's findings.";

        } else if ("Collect physical evidence".equals(title)) {
            return "Action: Collect physical evidence.\n\n"
                    + "Physical items at the scene are catalogued and preserved. "
                    + "This includes anything that could place " + subject
                    + " at the location, establish a timeline, or link a third party "
                    + "to the incident. Proper chain of custody must be maintained.";

        } else if (title.startsWith("Interview a contact of")) {
            return "Action: Interview a contact of " + subject + ".\n\n"
                    + "The investigator approaches someone in " + subject
                    + "'s social or professional circle. "
                    + "The goal is to establish " + subject
                    + "'s movements, relationships, and possible motive. "
                    + "The contact's cooperativeness and honesty will affect what is revealed.";

        } else if ("Review documents or records".equals(title)) {
            return "Action: Review documents or records.\n\n"
                    + "Relevant paperwork — financial statements, phone records, "
                    + "employment files, or official reports — is obtained and analysed. "
                    + "Discrepancies between the documented record and witness accounts "
                    + "may surface hidden connections between " + subject
                    + " and " + victim + ".";

        } else {
            // Generic fallback for manually added or custom actions
            return "Action: " + title + ".\n\n"
                    + "The investigator carries out this action as part of the current "
                    + "minor objective. Document findings carefully — any detail could "
                    + "become relevant as the case develops.";
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

    private void refreshMotiveCombo() {
        motiveCombo.removeAllItems();
        if (categoryData == null) return;

        // Determine the selected case type code for filtering
        String selectedCaseTypeCode = getSelectedCaseTypeCode();

        for (CategoryEntry e : categoryData.getMotive_categories()) {
            // If the motive has case_types defined, only include it if it matches
            if (selectedCaseTypeCode != null && e.getCase_types() != null
                    && !e.getCase_types().isEmpty()
                    && !e.getCase_types().contains(selectedCaseTypeCode)) {
                continue;
            }
            String name = e.getName() != null ? e.getName() : e.getCode();
            motiveCombo.addItem(name);
        }
        updateMotiveDescription();
    }

    /** Returns the code of the currently selected case type, or null if none. */
    private String getSelectedCaseTypeCode() {
        if (categoryData == null || caseTypeCombo.getSelectedIndex() < 0) return null;
        int idx = caseTypeCombo.getSelectedIndex();
        List<CategoryEntry> types = categoryData.getCase_types();
        if (idx >= 0 && idx < types.size()) {
            return types.get(idx).getCode();
        }
        return null;
    }

    private void updateMotiveDescription() {
        if (categoryData == null || motiveCombo.getSelectedIndex() < 0) {
            motiveDesc.setText(" ");
            return;
        }
        CategoryEntry motive = getSelectedMotiveEntry();
        if (motive != null) {
            String desc = motive.getDescription();
            motiveDesc.setText(desc != null ? desc : " ");
        } else {
            motiveDesc.setText(" ");
        }
    }

    /** Finds the CategoryEntry matching the currently selected motive combo item. */
    private CategoryEntry getSelectedMotiveEntry() {
        if (categoryData == null || motiveCombo.getSelectedIndex() < 0) return null;
        String selected = (String) motiveCombo.getSelectedItem();
        if (selected == null) return null;
        for (CategoryEntry e : categoryData.getMotive_categories()) {
            String name = e.getName() != null ? e.getName() : e.getCode();
            if (selected.equals(name)) {
                return e;
            }
        }
        return null;
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
        refreshMotiveCombo();
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
            if (motiveCombo.getItemCount() > 0) {
                motiveCombo.setSelectedIndex(random.nextInt(motiveCombo.getItemCount()));
            }
            complexitySpinner.setValue(1 + random.nextInt(3));
            statusLabel.setText("Randomly selected: " + caseTypeCombo.getSelectedItem()
                    + " / " + motiveCombo.getSelectedItem());
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
    /**
     * Returns NPC roles for the given case type and complexity.
     * Higher complexity adds additional suspects — NPCs who all had
     * opportunity but can be thinned out using their attributes.
     *
     * @param caseType   the selected case type
     * @param complexity 1-3; controls additional suspect count (0, 1-2, 2-3 extras)
     */
    private String[] rolesForCaseType(String caseType, int complexity) {
        List<String> roles = new ArrayList<>();
        switch (caseType) {
            case "Missing Person":
                roles.addAll(java.util.Arrays.asList(
                        "Client", "Subject (Missing)", "Witness",
                        "Last-Known Contact", "Neighbour"));
                break;
            case "Infidelity":
                roles.addAll(java.util.Arrays.asList(
                        "Client", "Subject (Partner)", "Other Party",
                        "Mutual Friend", "Witness"));
                break;
            case "Theft":
                roles.addAll(java.util.Arrays.asList(
                        "Client (Victim)", "Subject (Suspect)",
                        "Witness", "Insurance Adjuster", "Fence/Dealer"));
                break;
            case "Fraud":
                roles.addAll(java.util.Arrays.asList(
                        "Client (Victim)", "Subject (Perpetrator)",
                        "Accountant", "Business Associate", "Insider Witness"));
                break;
            case "Blackmail":
                roles.addAll(java.util.Arrays.asList(
                        "Client (Victim)", "Subject (Blackmailer)",
                        "Witness", "Intermediary", "Confidant"));
                break;
            case "Murder":
                roles.addAll(java.util.Arrays.asList(
                        "Client", "Subject (Suspect)", "Victim",
                        "Key Witness", "Victim's Associate", "Police Contact"));
                break;
            case "Stalking":
                roles.addAll(java.util.Arrays.asList(
                        "Client (Victim)", "Subject (Stalker)",
                        "Neighbour Witness", "Ex-Partner", "Friend of Client"));
                break;
            case "Corporate Espionage":
                roles.addAll(java.util.Arrays.asList(
                        "Client (Employer)", "Subject (Leak)",
                        "Rival Contact", "Trusted Colleague", "IT Specialist"));
                break;
            default:
                roles.addAll(java.util.Arrays.asList("Client", "Subject", "Witness"));
                break;
        }

        // Additional suspects based on complexity:
        //   complexity 1 → 0 extra suspects
        //   complexity 2 → 1-2 extra suspects
        //   complexity 3 → 2-3 extra suspects
        int extraSuspects = 0;
        if (complexity == 2) {
            extraSuspects = 1 + random.nextInt(2);  // 1-2
        } else if (complexity >= 3) {
            extraSuspects = 2 + random.nextInt(2);  // 2-3
        }

        String[] suspectLabels = {
                "Suspect — Neighbour", "Suspect — Colleague",
                "Suspect — Associate", "Suspect — Ex-Partner",
                "Suspect — Acquaintance"
        };
        for (int s = 0; s < extraSuspects && s < suspectLabels.length; s++) {
            roles.add(suspectLabels[s]);
        }

        return roles.toArray(new String[0]);
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

    /** Picks a random value from the array that differs from {@code exclude}. */
    private String pickDifferent(String[] options, String exclude) {
        if (options.length <= 1) return options[0];
        String result;
        do {
            result = options[random.nextInt(options.length)];
        } while (result.equals(exclude));
        return result;
    }

    private String randomName(String gender) {
        String first = "F".equals(gender)
                ? FEMALE_NAMES[random.nextInt(FEMALE_NAMES.length)]
                : MALE_NAMES[random.nextInt(MALE_NAMES.length)];
        String last = SURNAMES[random.nextInt(SURNAMES.length)];
        return first + " " + last;
    }

    private static final String[] HAIR_COLORS =
            {"black", "brown", "blonde", "red", "gray", "white"};
    private static final String[] BEARD_STYLES_M =
            {"clean-shaven", "stubble", "short beard", "long beard", "goatee", "moustache"};
    private static final String[] OPPORTUNITY_LABELS =
            {"was near the scene", "had keys to the building",
             "was seen in the area", "lives close by",
             "visited the location earlier that day"};
    private static final String[] ACCESS_LABELS =
            {"owns a firearm", "had access to the victim's home",
             "has a key to the office", "had access to the safe",
             "drives a vehicle matching witness description",
             "works in the same building"};

    /** Location labels used to assign a default location to each NPC. */
    private static final String[] LOCATION_LABELS = {
        "Café", "Bar", "Office", "Public Park", "Library",
        "Restaurant", "Their Home", "Gym", "Warehouse District",
        "Church", "Hospital", "Diner", "Hotel Lobby",
        "Bus Station", "Street Market", "Courthouse"
    };

    /**
     * Generates a realistic-looking phone number in the format "555-XXXX".
     * The 555 prefix signals it is a fictional number.
     */
    private String generatePhoneNumber() {
        int suffix = 100 + random.nextInt(9900); // 100–9999, formatted as 0100–9999
        return "555-" + String.format("%04d", suffix);
    }

    /**
     * Picks a default location for an NPC based on their role.
     * Some roles have plausible fixed locations; others are random.
     */
    private String locationForRole(String role) {
        if (role.contains("Police"))         return "Police Station";
        if (role.contains("Accountant"))     return "Office";
        if (role.contains("Insurance"))      return "Office";
        if (role.contains("IT"))             return "Office";
        if (role.contains("Colleague"))      return "Office";
        if (role.contains("Neighbour"))      return "Their Home";
        if (role.contains("Friend"))         return pick("Café", "Bar", "Restaurant", "Public Park");
        if (role.contains("Associate"))      return pick("Office", "Restaurant", "Hotel Lobby");
        if (role.contains("Witness"))        return pick("Café", "Diner", "Public Park", "Their Home");
        if (role.contains("Fence") || role.contains("Dealer"))
            return pick("Bar", "Warehouse District", "Street Market");
        if (role.contains("Intermediary"))   return pick("Café", "Hotel Lobby", "Bus Station");
        if (role.contains("Confidant"))      return pick("Church", "Café", "Library");
        if (role.contains("Rival"))          return pick("Bar", "Restaurant", "Office");
        if (role.contains("Other Party"))    return pick("Gym", "Bar", "Restaurant");
        if (role.contains("Contact"))        return pick("Café", "Diner", "Bus Station");
        if (role.startsWith("Client"))       return pick("Café", "Office", "Restaurant", "Their Home");
        if (role.startsWith("Subject"))      return pick("Bar", "Their Home", "Gym");
        return LOCATION_LABELS[random.nextInt(LOCATION_LABELS.length)];
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

        int complexity = (int) complexitySpinner.getValue();
        String[] roles = rolesForCaseType(caseType, complexity);
        String client  = clientNameField.getText().trim();
        String subject = subjectNameField.getText().trim();

        String victim = victimNameField.getText().trim();

        // The primary subject's distinguishing attributes (all suspects share
        // opportunity but only the real perpetrator matches every attribute).
        String perpetratorHair  = HAIR_COLORS[random.nextInt(HAIR_COLORS.length)];
        String perpetratorBeard = "clean-shaven";
        String perpetratorOpp   = OPPORTUNITY_LABELS[random.nextInt(OPPORTUNITY_LABELS.length)];
        String perpetratorAcc   = ACCESS_LABELS[random.nextInt(ACCESS_LABELS.length)];

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
            } else if (role.startsWith("Subject") || role.startsWith("Suspect")) {
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

            // --- Suspect-distinguishing attributes ---
            // All suspects (Subject + additional Suspect roles) had opportunity;
            // only the primary Subject matches ALL criteria so the player can
            // thin out extra suspects using hair, beard, access, and motive.
            boolean isSuspectRole = role.startsWith("Subject") || role.startsWith("Suspect");
            String hairColor  = "";
            String beardStyle = "";
            String opportunity = "";
            String access      = "";
            boolean hasMotive  = false;

            if (isSuspectRole) {
                if (role.startsWith("Subject")) {
                    // The true perpetrator — matches everything
                    hairColor   = perpetratorHair;
                    beardStyle  = "M".equals(gender)
                            ? BEARD_STYLES_M[random.nextInt(BEARD_STYLES_M.length)]
                            : "none";
                    perpetratorBeard = beardStyle;
                    opportunity = perpetratorOpp;
                    access      = perpetratorAcc;
                    hasMotive   = true;
                } else {
                    // Additional suspect — shares some attributes but NOT all.
                    // Always has opportunity (plausible), but at least one
                    // other attribute differs so the player can eliminate them.
                    opportunity = OPPORTUNITY_LABELS[random.nextInt(OPPORTUNITY_LABELS.length)];

                    // Randomly decide which attributes match vs differ
                    // (ensure at least one differs)
                    boolean matchHair   = random.nextBoolean();
                    boolean matchBeard  = random.nextBoolean();
                    boolean matchAccess = random.nextBoolean();
                    boolean matchMotive = random.nextBoolean();
                    // Guarantee at least one mismatch
                    if (matchHair && matchBeard && matchAccess && matchMotive) {
                        // Force one to differ
                        switch (random.nextInt(4)) {
                            case 0: matchHair = false; break;
                            case 1: matchBeard = false; break;
                            case 2: matchAccess = false; break;
                            default: matchMotive = false; break;
                        }
                    }

                    hairColor = matchHair
                            ? perpetratorHair
                            : pickDifferent(HAIR_COLORS, perpetratorHair);
                    if ("M".equals(gender)) {
                        beardStyle = matchBeard
                                ? perpetratorBeard
                                : pickDifferent(BEARD_STYLES_M, perpetratorBeard);
                    } else {
                        beardStyle = "none";
                    }
                    access = matchAccess
                            ? perpetratorAcc
                            : ACCESS_LABELS[random.nextInt(ACCESS_LABELS.length)];
                    hasMotive = matchMotive;
                }
            }

            npcModel.addRow(new Object[]{
                    role, name, gender, age, occupation,
                    cooperativeness, honesty, nervousness,
                    dead, deathDateTime, deathVariance,
                    hairColor, beardStyle, opportunity, access, hasMotive,
                    generatePhoneNumber(),
                    role.startsWith("Client"),  // Client phone is always discovered
                    locationForRole(role)
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

    /**
     * Generates interview scripts for all living NPCs in the NPC table.
     *
     * <p>Each NPC gets a set of pre-generated question/answer pairs covering
     * alibis, whereabouts, opinions of other characters, relationships,
     * last contact, observations, and possible motives.  Responses vary
     * based on the NPC's role, and subjects (suspects) may give deceptive
     * answers marked as non-truthful.
     */
    private void generateInterviews() {
        generateInterviews(false);
    }

    private void generateInterviews(boolean silent) {
        if (npcModel.getRowCount() < 2) {
            if (!silent) {
                JOptionPane.showMessageDialog(this, "Generate NPCs first.",
                        "Missing Data", JOptionPane.WARNING_MESSAGE);
            }
            return;
        }

        interviewModel.setRowCount(0);

        String caseType = (String) caseTypeCombo.getSelectedItem();
        if (caseType == null) caseType = "Investigation";
        boolean isMurder = "Murder".equals(caseType);

        String client  = clientNameField.getText().trim();
        String subject = subjectNameField.getText().trim();
        String victim  = victimNameField.getText().trim();
        if (client.isEmpty())  client  = "the client";
        if (subject.isEmpty()) subject = "the subject";
        if (victim.isEmpty())  victim  = "the victim";

        String targetPerson = isMurder ? victim : subject;

        // Collect all NPC names and roles
        List<String> npcNames = new ArrayList<>();
        List<String> npcRoles = new ArrayList<>();
        List<Boolean> npcDead = new ArrayList<>();
        for (int i = 0; i < npcModel.getRowCount(); i++) {
            npcRoles.add(String.valueOf(npcModel.getValueAt(i, 0)));
            npcNames.add(String.valueOf(npcModel.getValueAt(i, 1)));
            npcDead.add(Boolean.TRUE.equals(npcModel.getValueAt(i, 8)));
        }

        for (int i = 0; i < npcNames.size(); i++) {
            if (npcDead.get(i)) continue;  // Dead NPCs cannot be interviewed

            String npcName = npcNames.get(i);
            String npcRole = npcRoles.get(i);
            boolean isSubject = npcRole.startsWith("Subject");
            boolean isSuspect = npcRole.startsWith("Suspect");
            boolean isClient  = npcRole.startsWith("Client");

            // --- ALIBI ---
            String alibi = buildInterviewAlibi(npcRole, isSubject || isSuspect);
            boolean alibiTruthful = (isSubject || isSuspect) ? random.nextInt(10) < 3 : true;
            addInterviewRow(npcName, npcRole, "Alibi",
                    "Where were you at the time of the incident?",
                    alibi, alibiTruthful, "");

            // --- OPINION of other NPCs (Empathy-gated for opinions about the subject) ---
            for (int j = 0; j < npcNames.size(); j++) {
                if (j == i || npcDead.get(j)) continue;  // Skip self and dead NPCs
                String otherName = npcNames.get(j);
                String otherRole = npcRoles.get(j);
                String opinion = buildInterviewOpinion(npcRole, otherName, otherRole,
                        subject, victim, isMurder);
                boolean opTruthful = (isSubject || isSuspect) ? random.nextBoolean() : true;
                // Opinions about the subject get Empathy gate
                if (otherName.equals(subject) && !isSubject && !isSuspect) {
                    String altOpinion = buildInterviewOpinionGeneric(otherName);
                    addInterviewRow(npcName, npcRole, "Opinion",
                            "What do you think of " + otherName + "?",
                            opinion, opTruthful, otherName,
                            "Empathy", 5, altOpinion);
                } else {
                    addInterviewRow(npcName, npcRole, "Opinion",
                            "What do you think of " + otherName + "?",
                            opinion, opTruthful, otherName);
                }
            }

            // --- WHEREABOUTS of subject (if not the subject or a suspect) ---
            if (!isSubject && !isSuspect) {
                String whereabouts = buildInterviewWhereabouts(npcRole, subject, isMurder);
                addInterviewRow(npcName, npcRole, "Whereabouts",
                        "Do you know where " + subject + " was at the time?",
                        whereabouts, true, subject);
            }

            // --- LAST CONTACT with target ---
            String lastContact = buildInterviewLastContact(npcRole, targetPerson);
            boolean contactTruthful = (isSubject || isSuspect) ? random.nextInt(10) < 4 : true;
            addInterviewRow(npcName, npcRole, "Last Contact",
                    "When did you last see " + targetPerson + "?",
                    lastContact, contactTruthful, targetPerson);

            // --- OBSERVATION (attribute-gated) ---
            String observation = buildInterviewObservation(npcRole, subject,
                    targetPerson, isSubject || isSuspect);
            boolean obsTruthful = (isSubject || isSuspect) ? random.nextBoolean() : true;
            if (isSubject || isSuspect) {
                // Subject/Suspect: Intimidation reveals more under pressure
                String revealObs = buildInterviewObservationRevealed(subject, targetPerson);
                addInterviewRow(npcName, npcRole, "Observation",
                        "Did you notice anything unusual around the time of the incident?",
                        revealObs, obsTruthful, "",
                        "Intimidation", 6, observation);
            } else {
                // Others: Perception reveals specific details
                String genericObs = buildInterviewObservationGeneric();
                addInterviewRow(npcName, npcRole, "Observation",
                        "Did you notice anything unusual around the time of the incident?",
                        observation, obsTruthful, "",
                        "Perception", 5, genericObs);
            }

            // --- MOTIVE (murder cases only, attribute-gated) ---
            if (isMurder) {
                String motive = buildInterviewMotive(npcRole, subject, victim,
                        isSubject || isSuspect);
                boolean motTruthful = (isSubject || isSuspect) ? random.nextBoolean() : true;
                if (isSubject || isSuspect) {
                    // Subject/Suspect: Intimidation reveals more under pressure
                    String motiveRevealed = buildInterviewMotiveRevealed(subject, victim);
                    addInterviewRow(npcName, npcRole, "Motive",
                            "Can you think of anyone who would want to harm " + victim + "?",
                            motiveRevealed, motTruthful, victim,
                            "Intimidation", 7, motive);
                } else {
                    // Others: Intuition reveals motive insight
                    String motiveGeneric = buildInterviewMotiveGeneric();
                    addInterviewRow(npcName, npcRole, "Motive",
                            "Can you think of anyone who would want to harm " + victim + "?",
                            motive, motTruthful, victim,
                            "Intuition", 5, motiveGeneric);
                }
            }

            // --- RELATIONSHIP (with the target person) ---
            if (!npcName.equals(targetPerson)) {
                String relationship = buildInterviewRelationship(npcRole, npcName,
                        targetPerson, isMurder);
                addInterviewRow(npcName, npcRole, "Relationship",
                        "How do you know " + targetPerson + "?",
                        relationship, true, targetPerson);
            }

            // --- CONTACT_INFO (phone number discovery, Charisma-gated) ---
            // Each NPC can reveal contact info for other NPCs they know.
            // This is the primary mechanic for discovering phone numbers.
            for (int j = 0; j < npcNames.size(); j++) {
                if (j == i || npcDead.get(j)) continue;
                String otherName = npcNames.get(j);
                String otherRole = npcRoles.get(j);

                // Only generate contact info responses for NPCs this character
                // plausibly knows (skip subjects talking about each other's contacts)
                boolean knows = !isSubject || otherRole.startsWith("Client")
                        || otherRole.contains("Associate");
                if (!knows && isSuspect) continue;

                // Look up the other NPC's phone number and location from the table
                String otherPhone    = String.valueOf(npcModel.getValueAt(j, 16));
                String otherLocation = String.valueOf(npcModel.getValueAt(j, 18));

                String contactAnswer = buildInterviewContactInfo(
                        npcRole, otherName, otherRole, otherPhone, otherLocation);
                String contactGeneric = buildInterviewContactInfoGeneric(otherName);

                // Charisma gate: player needs Charisma ≥ 4 to get the phone number
                addInterviewRow(npcName, npcRole, "Contact Info",
                        "Do you have a way to reach " + otherName + "?",
                        contactAnswer, true, otherName,
                        "Charisma", 4, contactGeneric);
            }
        }

        statusLabel.setText("Generated " + interviewModel.getRowCount()
                + " interview responses for " + countInterviewedNpcs() + " NPCs.");
    }

    /** Adds a single row to the interview table (no attribute gate). */
    private void addInterviewRow(String npcName, String npcRole, String topic,
                                  String question, String answer,
                                  boolean truthful, String aboutNpc) {
        addInterviewRow(npcName, npcRole, topic, question, answer,
                truthful, aboutNpc, "", 0, "");
    }

    /** Adds a single row to the interview table with optional attribute gate. */
    private void addInterviewRow(String npcName, String npcRole, String topic,
                                  String question, String answer,
                                  boolean truthful, String aboutNpc,
                                  String reqAttribute, int reqValue,
                                  String altAnswer) {
        interviewModel.addRow(new Object[]{
                npcName, npcRole, topic, question, answer,
                Boolean.valueOf(truthful), aboutNpc,
                reqAttribute, Integer.valueOf(reqValue), altAnswer});
    }

    /** Counts the number of distinct NPC names in the interview table. */
    private int countInterviewedNpcs() {
        java.util.Set<String> names = new java.util.HashSet<>();
        for (int i = 0; i < interviewModel.getRowCount(); i++) {
            names.add(String.valueOf(interviewModel.getValueAt(i, 0)));
        }
        return names.size();
    }

    // ---- Interview answer generators ----------------------------------------

    private String buildInterviewAlibi(String role, boolean isSubject) {
        if (isSubject) {
            String[] pool = {
                "I was with a friend all night. We were playing cards until past midnight.",
                "I was working late at the office. Check the security cameras if you want.",
                "I was at a restaurant across town. I'm sure they'll remember me.",
                "I was at home alone that evening. I fell asleep early.",
                "I was at a bar downtown. The bartender knows me."
            };
            return pool[random.nextInt(pool.length)];
        } else if (role.startsWith("Client")) {
            String[] pool = {
                "I was at home all evening. My phone records will confirm it.",
                "I was having dinner with friends. They'll vouch for me.",
                "I was at work late — the security desk will have me on the log.",
                "I was visiting family out of town. I have the tickets."
            };
            return pool[random.nextInt(pool.length)];
        } else {
            String[] pool = {
                "I was in the neighbourhood, on my way home from work.",
                "I was at the local shop. It was close to closing time.",
                "I was walking the dog. That's how I ended up seeing what I saw.",
                "I was at home that evening. Went to bed early.",
                "I was out with colleagues after work until about 10 PM."
            };
            return pool[random.nextInt(pool.length)];
        }
    }

    private String buildInterviewOpinion(String npcRole, String otherName, String otherRole,
                                          String subject, String victim, boolean isMurder) {
        // Subject role opinions about others
        if (npcRole.startsWith("Subject")) {
            if (otherRole.startsWith("Client")) {
                String[] pool = {
                    otherName + " has always had it in for me. Don't believe everything you hear.",
                    "I barely know " + otherName + ". We've spoken maybe twice.",
                    otherName + " is paranoid. Sees conspiracies everywhere."
                };
                return pool[random.nextInt(pool.length)];
            }
            String[] pool = {
                "I don't really know " + otherName + " well enough to comment.",
                otherName + "? We get along fine. No issues between us.",
                "I've nothing to say about " + otherName + "."
            };
            return pool[random.nextInt(pool.length)];
        }

        // Opinions about the subject — focus on character traits
        if (otherName.equals(subject)) {
            String[] pool = {
                otherName + " has always been the jealous type. Envious of anyone who had more.",
                "I've heard " + otherName + " has a short temper. People are wary of confrontation.",
                otherName + " seemed charming on the surface, but there was something calculating underneath.",
                otherName + " was possessive. Couldn't stand others having what they wanted.",
                otherName + " was competitive to the point of obsession. Always comparing.",
                otherName + " was resentful — especially about money. Always felt shortchanged.",
                "I wouldn't call " + otherName + " violent, but there was a bitterness. A deep grudge.",
                otherName + " was manipulative. Tells people what they want to hear.",
                "I've seen " + otherName + " fly into a rage over small things."
            };
            return pool[random.nextInt(pool.length)];
        }

        // Opinions about the victim
        if (isMurder && otherName.equals(victim)) {
            String[] pool = {
                otherName + " was well-liked by most people. I can't imagine who would do this.",
                otherName + " had a way of rubbing some people the wrong way, but nothing serious.",
                "Everyone knew " + otherName + ". A decent person. This has shaken everyone.",
                otherName + " was private. Kept to themselves. I don't know much about their personal life."
            };
            return pool[random.nextInt(pool.length)];
        }

        // Generic opinions about other NPCs
        String[] pool = {
            otherName + " seems reliable enough. We've had no problems.",
            "I don't know " + otherName + " very well, to be honest.",
            otherName + " is a decent person as far as I can tell.",
            "I've heard mixed things about " + otherName + ", but nothing specific.",
            otherName + " keeps to themselves mostly. Hard to read."
        };
        return pool[random.nextInt(pool.length)];
    }

    private String buildInterviewWhereabouts(String npcRole, String subject, boolean isMurder) {
        if (npcRole.contains("Witness")) {
            String[] pool = {
                "I saw " + subject + " near the area that evening. " + subject + " looked agitated.",
                "I'm pretty sure " + subject + " was in the area. I recognised the car.",
                "I didn't see " + subject + " personally, but a neighbour mentioned spotting someone.",
                subject + " was definitely around. I saw someone matching the description leaving in a hurry."
            };
            return pool[random.nextInt(pool.length)];
        }
        String[] pool = {
            "I believe " + subject + " was supposed to be somewhere else that night.",
            subject + " told me they'd be at home. Whether that's true, I don't know.",
            "I haven't spoken to " + subject + " about that night.",
            "Someone mentioned seeing " + subject + " in the area, but I can't say for certain."
        };
        return pool[random.nextInt(pool.length)];
    }

    private String buildInterviewLastContact(String role, String targetPerson) {
        if (role.startsWith("Client")) {
            String[] pool = {
                "I last saw " + targetPerson + " about two days before everything happened.",
                "We spoke on the phone three days ago. Seemed normal.",
                "I saw " + targetPerson + " the morning before the incident.",
                "It's been over a week since I last spoke to " + targetPerson + "."
            };
            return pool[random.nextInt(pool.length)];
        } else if (role.startsWith("Subject")) {
            String[] pool = {
                "I haven't seen " + targetPerson + " in weeks.",
                "I can't remember the last time I saw " + targetPerson + ".",
                "I saw " + targetPerson + " that same day, earlier in the afternoon.",
                "We spoke on the phone that morning. It was brief."
            };
            return pool[random.nextInt(pool.length)];
        } else {
            String[] pool = {
                "I said hello to " + targetPerson + " the day before. Seemed fine.",
                "I saw " + targetPerson + " a few days ago at the shops.",
                "We're not close, but I saw " + targetPerson + " that week.",
                "Maybe three or four days before — I can't remember exactly."
            };
            return pool[random.nextInt(pool.length)];
        }
    }

    private String buildInterviewObservation(String role, String subject,
                                              String targetPerson, boolean isSubject) {
        if (isSubject) {
            String[] pool = {
                "I didn't notice anything. I've been keeping to myself lately.",
                "Nothing unusual. Everything seemed normal to me.",
                "I try to mind my own business.",
                "Look, I don't watch people. I had my own things going on."
            };
            return pool[random.nextInt(pool.length)];
        }
        String[] pool = {
            "Now that you mention it, I did notice " + subject + " acting strangely.",
            "I saw an unfamiliar car parked near " + targetPerson + "'s place more than once.",
            "There were raised voices coming from " + targetPerson + "'s place the night before.",
            "I noticed " + subject + " was unusually nervous the last time we spoke.",
            "I heard arguments in the days before. Things had been tense.",
            "I saw someone running from the area around 11 PM. I couldn't make out who.",
            "The lights were on unusually late at " + targetPerson + "'s. That's not normal."
        };
        return pool[random.nextInt(pool.length)];
    }

    private String buildInterviewMotive(String role, String subject, String victim,
                                         boolean isSubject) {
        if (isSubject) {
            String[] pool = {
                "Why are you asking me? You should be looking elsewhere.",
                "I don't know who would do this. But I know it wasn't me.",
                "Plenty of people had issues with " + victim + ". I'm not the only one.",
                "Have you checked " + victim + "'s financial records? There were debts."
            };
            return pool[random.nextInt(pool.length)];
        }
        String[] pool = {
            "I always thought " + subject + " was jealous of " + victim + ". It was obvious.",
            "There was bad blood between " + subject + " and " + victim + ". Everyone could see it.",
            subject + " once said something about " + victim + " getting what was coming.",
            subject + " and " + victim + " had a bitter dispute about money.",
            "I know " + subject + " was jealous of " + victim + "'s position. Felt passed over.",
            victim + " knew something about " + subject + " that " + subject + " wanted kept quiet.",
            subject + " was possessive about " + victim + ". When things soured, it turned ugly.",
            "There was a rivalry between them. " + subject + " couldn't stand " + victim + " doing better."
        };
        return pool[random.nextInt(pool.length)];
    }

    private String buildInterviewRelationship(String role, String npcName,
                                               String targetPerson, boolean isMurder) {
        if (role.startsWith("Client")) {
            String[] pool = {
                "We've known each other for years. That's why this hurts so much.",
                targetPerson + " and I are — were — close.",
                "We're family. Or as close as family gets.",
                "I've known " + targetPerson + " most of my life."
            };
            return pool[random.nextInt(pool.length)];
        } else if (role.startsWith("Subject")) {
            String[] pool = {
                "We were acquaintances. Nothing more.",
                "I knew " + targetPerson + " through work. Strictly professional.",
                "We moved in the same circles. That's all.",
                "I barely know " + targetPerson + ". We crossed paths occasionally."
            };
            return pool[random.nextInt(pool.length)];
        } else if (role.contains("Associate")) {
            String[] pool = {
                targetPerson + " and I were close. We'd known each other for years.",
                "I worked with " + targetPerson + " for a long time. Colleagues and friends.",
                targetPerson + " was like family to me.",
                "We weren't best friends, but I respected " + targetPerson + "."
            };
            return pool[random.nextInt(pool.length)];
        } else {
            String[] pool = {
                "I know " + targetPerson + " from the neighbourhood.",
                "We're acquaintances. I see " + targetPerson + " around.",
                "We've spoken a few times. Nothing deep.",
                "I know " + targetPerson + " by sight. We've exchanged pleasantries."
            };
            return pool[random.nextInt(pool.length)];
        }
    }

    // ---- Alternate/generic/revealed answer generators for attribute gates ----

    /** Generic opinion when Empathy requirement is not met. */
    private String buildInterviewOpinionGeneric(String otherName) {
        String[] pool = {
            "I don't really know " + otherName + " well enough to say.",
            otherName + "? I couldn't tell you much. We weren't close.",
            "I don't have strong feelings about " + otherName + " either way.",
            "I'd rather not comment on " + otherName + ". I barely know them."
        };
        return pool[random.nextInt(pool.length)];
    }

    /** Generic observation when Perception requirement is not met. */
    private String buildInterviewObservationGeneric() {
        String[] pool = {
            "I'm not sure. I don't think I noticed anything out of the ordinary.",
            "Nothing comes to mind. Sorry, I wish I could be more helpful.",
            "I wasn't really paying attention to anything in particular.",
            "Everything seemed normal to me. I can't think of anything specific."
        };
        return pool[random.nextInt(pool.length)];
    }

    /** Revealed observation when Intimidation gets through to the subject. */
    private String buildInterviewObservationRevealed(String subject, String targetPerson) {
        String[] pool = {
            "Fine. I did see something that night. There was someone else hanging around, but I don't know who.",
            "Alright, alright. I noticed things were off. " + targetPerson + " had been acting scared for days.",
            "Okay, look — there was a meeting. I overheard part of it. Voices were raised.",
            "I'll tell you this much — " + targetPerson + " was expecting trouble. They told me so."
        };
        return pool[random.nextInt(pool.length)];
    }

    /** Generic motive when Intuition requirement is not met. */
    private String buildInterviewMotiveGeneric() {
        String[] pool = {
            "I don't know why anyone would do this. It's terrible.",
            "I can't think of anyone specific. I'm sorry.",
            "I wish I knew. I've been asking myself the same question.",
            "I wouldn't want to accuse anyone. I really don't know."
        };
        return pool[random.nextInt(pool.length)];
    }

    /** Revealed motive when Intimidation breaks through subject's defences. */
    private String buildInterviewMotiveRevealed(String subject, String victim) {
        String[] pool = {
            "Fine. " + victim + " and I had problems. But there are others who had it worse with " + victim + ".",
            "You want the truth? " + victim + " made enemies. I was one of them, but I'm not the only one.",
            "Look, I'll admit it — " + victim + " wronged me. But I didn't do this. Check other people's stories.",
            "Alright. Yes, I was angry with " + victim + ". But killing? That's not me. Someone else was circling."
        };
        return pool[random.nextInt(pool.length)];
    }

    /**
     * Builds a contact-info answer that includes the other NPC's phone number
     * and/or usual location.  This is the "full" answer revealed when the
     * Charisma gate is met.
     */
    private String buildInterviewContactInfo(String npcRole, String otherName,
                                              String otherRole, String phone,
                                              String location) {
        String[] pool = {
            "Sure, I have " + otherName + "'s number. It's " + phone
                    + ". You can usually find them at the " + location + ".",
            "Yes — " + otherName + " can be reached at " + phone
                    + ". They spend most of their time at the " + location + ".",
            otherName + "? Their number is " + phone
                    + ". Last I heard they hang around the " + location + " most days.",
            "I've got " + otherName + "'s number right here: " + phone
                    + ". If you want to meet in person, try the " + location + ".",
            "Here — " + phone + ". That's " + otherName + "'s direct number."
                    + " They're usually at the " + location + " in the evenings."
        };
        return pool[random.nextInt(pool.length)];
    }

    /**
     * Generic contact-info answer when the Charisma requirement is not met.
     * The NPC refuses to share the phone number.
     */
    private String buildInterviewContactInfoGeneric(String otherName) {
        String[] pool = {
            "I'm not comfortable sharing " + otherName + "'s details with a stranger.",
            "I don't think " + otherName + " would want me giving out their number.",
            "You'd have to ask someone else. I don't give out people's information.",
            "I might have their number somewhere, but I'm not sharing it with you.",
            "Sorry, I don't feel right handing out " + otherName + "'s contact details."
        };
        return pool[random.nextInt(pool.length)];
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

        // Clear models — facts, leads, and interviews are generated inline by the story
        factsModel.setRowCount(0);
        leadsModel.setRowCount(0);
        interviewModel.setRowCount(0);

        int complexity = (int) complexitySpinner.getValue();
        String caseType = (String) caseTypeCombo.getSelectedItem();
        if (caseType == null) caseType = "Investigation";
        String motive = (String) motiveCombo.getSelectedItem();
        if (motive == null) motive = "Unknown";
        CategoryEntry motiveEntry = getSelectedMotiveEntry();
        String motiveCode = (motiveEntry != null && motiveEntry.getCode() != null)
                ? motiveEntry.getCode() : "UNKNOWN";

        storyRoot.setUserObject("Story Root [" + caseType + " / " + motive
                + " / complexity=" + complexity + "]");

        String subject = subjectNameField.getText().trim();
        if (subject.isEmpty()) subject = "the subject";
        String victim = victimNameField.getText().trim();
        if (victim.isEmpty()) victim = "the victim";
        String client = clientNameField.getText().trim();
        if (client.isEmpty()) client = "the client";

        // Running ID counters for facts and leads created inline
        int[] factIdCounter = {1};
        int[] leadIdCounter = {1};

        // Discovery methods for inline lead creation
        List<CategoryEntry> methods = (categoryData != null)
                ? categoryData.getDiscovery_methods() : new ArrayList<>();

        // ---------- Seed: one KNOWN fact so the investigator has a starting point ----------
        String seedFact = client + " has reported a suspicious incident involving " + victim + ".";
        addFact(factIdCounter, "DATE", seedFact, "KNOWN", System.currentTimeMillis(),
                client, victim, "", "", "", 0);

        // ---------- UNKNOWN facts: these are what the investigator must solve ----------
        generateUnknownFacts(factIdCounter, caseType, subject, victim, client, motiveCode);

        // ---------- Build phases — facts and leads are created as the story demands ----------
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
                        DefaultMutableTreeNode actionNode = new DefaultMutableTreeNode(
                                "[ACTION:" + actionTitle + "]");

                        // Pick an attribute that makes sense for this action type
                        String[] pool = attributesForAction(actionTitle);
                        String attr = pool[random.nextInt(pool.length)];
                        int threshold = 2 + random.nextInt(4); // 2..5

                        // --- ok result: create a HIGH-importance HIDDEN fact ---
                        String okFactText = buildInlineFact(actionTitle, subject, victim,
                                phase, major, true);
                        String okFactId = addFact(factIdCounter, categoryForAction(actionTitle),
                                okFactText, "HIDDEN", 0L, subject, victim, "", "", "",
                                3 + random.nextInt(3)); // importance 3–5
                        String okRef = "ok:fact:" + okFactId;

                        // --- fail result: create either a low-importance fact or a lead ---
                        String failRef;
                        boolean failUsesLead = !methods.isEmpty() && random.nextBoolean();
                        if (failUsesLead) {
                            String leadId = addInlineLead(leadIdCounter, methods, subject);
                            failRef = "fail:lead:" + leadId;
                        } else {
                            String failFactText = buildInlineFact(actionTitle, subject, victim,
                                    phase, major, false);
                            String failFactId = addFact(factIdCounter,
                                    categoryForAction(actionTitle),
                                    failFactText, "HIDDEN", 0L, "", "", "", "", "",
                                    random.nextInt(3)); // importance 0–2
                            failRef = "fail:fact:" + failFactId;
                        }

                        String desc = buildResultDescription(actionTitle);
                        StringBuilder label = new StringBuilder("[RESULT] ").append(desc)
                                .append(" | req:").append(attr).append(":").append(threshold);
                        label.append(" | action:").append(actionTitle);
                        label.append(" | ").append(okRef);
                        label.append(" | ").append(failRef);

                        actionNode.add(new DefaultMutableTreeNode(label.toString()));
                        minorNode.add(actionNode);
                    }
                    majorNode.add(minorNode);
                }
                phaseNode.add(majorNode);
            }
            storyRoot.add(phaseNode);
        }

        // ---------- Generate interviews now that NPCs and case details are set ----------
        if (npcModel.getRowCount() >= 2) {
            generateInterviews(true);
        }

        // ---------- Build the KNOWN_FACTS section from whatever was generated ----------
        DefaultMutableTreeNode knownSection = new DefaultMutableTreeNode(
                "[KNOWN_FACTS] Known Facts & Leads");
        for (int r = 0; r < factsModel.getRowCount(); r++) {
            if ("KNOWN".equals(String.valueOf(factsModel.getValueAt(r, 3)))) {
                String fId   = String.valueOf(factsModel.getValueAt(r, 0));
                String fCat  = String.valueOf(factsModel.getValueAt(r, 1));
                String fText = String.valueOf(factsModel.getValueAt(r, 2));
                knownSection.add(new DefaultMutableTreeNode(
                        "[FACT] " + fId + " (" + fCat + "): " + fText));
            }
        }
        for (int r = 0; r < leadsModel.getRowCount(); r++) {
            String lId     = String.valueOf(leadsModel.getValueAt(r, 0));
            String lHint   = String.valueOf(leadsModel.getValueAt(r, 1));
            String lMethod = String.valueOf(leadsModel.getValueAt(r, 2));
            boolean redHerring = random.nextInt(100) < 25;
            String tag = redHerring ? "[LEAD:RED_HERRING]" : "[LEAD]";
            knownSection.add(new DefaultMutableTreeNode(
                    tag + " " + lId + " via " + lMethod + ": " + lHint));
        }
        for (int r = 0; r < interviewModel.getRowCount(); r++) {
            String npcName = String.valueOf(interviewModel.getValueAt(r, 0));
            String npcRole = String.valueOf(interviewModel.getValueAt(r, 1));
            String topic   = String.valueOf(interviewModel.getValueAt(r, 2));
            String question = String.valueOf(interviewModel.getValueAt(r, 3));
            knownSection.add(new DefaultMutableTreeNode(
                    "[INTERVIEW] " + npcName + " (" + npcRole + ") / " + topic + ": " + question));
        }
        storyRoot.insert(knownSection, 0); // first child

        // ---------- Build the UNKNOWN_FACTS section — what needs to be solved ----------
        DefaultMutableTreeNode unknownSection = new DefaultMutableTreeNode(
                "[UNKNOWN_FACTS] Unknown Facts (To Be Solved)");
        for (int r = 0; r < factsModel.getRowCount(); r++) {
            if ("UNKNOWN".equals(String.valueOf(factsModel.getValueAt(r, 3)))) {
                String fId   = String.valueOf(factsModel.getValueAt(r, 0));
                String fCat  = String.valueOf(factsModel.getValueAt(r, 1));
                String fText = String.valueOf(factsModel.getValueAt(r, 2));
                Object imp   = factsModel.getValueAt(r, 10);
                int importance = (imp instanceof Number) ? ((Number) imp).intValue() : 0;
                unknownSection.add(new DefaultMutableTreeNode(
                        "[FACT:UNKNOWN] " + fId + " (" + fCat + ", importance=" + importance + "): " + fText));
            }
        }
        storyRoot.insert(unknownSection, 1); // second child, after KNOWN_FACTS

        treeModel.reload();
        expandAll(storyTree);
        int unknownCount = 0;
        for (int r = 0; r < factsModel.getRowCount(); r++) {
            if ("UNKNOWN".equals(String.valueOf(factsModel.getValueAt(r, 3)))) unknownCount++;
        }
        statusLabel.setText("Generated story tree with " + complexity + " phase(s), "
                + factsModel.getRowCount() + " facts (" + unknownCount + " unknown), "
                + leadsModel.getRowCount() + " leads, "
                + interviewModel.getRowCount() + " interview responses.");
    }

    // ---- Inline helpers for story-driven fact/lead creation ------------------

    /** Adds a fact row to factsModel and returns the assigned ID string. */
    private String addFact(int[] idCounter, String category, String text, String status,
                           long epoch, String char1, String char2, String relType,
                           String itemId, String evidId, int importance) {
        String id = "fact-" + idCounter[0]++;
        factsModel.addRow(new Object[]{id, category, text, status,
                epoch, char1, char2, relType, itemId, evidId, importance});
        return id;
    }

    /**
     * Generates UNKNOWN facts — the critical mysteries the investigator must solve.
     * Each fact has a concrete value (the truth) — its UNKNOWN status means
     * the investigator hasn't discovered it yet, but the case author knows the answer.
     */
    private void generateUnknownFacts(int[] factIdCounter, String caseType,
                                       String subject, String victim, String client,
                                       String motiveCode) {
        boolean isMurder = "Murder".equalsIgnoreCase(caseType);

        // Core unknown: how was the crime committed?
        if (isMurder) {
            String[] methodPool = {
                victim + " was poisoned over several days using a slow-acting compound.",
                victim + " was strangled in a secluded area with no witnesses.",
                victim + " was struck from behind with a heavy object, causing fatal head trauma.",
                victim + " was stabbed during what appeared to be a struggle.",
                victim + " was pushed from a height, staged to look like a fall."};
            addFact(factIdCounter, "METHOD", methodPool[random.nextInt(methodPool.length)],
                    "UNKNOWN", 0L, "", victim, "", "", "", 5);
        } else {
            String[] methodPool = {
                "The crime was executed through a carefully forged set of documents.",
                "A digital intrusion was used to gain unauthorised access to the target.",
                "The operation relied on an insider who provided access at the right moment.",
                subject + " exploited a position of trust to carry out the scheme undetected."};
            addFact(factIdCounter, "METHOD", methodPool[random.nextInt(methodPool.length)],
                    "UNKNOWN", 0L, "", "", "", "", "", 5);
        }

        // Motive — a concrete, case-specific narrative
        String motiveNarrative = buildMotiveNarrative(motiveCode, subject, victim);
        addFact(factIdCounter, "MOTIVE",
                motiveCode + ": " + motiveNarrative,
                "UNKNOWN", 0L, subject, "", "", "", "", 5);

        // Weapon / instrument
        if (isMurder) {
            String[] weaponPool = {
                "The murder weapon was a kitchen knife taken from " + victim + "'s own home.",
                "A heavy brass paperweight found hidden in " + subject + "'s vehicle was used.",
                "Traces of a rare toxin were found — sourced from an online purchase linked to " + subject + ".",
                "A length of cord matching material from " + subject + "'s workshop was the instrument.",
                "A firearm registered to a third party but last handled by " + subject + " was used."};
            addFact(factIdCounter, "EVIDENCE",
                    weaponPool[random.nextInt(weaponPool.length)],
                    "UNKNOWN", 0L, "", "", "", "", "", 4);
        }

        // Opportunity / timeline
        String[] timelinePool = {
            "The crime occurred between 10 PM and midnight while " + victim + " was alone.",
            subject + " used a 45-minute gap in the security footage to act unobserved.",
            "The incident took place during a power outage that " + subject + " may have arranged.",
            "Phone records place " + subject + " near the scene at 11:30 PM on the night in question."};
        addFact(factIdCounter, "DATE",
                timelinePool[random.nextInt(timelinePool.length)],
                "UNKNOWN", 0L, "", "", "", "", "", 4);

        // Location specifics
        String[] locationPool = {
            "The crime was committed in the back office of " + victim + "'s business premises.",
            "It took place at " + victim + "'s residence, in the upstairs study.",
            "The incident occurred in a rented storage unit on the outskirts of town.",
            "A secluded car park behind the old warehouse was where " + subject + " confronted " + victim + "."};
        addFact(factIdCounter, "ITEM",
                locationPool[random.nextInt(locationPool.length)],
                "UNKNOWN", 0L, "", "", "", "", "", 3);

        // Accomplices
        String[] accomplicePool = {
            subject + " acted alone, relying on meticulous planning to avoid detection.",
            subject + " had a single accomplice who provided a vehicle and a false alibi.",
            "An unidentified associate helped " + subject + " dispose of key evidence.",
            subject + " coerced a reluctant colleague into acting as a lookout."};
        addFact(factIdCounter, "RELATIONSHIP",
                accomplicePool[random.nextInt(accomplicePool.length)],
                "UNKNOWN", 0L, subject, "", "", "", "", 3);

        // Alibi verification
        String[] alibiPool = {
            subject + "'s alibi of being at a restaurant that evening is contradicted by CCTV footage.",
            subject + " claims to have been home alone, but phone GPS data tells a different story.",
            "The colleague " + subject + " named as an alibi witness has since recanted their statement.",
            subject + "'s car was recorded by toll cameras heading towards the scene at the critical time."};
        addFact(factIdCounter, "DATE",
                alibiPool[random.nextInt(alibiPool.length)],
                "UNKNOWN", 0L, subject, "", "", "", "", 4);

        // Cover-up
        String[] coverupPool = {
            subject + " wiped down surfaces and removed personal items from the scene.",
            "Security footage from a nearby camera was deliberately deleted that night.",
            subject + " disposed of clothing and shoes in a skip two streets away.",
            "A hastily cleaned area in " + subject + "'s home shows traces of bleach and scrubbing."};
        addFact(factIdCounter, "ITEM",
                coverupPool[random.nextInt(coverupPool.length)],
                "UNKNOWN", 0L, "", "", "", "", "", 3);
    }

    /**
     * Builds a case-specific motive narrative for the given motive code.
     * Each code has multiple templates using the subject/victim names, ensuring
     * that every case gets a unique, tailored motivation story.
     */
    private String buildMotiveNarrative(String motiveCode, String subject, String victim) {
        String[] pool;
        switch (motiveCode) {
            case "FINANCIAL_GAIN":
                pool = new String[]{
                    "Having fallen on hard times, " + subject + " decided this would be a quick solution to all financial problems.",
                    subject + " discovered a lucrative insurance policy on " + victim + " and devised a plan to collect.",
                    "Mounting debts and a failing business drove " + subject + " to target " + victim + "'s estate.",
                    subject + " had been secretly siphoning funds and needed " + victim + " out of the way before an audit."};
                break;
            case "REVENGE":
                pool = new String[]{
                    subject + " had nursed a grudge against " + victim + " for years after a devastating public humiliation.",
                    "After " + victim + " destroyed " + subject + "'s career, " + subject + " spent months planning retribution.",
                    subject + " blamed " + victim + " for the death of a loved one and vowed to settle the score.",
                    "A bitter feud over a broken promise drove " + subject + " to take drastic action against " + victim + "."};
                break;
            case "JEALOUSY":
                pool = new String[]{
                    subject + " could not accept that " + victim + " had been promoted over them despite fewer qualifications.",
                    "A romantic rivalry between " + subject + " and " + victim + " escalated beyond control.",
                    subject + " envied " + victim + "'s social standing and growing influence in the community.",
                    "Watching " + victim + " succeed where " + subject + " had failed became an unbearable obsession."};
                break;
            case "COERCION":
                pool = new String[]{
                    subject + " was being blackmailed with compromising photographs and saw no other way out.",
                    "A criminal associate threatened " + subject + "'s family unless " + victim + " was dealt with.",
                    subject + " was manipulated by a third party who stood to gain from " + victim + "'s downfall.",
                    "Under extreme pressure from mounting threats, " + subject + " reluctantly carried out someone else's plan."};
                break;
            case "POWER":
                pool = new String[]{
                    subject + " saw " + victim + " as the only obstacle to seizing control of the organisation.",
                    "With " + victim + " out of the picture, " + subject + " would inherit full authority over the estate.",
                    subject + " had long resented " + victim + "'s dominance and orchestrated a takeover.",
                    "Eliminating " + victim + " was the final move in " + subject + "'s carefully planned bid for control."};
                break;
            case "SELF_DEFENSE":
                pool = new String[]{
                    subject + " believed " + victim + " was about to expose a secret that would ruin everything.",
                    "After receiving threatening messages from " + victim + ", " + subject + " acted out of genuine fear.",
                    subject + " claimed " + victim + " attacked first, but the evidence suggests a premeditated response.",
                    "Cornered by " + victim + "'s escalating threats, " + subject + " felt there was no alternative."};
                break;
            case "IDEOLOGY":
                pool = new String[]{
                    subject + " viewed " + victim + "'s activities as a betrayal of deeply held principles and acted accordingly.",
                    "Radicalised through online forums, " + subject + " targeted " + victim + " as a symbol of everything wrong.",
                    subject + " believed silencing " + victim + " would advance a political cause they were devoted to.",
                    "A fanatical commitment to a fringe movement drove " + subject + " to act against " + victim + "."};
                break;
            case "CONCEALMENT":
                pool = new String[]{
                    subject + " had committed a prior offence that " + victim + " was about to report to the authorities.",
                    victim + " stumbled upon " + subject + "'s embezzlement scheme and had to be silenced.",
                    subject + " needed to destroy evidence of a previous fraud before " + victim + " could hand it over.",
                    "With " + victim + " threatening to reveal the truth, " + subject + " acted to protect a web of lies."};
                break;
            case "PASSION":
                pool = new String[]{
                    "An intense argument between " + subject + " and " + victim + " escalated into a violent confrontation.",
                    subject + "'s uncontrollable rage after discovering " + victim + "'s betrayal led to a fatal outburst.",
                    "Years of suppressed emotion erupted when " + subject + " confronted " + victim + " about the affair.",
                    "A moment of blind fury during a heated exchange drove " + subject + " to act without thinking."};
                break;
            case "LOYALTY":
                pool = new String[]{
                    subject + " acted to protect a family member who " + victim + " was threatening to expose.",
                    "A close friend of " + subject + " asked for help dealing with " + victim + ", and " + subject + " couldn't refuse.",
                    subject + " took the fall for an associate, believing loyalty demanded sacrifice.",
                    "To shield a loved one from " + victim + "'s harassment, " + subject + " decided to intervene permanently."};
                break;
            default:
                pool = new String[]{
                    subject + "'s true motivation remains complex — a mix of personal grievance and opportunity.",
                    "The exact reason " + subject + " targeted " + victim + " stems from a private conflict not yet fully understood.",
                    subject + " was driven by circumstances that created a perfect storm of desperation and opportunity."};
                break;
        }
        return pool[random.nextInt(pool.length)];
    }

    /** Creates a lead row in leadsModel and returns its ID. */
    private String addInlineLead(int[] idCounter, List<CategoryEntry> methods, String subject) {
        CategoryEntry method = methods.get(random.nextInt(methods.size()));
        String methodName = method.getName() != null ? method.getName() : method.getCode();
        String hint = buildLeadHint(idCounter[0], methodName, subject);
        String desc = buildLeadDescription(idCounter[0], methodName, subject);
        String id = "lead-" + idCounter[0]++;
        leadsModel.addRow(new Object[]{id, hint, methodName, desc});
        return id;
    }

    /** Picks a fact category based on the action being performed. */
    private String categoryForAction(String actionTitle) {
        ActionType type = ActionType.classify(actionTitle);
        return type != null ? type.getFactCategory() : "ITEM";
    }

    /** Generates a contextual fact sentence driven by the current action. */
    private String buildInlineFact(String actionTitle, String subject, String victim,
                                   int phase, int major, boolean highImportance) {
        ActionType actionType = ActionType.classify(actionTitle);
        if (actionType == ActionType.PHOTOGRAPH) {
            String[] high = {
                "Scene photos reveal a second set of footprints near " + victim + "'s location.",
                "Documentation shows the door was forced from the inside, implicating " + subject + ".",
                "A sketch of the layout reveals an unaccounted exit used after the incident."};
            String[] low = {
                "General scene photos show no obvious signs of struggle.",
                "The layout appears consistent with the initial report.",
                "Photographs capture minor damage unrelated to the main incident."};
            String[] pool = highImportance ? high : low;
            return pool[random.nextInt(pool.length)];
        } else if (actionType == ActionType.EVIDENCE) {
            String[] high = {
                "Trace evidence places " + subject + " at the scene within the critical window.",
                "A recovered item bears " + subject + "'s fingerprints and traces of " + victim + "'s blood.",
                "Lab analysis links the recovered substance to a toxin found in " + victim + "'s system."};
            String[] low = {
                "A common fibre sample was collected — inconclusive but logged.",
                "Low-quality latent print recovered — partial match pending.",
                "Residue collected may be from a cleaning product, not evidence."};
            String[] pool = highImportance ? high : low;
            return pool[random.nextInt(pool.length)];
        } else if (actionType == ActionType.INTERVIEW) {
            String[] high = {
                "A witness confirms seeing " + subject + " near " + victim + " shortly before the incident.",
                "An associate reveals that " + subject + " and " + victim + " had a secret arrangement.",
                "The interviewee discloses that " + subject + " threatened " + victim + " days earlier."};
            String[] low = {
                "The contact says they barely know " + subject + " — little useful information.",
                "A neighbour heard raised voices but can't identify who was involved.",
                "The associate repeats the official story without adding new details."};
            String[] pool = highImportance ? high : low;
            return pool[random.nextInt(pool.length)];
        } else {
            String[] high = {
                "Financial records show " + subject + " received a large payment around the incident date.",
                "A document links " + subject + " to a shell company connected to " + victim + ".",
                "Digital records reveal " + subject + " searched for information about " + victim + "'s routine."};
            String[] low = {
                "Records show routine transactions with no obvious irregularities.",
                "A public filing lists " + subject + " at a different address — minor note.",
                "Background check returns no prior criminal record for " + subject + "."};
            String[] pool = highImportance ? high : low;
            return pool[random.nextInt(pool.length)];
        }
    }

    /**
     * Returns a subset of attributes that make narrative sense for the given action.
     * Delegates to {@link ActionType#attributesFor(String)}.
     */
    private String[] attributesForAction(String actionTitle) {
        return ActionType.attributesFor(actionTitle);
    }

    /** Returns a random short result description appropriate for the given action title. */
    private String buildResultDescription(String actionTitle) {
        ActionType actionType = ActionType.classify(actionTitle);
        String[][] pools;
        if (actionType == ActionType.PHOTOGRAPH) {
            pools = new String[][]{
                    {"Photo reveals an inconsistency in the official report",
                     "Photograph captures a detail that contradicts the coroner's findings",
                     "Image shows a disturbance inconsistent with the stated timeline"},
                    {"A hidden marking or tag is visible in the photo",
                     "Angle of entry suggests a different point of origin",
                     "Background detail connects the scene to a known location"}};
        } else if (actionType == ActionType.EVIDENCE) {
            pools = new String[][]{
                    {"Trace evidence collected links to a key person",
                     "Sample retrieved matches a known substance in the file",
                     "Fingerprint partial places a third party at the scene"},
                    {"Concealed item recovered — origin requires investigation",
                     "Secondary sample may widen the suspect pool",
                     "Low-grade trace is inconclusive but points to a direction"}};
        } else if (actionType == ActionType.INTERVIEW) {
            pools = new String[][]{
                    {"Contact reveals a hidden relationship",
                     "Associate discloses a secret meeting that contradicts the alibi",
                     "Neighbour saw something they haven't told police"},
                    {"Contact discloses information under pressure",
                     "Associate lets slip a detail worth following up",
                     "Subject is evasive — their reluctance itself is a clue"}};
        } else {
            pools = new String[][]{
                    {"Record anomaly found — contradicts the official timeline",
                     "Document trail links subject to a previously unknown account",
                     "Financial record shows a payment that shouldn't exist"},
                    {"Forged or altered entry identified in the records",
                     "Gap in the paper trail suggests deliberate concealment",
                     "Cross-reference reveals a connection to a second individual"}};
        }
        // Pick one at random from either sub-pool
        String[] pool = pools[random.nextInt(pools.length)];
        return pool[random.nextInt(pool.length)];
    }

    private String buildMajorTitle(int phase, int major, String caseType) {
        String[][] pool = (phase == 1)
                ? new String[][]{
                        {"Gather Primary Evidence", "Secure the Crime Scene",
                         "Establish a Timeline",   "Identify the Victim's Last Contact"},
                        {"Identify Key Persons",   "Map Suspect Connections",
                         "Locate Potential Witnesses", "Profile the Principal Suspect"}}
                : new String[][]{
                        {"Investigate New Lead",   "Re-examine Discarded Evidence",
                         "Follow the Money Trail", "Pursue an Unexpected Tip"},
                        {"Confirm Revised Theory", "Challenge the Official Account",
                         "Expose the Cover-up",    "Connect the Remaining Dots"}};
        return pool[major - 1][random.nextInt(pool[major - 1].length)];
    }

    private String buildMinorTitle(int major, int minor) {
        String[][] pool = (major == 1)
                ? new String[][]{
                        {"Scene Investigation",   "Physical Evidence Survey",
                         "Site Documentation",    "Evidence Recovery"},
                        {"Witness Contact",        "Informant Outreach",
                         "Associate Interview",   "Neighbourhood Canvass"}}
                : new String[][]{
                        {"Background Research",   "Records Analysis",
                         "Digital Footprint Check", "Financial Audit"},
                        {"Surveillance Operation", "Tail and Observe",
                         "Covert Monitoring",     "Pattern Analysis"}};
        return pool[minor - 1][random.nextInt(pool[minor - 1].length)];
    }

    private String buildActionTitle(int minor, int action, String subject) {
        if (minor == 1) {
            String[] slot1 = {"Photograph the scene", "Sketch the layout", "Map entry and exit points"};
            String[] slot2 = {"Collect physical evidence", "Bag and tag trace samples",
                               "Recover latent fingerprints"};
            return (action == 1)
                    ? slot1[random.nextInt(slot1.length)]
                    : slot2[random.nextInt(slot2.length)];
        } else {
            String[] slot1 = {"Interview a contact of " + subject,
                               "Speak to a known associate of " + subject,
                               "Question a neighbour of " + subject};
            String[] slot2 = {"Review documents or records", "Analyse financial records",
                               "Search public records for " + subject};
            return (action == 1)
                    ? slot1[random.nextInt(slot1.length)]
                    : slot2[random.nextInt(slot2.length)];
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
            // Phone & location info
            String phone = String.valueOf(npcModel.getValueAt(i, 16));
            boolean phoneDisco = Boolean.TRUE.equals(npcModel.getValueAt(i, 17));
            String loc = String.valueOf(npcModel.getValueAt(i, 18));
            if (!phone.isEmpty()) {
                sb.append("  phone=").append(phone);
                sb.append(phoneDisco ? " [KNOWN]" : " [HIDDEN]");
            }
            if (!loc.isEmpty()) {
                sb.append("  loc=").append(loc);
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

        sb.append('\n');
        sb.append("--- Interviews (").append(interviewModel.getRowCount()).append(" responses) ---\n");
        String lastNpc = "";
        for (int i = 0; i < interviewModel.getRowCount(); i++) {
            String npc = String.valueOf(interviewModel.getValueAt(i, 0));
            if (!npc.equals(lastNpc)) {
                if (!lastNpc.isEmpty()) sb.append('\n');
                String role = String.valueOf(interviewModel.getValueAt(i, 1));
                sb.append("  ").append(npc).append(" (").append(role).append("):\n");
                lastNpc = npc;
            }
            String topic    = String.valueOf(interviewModel.getValueAt(i, 2));
            String question = String.valueOf(interviewModel.getValueAt(i, 3));
            String answer   = String.valueOf(interviewModel.getValueAt(i, 4));
            Object truthObj = interviewModel.getValueAt(i, 5);
            boolean truthful = Boolean.TRUE.equals(truthObj);
            String aboutNpc = String.valueOf(interviewModel.getValueAt(i, 6));
            sb.append("    [").append(topic).append("]");
            if (!aboutNpc.isEmpty()) sb.append(" (about ").append(aboutNpc).append(")");
            sb.append(truthful ? " ✔" : " ✘").append('\n');
            sb.append("      Q: ").append(question).append('\n');
            sb.append("      A: ").append(answer).append('\n');
        }

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

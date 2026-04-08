package eb.admin.ui;

import eb.admin.model.CategoryData;
import eb.admin.model.CategoryEntry;

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
 *   <li><b>Client &amp; Subject</b> — enter or generate names</li>
 *   <li><b>NPC Characters</b> — generate case-specific NPCs with roles and relationships</li>
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

    // Step 2 – Client & Subject
    private final JTextField clientNameField  = new JTextField(20);
    private final JTextField subjectNameField = new JTextField(20);

    // Step 3 – NPC Characters
    private final DefaultTableModel npcModel =
            new DefaultTableModel(new String[]{
                    "Role", "Name", "Gender", "Age", "Occupation",
                    "Cooperativeness", "Honesty", "Nervousness"}, 0);
    private final DefaultTableModel relationshipModel =
            new DefaultTableModel(new String[]{
                    "From", "To", "Type", "Opinion"}, 0);

    // Step 4 – Description & Objective
    private final JTextArea descriptionArea = new JTextArea(4, 60);
    private final JTextArea objectiveArea   = new JTextArea(2, 60);

    // Step 5 – Leads
    private final DefaultTableModel leadsModel =
            new DefaultTableModel(new String[]{"ID", "Hint", "Discovery Method", "Description"}, 0);

    // Step 6 – Story Tree
    private final DefaultMutableTreeNode storyRoot = new DefaultMutableTreeNode("Story Root");
    private final DefaultTreeModel       treeModel = new DefaultTreeModel(storyRoot);
    private final JTree                  storyTree = new JTree(treeModel);

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
        npcModel.setRowCount(0);
        relationshipModel.setRowCount(0);
        descriptionArea.setText("");
        objectiveArea.setText("");
        leadsModel.setRowCount(0);
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
        steps.addTab("2. Client & Subject",       buildNamesStep());
        steps.addTab("3. NPC Characters",         buildNpcStep());
        steps.addTab("4. Description & Objective", buildDescriptionStep());
        steps.addTab("5. Leads",                  buildLeadsStep());
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

    // -- Step 2 ---------------------------------------------------------------

    private JPanel buildNamesStep() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0;
        form.add(new JLabel("Client Name:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1;
        form.add(clientNameField, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        form.add(new JLabel("Subject Name:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1;
        form.add(subjectNameField, gbc);

        JButton generateBtn = new JButton("Generate Description & Objective");
        generateBtn.addActionListener(e -> generateDescriptionAndObjective());
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.CENTER;
        form.add(generateBtn, gbc);

        panel.add(form, BorderLayout.NORTH);
        return panel;
    }

    // -- Step 3: NPC Characters ------------------------------------------------

    private JPanel buildNpcStep() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // --- NPC table (top half) ---
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

        // --- Relationship table (bottom half) ---
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
            npcModel.addRow(new Object[]{"", "", "M", 30, "", 5, 5, 5});
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

        panel.add(split,   BorderLayout.CENTER);
        panel.add(buttons, BorderLayout.SOUTH);
        return panel;
    }

    // -- Step 4 ---------------------------------------------------------------

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

        panel.add(form, BorderLayout.CENTER);
        return panel;
    }

    // -- Step 5 ---------------------------------------------------------------

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

    // -- Step 6 ---------------------------------------------------------------

    private JPanel buildStoryTreeStep() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        storyTree.setRootVisible(true);
        storyTree.setShowsRootHandles(true);
        JScrollPane scroll = new JScrollPane(storyTree);

        JButton genTreeBtn   = new JButton("Generate Story Tree");
        JButton addChildBtn  = new JButton("Add Child");
        JButton deleteNodeBtn = new JButton("Delete Node");
        JButton expandBtn    = new JButton("Expand All");

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

        panel.add(scroll,  BorderLayout.CENTER);
        panel.add(buttons, BorderLayout.SOUTH);
        return panel;
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
            return;
        }
        int idx = caseTypeCombo.getSelectedIndex();
        List<CategoryEntry> types = categoryData.getCase_types();
        if (idx >= 0 && idx < types.size()) {
            String desc = types.get(idx).getDescription();
            caseTypeDesc.setText(desc != null ? desc : " ");
        }
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
                return new String[]{"Client", "Subject (Suspect)",
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

        for (int i = 0; i < roles.length; i++) {
            String role   = roles[i];
            String gender = random.nextBoolean() ? "M" : "F";
            String name;
            // Use the names from Step 2 for the first two NPCs
            if (i == 0 && !client.isEmpty()) {
                name = client;
            } else if (i == 1 && !subject.isEmpty()) {
                name = subject;
            } else {
                name = randomName(gender);
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

            npcModel.addRow(new Object[]{
                    role, name, gender, age, occupation,
                    cooperativeness, honesty, nervousness
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
        String caseType = (String) caseTypeCombo.getSelectedItem();
        String client   = clientNameField.getText().trim();
        String subject  = subjectNameField.getText().trim();

        if (caseType == null || caseType.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select a case type first.",
                    "Missing Data", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (client.isEmpty()) client = "the client";
        if (subject.isEmpty()) subject = "the subject";

        String desc = buildDescription(caseType, client, subject);
        String obj  = buildObjective(caseType, subject);

        descriptionArea.setText(desc);
        objectiveArea.setText(obj);
        statusLabel.setText("Generated description and objective for " + caseType + " case.");
    }

    private String buildDescription(String caseType, String client, String subject) {
        switch (caseType) {
            case "Missing Person":
                return client + " has hired you to find " + subject
                        + ", who went missing several days ago. The family is desperate for answers. "
                        + "The police filed a report but seem to have moved on to other cases.";
            case "Infidelity":
                return client + " suspects that " + subject
                        + " has been unfaithful and wants concrete proof. "
                        + "There have been late nights, unexplained absences, and secretive phone calls.";
            case "Theft":
                return client + " reported that valuable property was stolen and believes "
                        + subject + " is responsible. The police have not made an arrest.";
            case "Fraud":
                return client + " has uncovered financial irregularities pointing to "
                        + subject + ". The discrepancies may go back months.";
            case "Blackmail":
                return client + " is being blackmailed and believes " + subject
                        + " is behind the threats. The messages have been escalating.";
            case "Murder":
                return client + " believes the death linked to " + subject
                        + " was not accidental. The official investigation was closed too quickly.";
            case "Stalking":
                return client + " has been followed and watched by " + subject
                        + " for weeks. The behaviour is becoming more aggressive.";
            case "Corporate Espionage":
                return client + " suspects " + subject
                        + " of leaking confidential information to a competitor.";
            default:
                return client + " has hired you to investigate " + subject + ".";
        }
    }

    private String buildObjective(String caseType, String subject) {
        switch (caseType) {
            case "Missing Person":
                return "Locate " + subject + " and determine what happened to them.";
            case "Infidelity":
                return "Gather conclusive evidence of " + subject + "'s infidelity.";
            case "Theft":
                return "Identify who stole the property and recover it if possible.";
            case "Fraud":
                return "Prove that " + subject + " committed fraud and document the extent.";
            case "Blackmail":
                return "Identify the blackmailer and obtain evidence to stop the threats.";
            case "Murder":
                return "Prove that " + subject + "'s death was not accidental and identify the killer.";
            case "Stalking":
                return "Identify and document " + subject + "'s stalking behaviour for a restraining order.";
            case "Corporate Espionage":
                return "Prove that " + subject + " is leaking information and identify the recipient.";
            default:
                return "Investigate " + subject + " and report what you find.";
        }
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
              .append(" nerv=").append(npcModel.getValueAt(i, 7))
              .append('\n');
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

package eb.admin.ui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Admin panel for viewing and editing the case-description metadata and
 * template pools in {@code assets/text/case_templates_en.json}.
 *
 * <p>The panel shows a list of case types on the left and, for each selected
 * type, an editable view of the base NPC roles, extra-suspect ranges, suspect
 * labels, summary description, and complexity-based description templates.
 *
 * <p>Changes can be saved back to disk — the panel merges its edits into the
 * full JSON file so other sections ({@code descriptions}, {@code objectives},
 * {@code caseSeeds}) are preserved.
 */
public class CaseDescriptionTemplatePanel extends JPanel {

    private static final String DEFAULT_PATH =
            "assets/text/case_templates_en.json";

    private final JLabel statusLabel;

    // ── File meta ─────────────────────────────────────────────────────────────
    private final JTextField fileField = new JTextField();
    private File currentFile;

    // ── Left: case-type list ─────────────────────────────────────────────────
    private final DefaultListModel<String> typeListModel = new DefaultListModel<>();
    private final JList<String> typeList = new JList<>(typeListModel);

    // ── Right: role / suspect editor ─────────────────────────────────────────
    private final DefaultTableModel rolesModel =
            new DefaultTableModel(new String[]{"#", "Role"}, 0) {
                @Override public boolean isCellEditable(int row, int col) {
                    return col == 1;
                }
            };
    private final JTable rolesTable = new JTable(rolesModel);

    private final DefaultTableModel suspectLabelsModel =
            new DefaultTableModel(new String[]{"#", "Suspect Label"}, 0) {
                @Override public boolean isCellEditable(int row, int col) {
                    return col == 1;
                }
            };
    private final JTable suspectLabelsTable = new JTable(suspectLabelsModel);

    private final JTextField extraC2Field = new JTextField(6);
    private final JTextField extraC3Field = new JTextField(6);
    private final JTextArea  summaryArea  = new JTextArea(3, 40);
    private final Map<Integer, DefaultTableModel> descriptionModels = new LinkedHashMap<>();

    /**
     * In-memory representation: case-type name → mutable data.
     * Mutated by the UI; written back on save.
     */
    private final LinkedHashMap<String, CaseDescEntry> entries = new LinkedHashMap<>();

    /** Currently selected case-type key (may be null). */
    private String selectedType = null;
    private boolean loadingUi = false;

    // ── Status ────────────────────────────────────────────────────────────────
    private final JLabel localStatus = new JLabel(" ");

    // =========================================================================
    // Inner data holder
    // =========================================================================

    private static class CaseDescEntry {
        final List<String> roles          = new ArrayList<>();
        String extraSuspectsComplexity2   = "0";
        String extraSuspectsComplexity3   = "0";
        final List<String> suspectLabels  = new ArrayList<>();
        final Map<Integer, List<String>> descriptionPools = new LinkedHashMap<>();
        String summary                    = "";

        CaseDescEntry() {
            for (int complexity = 1; complexity <= 3; complexity++) {
                descriptionPools.put(complexity, new ArrayList<String>());
            }
        }
    }

    // =========================================================================
    // Construction
    // =========================================================================

    public CaseDescriptionTemplatePanel(JLabel sharedStatusLabel) {
        super(new BorderLayout(0, 4));
        this.statusLabel = sharedStatusLabel;
        buildUI();
        tryLoadDefault();
    }

    // =========================================================================
    // UI construction
    // =========================================================================

    private void buildUI() {
        // ── Toolbar ──────────────────────────────────────────────────────
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        JButton openBtn = new JButton("Open…");
        JButton saveBtn = new JButton("Save");
        toolbar.add(openBtn);
        toolbar.add(saveBtn);
        toolbar.add(new JLabel("  File:"));
        fileField.setEditable(false);
        fileField.setColumns(30);
        toolbar.add(fileField);

        openBtn.addActionListener(e -> openFile());
        saveBtn.addActionListener(e -> saveFile());

        add(toolbar, BorderLayout.NORTH);

        // ── Left: type list ─────────────────────────────────────────────
        typeList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        typeList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) onTypeSelected();
        });
        JScrollPane typeScroll = new JScrollPane(typeList);
        typeScroll.setPreferredSize(new Dimension(180, 0));
        typeScroll.setBorder(BorderFactory.createTitledBorder("Case Types"));

        // ── Right: editor ───────────────────────────────────────────────
        JPanel editor = new JPanel(new BorderLayout(6, 6));

        // Roles table
        rolesTable.setRowHeight(24);
        rolesTable.getColumnModel().getColumn(0).setPreferredWidth(30);
        rolesTable.getColumnModel().getColumn(1).setPreferredWidth(250);
        JScrollPane rolesScroll = new JScrollPane(rolesTable);
        rolesScroll.setBorder(BorderFactory.createTitledBorder("Base NPC Roles"));
        rolesScroll.setPreferredSize(new Dimension(0, 160));

        JButton addRoleBtn = new JButton("Add Role");
        JButton delRoleBtn = new JButton("Delete Role");
        addRoleBtn.addActionListener(e -> {
            int num = rolesModel.getRowCount() + 1;
            rolesModel.addRow(new Object[]{num, ""});
            commitCurrentType();
        });
        delRoleBtn.addActionListener(e -> {
            int row = rolesTable.getSelectedRow();
            if (row >= 0) {
                rolesModel.removeRow(row);
                renumberRows(rolesModel);
                commitCurrentType();
            }
        });
        JPanel rolesBtnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        rolesBtnPanel.add(addRoleBtn);
        rolesBtnPanel.add(delRoleBtn);

        JPanel rolesPanel = new JPanel(new BorderLayout());
        rolesPanel.add(rolesScroll, BorderLayout.CENTER);
        rolesPanel.add(rolesBtnPanel, BorderLayout.SOUTH);

        // Extra suspects + suspect labels
        JPanel extraPanel = new JPanel(new GridBagLayout());
        extraPanel.setBorder(BorderFactory.createTitledBorder("Additional Suspects"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 4, 2, 4);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0;
        extraPanel.add(new JLabel("Extra suspects at complexity 2:"), gbc);
        gbc.gridx = 1;
        extraPanel.add(extraC2Field, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        extraPanel.add(new JLabel("Extra suspects at complexity 3:"), gbc);
        gbc.gridx = 1;
        extraPanel.add(extraC3Field, gbc);

        // Wire text field changes to commit
        extraC2Field.getDocument().addDocumentListener(new SimpleDocListener(this::commitCurrentType));
        extraC3Field.getDocument().addDocumentListener(new SimpleDocListener(this::commitCurrentType));

        // Suspect labels table
        suspectLabelsTable.setRowHeight(24);
        suspectLabelsTable.getColumnModel().getColumn(0).setPreferredWidth(30);
        suspectLabelsTable.getColumnModel().getColumn(1).setPreferredWidth(250);
        JScrollPane slScroll = new JScrollPane(suspectLabelsTable);
        slScroll.setBorder(BorderFactory.createTitledBorder("Suspect Role Labels"));
        slScroll.setPreferredSize(new Dimension(0, 120));

        JButton addSlBtn = new JButton("Add Label");
        JButton delSlBtn = new JButton("Delete Label");
        addSlBtn.addActionListener(e -> {
            int num = suspectLabelsModel.getRowCount() + 1;
            suspectLabelsModel.addRow(new Object[]{num, ""});
            commitCurrentType();
        });
        delSlBtn.addActionListener(e -> {
            int row = suspectLabelsTable.getSelectedRow();
            if (row >= 0) {
                suspectLabelsModel.removeRow(row);
                renumberRows(suspectLabelsModel);
                commitCurrentType();
            }
        });
        JPanel slBtnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        slBtnPanel.add(addSlBtn);
        slBtnPanel.add(delSlBtn);

        JPanel slPanel = new JPanel(new BorderLayout());
        slPanel.add(slScroll, BorderLayout.CENTER);
        slPanel.add(slBtnPanel, BorderLayout.SOUTH);

        // Summary
        summaryArea.setLineWrap(true);
        summaryArea.setWrapStyleWord(true);
        summaryArea.getDocument().addDocumentListener(new SimpleDocListener(this::commitCurrentType));
        JScrollPane summaryScroll = new JScrollPane(summaryArea);
        summaryScroll.setBorder(BorderFactory.createTitledBorder("Summary"));
        summaryScroll.setPreferredSize(new Dimension(0, 80));

        JTabbedPane descriptionTabs = new JTabbedPane();
        for (int complexity = 1; complexity <= 3; complexity++) {
            DefaultTableModel model =
                    new DefaultTableModel(new String[]{"#", "Template"}, 0) {
                        @Override public boolean isCellEditable(int row, int col) {
                            return col == 1;
                        }
                    };
            JTable table = new JTable(model);
            table.setRowHeight(24);
            table.getColumnModel().getColumn(0).setPreferredWidth(30);
            table.getColumnModel().getColumn(1).setPreferredWidth(600);

            JButton addTemplateBtn = new JButton("Add Template");
            JButton delTemplateBtn = new JButton("Delete Template");
            final int currentComplexity = complexity;
            addTemplateBtn.addActionListener(e -> {
                int num = model.getRowCount() + 1;
                model.addRow(new Object[]{num, ""});
                commitCurrentType();
            });
            delTemplateBtn.addActionListener(e -> {
                int row = table.getSelectedRow();
                if (row >= 0) {
                    model.removeRow(row);
                    renumberRows(model);
                    commitCurrentType();
                }
            });

            JPanel templateButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
            templateButtons.add(addTemplateBtn);
            templateButtons.add(delTemplateBtn);

            JPanel templatePanel = new JPanel(new BorderLayout(0, 4));
            JScrollPane templateScroll = new JScrollPane(table);
            templateScroll.setBorder(BorderFactory.createTitledBorder(
                    "Description Templates (Complexity " + currentComplexity + ")"));
            templatePanel.add(templateScroll, BorderLayout.CENTER);
            templatePanel.add(templateButtons, BorderLayout.SOUTH);

            descriptionModels.put(complexity, model);
            descriptionTabs.addTab("Complexity " + complexity, templatePanel);
        }

        JPanel rightUpper = new JPanel(new GridLayout(1, 2, 6, 0));
        rightUpper.add(rolesPanel);

        JPanel rightSuspectsCol = new JPanel(new BorderLayout(0, 4));
        rightSuspectsCol.add(extraPanel, BorderLayout.NORTH);
        rightSuspectsCol.add(slPanel, BorderLayout.CENTER);
        rightUpper.add(rightSuspectsCol);

        JPanel lowerPanel = new JPanel(new BorderLayout(0, 6));
        lowerPanel.add(summaryScroll, BorderLayout.NORTH);
        lowerPanel.add(descriptionTabs, BorderLayout.CENTER);

        JSplitPane editorSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, rightUpper, lowerPanel);
        editorSplit.setDividerLocation(220);
        editorSplit.setResizeWeight(0.35);

        editor.add(editorSplit, BorderLayout.CENTER);

        // ── Split ───────────────────────────────────────────────────────
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, typeScroll, editor);
        split.setDividerLocation(180);
        split.setResizeWeight(0.15);

        add(split, BorderLayout.CENTER);
        add(localStatus, BorderLayout.SOUTH);
    }

    // =========================================================================
    // Type selection
    // =========================================================================

    private void onTypeSelected() {
        String key = typeList.getSelectedValue();
        if (key == null) return;
        commitCurrentType();      // save any pending edits for previous type
        selectedType = key;
        loadTypeIntoUI(key);
    }

    private void loadTypeIntoUI(String key) {
        CaseDescEntry entry = entries.get(key);
        if (entry == null) return;
        loadingUi = true;

        try {
            // Roles
            rolesModel.setRowCount(0);
            for (int i = 0; i < entry.roles.size(); i++) {
                rolesModel.addRow(new Object[]{i + 1, entry.roles.get(i)});
            }

            // Extra suspects
            extraC2Field.setText(entry.extraSuspectsComplexity2);
            extraC3Field.setText(entry.extraSuspectsComplexity3);

            // Suspect labels
            suspectLabelsModel.setRowCount(0);
            for (int i = 0; i < entry.suspectLabels.size(); i++) {
                suspectLabelsModel.addRow(new Object[]{i + 1, entry.suspectLabels.get(i)});
            }

            // Summary
            summaryArea.setText(entry.summary);

            for (int complexity = 1; complexity <= 3; complexity++) {
                DefaultTableModel model = descriptionModels.get(complexity);
                model.setRowCount(0);
                List<String> templates = entry.descriptionPools.get(complexity);
                for (int i = 0; i < templates.size(); i++) {
                    model.addRow(new Object[]{i + 1, templates.get(i)});
                }
            }
        } finally {
            loadingUi = false;
        }
    }

    /** Writes current UI state back into the in-memory entry for {@link #selectedType}. */
    private void commitCurrentType() {
        if (loadingUi) return;
        if (selectedType == null) return;
        CaseDescEntry entry = entries.get(selectedType);
        if (entry == null) return;

        entry.roles.clear();
        for (int r = 0; r < rolesModel.getRowCount(); r++) {
            String role = String.valueOf(rolesModel.getValueAt(r, 1));
            entry.roles.add(role);
        }
        entry.extraSuspectsComplexity2 = extraC2Field.getText().trim();
        entry.extraSuspectsComplexity3 = extraC3Field.getText().trim();

        entry.suspectLabels.clear();
        for (int r = 0; r < suspectLabelsModel.getRowCount(); r++) {
            String lbl = String.valueOf(suspectLabelsModel.getValueAt(r, 1));
            entry.suspectLabels.add(lbl);
        }
        entry.summary = summaryArea.getText();

        for (int complexity = 1; complexity <= 3; complexity++) {
            List<String> templates = entry.descriptionPools.get(complexity);
            templates.clear();
            DefaultTableModel model = descriptionModels.get(complexity);
            for (int r = 0; r < model.getRowCount(); r++) {
                String template = String.valueOf(model.getValueAt(r, 1));
                templates.add(template);
            }
        }
    }

    // =========================================================================
    // File I/O
    // =========================================================================

    private void tryLoadDefault() {
        File f = new File(DEFAULT_PATH);
        if (f.exists()) loadFromFile(f);
    }

    private void openFile() {
        JFileChooser chooser = new JFileChooser("assets/text");
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "JSON files", "json"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            loadFromFile(chooser.getSelectedFile());
        }
    }

    private void loadFromFile(File file) {
        try {
            String json = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            Gson gson = new Gson();
            JsonObject root = gson.fromJson(json, JsonObject.class);

            entries.clear();
            typeListModel.clear();

            JsonObject cdObj = root.getAsJsonObject("caseDescriptions");
            if (cdObj != null) {
                for (Map.Entry<String, JsonElement> e : cdObj.entrySet()) {
                    String key = e.getKey();
                    JsonObject val = e.getValue().getAsJsonObject();

                    CaseDescEntry entry = new CaseDescEntry();
                    if (val.has("roles")) {
                        for (JsonElement r : val.getAsJsonArray("roles")) {
                            entry.roles.add(r.getAsString());
                        }
                    }
                    entry.extraSuspectsComplexity2 = val.has("extraSuspectsComplexity2")
                            ? val.get("extraSuspectsComplexity2").getAsString() : "0";
                    entry.extraSuspectsComplexity3 = val.has("extraSuspectsComplexity3")
                            ? val.get("extraSuspectsComplexity3").getAsString() : "0";
                    if (val.has("suspectLabels")) {
                        for (JsonElement s : val.getAsJsonArray("suspectLabels")) {
                            entry.suspectLabels.add(s.getAsString());
                        }
                    }
                    entry.summary = val.has("summary") ? val.get("summary").getAsString() : "";

                    entries.put(key, entry);
                    typeListModel.addElement(key);
                }
            }

            JsonObject descriptionsObj = root.getAsJsonObject("descriptions");
            if (descriptionsObj != null) {
                for (Map.Entry<String, JsonElement> e : descriptionsObj.entrySet()) {
                    String key = e.getKey();
                    int dot = key.lastIndexOf('.');
                    if (dot <= 0 || dot >= key.length() - 1) continue;

                    String typeKey = key.substring(0, dot);
                    int complexity;
                    try {
                        complexity = Integer.parseInt(key.substring(dot + 1));
                    } catch (NumberFormatException ex) {
                        continue;
                    }
                    if (complexity < 1 || complexity > 3) continue;

                    CaseDescEntry entry = entries.get(typeKey);
                    if (entry == null) {
                        entry = new CaseDescEntry();
                        entries.put(typeKey, entry);
                        typeListModel.addElement(typeKey);
                    }

                    List<String> templates = entry.descriptionPools.get(complexity);
                    templates.clear();
                    for (JsonElement template : e.getValue().getAsJsonArray()) {
                        templates.add(template.getAsString());
                    }
                }
            }

            currentFile = file;
            fileField.setText(file.getPath());
            localStatus.setText("Loaded " + entries.size() + " case description entries.");
            statusLabel.setText("Case description templates loaded from " + file.getName());

            if (!typeListModel.isEmpty()) {
                typeList.setSelectedIndex(0);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Failed to load: " + ex.getMessage(),
                    "Load Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveFile() {
        commitCurrentType();  // flush any pending UI edits

        File target = currentFile;
        if (target == null) {
            JFileChooser chooser = new JFileChooser("assets/text");
            chooser.setSelectedFile(new File(DEFAULT_PATH));
            if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
            target = chooser.getSelectedFile();
        }

        try {
            // Read existing JSON so we only replace the caseDescriptions section
            JsonObject root;
            if (target.exists()) {
                String existing = new String(Files.readAllBytes(target.toPath()), StandardCharsets.UTF_8);
                root = new Gson().fromJson(existing, JsonObject.class);
            } else {
                root = new JsonObject();
            }

            // Build the caseDescriptions object
            JsonObject cdObj = new JsonObject();
            JsonObject descriptionsObj = new JsonObject();
            for (Map.Entry<String, CaseDescEntry> e : entries.entrySet()) {
                JsonObject val = new JsonObject();
                CaseDescEntry entry = e.getValue();

                JsonArray rolesArr = new JsonArray();
                for (String r : entry.roles) rolesArr.add(r);
                val.add("roles", rolesArr);

                val.addProperty("extraSuspectsComplexity2", entry.extraSuspectsComplexity2);
                val.addProperty("extraSuspectsComplexity3", entry.extraSuspectsComplexity3);

                JsonArray slArr = new JsonArray();
                for (String s : entry.suspectLabels) slArr.add(s);
                val.add("suspectLabels", slArr);

                val.addProperty("summary", entry.summary);

                cdObj.add(e.getKey(), val);

                for (int complexity = 1; complexity <= 3; complexity++) {
                    JsonArray templatesArr = new JsonArray();
                    List<String> templates = entry.descriptionPools.get(complexity);
                    for (String template : templates) templatesArr.add(template);
                    descriptionsObj.add(e.getKey() + "." + complexity, templatesArr);
                }
            }
            root.add("caseDescriptions", cdObj);
            root.add("descriptions", descriptionsObj);

            Gson prettyGson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
            try (Writer w = new FileWriter(target, StandardCharsets.UTF_8)) {
                prettyGson.toJson(root, w);
            }

            currentFile = target;
            fileField.setText(target.getPath());
            localStatus.setText("Saved " + entries.size() + " case description entries.");
            statusLabel.setText("Case description templates saved to " + target.getName());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    "Failed to save: " + ex.getMessage(),
                    "Save Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private static void renumberRows(DefaultTableModel model) {
        for (int r = 0; r < model.getRowCount(); r++) {
            model.setValueAt(r + 1, r, 0);
        }
    }

    /**
     * Minimal document listener that calls a callback on every change.
     */
    private static class SimpleDocListener implements javax.swing.event.DocumentListener {
        private final Runnable callback;
        SimpleDocListener(Runnable callback) { this.callback = callback; }
        @Override public void insertUpdate(javax.swing.event.DocumentEvent e)  { callback.run(); }
        @Override public void removeUpdate(javax.swing.event.DocumentEvent e)  { callback.run(); }
        @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { callback.run(); }
    }
}

package eb.admin.ui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
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
 * Admin panel for viewing and editing the interview template text pools stored in
 * {@code assets/text/interview_templates_en.json}.
 *
 * <h3>Layout</h3>
 * <ul>
 *   <li><b>Pools</b> sub-tab — left list of pool keys; right editable list of
 *       text entries for the selected pool.  Supports adding, deleting, and
 *       editing entries.  An "Add Pool" / "Delete Pool" toolbar manages the key
 *       list itself.</li>
 *   <li><b>Questions</b> sub-tab — table of (key → question string) pairs with
 *       full inline editing.</li>
 * </ul>
 *
 * <p>Open and Save operations use a toolbar at the top of the panel.  The
 * default file path is {@code assets/text/interview_templates_en.json} relative
 * to the working directory.
 */
public class InterviewTemplateEditorPanel extends JPanel {

    private static final String DEFAULT_PATH =
            "assets/text/interview_templates_en.json";

    private final JLabel statusLabel;

    // ── File meta ─────────────────────────────────────────────────────────────
    private final JTextField fileField     = new JTextField();
    private File             currentFile;

    // ── Pools sub-tab ─────────────────────────────────────────────────────────
    /** Ordered map of pool-key → list of entry strings.  Mutated by the UI. */
    private final LinkedHashMap<String, List<String>> pools = new LinkedHashMap<>();
    /** List model backing the pool-key JList on the left. */
    private final DefaultListModel<String> poolKeyModel = new DefaultListModel<>();
    private final JList<String>            poolKeyList  = new JList<>(poolKeyModel);
    /** List model backing the entries JList on the right. */
    private final DefaultListModel<String> entryModel   = new DefaultListModel<>();
    private final JList<String>            entryList    = new JList<>(entryModel);
    /** Currently selected pool key (may be null). */
    private String selectedPoolKey = null;

    // ── Questions sub-tab ────────────────────────────────────────────────────
    private final DefaultTableModel questionsModel = new DefaultTableModel(
            new String[]{"Key", "Question"}, 0);
    private final JTable questionsTable = new JTable(questionsModel);

    // ── Status ────────────────────────────────────────────────────────────────
    private final JLabel localStatus = new JLabel(" ");

    // =========================================================================
    // Construction
    // =========================================================================

    public InterviewTemplateEditorPanel(JLabel sharedStatusLabel) {
        super(new BorderLayout(0, 4));
        this.statusLabel = sharedStatusLabel;
        setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        add(buildToolbar(),       BorderLayout.NORTH);
        add(buildEditorTabs(),    BorderLayout.CENTER);
        add(localStatus,          BorderLayout.SOUTH);

        tryLoadDefaultFile();
    }

    // =========================================================================
    // UI builders
    // =========================================================================

    private JPanel buildToolbar() {
        JPanel bar = new JPanel(new BorderLayout(4, 0));
        bar.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        JButton openBtn = new JButton("Open…");
        JButton saveBtn = new JButton("Save");
        JButton saveAsBtn = new JButton("Save As…");
        openBtn.addActionListener(this::onOpen);
        saveBtn.addActionListener(e -> onSave(false));
        saveAsBtn.addActionListener(e -> onSave(true));
        left.add(openBtn);
        left.add(saveBtn);
        left.add(saveAsBtn);

        fileField.setEditable(false);
        fileField.setFont(fileField.getFont().deriveFont(Font.ITALIC));

        bar.add(left,      BorderLayout.WEST);
        bar.add(fileField, BorderLayout.CENTER);
        return bar;
    }

    private JTabbedPane buildEditorTabs() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Pools",     buildPoolsPanel());
        tabs.addTab("Questions", buildQuestionsPanel());
        return tabs;
    }

    // ── Pools panel ──────────────────────────────────────────────────────────

    private JPanel buildPoolsPanel() {
        JPanel panel = new JPanel(new BorderLayout(6, 4));
        panel.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));

        // Left: pool-key list + pool management toolbar
        poolKeyList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        poolKeyList.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        poolKeyList.addListSelectionListener(this::onPoolKeySelected);
        JScrollPane keyScroll = new JScrollPane(poolKeyList);
        keyScroll.setPreferredSize(new Dimension(240, 0));
        keyScroll.setBorder(BorderFactory.createTitledBorder("Pool Keys"));

        JPanel keyToolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        JButton addPoolBtn    = new JButton("+ Pool");
        JButton renamePoolBtn = new JButton("Rename");
        JButton deletePoolBtn = new JButton("✕ Pool");
        addPoolBtn.addActionListener(e -> onAddPool());
        renamePoolBtn.addActionListener(e -> onRenamePool());
        deletePoolBtn.addActionListener(e -> onDeletePool());
        keyToolbar.add(addPoolBtn);
        keyToolbar.add(renamePoolBtn);
        keyToolbar.add(deletePoolBtn);

        JPanel leftPanel = new JPanel(new BorderLayout(0, 2));
        leftPanel.add(keyScroll,   BorderLayout.CENTER);
        leftPanel.add(keyToolbar,  BorderLayout.SOUTH);

        // Right: entry list + entry management toolbar
        entryList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        entryList.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        JScrollPane entryScroll = new JScrollPane(entryList);
        entryScroll.setBorder(BorderFactory.createTitledBorder("Entries (select a pool key)"));

        JPanel entryToolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        JButton addEntryBtn    = new JButton("+ Entry");
        JButton editEntryBtn   = new JButton("Edit");
        JButton moveUpBtn      = new JButton("▲");
        JButton moveDownBtn    = new JButton("▼");
        JButton deleteEntryBtn = new JButton("✕ Entry");
        addEntryBtn.addActionListener(e -> onAddEntry());
        editEntryBtn.addActionListener(e -> onEditEntry());
        moveUpBtn.addActionListener(e -> onMoveEntry(-1));
        moveDownBtn.addActionListener(e -> onMoveEntry(1));
        deleteEntryBtn.addActionListener(e -> onDeleteEntry());
        entryToolbar.add(addEntryBtn);
        entryToolbar.add(editEntryBtn);
        entryToolbar.add(moveUpBtn);
        entryToolbar.add(moveDownBtn);
        entryToolbar.add(deleteEntryBtn);

        // Double-click to edit entry inline
        entryList.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) onEditEntry();
            }
        });

        JPanel rightPanel = new JPanel(new BorderLayout(0, 2));
        rightPanel.add(entryScroll,  BorderLayout.CENTER);
        rightPanel.add(entryToolbar, BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        split.setDividerLocation(260);
        split.setResizeWeight(0.25);

        panel.add(split, BorderLayout.CENTER);
        return panel;
    }

    // ── Questions panel ──────────────────────────────────────────────────────

    private JPanel buildQuestionsPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));

        questionsTable.setFillsViewportHeight(true);
        questionsTable.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        questionsTable.setRowHeight(22);
        questionsTable.getColumnModel().getColumn(0).setPreferredWidth(220);
        questionsTable.getColumnModel().getColumn(1).setPreferredWidth(600);
        JScrollPane scroll = new JScrollPane(questionsTable);

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        JButton addBtn    = new JButton("+ Question");
        JButton deleteBtn = new JButton("✕ Question");
        addBtn.addActionListener(e -> onAddQuestion());
        deleteBtn.addActionListener(e -> onDeleteQuestion());
        toolbar.add(addBtn);
        toolbar.add(deleteBtn);

        panel.add(scroll,   BorderLayout.CENTER);
        panel.add(toolbar,  BorderLayout.SOUTH);
        return panel;
    }

    // =========================================================================
    // Pool selection
    // =========================================================================

    private void onPoolKeySelected(ListSelectionEvent e) {
        if (e.getValueIsAdjusting()) return;
        commitEntriesToCurrentPool();
        selectedPoolKey = poolKeyList.getSelectedValue();
        reloadEntryList();
    }

    /** Pushes the current entryModel state back into the pools map for the previously selected key. */
    private void commitEntriesToCurrentPool() {
        if (selectedPoolKey == null || !pools.containsKey(selectedPoolKey)) return;
        List<String> entries = new ArrayList<>();
        for (int i = 0; i < entryModel.size(); i++) entries.add(entryModel.get(i));
        pools.put(selectedPoolKey, entries);
    }

    /** Fills entryModel from the pools map for the newly selected key. */
    private void reloadEntryList() {
        entryModel.clear();
        TitledBorder b = (TitledBorder) ((JScrollPane) entryList.getParent().getParent())
                .getBorder();
        if (selectedPoolKey == null) {
            b.setTitle("Entries (select a pool key)");
        } else {
            b.setTitle("Entries for: " + selectedPoolKey);
            List<String> entries = pools.getOrDefault(selectedPoolKey, new ArrayList<>());
            for (String entry : entries) entryModel.addElement(entry);
        }
        entryList.repaint();
    }

    // =========================================================================
    // Pool CRUD
    // =========================================================================

    private void onAddPool() {
        String key = JOptionPane.showInputDialog(this, "Pool key (e.g. client.alibi):",
                "Add Pool", JOptionPane.PLAIN_MESSAGE);
        if (key == null || key.trim().isEmpty()) return;
        key = key.trim();
        if (pools.containsKey(key)) {
            setStatus("Pool already exists: " + key);
            return;
        }
        pools.put(key, new ArrayList<>());
        poolKeyModel.addElement(key);
        poolKeyList.setSelectedValue(key, true);
        setStatus("Added pool: " + key);
    }

    private void onRenamePool() {
        int idx = poolKeyList.getSelectedIndex();
        if (idx < 0) { setStatus("Select a pool to rename."); return; }
        String oldKey = poolKeyModel.get(idx);
        String newKey = (String) JOptionPane.showInputDialog(this, "New key for pool:",
                "Rename Pool", JOptionPane.PLAIN_MESSAGE, null, null, oldKey);
        if (newKey == null || newKey.trim().isEmpty() || newKey.trim().equals(oldKey)) return;
        newKey = newKey.trim();
        if (pools.containsKey(newKey)) { setStatus("Key already exists: " + newKey); return; }
        commitEntriesToCurrentPool();
        List<String> entries = pools.remove(oldKey);
        // Preserve insertion order by rebuilding
        LinkedHashMap<String, List<String>> rebuilt = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> e : pools.entrySet()) rebuilt.put(e.getKey(), e.getValue());
        pools.clear();
        for (Map.Entry<String, List<String>> e : rebuilt.entrySet()) {
            if (e.getKey().equals(oldKey)) pools.put(newKey, entries);
            else pools.put(e.getKey(), e.getValue());
        }
        poolKeyModel.set(idx, newKey);
        selectedPoolKey = newKey;
        poolKeyList.setSelectedIndex(idx);
        setStatus("Renamed pool: " + oldKey + " → " + newKey);
    }

    private void onDeletePool() {
        int idx = poolKeyList.getSelectedIndex();
        if (idx < 0) { setStatus("Select a pool to delete."); return; }
        String key = poolKeyModel.get(idx);
        int confirm = JOptionPane.showConfirmDialog(this,
                "Delete pool "" + key + "" and all its entries?",
                "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;
        pools.remove(key);
        poolKeyModel.remove(idx);
        selectedPoolKey = null;
        entryModel.clear();
        setStatus("Deleted pool: " + key);
    }

    // =========================================================================
    // Entry CRUD
    // =========================================================================

    private void onAddEntry() {
        if (selectedPoolKey == null) { setStatus("Select a pool first."); return; }
        String text = promptEntry(null);
        if (text == null) return;
        entryModel.addElement(text);
        setStatus("Added entry to " + selectedPoolKey);
    }

    private void onEditEntry() {
        if (selectedPoolKey == null) { setStatus("Select a pool first."); return; }
        int idx = entryList.getSelectedIndex();
        if (idx < 0) { setStatus("Select an entry to edit."); return; }
        String text = promptEntry(entryModel.get(idx));
        if (text == null) return;
        entryModel.set(idx, text);
        setStatus("Updated entry in " + selectedPoolKey);
    }

    private void onMoveEntry(int delta) {
        int idx = entryList.getSelectedIndex();
        if (idx < 0) return;
        int target = idx + delta;
        if (target < 0 || target >= entryModel.size()) return;
        String a = entryModel.get(idx);
        String b = entryModel.get(target);
        entryModel.set(idx, b);
        entryModel.set(target, a);
        entryList.setSelectedIndex(target);
    }

    private void onDeleteEntry() {
        int idx = entryList.getSelectedIndex();
        if (idx < 0) { setStatus("Select an entry to delete."); return; }
        entryModel.remove(idx);
        setStatus("Deleted entry from " + selectedPoolKey);
    }

    /** Shows a multi-line text dialog for entering / editing a template string. */
    private String promptEntry(String existing) {
        JTextArea area = new JTextArea(existing != null ? existing : "", 6, 60);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        JScrollPane scroll = new JScrollPane(area);
        scroll.setPreferredSize(new Dimension(550, 120));
        JPanel msg = new JPanel(new BorderLayout(0, 4));
        msg.add(new JLabel("<html>Enter template text. Use placeholders: "
                + "<b>$client $subject $victim $targetPerson $pronoun $pronounCap "
                + "$hobby $social $likeDislike $locationClue $otherName $phone $location</b></html>"),
                BorderLayout.NORTH);
        msg.add(scroll, BorderLayout.CENTER);
        int result = JOptionPane.showConfirmDialog(this, msg,
                existing != null ? "Edit Entry" : "Add Entry",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return null;
        String text = area.getText().trim();
        return text.isEmpty() ? null : text;
    }

    // =========================================================================
    // Question CRUD
    // =========================================================================

    private void onAddQuestion() {
        questionsTable.clearSelection();
        // Commit any pending edit
        if (questionsTable.isEditing()) questionsTable.getCellEditor().stopCellEditing();
        questionsModel.addRow(new Object[]{"new.question.key", "Question text?"});
        int row = questionsModel.getRowCount() - 1;
        questionsTable.setRowSelectionInterval(row, row);
        questionsTable.scrollRectToVisible(questionsTable.getCellRect(row, 0, true));
        questionsTable.editCellAt(row, 0);
        setStatus("Added new question row.");
    }

    private void onDeleteQuestion() {
        if (questionsTable.isEditing()) questionsTable.getCellEditor().stopCellEditing();
        int row = questionsTable.getSelectedRow();
        if (row < 0) { setStatus("Select a question row to delete."); return; }
        String key = (String) questionsModel.getValueAt(row, 0);
        questionsModel.removeRow(row);
        setStatus("Deleted question: " + key);
    }

    // =========================================================================
    // File operations
    // =========================================================================

    private void tryLoadDefaultFile() {
        File f = new File(DEFAULT_PATH);
        if (f.exists()) loadFile(f);
    }

    private void onOpen(ActionEvent e) {
        JFileChooser fc = new JFileChooser("assets/text");
        fc.setDialogTitle("Open Interview Templates JSON");
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("JSON files", "json"));
        if (currentFile != null) fc.setCurrentDirectory(currentFile.getParentFile());
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        loadFile(fc.getSelectedFile());
    }

    private void onSave(boolean saveAs) {
        // Commit any in-progress edits
        commitEntriesToCurrentPool();
        if (questionsTable.isEditing()) questionsTable.getCellEditor().stopCellEditing();

        File target = currentFile;
        if (saveAs || target == null) {
            JFileChooser fc = new JFileChooser("assets/text");
            fc.setDialogTitle("Save Interview Templates JSON");
            fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("JSON files", "json"));
            if (currentFile != null) fc.setSelectedFile(currentFile);
            if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
            target = fc.getSelectedFile();
            if (!target.getName().endsWith(".json")) target = new File(target.getPath() + ".json");
        }

        try (Writer w = new FileWriter(target, StandardCharsets.UTF_8)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
            JsonObject root = buildJsonObject();
            gson.toJson(root, w);
            currentFile = target;
            fileField.setText(target.getAbsolutePath());
            setStatus("Saved: " + target.getName());
        } catch (IOException ex) {
            setStatus("Save failed: " + ex.getMessage());
            JOptionPane.showMessageDialog(this, "Save failed:\n" + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadFile(File file) {
        try {
            String json = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            Gson gson = new Gson();
            JsonObject root = gson.fromJson(json, JsonObject.class);
            if (root == null) throw new IOException("Empty or invalid JSON");

            pools.clear();
            poolKeyModel.clear();
            entryModel.clear();
            selectedPoolKey = null;

            JsonObject poolsNode = root.getAsJsonObject("pools");
            if (poolsNode != null) {
                for (Map.Entry<String, JsonElement> entry : poolsNode.entrySet()) {
                    String key = entry.getKey();
                    List<String> entries = new ArrayList<>();
                    if (entry.getValue().isJsonArray()) {
                        for (JsonElement el : entry.getValue().getAsJsonArray()) {
                            entries.add(el.getAsString());
                        }
                    }
                    pools.put(key, entries);
                    poolKeyModel.addElement(key);
                }
            }

            questionsModel.setRowCount(0);
            JsonObject questionsNode = root.getAsJsonObject("questions");
            if (questionsNode != null) {
                for (Map.Entry<String, JsonElement> entry : questionsNode.entrySet()) {
                    questionsModel.addRow(new Object[]{entry.getKey(), entry.getValue().getAsString()});
                }
            }

            currentFile = file;
            fileField.setText(file.getAbsolutePath());
            setStatus("Loaded: " + file.getName() + " (" + pools.size() + " pools, "
                    + questionsModel.getRowCount() + " questions)");
        } catch (Exception ex) {
            setStatus("Load failed: " + ex.getMessage());
            JOptionPane.showMessageDialog(this, "Load failed:\n" + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Builds the JSON object from the current UI state. */
    private JsonObject buildJsonObject() {
        JsonObject root = new JsonObject();

        JsonObject poolsNode = new JsonObject();
        for (Map.Entry<String, List<String>> e : pools.entrySet()) {
            JsonArray arr = new JsonArray();
            for (String s : e.getValue()) arr.add(s);
            poolsNode.add(e.getKey(), arr);
        }
        root.add("pools", poolsNode);

        JsonObject questionsNode = new JsonObject();
        for (int row = 0; row < questionsModel.getRowCount(); row++) {
            String key = String.valueOf(questionsModel.getValueAt(row, 0));
            String val = String.valueOf(questionsModel.getValueAt(row, 1));
            questionsNode.addProperty(key, val);
        }
        root.add("questions", questionsNode);

        return root;
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private void setStatus(String msg) {
        localStatus.setText(msg);
        if (statusLabel != null) statusLabel.setText(msg);
    }
}

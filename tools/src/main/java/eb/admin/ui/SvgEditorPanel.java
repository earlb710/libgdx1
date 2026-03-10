package eb.admin.ui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Top-level panel for the <b>SVG</b> tab in the Game Admin tool.
 *
 * <p>Contains one inner sub-tab:
 * <ul>
 *   <li><b>SVG Resource</b> – {@code assets/face/svgs-index.json}
 *       (Feature, ID, Gender columns)</li>
 * </ul>
 *
 * <p>The SVG Resource table flattens the nested JSON structure of
 * {@code svgs-index.json} into rows of {@code [Feature, ID, Gender]}:
 * <ul>
 *   <li>{@code Feature} – the feature category (e.g. {@code accessories},
 *       {@code eye}); editable so entirely new categories can be added</li>
 *   <li>{@code ID}      – the variant ID (e.g. {@code eye1},
 *       {@code hat})</li>
 *   <li>{@code Gender}  – gender compatibility: {@code both},
 *       {@code male}, or {@code female}</li>
 * </ul>
 *
 * <p>Open / Save / Save&nbsp;As buttons allow editing and persisting the
 * resource index file.
 */
public class SvgEditorPanel extends JPanel {

    private static final String DEFAULT_INDEX_PATH = "assets/face/svgs-index.json";

    private final JLabel statusLabel;

    // ── SVG Resource fields ───────────────────────────────────────────────────

    private final JTextField svgIndexFileField = new JTextField();

    /**
     * Table model for the SVG Resource table.
     * Columns: Feature (col 0), ID (col 1), Gender (col 2).
     * All columns are editable so users can add entirely new feature categories.
     */
    private final DefaultTableModel svgIndexModel =
            new DefaultTableModel(new String[]{"Feature", "ID", "Gender"}, 0) {
                @Override
                public boolean isCellEditable(int row, int col) {
                    return true;
                }
            };

    private final JTable svgIndexTable = new JTable(svgIndexModel);
    private File svgIndexFile;

    // ─────────────────────────────────────────────────────────────────────────

    public SvgEditorPanel(JLabel statusLabel) {
        this.statusLabel = statusLabel;
        buildUI();
        tryLoadDefaults();
    }

    // ── UI construction ───────────────────────────────────────────────────────

    private void buildUI() {
        setLayout(new BorderLayout());

        JTabbedPane subTabs = new JTabbedPane();
        subTabs.addTab("SVG Resource", buildSvgResourceTab());

        add(subTabs, BorderLayout.CENTER);
    }

    private JPanel buildSvgResourceTab() {
        svgIndexFileField.setEditable(false);

        configureTable(svgIndexTable);
        svgIndexTable.getColumnModel().getColumn(0).setPreferredWidth(120);
        svgIndexTable.getColumnModel().getColumn(1).setPreferredWidth(200);
        svgIndexTable.getColumnModel().getColumn(2).setPreferredWidth(80);

        // Gender column – use a JComboBox editor for convenience
        JComboBox<String> genderCombo = new JComboBox<>(new String[]{"both", "male", "female"});
        svgIndexTable.getColumnModel().getColumn(2)
                .setCellEditor(new DefaultCellEditor(genderCombo));

        JButton addBtn    = new JButton("Add Row");
        JButton deleteBtn = new JButton("Delete Row");
        JButton saveBtn   = new JButton("Save");

        addBtn.addActionListener((ActionEvent e) -> {
            // Seed the Feature cell from the selected row for convenience; a new
            // feature name can also be typed directly since the column is editable.
            String feature = "";
            int selected = svgIndexTable.getSelectedRow();
            if (selected >= 0) {
                Object val = svgIndexModel.getValueAt(selected, 0);
                feature = val != null ? val.toString() : "";
            }
            svgIndexModel.addRow(new Object[]{feature, "", "both"});
            int last = svgIndexModel.getRowCount() - 1;
            svgIndexTable.scrollRectToVisible(svgIndexTable.getCellRect(last, 0, true));
            svgIndexTable.setRowSelectionInterval(last, last);
            // Start editing the Feature cell so the user can set it immediately
            svgIndexTable.editCellAt(last, 0);
        });
        deleteBtn.addActionListener((ActionEvent e) -> deleteSelectedRow(svgIndexTable, svgIndexModel));
        saveBtn.addActionListener((ActionEvent e) -> saveSvgIndex(false));

        return buildTabLayout(svgIndexFileField,
                svgIndexTable,
                addBtn, deleteBtn, saveBtn,
                this::openSvgIndex,
                () -> saveSvgIndex(false),
                () -> saveSvgIndex(true));
    }

    // ── Generic layout builder ────────────────────────────────────────────────

    /**
     * Assembles the common BorderLayout panel structure:
     * a file-path strip at the top, a scrollable table in the centre, and
     * Add / Delete / Save buttons along the bottom.
     */
    private JPanel buildTabLayout(JTextField fileField,
                                   JTable table,
                                   JButton addBtn, JButton deleteBtn, JButton saveRowBtn,
                                   Runnable openAction, Runnable saveAction, Runnable saveAsAction) {

        JPanel fileRow = new JPanel(new BorderLayout(4, 0));
        fileRow.setBorder(BorderFactory.createEmptyBorder(0, 8, 2, 8));
        fileRow.add(new JLabel("File:"), BorderLayout.WEST);
        fileRow.add(fileField, BorderLayout.CENTER);

        JPanel meta = new JPanel(new BorderLayout());
        meta.setBorder(BorderFactory.createTitledBorder("File"));
        meta.add(fileRow, BorderLayout.CENTER);

        // File toolbar
        JButton openBtn   = new JButton("Open\u2026");
        JButton saveBtn   = new JButton("Save");
        JButton saveAsBtn = new JButton("Save As\u2026");
        openBtn.addActionListener((ActionEvent e) -> openAction.run());
        saveBtn.addActionListener((ActionEvent e) -> saveAction.run());
        saveAsBtn.addActionListener((ActionEvent e) -> saveAsAction.run());

        JPanel fileToolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        fileToolbar.add(openBtn);
        fileToolbar.add(saveBtn);
        fileToolbar.add(saveAsBtn);

        JPanel north = new JPanel(new BorderLayout());
        north.add(meta,        BorderLayout.CENTER);
        north.add(fileToolbar, BorderLayout.SOUTH);

        // Row toolbar
        JPanel rowToolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        rowToolbar.add(addBtn);
        rowToolbar.add(deleteBtn);
        rowToolbar.add(Box.createHorizontalStrut(12));
        rowToolbar.add(saveRowBtn);

        JScrollPane scroll = new JScrollPane(table,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.add(north,      BorderLayout.NORTH);
        panel.add(scroll,     BorderLayout.CENTER);
        panel.add(rowToolbar, BorderLayout.SOUTH);
        return panel;
    }

    // ── File operations: SVG Resource (svgs-index.json) ──────────────────────

    private void openSvgIndex() {
        File chosen = chooseOpenFile(svgIndexFile, "assets/face");
        if (chosen != null) loadSvgIndexFromFile(chosen);
    }

    /**
     * Parses {@code svgs-index.json} and populates the table.
     *
     * <p>The JSON structure is:
     * <pre>
     * {
     *   "svgsIndex":   { "feature": ["id1", "id2", ...], ... },
     *   "svgsGenders": { "feature": ["both", "male", ...], ... }
     * }
     * </pre>
     * Each (feature, id, gender) triple becomes one table row.
     */
    private void loadSvgIndexFromFile(File file) {
        try (Reader reader = new FileReader(file)) {
            JsonObject root = new Gson().fromJson(reader, JsonObject.class);
            svgIndexModel.setRowCount(0);

            JsonObject indexObj   = root.has("svgsIndex")   ? root.getAsJsonObject("svgsIndex")   : new JsonObject();
            JsonObject gendersObj = root.has("svgsGenders") ? root.getAsJsonObject("svgsGenders") : new JsonObject();

            for (Map.Entry<String, JsonElement> featureEntry : indexObj.entrySet()) {
                String    feature   = featureEntry.getKey();
                JsonArray ids       = featureEntry.getValue().getAsJsonArray();
                JsonArray genders   = gendersObj.has(feature)
                        ? gendersObj.getAsJsonArray(feature)
                        : new JsonArray();

                for (int i = 0; i < ids.size(); i++) {
                    String id     = ids.get(i).getAsString();
                    String gender = (i < genders.size()) ? genders.get(i).getAsString() : "both";
                    svgIndexModel.addRow(new Object[]{feature, id, gender});
                }
            }

            svgIndexFile = file;
            svgIndexFileField.setText(file.getAbsolutePath());
            statusLabel.setText("SVG index loaded: " + file.getAbsolutePath()
                    + "  (" + svgIndexModel.getRowCount() + " entries)");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Error loading file:\n" + ex.getMessage(),
                    "Load Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Serialises the table back to {@code svgs-index.json} format.
     *
     * <p>Rows are grouped by Feature; within each feature group the original
     * insertion order is preserved so that the regenerated file matches the
     * structure expected by {@link eb.framework1.face.FaceGenerator}.
     */
    private void saveSvgIndex(boolean saveAs) {
        svgIndexFile = resolveTargetFile(svgIndexFile, saveAs);
        if (svgIndexFile == null) return;

        // Collect rows, preserving feature insertion order; skip incomplete rows
        Map<String, List<String>> idMap     = new LinkedHashMap<>();
        Map<String, List<String>> genderMap = new LinkedHashMap<>();
        List<String> skipped = new ArrayList<>();
        for (int r = 0; r < svgIndexModel.getRowCount(); r++) {
            String feature = cellStr(svgIndexModel, r, 0).trim();
            String id      = cellStr(svgIndexModel, r, 1).trim();
            String gender  = cellStr(svgIndexModel, r, 2).trim();
            if (feature.isEmpty() || id.isEmpty()) {
                skipped.add("row " + (r + 1) + " (feature='" + feature + "', id='" + id + "')");
                continue;
            }
            idMap    .computeIfAbsent(feature, k -> new ArrayList<>()).add(id);
            genderMap.computeIfAbsent(feature, k -> new ArrayList<>()).add(
                    gender.isEmpty() ? "both" : gender);
        }
        if (!skipped.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "The following rows were skipped because Feature or ID is empty:\n"
                            + String.join("\n", skipped),
                    "Save Warning", JOptionPane.WARNING_MESSAGE);
        }

        // Build JSON
        JsonObject indexObj   = new JsonObject();
        JsonObject gendersObj = new JsonObject();
        for (String feature : idMap.keySet()) {
            JsonArray ids     = new JsonArray();
            JsonArray genders = new JsonArray();
            for (String id     : idMap    .get(feature)) ids    .add(id);
            for (String gender : genderMap.get(feature)) genders.add(gender);
            indexObj  .add(feature, ids);
            gendersObj.add(feature, genders);
        }

        JsonObject root = new JsonObject();
        root.add("svgsIndex",   indexObj);
        root.add("svgsGenders", gendersObj);

        try (Writer writer = new FileWriter(svgIndexFile)) {
            new GsonBuilder().setPrettyPrinting().create().toJson(root, writer);
            svgIndexFileField.setText(svgIndexFile.getAbsolutePath());
            statusLabel.setText("SVG index saved: " + svgIndexFile.getAbsolutePath());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    "Error saving file:\n" + ex.getMessage(),
                    "Save Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ── Default file auto-loading ─────────────────────────────────────────────

    private void tryLoadDefaults() {
        File f = new File(DEFAULT_INDEX_PATH);
        if (f.exists()) loadSvgIndexFromFile(f);
    }

    // ── Shared helpers ────────────────────────────────────────────────────────

    private static void configureTable(JTable table) {
        table.setRowHeight(26);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
    }

    private static void deleteSelectedRow(JTable table, DefaultTableModel model) {
        int row = table.getSelectedRow();
        if (row >= 0) {
            model.removeRow(row);
        } else {
            JOptionPane.showMessageDialog(table,
                    "Please select a row to delete.",
                    "No Selection", JOptionPane.WARNING_MESSAGE);
        }
    }

    private static String cellStr(DefaultTableModel model, int row, int col) {
        Object val = model.getValueAt(row, col);
        return val != null ? val.toString() : "";
    }

    private File chooseOpenFile(File currentFile, String defaultDirPath) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("JSON files (*.json)", "json"));
        if (currentFile != null) {
            chooser.setCurrentDirectory(currentFile.getParentFile());
        } else {
            File d = new File(defaultDirPath);
            chooser.setCurrentDirectory(d.isDirectory() ? d : new File("."));
        }
        return chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION
                ? chooser.getSelectedFile() : null;
    }

    private File resolveTargetFile(File currentFile, boolean saveAs) {
        if (currentFile == null || saveAs) {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(new FileNameExtensionFilter("JSON files (*.json)", "json"));
            if (currentFile != null) chooser.setSelectedFile(currentFile);
            if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return null;
            File chosen = chooser.getSelectedFile();
            return chosen.getName().endsWith(".json") ? chosen
                    : new File(chosen.getAbsolutePath() + ".json");
        }
        return currentFile;
    }
}

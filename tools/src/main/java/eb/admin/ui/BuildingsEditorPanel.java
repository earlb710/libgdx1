package eb.admin.ui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Panel that displays and edits the contents of buildings_en.json.
 *
 * The table shows each building's ID, Name, Category, and
 * Improvements (as a comma-separated list).  All other numeric fields
 * (minFloors, maxFloors, unitsPerFloor, capacity, percentage) are
 * preserved transparently on load and save.  Annotation support mirrors
 * the pattern used by {@link CompanyTypesEditorPanel}.
 */
public class BuildingsEditorPanel extends JPanel {

    private static final String DEFAULT_JSON_PATH = "assets/text/buildings_en.json";
    private static final Color ANNOTATION_FOREGROUND = new Color(0, 0, 139);
    private static final String[] RATINGS = {"Excellent", "Good", "Sufficient", "Bad", "Very Bad"};

    private final JTextField versionField  = new JTextField(8);
    private final JComboBox<String> languageCombo = new JComboBox<>(EditorUtils.LANGUAGES);
    private final JTextField fileField = new JTextField();
    /** When true, combo ActionListener does not trigger a language switch. */
    private boolean suppressLangListener = false;

    private final DefaultTableModel tableModel = createModel();
    private final Map<String, String> annotationRatings = new HashMap<>();
    /** Parallel list – preserves the original JSON object for each table row. */
    private final List<JsonObject> originalObjects = new ArrayList<>();

    private final JTable table = new JTable(tableModel) {
        @Override
        public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
            Component c = super.prepareRenderer(renderer, row, column);
            Object keyVal = tableModel.getValueAt(row, 0);
            String key = keyVal != null ? keyVal.toString() : "";
            String colName = tableModel.getColumnName(column);
            String ratingId = annotationRatings.get(key + "\0" + colName);
            Color bg = DescriptionEditorPanel.ratingToColor(ratingId);
            if (bg != null) {
                c.setBackground(bg);
                c.setForeground(ANNOTATION_FOREGROUND);
            } else if (!isRowSelected(row)) {
                c.setBackground(getBackground());
                c.setForeground(getForeground());
            }
            return c;
        }
    };

    private File currentFile;
    private final JLabel statusLabel;

    public BuildingsEditorPanel(JLabel statusLabel) {
        this.statusLabel = statusLabel;
        buildUI();
        tryLoadDefaultFile();
    }

    // -------------------------------------------------------------------------
    // UI construction
    // -------------------------------------------------------------------------

    private void buildUI() {
        setLayout(new BorderLayout(0, 4));

        languageCombo.addActionListener(e -> {
            if (!suppressLangListener) {
                switchLanguage((String) languageCombo.getSelectedItem());
            }
        });

        add(buildNorthPanel(), BorderLayout.NORTH);

        table.setRowHeight(26);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
        table.getColumnModel().getColumn(0).setPreferredWidth(160);
        table.getColumnModel().getColumn(1).setPreferredWidth(180);
        table.getColumnModel().getColumn(2).setPreferredWidth(120);
        table.getColumnModel().getColumn(3).setPreferredWidth(320);
        table.getColumnModel().getColumn(4).setPreferredWidth(420);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        JButton addBtn    = new JButton("Add Row");
        JButton deleteBtn = new JButton("Delete Row");
        JButton saveBtn   = new JButton("Save");

        addBtn.addActionListener((ActionEvent e) -> {
            tableModel.addRow(new Object[]{"", "", "", "", ""});
            originalObjects.add(new JsonObject());
            int last = tableModel.getRowCount() - 1;
            table.scrollRectToVisible(table.getCellRect(last, 0, true));
            table.setRowSelectionInterval(last, last);
        });

        deleteBtn.addActionListener((ActionEvent e) -> {
            int row = table.getSelectedRow();
            if (row >= 0) {
                tableModel.removeRow(row);
                originalObjects.remove(row);
            } else {
                JOptionPane.showMessageDialog(this,
                        "Please select a row to delete.",
                        "No Selection", JOptionPane.WARNING_MESSAGE);
            }
        });

        saveBtn.addActionListener((ActionEvent e) -> saveFile(false));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        buttons.add(addBtn);
        buttons.add(deleteBtn);
        buttons.add(Box.createHorizontalStrut(12));
        buttons.add(saveBtn);

        JScrollPane tableScrollPane = new JScrollPane(table,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);

        JPanel center = new JPanel(new BorderLayout());
        center.add(tableScrollPane, BorderLayout.CENTER);
        center.add(buttons,         BorderLayout.SOUTH);
        add(center, BorderLayout.CENTER);

        // Right-click opens annotation rating menu for any editable cell
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    int row = table.rowAtPoint(e.getPoint());
                    int col = table.columnAtPoint(e.getPoint());
                    if (row >= 0 && col >= 0 && tableModel.isCellEditable(row, col)) {
                        table.setRowSelectionInterval(row, row);
                        showAnnotationMenu(row, col, e.getX(), e.getY());
                    }
                }
            }
        });
    }

    private JPanel buildNorthPanel() {
        fileField.setEditable(false);

        JPanel metaTop = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        metaTop.add(new JLabel("Version:"));
        metaTop.add(versionField);
        metaTop.add(Box.createHorizontalStrut(12));
        metaTop.add(new JLabel("Language:"));
        metaTop.add(languageCombo);

        JPanel fileRow = new JPanel(new BorderLayout(4, 0));
        fileRow.setBorder(BorderFactory.createEmptyBorder(0, 8, 2, 8));
        fileRow.add(new JLabel("File:"), BorderLayout.WEST);
        fileRow.add(fileField, BorderLayout.CENTER);

        JPanel meta = new JPanel(new BorderLayout());
        meta.setBorder(BorderFactory.createTitledBorder("File Metadata"));
        meta.add(metaTop,  BorderLayout.NORTH);
        meta.add(fileRow,  BorderLayout.CENTER);

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        JButton openBtn   = new JButton("Open…");
        JButton saveBtn   = new JButton("Save");
        JButton saveAsBtn = new JButton("Save As…");
        openBtn.addActionListener((ActionEvent e) -> openFile());
        saveBtn.addActionListener((ActionEvent e) -> saveFile(false));
        saveAsBtn.addActionListener((ActionEvent e) -> saveFile(true));
        toolbar.add(openBtn);
        toolbar.add(saveBtn);
        toolbar.add(saveAsBtn);

        JPanel north = new JPanel(new BorderLayout());
        north.add(meta,    BorderLayout.CENTER);
        north.add(toolbar, BorderLayout.SOUTH);
        return north;
    }

    // -------------------------------------------------------------------------
    // File operations
    // -------------------------------------------------------------------------

    private void tryLoadDefaultFile() {
        File defaultFile = new File(DEFAULT_JSON_PATH);
        if (defaultFile.exists()) {
            loadFromFile(defaultFile);
        }
    }

    private void openFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("JSON files (*.json)", "json"));
        if (currentFile != null) {
            chooser.setCurrentDirectory(currentFile.getParentFile());
        } else {
            File assetsText = new File("assets/text");
            chooser.setCurrentDirectory(assetsText.isDirectory() ? assetsText : new File("."));
        }
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            loadFromFile(chooser.getSelectedFile());
        }
    }

    private void loadFromFile(File file) {
        try (Reader reader = new FileReader(file)) {
            JsonObject root = new Gson().fromJson(reader, JsonObject.class);
            if (root == null) {
                throw new IllegalArgumentException("File does not contain a valid JSON object.");
            }

            versionField.setText(root.has("version")  ? root.get("version").getAsString()  : "");
            suppressLangListener = true;
            languageCombo.setSelectedItem(root.has("language") ? root.get("language").getAsString() : "en");
            suppressLangListener = false;

            tableModel.setRowCount(0);
            originalObjects.clear();
            if (root.has("buildings")) {
                List<JsonObject> rows = new ArrayList<>();
                for (JsonElement el : root.getAsJsonArray("buildings")) {
                    rows.add(el.getAsJsonObject());
                }
                rows.sort(Comparator.comparing(o -> (o.has("id") ? o.get("id").getAsString() : "")));
                for (JsonObject entry : rows) {
                    String id   = entry.has("id")       ? entry.get("id").getAsString()       : "";
                    String name = entry.has("name")     ? entry.get("name").getAsString()     : "";
                    String cat  = entry.has("category") ? entry.get("category").getAsString() : "";
                    String improvements = "";
                    if (entry.has("improvements") && entry.get("improvements").isJsonArray()) {
                        List<String> imps = new ArrayList<>();
                        for (JsonElement imp : entry.getAsJsonArray("improvements")) {
                            if (imp != null && !imp.isJsonNull()) {
                                imps.add(imp.getAsString());
                            }
                        }
                        improvements = String.join(", ", imps);
                    }
                    tableModel.addRow(new Object[]{id, name, cat, improvements});
                    originalObjects.add(entry);
                }
            }

            currentFile = file;
            fileField.setText(file.getAbsolutePath());
            statusLabel.setText("Buildings loaded: " + file.getAbsolutePath());
            loadAnnotationColors();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Error loading file:\n" + ex.getMessage(),
                    "Load Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveFile(boolean saveAs) {
        if (currentFile == null || saveAs) {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(new FileNameExtensionFilter("JSON files (*.json)", "json"));
            if (currentFile != null) {
                chooser.setSelectedFile(currentFile);
            }
            if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
                return;
            }
            File chosen = chooser.getSelectedFile();
            currentFile = chosen.getName().endsWith(".json") ? chosen
                    : new File(chosen.getAbsolutePath() + ".json");
        }

        try {
            JsonObject root = new JsonObject();
            root.addProperty("version",  versionField.getText().trim());
            root.addProperty("language", (String) languageCombo.getSelectedItem());

            JsonArray buildings = new JsonArray();
            for (int r = 0; r < tableModel.getRowCount(); r++) {
                String id              = cellStr(r, 0);
                String name            = cellStr(r, 1);
                String category        = cellStr(r, 2);
                String improvementsStr = cellStr(r, 3);

                // Start from the original object to preserve numeric fields
                JsonObject original = r < originalObjects.size() ? originalObjects.get(r) : new JsonObject();
                JsonObject entry = new JsonObject();
                entry.addProperty("id",          id);
                entry.addProperty("name",        name);
                entry.addProperty("category",    category);
                // Copy preserved numeric fields in their original order
                for (String numField : new String[]{"minFloors", "maxFloors", "unitsPerFloor", "capacity", "percentage"}) {
                    if (original.has(numField)) {
                        entry.add(numField, original.get(numField));
                    }
                }
                JsonArray impsArray = new JsonArray();
                if (!improvementsStr.isEmpty()) {
                    for (String imp : improvementsStr.split(",")) {
                        String trimmed = imp.trim();
                        if (!trimmed.isEmpty()) {
                            impsArray.add(trimmed);
                        }
                    }
                }
                entry.add("improvements", impsArray);
                // Preserve attribute_modifiers and description if present in the original
                if (original.has("attribute_modifiers")) {
                    entry.add("attribute_modifiers", original.get("attribute_modifiers"));
                }
                if (original.has("description")) {
                    entry.add("description", original.get("description"));
                }
                buildings.add(entry);
            }
            root.add("buildings", buildings);

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            try (Writer writer = new FileWriter(currentFile)) {
                gson.toJson(root, writer);
            }
            statusLabel.setText("Buildings saved: " + currentFile.getAbsolutePath());
            fileField.setText(currentFile.getAbsolutePath());
            JOptionPane.showMessageDialog(this,
                    "File saved successfully.",
                    "Saved", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Error saving file:\n" + ex.getMessage(),
                    "Save Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void switchLanguage(String newLang) {
        if (currentFile == null) return;
        File newFile = EditorUtils.deriveFileForLanguage(currentFile, newLang);
        if (newFile == null) return;
        if (newFile.exists()) {
            loadFromFile(newFile);
        } else {
            tableModel.setRowCount(0);
            originalObjects.clear();
            versionField.setText("");
            currentFile = newFile;
            fileField.setText(newFile.getAbsolutePath());
            statusLabel.setText("New file (not yet saved): " + newFile.getAbsolutePath());
        }
    }

    private static DefaultTableModel createModel() {
        return new DefaultTableModel(new String[]{"ID", "Name", "Category", "Improvements"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return true;
            }
        };
    }

    private String cellStr(int row, int col) {
        Object val = tableModel.getValueAt(row, col);
        return val != null ? val.toString() : "";
    }

    // -------------------------------------------------------------------------
    // Annotation support
    // -------------------------------------------------------------------------

    /** Shows a popup menu with rating options at (x, y) relative to the table. */
    private void showAnnotationMenu(int row, int col, int x, int y) {
        String key = cellStr(row, 0);
        String existingRating = findExistingRating(key, tableModel.getColumnName(col));
        JPopupMenu menu = new JPopupMenu("Rate");
        for (String rating : RATINGS) {
            String ratingId = rating.toLowerCase().replace(' ', '_');
            JCheckBoxMenuItem item = new JCheckBoxMenuItem(rating, ratingId.equals(existingRating));
            item.addActionListener(e -> promptAndSaveAnnotation(row, col, ratingId));
            menu.add(item);
        }
        menu.addSeparator();
        JMenuItem clearItem = new JMenuItem("Clear");
        clearItem.addActionListener(e -> clearAnnotationsForItem(key));
        menu.add(clearItem);
        menu.show(table, x, y);
    }

    private File annotationFile() {
        return currentFile != null
                ? new File(currentFile.getParent(), "annotation.json")
                : new File("annotation.json");
    }

    private String annotationFilePath() {
        return currentFile != null ? currentFile.getAbsolutePath() : "";
    }

    private String findExistingRating(String key, String column) {
        File af = annotationFile();
        if (!af.exists()) return null;
        try (Reader r = Files.newBufferedReader(af.toPath(), StandardCharsets.UTF_8)) {
            JsonObject existing = new Gson().fromJson(r, JsonObject.class);
            if (existing == null || !existing.has("annotations")) return null;
            String lastRating = null;
            String fp = annotationFilePath();
            for (JsonElement el : existing.getAsJsonArray("annotations")) {
                JsonObject e = el.getAsJsonObject();
                JsonElement fileEl = e.get("file"), keyEl = e.get("key"), colEl = e.get("column");
                if (fileEl == null || keyEl == null || colEl == null) continue;
                if (fp.equals(fileEl.getAsString()) && key.equals(keyEl.getAsString())
                        && column.equals(colEl.getAsString())) {
                    JsonElement re = e.get("rating");
                    lastRating = re != null ? re.getAsString() : null;
                }
            }
            return lastRating;
        } catch (IOException | RuntimeException ex) {
            System.err.println("Could not read annotation.json for rating lookup: " + ex.getMessage());
            return null;
        }
    }

    private String findExistingComment(String key, String column) {
        File af = annotationFile();
        if (!af.exists()) return "";
        try (Reader r = Files.newBufferedReader(af.toPath(), StandardCharsets.UTF_8)) {
            JsonObject existing = new Gson().fromJson(r, JsonObject.class);
            if (existing == null || !existing.has("annotations")) return "";
            String lastComment = "";
            String fp = annotationFilePath();
            for (JsonElement el : existing.getAsJsonArray("annotations")) {
                JsonObject e = el.getAsJsonObject();
                JsonElement fileEl = e.get("file"), keyEl = e.get("key"), colEl = e.get("column");
                if (fileEl == null || keyEl == null || colEl == null) continue;
                if (fp.equals(fileEl.getAsString()) && key.equals(keyEl.getAsString())
                        && column.equals(colEl.getAsString())) {
                    JsonElement ce = e.get("comment");
                    lastComment = ce != null ? ce.getAsString() : "";
                }
            }
            return lastComment;
        } catch (IOException | RuntimeException ex) {
            System.err.println("Could not read annotation.json for comment lookup: " + ex.getMessage());
            return "";
        }
    }

    private void promptAndSaveAnnotation(int row, int col, String rating) {
        String existingComment = findExistingComment(cellStr(row, 0), tableModel.getColumnName(col));
        JTextArea textArea = new JTextArea(existingComment, 6, 50);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        JScrollPane scroll = new JScrollPane(textArea);
        scroll.setPreferredSize(new Dimension(520, 160));

        int result = JOptionPane.showConfirmDialog(
                this, scroll, "Annotation \u2013 " + rating + " (clear comment to remove)",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;
        saveAnnotation(cellStr(row, 0), tableModel.getColumnName(col), rating, textArea.getText().trim());
    }

    private void saveAnnotation(String key, String column, String rating, String comment) {
        File af = annotationFile();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonArray annotations = new JsonArray();
        String fp = annotationFilePath();
        if (af.exists()) {
            try (Reader r = Files.newBufferedReader(af.toPath(), StandardCharsets.UTF_8)) {
                JsonObject existing = gson.fromJson(r, JsonObject.class);
                if (existing != null && existing.has("annotations")) {
                    for (JsonElement el : existing.getAsJsonArray("annotations")) {
                        JsonObject e = el.getAsJsonObject();
                        JsonElement fileEl = e.get("file"), keyEl = e.get("key"), colEl = e.get("column");
                        if (fileEl == null || keyEl == null || colEl == null) {
                            annotations.add(e);
                            continue;
                        }
                        if (fp.equals(fileEl.getAsString()) && key.equals(keyEl.getAsString())
                                && column.equals(colEl.getAsString())) {
                            continue;
                        }
                        annotations.add(e);
                    }
                }
            } catch (IOException | RuntimeException ex) {
                System.err.println("Could not read annotation.json, starting fresh: " + ex.getMessage());
            }
        }
        if (!comment.isEmpty()) {
            JsonObject entry = new JsonObject();
            entry.addProperty("file",    fp);
            entry.addProperty("key",     key);
            entry.addProperty("column",  column);
            entry.addProperty("rating",  rating);
            entry.addProperty("comment", comment);
            annotations.add(entry);
        }
        JsonObject root = new JsonObject();
        root.add("annotations", annotations);
        try (Writer w = Files.newBufferedWriter(af.toPath(), StandardCharsets.UTF_8)) {
            gson.toJson(root, w);
            w.flush();
            statusLabel.setText("Annotation saved: " + af.getAbsolutePath());
            loadAnnotationColors();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Error saving annotation:\n" + ex.getMessage(),
                    "Annotation Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Removes all annotations for the given item key from annotation.json. */
    private void clearAnnotationsForItem(String key) {
        File af = annotationFile();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonArray annotations = new JsonArray();
        String fp = annotationFilePath();
        if (af.exists()) {
            try (Reader r = Files.newBufferedReader(af.toPath(), StandardCharsets.UTF_8)) {
                JsonObject existing = gson.fromJson(r, JsonObject.class);
                if (existing != null && existing.has("annotations")) {
                    for (JsonElement el : existing.getAsJsonArray("annotations")) {
                        JsonObject e = el.getAsJsonObject();
                        JsonElement fileEl = e.get("file");
                        JsonElement keyEl  = e.get("key");
                        if (fileEl == null || keyEl == null) {
                            annotations.add(e);
                            continue;
                        }
                        if (fp.equals(fileEl.getAsString()) && key.equals(keyEl.getAsString())) {
                            continue;
                        }
                        annotations.add(e);
                    }
                }
            } catch (IOException | RuntimeException ex) {
                JOptionPane.showMessageDialog(this,
                        "Could not read annotation.json:\n" + ex.getMessage(),
                        "Annotation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }
        JsonObject root = new JsonObject();
        root.add("annotations", annotations);
        try (Writer w = Files.newBufferedWriter(af.toPath(), StandardCharsets.UTF_8)) {
            gson.toJson(root, w);
            w.flush();
            statusLabel.setText("Annotations cleared: " + af.getAbsolutePath());
            loadAnnotationColors();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Error clearing annotation:\n" + ex.getMessage(),
                    "Annotation Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadAnnotationColors() {
        annotationRatings.clear();
        File af = annotationFile();
        if (af.exists()) {
            try (Reader r = Files.newBufferedReader(af.toPath(), StandardCharsets.UTF_8)) {
                JsonObject existing = new Gson().fromJson(r, JsonObject.class);
                if (existing != null && existing.has("annotations")) {
                    String fp = annotationFilePath();
                    for (JsonElement el : existing.getAsJsonArray("annotations")) {
                        JsonObject e = el.getAsJsonObject();
                        JsonElement fileEl   = e.get("file");
                        JsonElement keyEl    = e.get("key");
                        JsonElement colEl    = e.get("column");
                        JsonElement ratingEl = e.get("rating");
                        if (fileEl == null || keyEl == null || colEl == null || ratingEl == null) continue;
                        if (fp.equals(fileEl.getAsString())) {
                            annotationRatings.put(
                                    keyEl.getAsString() + "\0" + colEl.getAsString(),
                                    ratingEl.getAsString());
                        }
                    }
                }
            } catch (IOException | RuntimeException ex) {
                System.err.println("Could not read annotation.json for color highlighting: " + ex.getMessage());
            }
        }
        table.repaint();
    }
}

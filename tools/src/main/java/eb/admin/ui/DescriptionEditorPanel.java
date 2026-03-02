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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * Panel that displays and edits the contents of description_en.json.
 *
 * The table shows each location key and its "default" description text.
 * All other nested fields (time, attribute, gender, state) are preserved
 * transparently on load and save.
 */
public class DescriptionEditorPanel extends JPanel {

    private static final String DEFAULT_JSON_PATH = "assets/text/description_en.json";

    private final JTextField versionField = new JTextField(8);
    private final JTextField languageField = new JTextField(8);

    private final DefaultTableModel tableModel = createModel();
    private final JTable table = new JTable(tableModel) {
        @Override
        public boolean editCellAt(int row, int column, java.util.EventObject e) {
            if (e instanceof java.awt.event.MouseEvent
                    && ((java.awt.event.MouseEvent) e).getClickCount() >= 2
                    && tableModel.isCellEditable(row, column)) {
                SwingUtilities.invokeLater(() -> openMultilineEditor(row, column));
                return false;
            }
            return super.editCellAt(row, column, e);
        }
    };

    /** Parallel list – preserves the original JSON value (object or array) for each table row. */
    private final List<JsonElement> entryObjects = new ArrayList<>();

    private File currentFile;
    private final JLabel statusLabel;

    public DescriptionEditorPanel(JLabel statusLabel) {
        this.statusLabel = statusLabel;
        buildUI();
        tryLoadDefaultFile();
    }

    // -------------------------------------------------------------------------
    // UI construction
    // -------------------------------------------------------------------------

    private void buildUI() {
        setLayout(new BorderLayout(0, 4));
        add(buildNorthPanel(), BorderLayout.NORTH);

        table.setRowHeight(26);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
        table.getColumnModel().getColumn(0).setPreferredWidth(200);
        table.getColumnModel().getColumn(1).setPreferredWidth(700);

        JButton addBtn    = new JButton("Add Row");
        JButton deleteBtn = new JButton("Delete Row");
        JButton saveBtn   = new JButton("Save");

        addBtn.addActionListener((ActionEvent e) -> {
            int cols = tableModel.getColumnCount();
            tableModel.addRow(new Object[cols]);
            entryObjects.add(new JsonObject());
            int last = tableModel.getRowCount() - 1;
            table.scrollRectToVisible(table.getCellRect(last, 0, true));
            table.setRowSelectionInterval(last, last);
        });

        deleteBtn.addActionListener((ActionEvent e) -> {
            int row = table.getSelectedRow();
            if (row >= 0) {
                tableModel.removeRow(row);
                entryObjects.remove(row);
            } else {
                showScrollableError(this,
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

        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

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

    /** Opens a scrollable multiline dialog pre-filled with the current cell value. */
    private void openMultilineEditor(int row, int col) {
        String current = cellStr(row, col);
        String colName = tableModel.getColumnName(col);

        JTextArea textArea = new JTextArea(current, 8, 50);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        JScrollPane scroll = new JScrollPane(textArea);
        scroll.setPreferredSize(new Dimension(520, 200));

        int result = JOptionPane.showConfirmDialog(
                this, scroll, "Edit: " + colName,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            tableModel.setValueAt(textArea.getText(), row, col);
        }
    }

    private JPanel buildNorthPanel() {
        JPanel meta = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        meta.setBorder(BorderFactory.createTitledBorder("File Metadata"));
        meta.add(new JLabel("Version:"));
        meta.add(versionField);
        meta.add(Box.createHorizontalStrut(12));
        meta.add(new JLabel("Language:"));
        meta.add(languageField);

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

            versionField.setText(root.has("version") ? root.get("version").getAsString() : "");
            languageField.setText(root.has("language") ? root.get("language").getAsString() : "");

            // First pass: collect all variant keys in encounter order (LinkedHashSet for O(1) dedup)
            LinkedHashSet<String> seen = new LinkedHashSet<>();
            if (root.has("descriptions")) {
                for (Map.Entry<String, JsonElement> descEntry : root.getAsJsonObject("descriptions").entrySet()) {
                    JsonObject entry = firstObjectOf(descEntry.getValue());
                    for (Map.Entry<String, JsonElement> field : entry.entrySet()) {
                        if ("default".equals(field.getKey())) continue;
                        if (field.getValue().isJsonObject()) {
                            for (String subKey : field.getValue().getAsJsonObject().keySet()) {
                                seen.add(field.getKey() + "." + subKey);
                            }
                        }
                    }
                }
            }
            List<String> variantKeys = new ArrayList<>(seen);

            // Rebuild columns: Key, Default Description, [variant…]
            List<String> columnNames = new ArrayList<>();
            columnNames.add("Key");
            columnNames.add("Default Description");
            columnNames.addAll(variantKeys);
            tableModel.setColumnIdentifiers(columnNames.toArray());

            // Set column widths
            table.getColumnModel().getColumn(0).setPreferredWidth(200);
            table.getColumnModel().getColumn(1).setPreferredWidth(400);
            for (int i = 2; i < columnNames.size(); i++) {
                table.getColumnModel().getColumn(i).setPreferredWidth(300);
            }

            tableModel.setRowCount(0);
            entryObjects.clear();

            if (root.has("descriptions")) {
                for (Map.Entry<String, JsonElement> descEntry : root.getAsJsonObject("descriptions").entrySet()) {
                    JsonElement value = descEntry.getValue();
                    JsonObject entry = firstObjectOf(value);
                    Object[] row = new Object[columnNames.size()];
                    row[0] = descEntry.getKey();
                    row[1] = entry.has("default") ? entry.get("default").getAsString() : "";
                    for (int i = 2; i < columnNames.size(); i++) {
                        String[] parts = columnNames.get(i).split("\\.", 2);
                        if (parts.length == 2 && entry.has(parts[0]) && entry.get(parts[0]).isJsonObject()) {
                            JsonObject grp = entry.get(parts[0]).getAsJsonObject();
                            row[i] = grp.has(parts[1]) ? grp.get(parts[1]).getAsString() : "";
                        } else {
                            row[i] = "";
                        }
                    }
                    tableModel.addRow(row);
                    entryObjects.add(value);
                }
            }

            currentFile = file;
            statusLabel.setText("Descriptions loaded: " + file.getAbsolutePath());
        } catch (Exception ex) {
            showScrollableError(this,
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
            root.addProperty("version", versionField.getText().trim());
            root.addProperty("language", languageField.getText().trim());

            JsonObject descriptions = new JsonObject();
            int colCount = tableModel.getColumnCount();
            for (int r = 0; r < tableModel.getRowCount(); r++) {
                String key = cellStr(r, 0);
                if (key.isEmpty()) {
                    continue;
                }
                JsonElement original = r < entryObjects.size() ? entryObjects.get(r) : new JsonObject();
                if (original.isJsonArray()) {
                    JsonElement arrayCopy = original.deepCopy();
                    JsonObject targetObj = null;
                    for (JsonElement item : arrayCopy.getAsJsonArray()) {
                        if (item.isJsonObject()) {
                            targetObj = item.getAsJsonObject();
                            break;
                        }
                    }
                    if (targetObj == null) {
                        targetObj = new JsonObject();
                        arrayCopy.getAsJsonArray().add(targetObj);
                    }
                    targetObj.addProperty("default", cellStr(r, 1));
                    applyVariantColumns(targetObj, r, colCount);
                    descriptions.add(key, arrayCopy);
                } else {
                    JsonObject entry = original.isJsonObject()
                            ? original.deepCopy().getAsJsonObject()
                            : new JsonObject();
                    entry.addProperty("default", cellStr(r, 1));
                    applyVariantColumns(entry, r, colCount);
                    descriptions.add(key, entry);
                }
            }
            root.add("descriptions", descriptions);

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            try (Writer writer = new FileWriter(currentFile)) {
                gson.toJson(root, writer);
            }
            statusLabel.setText("Descriptions saved: " + currentFile.getAbsolutePath());
            JOptionPane.showMessageDialog(this,
                    "File saved successfully.",
                    "Saved", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            showScrollableError(this,
                    "Error saving file:\n" + ex.getMessage(),
                    "Save Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Writes edited variant column values (col ≥ 2) back into the given JSON object.
     * If a cell value is non-empty the corresponding sub-key is set; if it is empty
     * the sub-key is removed, and if the parent group object becomes empty after removal
     * the whole group key is also removed from the entry.
     */
    private void applyVariantColumns(JsonObject entry, int row, int colCount) {
        for (int c = 2; c < colCount; c++) {
            String[] parts = tableModel.getColumnName(c).split("\\.", 2);
            if (parts.length != 2) continue;
            String groupKey = parts[0], subKey = parts[1];
            String val = cellStr(row, c);
            JsonObject grp = entry.has(groupKey) && entry.get(groupKey).isJsonObject()
                    ? entry.get(groupKey).getAsJsonObject()
                    : new JsonObject();
            if (!val.isEmpty()) {
                grp.addProperty(subKey, val);
                entry.add(groupKey, grp);
            } else if (grp.has(subKey)) {
                grp.remove(subKey);
                if (!grp.entrySet().isEmpty()) {
                    entry.add(groupKey, grp);
                } else {
                    entry.remove(groupKey);
                }
            }
        }
    }

    /**
     * Returns the first JsonObject from a JsonElement.
     * If the element is a JsonArray, returns the first JsonObject element (if any).
     * Falls back to an empty JsonObject for unrecognized shapes.
     */
    private static JsonObject firstObjectOf(JsonElement element) {
        if (element.isJsonObject()) {
            return element.getAsJsonObject();
        }
        if (element.isJsonArray()) {
            for (JsonElement item : element.getAsJsonArray()) {
                if (item.isJsonObject()) {
                    return item.getAsJsonObject();
                }
            }
        }
        return new JsonObject();
    }

    /**
     * Shows an error dialog with the message in a scrollable text area so that
     * very long messages do not run off the screen.
     */
    private static void showScrollableError(Component parent, String message, String title, int messageType) {
        JTextArea textArea = new JTextArea(message);
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setFont(UIManager.getFont("Label.font"));
        textArea.setBackground(UIManager.getColor("OptionPane.background"));
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(500, 160));
        scrollPane.setBorder(null);
        JOptionPane.showMessageDialog(parent, scrollPane, title, messageType);
    }

    private static DefaultTableModel createModel() {
        return new DefaultTableModel(new String[]{"Key", "Default Description"}, 0) {
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
    // Annotation system
    // -------------------------------------------------------------------------

    private static final String[] RATINGS = {"Excellent", "Good", "Sufficient", "Bad", "Very Bad"};

    /** Shows a popup menu with rating options at (x, y) relative to the table. */
    private void showAnnotationMenu(int row, int col, int x, int y) {
        JPopupMenu menu = new JPopupMenu("Rate");
        for (String rating : RATINGS) {
            String ratingId = rating.toLowerCase().replace(' ', '_');
            JMenuItem item = new JMenuItem(rating);
            item.addActionListener(e -> promptAndSaveAnnotation(row, col, ratingId));
            menu.add(item);
        }
        menu.show(table, x, y);
    }

    /** Asks for an optional comment using a multiline editor then persists the annotation. */
    private void promptAndSaveAnnotation(int row, int col, String rating) {
        JTextArea textArea = new JTextArea(6, 50);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        JScrollPane scroll = new JScrollPane(textArea);
        scroll.setPreferredSize(new Dimension(520, 160));

        int result = JOptionPane.showConfirmDialog(
                this, scroll, "Add Annotation – " + rating + " (comment optional)",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return; // cancelled
        }
        saveAnnotation(cellStr(row, 0), tableModel.getColumnName(col), rating, textArea.getText().trim());
    }

    /**
     * Appends an annotation entry to annotation.json located next to the currently
     * open file (or in the working directory when no file is loaded).
     * Each entry records: file, key, column, rating, comment.
     */
    private void saveAnnotation(String key, String column, String rating, String comment) {
        File annotationFile = currentFile != null
                ? new File(currentFile.getParent(), "annotation.json")
                : new File("annotation.json");

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonArray annotations = new JsonArray();

        if (annotationFile.exists()) {
            try (Reader r = Files.newBufferedReader(annotationFile.toPath(), StandardCharsets.UTF_8)) {
                JsonObject existing = gson.fromJson(r, JsonObject.class);
                if (existing != null && existing.has("annotations")) {
                    annotations = existing.get("annotations").getAsJsonArray();
                }
            } catch (IOException | RuntimeException ex) {
                System.err.println("Could not read annotation.json, starting fresh: " + ex.getMessage());
            }
        }

        JsonObject entry = new JsonObject();
        entry.addProperty("file",    currentFile != null ? currentFile.getAbsolutePath() : "");
        entry.addProperty("key",     key);
        entry.addProperty("column",  column);
        entry.addProperty("rating",  rating);
        entry.addProperty("comment", comment);
        annotations.add(entry);

        JsonObject root = new JsonObject();
        root.add("annotations", annotations);

        try (Writer w = Files.newBufferedWriter(annotationFile.toPath(), StandardCharsets.UTF_8)) {
            gson.toJson(root, w);
            statusLabel.setText("Annotation saved: " + annotationFile.getAbsolutePath());
        } catch (Exception ex) {
            showScrollableError(this,
                    "Error saving annotation:\n" + ex.getMessage(),
                    "Annotation Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}

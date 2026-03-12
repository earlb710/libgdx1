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
 * The table shows each building's ID, Name, Category, Icon, and
 * Improvements (as a comma-separated list).  All other numeric fields
 * (minFloors, maxFloors, unitsPerFloor, capacity, percentage) are
 * preserved transparently on load and save.  Annotation support mirrors
 * the pattern used by {@link CompanyTypesEditorPanel}.
 */
public class BuildingsEditorPanel extends JPanel {

    private static final String DEFAULT_JSON_PATH = "assets/text/buildings_en.json";
    private static final Color ANNOTATION_FOREGROUND = new Color(0, 0, 139);
    private static final String[] RATINGS = {"Excellent", "Good", "Sufficient", "Bad", "Very Bad"};

    /**
     * Description sub-field keys (relative to the "description" object).
     * Each entry maps to a table column starting at {@link #DESC_COL_START}.
     */
    private static final String[] DESC_COL_KEYS = {
        "default",
        "time.morning", "time.afternoon", "time.evening", "time.night",
        "gender.male", "gender.female",
        "state.good", "state.normal", "state.bad",
        "attribute.AGILITY", "attribute.CHARISMA", "attribute.EMPATHY",
        "attribute.INTELLIGENCE", "attribute.INTIMIDATION", "attribute.INTUITION",
        "attribute.MEMORY", "attribute.PERCEPTION", "attribute.STAMINA",
        "attribute.STEALTH", "attribute.STRENGTH"
    };
    /** Column index of the first description sub-field column. */
    private static final int DESC_COL_START = 11;

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
        public boolean editCellAt(int row, int column, java.util.EventObject e) {
            if (e instanceof java.awt.event.MouseEvent
                    && ((java.awt.event.MouseEvent) e).getClickCount() >= 2
                    && column >= DESC_COL_START
                    && tableModel.isCellEditable(row, column)) {
                SwingUtilities.invokeLater(() -> openMultilineEditor(row, column));
                return false;
            }
            return super.editCellAt(row, column, e);
        }

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
        table.getColumnModel().getColumn(0).setPreferredWidth(160);   // ID
        table.getColumnModel().getColumn(1).setPreferredWidth(180);   // Name
        table.getColumnModel().getColumn(2).setPreferredWidth(120);   // Category
        table.getColumnModel().getColumn(3).setPreferredWidth(180);   // Icon
        table.getColumnModel().getColumn(4).setPreferredWidth(80);    // Min Floors
        table.getColumnModel().getColumn(5).setPreferredWidth(80);    // Max Floors
        table.getColumnModel().getColumn(6).setPreferredWidth(90);    // Units/Floor
        table.getColumnModel().getColumn(7).setPreferredWidth(80);    // Capacity
        table.getColumnModel().getColumn(8).setPreferredWidth(90);    // Percentage
        table.getColumnModel().getColumn(9).setPreferredWidth(320);   // Improvements
        table.getColumnModel().getColumn(10).setPreferredWidth(260);  // Attr Modifiers
        // description sub-columns (cols 11-31)
        for (int i = DESC_COL_START; i < DESC_COL_START + DESC_COL_KEYS.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(300);
        }
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        JButton addBtn    = new JButton("Add Row");
        JButton deleteBtn = new JButton("Delete Row");
        JButton saveBtn   = new JButton("Save");

        addBtn.addActionListener((ActionEvent e) -> {
            tableModel.addRow(new Object[]{
                    "", "", "", "", "", "", "", "", "", "", "",  // 11 fixed cols
                    "", "", "", "", "", "", "", "", "", "",  // desc cols 11-20
                    "", "", "", "", "", "", "", "", "", "", ""  // desc cols 21-31
            });
            originalObjects.add(new JsonObject());
            int last = tableModel.getRowCount() - 1;
            table.scrollRectToVisible(table.getCellRect(last, 0, true));
            table.setRowSelectionInterval(last, last);
        });

        deleteBtn.addActionListener((ActionEvent e) -> {
            if (table.isEditing()) {
                table.getCellEditor().cancelCellEditing();
            }
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
                    String id            = entry.has("id")           ? entry.get("id").getAsString()           : "";
                    String name          = entry.has("name")         ? entry.get("name").getAsString()         : "";
                    String cat           = entry.has("category")     ? entry.get("category").getAsString()     : "";
                    String icon          = entry.has("icon")         ? entry.get("icon").getAsString()         : "";
                    String minFloors     = entry.has("minFloors")    ? entry.get("minFloors").getAsString()    : "";
                    String maxFloors     = entry.has("maxFloors")    ? entry.get("maxFloors").getAsString()    : "";
                    String unitsPerFloor = entry.has("unitsPerFloor") ? entry.get("unitsPerFloor").getAsString() : "";
                    String capacity      = entry.has("capacity")     ? entry.get("capacity").getAsString()     : "";
                    String percentage    = entry.has("percentage")   ? entry.get("percentage").getAsString()   : "";
                    String improvements  = "";
                    if (entry.has("improvements") && entry.get("improvements").isJsonArray()) {
                        List<String> imps = new ArrayList<>();
                        for (JsonElement imp : entry.getAsJsonArray("improvements")) {
                            if (imp != null && !imp.isJsonNull()) {
                                imps.add(imp.getAsString());
                            }
                        }
                        improvements = String.join(", ", imps);
                    }
                    // Attr modifiers: display as flat "KEY": value pairs without braces
                    String attrModifiers = "";
                    if (entry.has("attribute_modifiers") && entry.get("attribute_modifiers").isJsonObject()) {
                        attrModifiers = attrModToFlatString(entry.getAsJsonObject("attribute_modifiers"));
                    }
                    // Description: one cell per sub-field
                    JsonObject desc = entry.has("description") && entry.get("description").isJsonObject()
                            ? entry.getAsJsonObject("description") : null;
                    Object[] rowData = new Object[DESC_COL_START + DESC_COL_KEYS.length];
                    rowData[0] = id;
                    rowData[1] = name;
                    rowData[2] = cat;
                    rowData[3] = icon;
                    rowData[4] = minFloors;
                    rowData[5] = maxFloors;
                    rowData[6] = unitsPerFloor;
                    rowData[7] = capacity;
                    rowData[8] = percentage;
                    rowData[9] = improvements;
                    rowData[10] = attrModifiers;
                    for (int i = 0; i < DESC_COL_KEYS.length; i++) {
                        rowData[DESC_COL_START + i] = descFieldValue(desc, DESC_COL_KEYS[i]);
                    }
                    tableModel.addRow(rowData);
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
            Gson parseGson = new Gson();
            for (int r = 0; r < tableModel.getRowCount(); r++) {
                String id               = cellStr(r, 0);
                String name             = cellStr(r, 1);
                String category         = cellStr(r, 2);
                String icon             = cellStr(r, 3);
                String minFloorsStr     = cellStr(r, 4);
                String maxFloorsStr     = cellStr(r, 5);
                String unitsPerFloorStr = cellStr(r, 6);
                String capacityStr      = cellStr(r, 7);
                String percentageStr    = cellStr(r, 8);
                String improvementsStr  = cellStr(r, 9);
                String attrModStr       = cellStr(r, 10);

                JsonObject original = r < originalObjects.size() ? originalObjects.get(r) : new JsonObject();
                JsonObject entry = new JsonObject();
                entry.addProperty("id",       id);
                entry.addProperty("name",     name);
                entry.addProperty("category", category);
                if (!icon.isEmpty()) entry.addProperty("icon", icon);

                // Numeric fields – use the edited table value, fall back to original
                addIntField(entry,    "minFloors",     minFloorsStr,     original);
                addIntField(entry,    "maxFloors",     maxFloorsStr,     original);
                addIntField(entry,    "unitsPerFloor", unitsPerFloorStr, original);
                addIntField(entry,    "capacity",      capacityStr,      original);
                addDoubleField(entry, "percentage",    percentageStr,    original);

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

                // Attribute modifiers – flat string "KEY": value → JSON object
                if (!attrModStr.trim().isEmpty()) {
                    try {
                        JsonObject parsed = parseGson.fromJson("{" + attrModStr.trim() + "}", JsonObject.class);
                        if (parsed != null) {
                            entry.add("attribute_modifiers", parsed);
                        } else if (original.has("attribute_modifiers")) {
                            entry.add("attribute_modifiers", original.get("attribute_modifiers"));
                        }
                    } catch (Exception ex) {
                        if (original.has("attribute_modifiers")) {
                            entry.add("attribute_modifiers", original.get("attribute_modifiers"));
                        }
                    }
                } else if (original.has("attribute_modifiers")) {
                    entry.add("attribute_modifiers", original.get("attribute_modifiers"));
                }

                // Description – reconstruct from individual sub-field columns
                JsonObject descObj = new JsonObject();
                for (int i = 0; i < DESC_COL_KEYS.length; i++) {
                    String val = cellStr(r, DESC_COL_START + i);
                    if (!val.trim().isEmpty()) {
                        String[] parts = DESC_COL_KEYS[i].split("\\.", 2);
                        if (parts.length == 1) {
                            descObj.addProperty(parts[0], val);
                        } else {
                            if (!descObj.has(parts[0]) || !descObj.get(parts[0]).isJsonObject()) {
                                descObj.add(parts[0], new JsonObject());
                            }
                            descObj.getAsJsonObject(parts[0]).addProperty(parts[1], val);
                        }
                    }
                }
                if (descObj.size() > 0) {
                    entry.add("description", descObj);
                } else if (original.has("description")) {
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
        return new DefaultTableModel(
                new String[]{
                        "ID", "Name", "Category", "Icon",
                        "Min Floors", "Max Floors", "Units/Floor", "Capacity", "Percentage",
                        "Improvements", "Attr Modifiers",
                        // description sub-columns (no "desc." prefix)
                        "default",
                        "time.morning", "time.afternoon",
                        "time.evening", "time.night",
                        "gender.male", "gender.female",
                        "state.good", "state.normal", "state.bad",
                        "attribute.AGILITY", "attribute.CHARISMA",
                        "attribute.EMPATHY", "attribute.INTELLIGENCE",
                        "attribute.INTIMIDATION", "attribute.INTUITION",
                        "attribute.MEMORY", "attribute.PERCEPTION",
                        "attribute.STAMINA", "attribute.STEALTH",
                        "attribute.STRENGTH"
                }, 0) {
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

    /** Opens a scrollable multiline dialog pre-filled with the current cell value. */
    private void openMultilineEditor(int row, int col) {
        String current = cellStr(row, col);
        String colName = tableModel.getColumnName(col);

        JTextArea textArea = new JTextArea(current, 8, 50);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        JScrollPane scroll = new JScrollPane(textArea);
        scroll.setPreferredSize(new Dimension(520, 200));

        Window owner = SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(owner, "Edit: " + colName, Dialog.ModalityType.APPLICATION_MODAL);

        JButton okBtn     = new JButton("OK");
        JButton cancelBtn = new JButton("Cancel");

        okBtn.addActionListener(e -> {
            tableModel.setValueAt(textArea.getText(), row, col);
            dialog.dispose();
        });
        cancelBtn.addActionListener(e -> dialog.dispose());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 4));
        buttons.add(okBtn);
        buttons.add(cancelBtn);

        dialog.setLayout(new BorderLayout(0, 4));
        dialog.add(scroll,   BorderLayout.CENTER);
        dialog.add(buttons,  BorderLayout.SOUTH);
        dialog.getRootPane().setDefaultButton(okBtn);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private static void addIntField(JsonObject entry, String field, String raw, JsonObject original) {
        if (!raw.trim().isEmpty()) {
            try {
                entry.addProperty(field, Integer.parseInt(raw.trim()));
                return;
            } catch (NumberFormatException ignored) { /* fall through */ }
        }
        if (original.has(field)) {
            entry.add(field, original.get(field));
        }
    }

    private static void addDoubleField(JsonObject entry, String field, String raw, JsonObject original) {
        if (!raw.trim().isEmpty()) {
            try {
                entry.addProperty(field, Double.parseDouble(raw.trim()));
                return;
            } catch (NumberFormatException ignored) { /* fall through */ }
        }
        if (original.has(field)) {
            entry.add(field, original.get(field));
        }
    }

    /**
     * Converts an attribute_modifiers JSON object to a flat display string.
     * E.g. {"INTELLIGENCE": 3, "MEMORY": 2} → "INTELLIGENCE": 3, "MEMORY": 2
     * Keys are sorted alphabetically for consistent display order.
     * JsonElement.toString() is used to preserve correct JSON types (numbers
     * without extra quotes, strings with quotes) for round-trip save/load.
     */
    private static String attrModToFlatString(JsonObject attrMod) {
        StringBuilder sb = new StringBuilder();
        attrMod.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(kv -> {
                    if (sb.length() > 0) sb.append(", ");
                    sb.append('"').append(kv.getKey()).append("\": ").append(kv.getValue().toString());
                });
        return sb.toString();
    }

    /**
     * Reads a single leaf value from the description object using a dotted key such as
     * "default", "time.morning", "gender.male", "attribute.AGILITY".
     */
    private static String descFieldValue(JsonObject desc, String key) {
        if (desc == null) return "";
        String[] parts = key.split("\\.", 2);
        if (parts.length == 1) {
            JsonElement el = desc.get(parts[0]);
            return (el != null && !el.isJsonNull() && el.isJsonPrimitive()) ? el.getAsString() : "";
        }
        JsonElement parent = desc.get(parts[0]);
        if (parent == null || !parent.isJsonObject()) return "";
        JsonElement el = parent.getAsJsonObject().get(parts[1]);
        return (el != null && !el.isJsonNull() && el.isJsonPrimitive()) ? el.getAsString() : "";
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

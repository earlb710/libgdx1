package eb.admin.ui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import eb.admin.model.CategoryData;
import eb.admin.model.CategoryEntry;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Main AWT/Swing screen for the Game Admin tool.
 * Allows viewing and editing of all category sections inside category_en.json.
 *
 * Tabs:
 *   1. Building Categories  – code, description, color
 *   2. Item Categories      – code, description
 *   3. Evidence Categories  – code, description
 *   4. Case Types           – code, description
 *   5. Descriptions         – key, default description text (description_en.json)
 */
public class CategoryEditorScreen extends JFrame {

    private static final String WINDOW_TITLE = "Game Admin – Category Editor";
    private static final String DEFAULT_JSON_PATH = "assets/text/category_en.json";
    private static final Color ANNOTATION_FOREGROUND = new Color(0, 0, 139);

    // Metadata fields
    private final JTextField versionField = new JTextField(8);
    private final JTextField languageField = new JTextField(8);

    // Tab pane and table models
    private final JTabbedPane tabbedPane = new JTabbedPane();
    private final DefaultTableModel buildingModel = createModel(new String[]{"Code", "Description", "Color"});
    private final DefaultTableModel itemModel     = createModel(new String[]{"Code", "Description"});
    private final DefaultTableModel evidenceModel = createModel(new String[]{"Code", "Description"});
    private final DefaultTableModel caseModel     = createModel(new String[]{"Code", "Description"});

    // Status bar
    private final JLabel statusLabel = new JLabel("No file loaded – use File › Open to load a JSON file.");

    private final DescriptionEditorPanel descPanel     = new DescriptionEditorPanel(statusLabel);

    private File currentFile;

    // Annotation support for category tables
    private static final String[] RATINGS = {"Excellent", "Good", "Sufficient", "Bad", "Very Bad"};
    private final Map<String, String> annotationRatings = new HashMap<>();
    /** All category JTables registered during buildUI – used to repaint after annotation changes. */
    private final List<JTable> categoryTables = new ArrayList<>();

    public CategoryEditorScreen() {
        super(WINDOW_TITLE);
        buildUI();
        tryLoadDefaultFile();
    }

    // -------------------------------------------------------------------------
    // UI construction
    // -------------------------------------------------------------------------

    private void buildUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(980, 640);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(0, 4));

        setJMenuBar(buildMenuBar());

        // Inner category sub-tabs (Building / Item / Evidence / Case Types)
        JTabbedPane categoryTabs = new JTabbedPane();
        categoryTabs.addTab("Building Categories", buildTabPanel(buildingModel, true));
        categoryTabs.addTab("Item Categories",     buildTabPanel(itemModel,     false));
        categoryTabs.addTab("Evidence Categories", buildTabPanel(evidenceModel, false));
        categoryTabs.addTab("Case Types",          buildTabPanel(caseModel,     false));

        // "Categories" top-level panel: meta (version/lang) + inner sub-tabs
        JPanel categoriesPanel = new JPanel(new BorderLayout(0, 4));
        categoriesPanel.add(buildMetaPanel(), BorderLayout.NORTH);
        categoriesPanel.add(categoryTabs,     BorderLayout.CENTER);

        // Outer tabs: "Categories" and "Descriptions"
        tabbedPane.addTab("Categories",   categoriesPanel);
        tabbedPane.addTab("Descriptions", descPanel);
        add(tabbedPane, BorderLayout.CENTER);

        statusLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        add(statusLabel, BorderLayout.SOUTH);
    }

    private JMenuBar buildMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");

        JMenuItem openItem   = new JMenuItem("Open…");
        JMenuItem saveItem   = new JMenuItem("Save");
        JMenuItem saveAsItem = new JMenuItem("Save As…");
        JMenuItem exitItem   = new JMenuItem("Exit");

        openItem.addActionListener((ActionEvent e) -> openFile());
        saveItem.addActionListener((ActionEvent e) -> saveFile(false));
        saveAsItem.addActionListener((ActionEvent e) -> saveFile(true));
        exitItem.addActionListener((ActionEvent e) -> System.exit(0));

        fileMenu.add(openItem);
        fileMenu.add(saveItem);
        fileMenu.add(saveAsItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);

        menuBar.add(fileMenu);
        return menuBar;
    }

    private JPanel buildMetaPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        panel.setBorder(BorderFactory.createTitledBorder("File Metadata"));
        panel.add(new JLabel("Version:"));
        panel.add(versionField);
        panel.add(Box.createHorizontalStrut(12));
        panel.add(new JLabel("Language:"));
        panel.add(languageField);
        return panel;
    }

    /**
     * Builds a panel containing an editable JTable and a button toolbar.
     *
     * @param model    the table model to use
     * @param hasColor whether the table has a Color column (building categories only)
     */
    private JPanel buildTabPanel(DefaultTableModel model, boolean hasColor) {
        JTable table = new JTable(model) {
            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int col) {
                Component c = super.prepareRenderer(renderer, row, col);
                if ("Color".equals(getModel().getColumnName(col))) {
                    return c; // preserve ColorCellRenderer's hex-color background
                }
                Color bg = getAnnotationColor((DefaultTableModel) getModel(), row, col);
                if (bg != null) {
                    c.setBackground(bg);
                    c.setForeground(ANNOTATION_FOREGROUND);
                } else if (!isRowSelected(row)) {
                    c.setBackground(getBackground());
                    c.setForeground(getForeground());
                }
                return c;
            }

            @Override
            public javax.swing.table.TableCellEditor getCellEditor(int row, int column) {
                javax.swing.table.TableCellEditor editor = super.getCellEditor(row, column);
                if (!"Color".equals(getModel().getColumnName(column))) {
                    Color bg = getAnnotationColor((DefaultTableModel) getModel(), row, column);
                    if (bg != null && editor instanceof DefaultCellEditor) {
                        ((DefaultCellEditor) editor).getComponent().setBackground(bg);
                    }
                }
                return editor;
            }
        };
        categoryTables.add(table);
        table.setRowHeight(26);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);

        if (hasColor) {
            // Set preferred widths
            table.getColumnModel().getColumn(0).setPreferredWidth(140);
            table.getColumnModel().getColumn(1).setPreferredWidth(520);
            table.getColumnModel().getColumn(2).setPreferredWidth(100);
            // Color swatch renderer
            table.getColumnModel().getColumn(2).setCellRenderer(new ColorCellRenderer());
        } else {
            table.getColumnModel().getColumn(0).setPreferredWidth(180);
            table.getColumnModel().getColumn(1).setPreferredWidth(680);
        }

        JScrollPane scrollPane = new JScrollPane(table);

        // Right-click opens annotation rating menu for any editable cell
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    JTable src = (JTable) e.getSource();
                    int row = src.rowAtPoint(e.getPoint());
                    int col = src.columnAtPoint(e.getPoint());
                    if (row >= 0 && col >= 0 && src.getModel().isCellEditable(row, col)) {
                        src.setRowSelectionInterval(row, row);
                        showAnnotationMenu(src, row, col, e.getX(), e.getY());
                    }
                }
            }
        });

        // Button toolbar
        JButton addBtn    = new JButton("Add Row");
        JButton deleteBtn = new JButton("Delete Row");
        JButton saveBtn   = new JButton("Save");

        addBtn.addActionListener((ActionEvent e) -> {
            if (hasColor) {
                model.addRow(new Object[]{"", "", ""});
            } else {
                model.addRow(new Object[]{"", ""});
            }
            int last = model.getRowCount() - 1;
            table.scrollRectToVisible(table.getCellRect(last, 0, true));
            table.setRowSelectionInterval(last, last);
        });

        deleteBtn.addActionListener((ActionEvent e) -> {
            int row = table.getSelectedRow();
            if (row >= 0) {
                model.removeRow(row);
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

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(buttons,    BorderLayout.SOUTH);
        return panel;
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
            Gson gson = new Gson();
            CategoryData data = gson.fromJson(reader, CategoryData.class);

            versionField.setText(data.getVersion() != null ? data.getVersion() : "");
            languageField.setText(data.getLanguage() != null ? data.getLanguage() : "");

            populateModel(buildingModel, data.getBuilding_categories(), true);
            populateModel(itemModel,     data.getItem_categories(),     false);
            populateModel(evidenceModel, data.getEvidence_categories(), false);
            populateModel(caseModel,     data.getCase_types(),          false);

            currentFile = file;
            setTitle(WINDOW_TITLE + " – " + file.getName());
            statusLabel.setText("Loaded: " + file.getAbsolutePath());
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
            CategoryData data = buildCategoryData();
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            try (Writer writer = new FileWriter(currentFile)) {
                gson.toJson(data, writer);
            }
            setTitle(WINDOW_TITLE + " – " + currentFile.getName());
            statusLabel.setText("Saved: " + currentFile.getAbsolutePath());
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
    // Model helpers
    // -------------------------------------------------------------------------

    private static DefaultTableModel createModel(String[] columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return true;
            }
        };
    }

    private static void populateModel(DefaultTableModel model, List<CategoryEntry> entries, boolean hasColor) {
        model.setRowCount(0);
        if (entries == null) return;
        for (CategoryEntry e : entries) {
            if (hasColor) {
                model.addRow(new Object[]{
                        nvl(e.getCode()),
                        nvl(e.getDescription()),
                        nvl(e.getColor())
                });
            } else {
                model.addRow(new Object[]{
                        nvl(e.getCode()),
                        nvl(e.getDescription())
                });
            }
        }
    }

    private CategoryData buildCategoryData() {
        CategoryData data = new CategoryData();
        data.setVersion(versionField.getText().trim());
        data.setLanguage(languageField.getText().trim());
        data.setBuilding_categories(modelToEntries(buildingModel, true));
        data.setItem_categories(modelToEntries(itemModel, false));
        data.setEvidence_categories(modelToEntries(evidenceModel, false));
        data.setCase_types(modelToEntries(caseModel, false));
        return data;
    }

    private static List<CategoryEntry> modelToEntries(DefaultTableModel model, boolean hasColor) {
        List<CategoryEntry> list = new ArrayList<>();
        for (int r = 0; r < model.getRowCount(); r++) {
            CategoryEntry entry = new CategoryEntry();
            entry.setCode(cellStr(model, r, 0));
            entry.setDescription(cellStr(model, r, 1));
            if (hasColor) {
                entry.setColor(cellStr(model, r, 2));
            }
            list.add(entry);
        }
        return list;
    }

    private static String cellStr(DefaultTableModel model, int row, int col) {
        Object val = model.getValueAt(row, col);
        return val != null ? val.toString() : "";
    }

    private static String nvl(String s) {
        return s != null ? s : "";
    }

    // -------------------------------------------------------------------------
    // Annotation support for category tables
    // -------------------------------------------------------------------------

    /**
     * Returns the annotation background color for the cell at (row, column) in
     * the given model, or {@code null} if the cell has no annotation.
     */
    private Color getAnnotationColor(DefaultTableModel m, int row, int column) {
        Object keyVal = m.getValueAt(row, 0);
        String key = keyVal != null ? keyVal.toString() : "";
        String colName = m.getColumnName(column);
        String ratingId = annotationRatings.get(key + "\0" + colName);
        return DescriptionEditorPanel.ratingToColor(ratingId);
    }

    private File annotationFile() {
        return currentFile != null
                ? new File(currentFile.getParent(), "annotation.json")
                : new File("annotation.json");
    }

    private String annotationFilePath() {
        return currentFile != null ? currentFile.getAbsolutePath() : "";
    }

    /** Shows a popup menu with rating options at (x, y) relative to the given table. */
    private void showAnnotationMenu(JTable table, int row, int col, int x, int y) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        String key = cellStr(model, row, 0);
        String column = model.getColumnName(col);
        String existingRating = findExistingRating(key, column);
        JPopupMenu menu = new JPopupMenu("Rate");
        for (String rating : RATINGS) {
            String ratingId = rating.toLowerCase().replace(' ', '_');
            JCheckBoxMenuItem item = new JCheckBoxMenuItem(rating, ratingId.equals(existingRating));
            item.addActionListener(e -> promptAndSaveAnnotation(key, column, ratingId));
            menu.add(item);
        }
        menu.addSeparator();
        JMenuItem clearItem = new JMenuItem("Clear");
        clearItem.addActionListener(e -> clearAnnotationsForItem(key));
        menu.add(clearItem);
        menu.show(table, x, y);
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
                    for (JsonElement el : existing.get("annotations").getAsJsonArray()) {
                        JsonObject e = el.getAsJsonObject();
                        JsonElement fileEl = e.get("file");
                        JsonElement keyEl  = e.get("key");
                        if (fileEl == null || keyEl == null) {
                            annotations.add(e);
                            continue;
                        }
                        if (fp.equals(fileEl.getAsString()) && key.equals(keyEl.getAsString())) {
                            continue; // drop all entries for this item
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

    private String findExistingRating(String key, String column) {
        File af = annotationFile();
        if (!af.exists()) return null;
        try (Reader r = Files.newBufferedReader(af.toPath(), StandardCharsets.UTF_8)) {
            JsonObject existing = new Gson().fromJson(r, JsonObject.class);
            if (existing == null || !existing.has("annotations")) return null;
            String lastRating = null;
            String fp = annotationFilePath();
            for (JsonElement el : existing.get("annotations").getAsJsonArray()) {
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
            for (JsonElement el : existing.get("annotations").getAsJsonArray()) {
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

    private void promptAndSaveAnnotation(String key, String column, String rating) {
        String existingComment = findExistingComment(key, column);
        JTextArea textArea = new JTextArea(existingComment, 6, 50);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        JScrollPane scroll = new JScrollPane(textArea);
        scroll.setPreferredSize(new Dimension(520, 160));

        int result = JOptionPane.showConfirmDialog(
                this, scroll, "Annotation – " + rating + " (clear comment to remove)",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;
        saveAnnotation(key, column, rating, textArea.getText().trim());
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
                    for (JsonElement el : existing.get("annotations").getAsJsonArray()) {
                        JsonObject e = el.getAsJsonObject();
                        JsonElement fileEl = e.get("file"), keyEl = e.get("key"), colEl = e.get("column");
                        if (fileEl == null || keyEl == null || colEl == null) {
                            annotations.add(e);
                            continue;
                        }
                        if (fp.equals(fileEl.getAsString()) && key.equals(keyEl.getAsString())
                                && column.equals(colEl.getAsString())) {
                            continue; // drop existing entry for this cell
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

    private void loadAnnotationColors() {
        annotationRatings.clear();
        File af = annotationFile();
        if (af.exists()) {
            try (Reader r = Files.newBufferedReader(af.toPath(), StandardCharsets.UTF_8)) {
                JsonObject existing = new Gson().fromJson(r, JsonObject.class);
                if (existing != null && existing.has("annotations")) {
                    String fp = annotationFilePath();
                    for (JsonElement el : existing.get("annotations").getAsJsonArray()) {
                        JsonObject e = el.getAsJsonObject();
                        JsonElement fileEl = e.get("file"), keyEl = e.get("key"),
                                colEl = e.get("column"), ratingEl = e.get("rating");
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
        for (JTable t : categoryTables) {
            t.repaint();
        }
    }

    // -------------------------------------------------------------------------
    // Inner renderer: colour swatch for the Color column
    // -------------------------------------------------------------------------

    static final class ColorCellRenderer extends JLabel implements TableCellRenderer {

        ColorCellRenderer() {
            setOpaque(true);
            setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
        }

        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value,
                boolean isSelected, boolean hasFocus,
                int row, int col) {

            String hex = value != null ? value.toString().trim() : "";
            setText(hex);

            try {
                Color bg = Color.decode(hex);
                setBackground(bg);
                // Choose white or black foreground for contrast
                double lum = 0.299 * bg.getRed() + 0.587 * bg.getGreen() + 0.114 * bg.getBlue();
                setForeground(lum > 140 ? Color.BLACK : Color.WHITE);
            } catch (NumberFormatException e) {
                setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
                setForeground(isSelected ? table.getSelectionForeground() : table.getForeground());
            }
            return this;
        }
    }
}

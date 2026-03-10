package eb.admin.ui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel that displays and edits the contents of {@code svg_resource.json}.
 *
 * <p>The JSON is structured as a two-level hierarchy:
 * <ul>
 *   <li><b>File parts</b> – top table (File, Path), always showing 5 visible rows.</li>
 *   <li><b>Items</b>      – bottom table (ID, Pathname, Type, W, H, X, Y), showing
 *       the sub-records that belong to the currently selected file part.</li>
 * </ul>
 *
 * <p>Selecting a row in the file-parts table populates the items table.
 * Both tables support Add/Delete Row operations.  A single Save button
 * writes the complete two-level structure back to disk.
 */
public class SvgEditorPanel extends JPanel {

    private static final String DEFAULT_JSON_PATH = "assets/text/svg_resource.json";

    // -------------------------------------------------------------------------
    // File-parts table  (top table)
    // -------------------------------------------------------------------------

    private final DefaultTableModel filePartsModel = new DefaultTableModel(
            new String[]{"File", "Path"}, 0) {
        @Override public boolean isCellEditable(int row, int col) { return true; }
    };

    private final JTable filePartsTable = new JTable(filePartsModel);

    /**
     * Parallel list – one entry per row in {@link #filePartsModel}.
     * Each entry is the list of item rows that belong to that file part.
     * An item row is Object[]{id, pathname, type, w, h, x, y}.
     */
    private final List<List<Object[]>> allItems = new ArrayList<>();

    // -------------------------------------------------------------------------
    // Items table  (bottom table)
    // -------------------------------------------------------------------------

    private final DefaultTableModel itemsModel = new DefaultTableModel(
            new String[]{"ID", "Pathname", "Type", "W", "H", "X", "Y"}, 0) {
        @Override public boolean isCellEditable(int row, int col) { return true; }
    };

    private final JTable itemsTable = new JTable(itemsModel);

    /** Row index currently shown in the items table; -1 when none. */
    private int selectedFilePartRow = -1;

    // -------------------------------------------------------------------------
    // Metadata / file state
    // -------------------------------------------------------------------------

    private final JTextField versionField = new JTextField(8);
    private final JTextField fileField    = new JTextField();
    private File currentFile;
    private final JLabel statusLabel;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    public SvgEditorPanel(JLabel statusLabel) {
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
        add(buildCenterPanel(), BorderLayout.CENTER);
    }

    private JPanel buildNorthPanel() {
        fileField.setEditable(false);

        JPanel metaTop = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        metaTop.add(new JLabel("Version:"));
        metaTop.add(versionField);

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

    private JPanel buildCenterPanel() {
        // ---- file-parts table (top) ----
        filePartsTable.setRowHeight(26);
        filePartsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        filePartsTable.getTableHeader().setReorderingAllowed(false);
        filePartsTable.getColumnModel().getColumn(0).setPreferredWidth(200); // File
        filePartsTable.getColumnModel().getColumn(1).setPreferredWidth(260); // Path
        filePartsTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        // Show exactly 5 rows of height in the viewport
        filePartsTable.setPreferredScrollableViewportSize(
                new Dimension(filePartsTable.getPreferredScrollableViewportSize().width,
                        5 * filePartsTable.getRowHeight()));

        filePartsTable.getSelectionModel().addListSelectionListener(
                (ListSelectionEvent e) -> {
                    if (!e.getValueIsAdjusting()) {
                        onFilePartSelectionChanged();
                    }
                });

        JButton fpAddBtn    = new JButton("Add File Part");
        JButton fpDeleteBtn = new JButton("Delete File Part");
        fpAddBtn.addActionListener((ActionEvent e) -> {
            flushItemsToCurrentPart();
            filePartsModel.addRow(new Object[]{"", ""});
            allItems.add(new ArrayList<>());
            int last = filePartsModel.getRowCount() - 1;
            filePartsTable.scrollRectToVisible(filePartsTable.getCellRect(last, 0, true));
            filePartsTable.setRowSelectionInterval(last, last);
        });
        fpDeleteBtn.addActionListener((ActionEvent e) -> {
            int row = filePartsTable.getSelectedRow();
            if (row >= 0) {
                filePartsModel.removeRow(row);
                allItems.remove(row);
                selectedFilePartRow = -1;
                itemsModel.setRowCount(0);
                // re-select nearest row
                if (filePartsModel.getRowCount() > 0) {
                    int next = Math.min(row, filePartsModel.getRowCount() - 1);
                    filePartsTable.setRowSelectionInterval(next, next);
                }
            } else {
                JOptionPane.showMessageDialog(this,
                        "Please select a file part to delete.",
                        "No Selection", JOptionPane.WARNING_MESSAGE);
            }
        });

        JPanel fpButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        fpButtons.add(fpAddBtn);
        fpButtons.add(fpDeleteBtn);

        JScrollPane fpScroll = new JScrollPane(filePartsTable,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        JPanel fpPanel = new JPanel(new BorderLayout());
        fpPanel.setBorder(BorderFactory.createTitledBorder("SVG File Parts"));
        fpPanel.add(fpScroll,   BorderLayout.CENTER);
        fpPanel.add(fpButtons,  BorderLayout.SOUTH);

        // ---- items table (bottom) ----
        itemsTable.setRowHeight(26);
        itemsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        itemsTable.getTableHeader().setReorderingAllowed(false);
        itemsTable.getColumnModel().getColumn(0).setPreferredWidth(80);   // ID
        itemsTable.getColumnModel().getColumn(1).setPreferredWidth(160);  // Pathname
        itemsTable.getColumnModel().getColumn(2).setPreferredWidth(100);  // Type
        itemsTable.getColumnModel().getColumn(3).setPreferredWidth(60);   // W
        itemsTable.getColumnModel().getColumn(4).setPreferredWidth(60);   // H
        itemsTable.getColumnModel().getColumn(5).setPreferredWidth(60);   // X
        itemsTable.getColumnModel().getColumn(6).setPreferredWidth(60);   // Y
        itemsTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        JButton itemAddBtn    = new JButton("Add Item");
        JButton itemDeleteBtn = new JButton("Delete Item");
        itemAddBtn.addActionListener((ActionEvent e) -> {
            if (selectedFilePartRow < 0) {
                JOptionPane.showMessageDialog(this,
                        "Please select a file part first.",
                        "No File Part Selected", JOptionPane.WARNING_MESSAGE);
                return;
            }
            itemsModel.addRow(new Object[]{"", "", "", 0, 0, 0, 0});
            int last = itemsModel.getRowCount() - 1;
            itemsTable.scrollRectToVisible(itemsTable.getCellRect(last, 0, true));
            itemsTable.setRowSelectionInterval(last, last);
        });
        itemDeleteBtn.addActionListener((ActionEvent e) -> {
            int row = itemsTable.getSelectedRow();
            if (row >= 0) {
                itemsModel.removeRow(row);
            } else {
                JOptionPane.showMessageDialog(this,
                        "Please select an item to delete.",
                        "No Selection", JOptionPane.WARNING_MESSAGE);
            }
        });

        JPanel itemButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        itemButtons.add(itemAddBtn);
        itemButtons.add(itemDeleteBtn);

        JScrollPane itemScroll = new JScrollPane(itemsTable,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        JPanel itemPanel = new JPanel(new BorderLayout());
        itemPanel.setBorder(BorderFactory.createTitledBorder("Items for Selected File Part"));
        itemPanel.add(itemScroll,   BorderLayout.CENTER);
        itemPanel.add(itemButtons,  BorderLayout.SOUTH);

        // ---- split pane ----
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, fpPanel, itemPanel);
        split.setResizeWeight(0.4);
        split.setOneTouchExpandable(true);

        JPanel center = new JPanel(new BorderLayout());
        center.add(split, BorderLayout.CENTER);
        return center;
    }

    // -------------------------------------------------------------------------
    // Selection handler
    // -------------------------------------------------------------------------

    private void onFilePartSelectionChanged() {
        int newRow = filePartsTable.getSelectedRow();
        if (newRow == selectedFilePartRow) return;

        // Flush current items table back to the previously selected part
        flushItemsToCurrentPart();

        selectedFilePartRow = newRow;

        // Load items for the newly selected part
        itemsModel.setRowCount(0);
        if (newRow >= 0 && newRow < allItems.size()) {
            for (Object[] item : allItems.get(newRow)) {
                itemsModel.addRow(item);
            }
        }
    }

    /**
     * Saves the current state of the items table back into {@link #allItems}
     * for {@link #selectedFilePartRow}.  No-op if no part is selected.
     */
    private void flushItemsToCurrentPart() {
        if (selectedFilePartRow < 0 || selectedFilePartRow >= allItems.size()) return;
        // Stop any active editor first
        if (itemsTable.isEditing()) {
            itemsTable.getCellEditor().stopCellEditing();
        }
        List<Object[]> rows = new ArrayList<>();
        for (int r = 0; r < itemsModel.getRowCount(); r++) {
            rows.add(new Object[]{
                    itemsModel.getValueAt(r, 0),
                    itemsModel.getValueAt(r, 1),
                    itemsModel.getValueAt(r, 2),
                    itemsModel.getValueAt(r, 3),
                    itemsModel.getValueAt(r, 4),
                    itemsModel.getValueAt(r, 5),
                    itemsModel.getValueAt(r, 6)
            });
        }
        allItems.set(selectedFilePartRow, rows);
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

            filePartsModel.setRowCount(0);
            allItems.clear();
            selectedFilePartRow = -1;
            itemsModel.setRowCount(0);

            if (root.has("svg_resources")) {
                for (JsonElement el : root.getAsJsonArray("svg_resources")) {
                    JsonObject part = el.getAsJsonObject();
                    String partFile = part.has("file") ? part.get("file").getAsString() : "";
                    String partPath = part.has("path") ? part.get("path").getAsString() : "";
                    filePartsModel.addRow(new Object[]{partFile, partPath});

                    List<Object[]> itemRows = new ArrayList<>();
                    if (part.has("items") && part.get("items").isJsonArray()) {
                        for (JsonElement ie : part.getAsJsonArray("items")) {
                            JsonObject item = ie.getAsJsonObject();
                            itemRows.add(new Object[]{
                                    item.has("id")       ? item.get("id").getAsString()       : "",
                                    item.has("pathname") ? item.get("pathname").getAsString() : "",
                                    item.has("type")     ? item.get("type").getAsString()     : "",
                                    item.has("w")        ? item.get("w").getAsInt() : 0,
                                    item.has("h")        ? item.get("h").getAsInt() : 0,
                                    item.has("x")        ? item.get("x").getAsInt() : 0,
                                    item.has("y")        ? item.get("y").getAsInt() : 0
                            });
                        }
                    }
                    allItems.add(itemRows);
                }
            }

            currentFile = file;
            fileField.setText(file.getAbsolutePath());
            statusLabel.setText("SVG resources loaded: " + file.getAbsolutePath());

            // Select first row automatically
            if (filePartsModel.getRowCount() > 0) {
                filePartsTable.setRowSelectionInterval(0, 0);
            }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Error loading file:\n" + ex.getMessage(),
                    "Load Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveFile(boolean saveAs) {
        // Flush pending edits
        if (filePartsTable.isEditing()) {
            filePartsTable.getCellEditor().stopCellEditing();
        }
        flushItemsToCurrentPart();

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

            JsonArray svgResources = new JsonArray();
            for (int r = 0; r < filePartsModel.getRowCount(); r++) {
                JsonObject part = new JsonObject();
                part.addProperty("file", cellStr(filePartsModel, r, 0));
                part.addProperty("path", cellStr(filePartsModel, r, 1));

                JsonArray items = new JsonArray();
                List<Object[]> itemRows = allItems.get(r);
                for (Object[] row : itemRows) {
                    JsonObject item = new JsonObject();
                    item.addProperty("id",       str(row[0]));
                    item.addProperty("pathname", str(row[1]));
                    item.addProperty("type",     str(row[2]));
                    item.addProperty("w",        intVal(row[3]));
                    item.addProperty("h",        intVal(row[4]));
                    item.addProperty("x",        intVal(row[5]));
                    item.addProperty("y",        intVal(row[6]));
                    items.add(item);
                }
                part.add("items", items);
                svgResources.add(part);
            }
            root.add("svg_resources", svgResources);

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            try (Writer writer = new FileWriter(currentFile)) {
                gson.toJson(root, writer);
            }
            statusLabel.setText("SVG resources saved: " + currentFile.getAbsolutePath());
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

    private static String cellStr(DefaultTableModel m, int row, int col) {
        Object v = m.getValueAt(row, col);
        return v != null ? v.toString().trim() : "";
    }

    private static String str(Object v) {
        return v != null ? v.toString().trim() : "";
    }

    private static int intVal(Object v) {
        if (v == null) return 0;
        try { return Integer.parseInt(v.toString().trim()); } catch (NumberFormatException e) { return 0; }
    }
}

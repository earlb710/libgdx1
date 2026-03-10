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
 * <p>Contains two inner sub-tabs:
 * <ul>
 *   <li><b>SVG Resource</b> – {@code assets/text/svg_resource.json}
 *       (File Parts + Items two-level hierarchy)</li>
 *   <li><b>SVG Index</b>    – {@code assets/face/svgs-index.json}
 *       (Feature, ID, Gender columns).  Selecting a row shows the
 *       corresponding SVG fragment from {@code assets/face/svgs.json}
 *       in a text area and renders it in a live preview panel.</li>
 * </ul>
 */
public class SvgEditorPanel extends JPanel {

    private static final String DEFAULT_RESOURCE_PATH = "assets/text/svg_resource.json";
    private static final String DEFAULT_INDEX_PATH    = "assets/face/svgs-index.json";
    private static final String DEFAULT_SVG_DATA_PATH = "assets/face/svgs.json";

    private final JLabel statusLabel;

    // ── SVG Resource (svg_resource.json) fields ───────────────────────────────

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

    private final DefaultTableModel itemsModel = new DefaultTableModel(
            new String[]{"ID", "Pathname", "Type", "W", "H", "X", "Y"}, 0) {
        @Override public boolean isCellEditable(int row, int col) { return true; }
    };
    private final JTable itemsTable = new JTable(itemsModel);

    /** Row index currently shown in the items table; -1 when none. */
    private int selectedFilePartRow = -1;

    private final JTextField versionField      = new JTextField(8);
    private final JTextField resourceFileField = new JTextField();
    private File resourceFile;

    // ── SVG Index (svgs-index.json) fields ────────────────────────────────────

    private final JTextField svgIndexFileField = new JTextField();

    /**
     * Table model for the SVG Index table.
     * Columns: Feature (col 0), ID (col 1), Gender (col 2).
     */
    private final DefaultTableModel svgIndexModel =
            new DefaultTableModel(new String[]{"Feature", "ID", "Gender"}, 0) {
                @Override
                public boolean isCellEditable(int row, int col) { return true; }
            };

    private final JTable svgIndexTable = new JTable(svgIndexModel);
    private File svgIndexFile;

    // ── svgs.json (detail data) fields ────────────────────────────────────────

    /** Root object of svgs.json – keyed by feature → id → SVG fragment string. */
    private JsonObject svgsData;

    private final JTextArea      svgDetailArea    = new JTextArea();
    private final SvgPreviewPanel svgPreviewPanel  = new SvgPreviewPanel();

    /**
     * Info label shown below the detail panel.
     * Its text changes based on the selected feature – a highlighted warning is
     * shown for {@code head} and {@code hair} features where position matters.
     */
    private final JLabel canvasNoteLabel = new JLabel();

    /** Features for which SVG position on the 400×600 canvas is significant. */
    private static final java.util.Set<String> POSITION_SENSITIVE_FEATURES =
            new java.util.HashSet<>(java.util.Arrays.asList("head", "hair", "hairBg", "body", "jersey"));

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
        subTabs.addTab("SVG Index",    buildSvgIndexTab());

        add(subTabs, BorderLayout.CENTER);
    }

    // ── SVG Resource tab ──────────────────────────────────────────────────────

    private JPanel buildSvgResourceTab() {
        resourceFileField.setEditable(false);

        // North panel: version field + file path + Open/Save/Save-As buttons
        JPanel metaTop = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        metaTop.add(new JLabel("Version:"));
        metaTop.add(versionField);

        JPanel fileRow = new JPanel(new BorderLayout(4, 0));
        fileRow.setBorder(BorderFactory.createEmptyBorder(0, 8, 2, 8));
        fileRow.add(new JLabel("File:"), BorderLayout.WEST);
        fileRow.add(resourceFileField, BorderLayout.CENTER);

        JPanel meta = new JPanel(new BorderLayout());
        meta.setBorder(BorderFactory.createTitledBorder("File Metadata"));
        meta.add(metaTop,  BorderLayout.NORTH);
        meta.add(fileRow,  BorderLayout.CENTER);

        JButton openBtn   = new JButton("Open\u2026");
        JButton saveBtn   = new JButton("Save");
        JButton saveAsBtn = new JButton("Save As\u2026");
        openBtn.addActionListener((ActionEvent e) -> openSvgResource());
        saveBtn.addActionListener((ActionEvent e) -> saveSvgResource(false));
        saveAsBtn.addActionListener((ActionEvent e) -> saveSvgResource(true));

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        toolbar.add(openBtn);
        toolbar.add(saveBtn);
        toolbar.add(saveAsBtn);

        JPanel north = new JPanel(new BorderLayout());
        north.add(meta,    BorderLayout.CENTER);
        north.add(toolbar, BorderLayout.SOUTH);

        JPanel tab = new JPanel(new BorderLayout(0, 4));
        tab.add(north,                  BorderLayout.NORTH);
        tab.add(buildSvgResourceCenter(), BorderLayout.CENTER);
        return tab;
    }

    private JPanel buildSvgResourceCenter() {
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

    // ── SVG Index tab ─────────────────────────────────────────────────────────

    private JPanel buildSvgIndexTab() {
        svgIndexFileField.setEditable(false);

        configureTable(svgIndexTable);
        svgIndexTable.getColumnModel().getColumn(0).setPreferredWidth(120);
        svgIndexTable.getColumnModel().getColumn(1).setPreferredWidth(200);
        svgIndexTable.getColumnModel().getColumn(2).setPreferredWidth(80);

        // Gender column – use a JComboBox editor for convenience
        JComboBox<String> genderCombo = new JComboBox<>(new String[]{"both", "male", "female"});
        svgIndexTable.getColumnModel().getColumn(2)
                .setCellEditor(new DefaultCellEditor(genderCombo));

        // Wire up row-selection → detail / preview update
        svgIndexTable.getSelectionModel().addListSelectionListener(
                (ListSelectionEvent e) -> {
                    if (!e.getValueIsAdjusting()) onSvgIndexRowSelected();
                });

        JButton addBtn    = new JButton("Add Row");
        JButton deleteBtn = new JButton("Delete Row");
        JButton saveBtn   = new JButton("Save");

        addBtn.addActionListener((ActionEvent e) -> {
            // Seed the Feature cell from the selected row for convenience
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

        // ── North: file path + Open/Save/Save-As toolbar ──────────────────────

        JPanel fileRow = new JPanel(new BorderLayout(4, 0));
        fileRow.setBorder(BorderFactory.createEmptyBorder(0, 8, 2, 8));
        fileRow.add(new JLabel("File:"), BorderLayout.WEST);
        fileRow.add(svgIndexFileField, BorderLayout.CENTER);

        JPanel fileMeta = new JPanel(new BorderLayout());
        fileMeta.setBorder(BorderFactory.createTitledBorder("File"));
        fileMeta.add(fileRow, BorderLayout.CENTER);

        JButton openBtn   = new JButton("Open\u2026");
        JButton saveBtn2  = new JButton("Save");
        JButton saveAsBtn = new JButton("Save As\u2026");
        openBtn.addActionListener((ActionEvent e) -> openSvgIndex());
        saveBtn2.addActionListener((ActionEvent e) -> saveSvgIndex(false));
        saveAsBtn.addActionListener((ActionEvent e) -> saveSvgIndex(true));

        JPanel fileToolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        fileToolbar.add(openBtn);
        fileToolbar.add(saveBtn2);
        fileToolbar.add(saveAsBtn);

        JPanel north = new JPanel(new BorderLayout());
        north.add(fileMeta,    BorderLayout.CENTER);
        north.add(fileToolbar, BorderLayout.SOUTH);

        // ── Row toolbar ───────────────────────────────────────────────────────

        JPanel rowToolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        rowToolbar.add(addBtn);
        rowToolbar.add(deleteBtn);
        rowToolbar.add(Box.createHorizontalStrut(12));
        rowToolbar.add(saveBtn);

        // ── Table panel (index) ───────────────────────────────────────────────

        JScrollPane indexScroll = new JScrollPane(svgIndexTable,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        JPanel tablePanel = new JPanel(new BorderLayout(0, 2));
        tablePanel.add(indexScroll, BorderLayout.CENTER);
        tablePanel.add(rowToolbar,  BorderLayout.SOUTH);

        // ── Detail area: text + SVG preview ──────────────────────────────────

        svgDetailArea.setEditable(false);
        svgDetailArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        svgDetailArea.setLineWrap(true);
        svgDetailArea.setWrapStyleWord(false);
        svgDetailArea.setText("(select a row to see the svgs.json fragment)");

        JScrollPane detailScroll = new JScrollPane(svgDetailArea,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        detailScroll.setBorder(BorderFactory.createTitledBorder("svgs.json fragment"));

        svgPreviewPanel.setBorder(BorderFactory.createTitledBorder(
                "Preview (canvas: " + SvgPreviewPanel.SVG_W
                        + "\u00d7" + SvgPreviewPanel.SVG_H + ")"));
        svgPreviewPanel.setPreferredSize(new Dimension(200, 200));
        svgPreviewPanel.setMinimumSize(new Dimension(100, 100));

        JSplitPane detailSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                detailScroll, svgPreviewPanel);
        detailSplit.setResizeWeight(0.6);
        detailSplit.setOneTouchExpandable(true);

        // ── Canvas note label ─────────────────────────────────────────────────
        initCanvasNoteLabel();

        JPanel detailPanel = new JPanel(new BorderLayout(0, 2));
        detailPanel.setBorder(BorderFactory.createTitledBorder("SVG Detail"));
        detailPanel.add(detailSplit,    BorderLayout.CENTER);
        detailPanel.add(canvasNoteLabel, BorderLayout.SOUTH);

        // ── Outer split: index table on top, detail on bottom ─────────────────

        JSplitPane mainSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                tablePanel, detailPanel);
        mainSplit.setResizeWeight(0.6);
        mainSplit.setOneTouchExpandable(true);

        JPanel tab = new JPanel(new BorderLayout(0, 4));
        tab.add(north,     BorderLayout.NORTH);
        tab.add(mainSplit, BorderLayout.CENTER);
        return tab;
    }

    /**
     * Initialises the {@link #canvasNoteLabel} with the default (no-selection) text.
     */
    private void initCanvasNoteLabel() {
        canvasNoteLabel.setOpaque(true);
        canvasNoteLabel.setBorder(BorderFactory.createEmptyBorder(3, 6, 3, 6));
        canvasNoteLabel.setFont(canvasNoteLabel.getFont().deriveFont(Font.PLAIN, 11f));
        updateCanvasNoteLabel("");
    }

    /**
     * Updates the canvas-note label for the given feature.
     *
     * <p>For position-sensitive features ({@code head}, {@code hair},
     * {@code hairBg}, {@code body}, {@code jersey}) a highlighted warning is
     * shown; all other features get a brief informational note.
     *
     * @param feature the selected feature name, or empty string when none selected
     */
    private void updateCanvasNoteLabel(String feature) {
        if (POSITION_SENSITIVE_FEATURES.contains(feature)) {
            canvasNoteLabel.setBackground(new Color(255, 243, 205));
            canvasNoteLabel.setForeground(new Color(120, 80, 0));
            canvasNoteLabel.setText(
                    "\u26a0  Position matters for \u2018" + feature + "\u2019 SVGs: "
                    + "draw on the full 400\u00d7600 canvas, matching the existing "
                    + feature + " SVGs so facial features align correctly.");
        } else {
            canvasNoteLabel.setBackground(new Color(235, 244, 255));
            canvasNoteLabel.setForeground(new Color(50, 80, 130));
            String posFeatures = new java.util.TreeSet<>(POSITION_SENSITIVE_FEATURES)
                    .stream().collect(java.util.stream.Collectors.joining(", "));
            canvasNoteLabel.setText(
                    "\u2139  Canvas: 400\u00d7600.  "
                    + "For most features position is ignored (auto-placed). "
                    + "Position is only critical for: " + posFeatures + ".");
        }
    }

    /** Called whenever the SVG Index table selection changes. */
    private void onSvgIndexRowSelected() {
        int row = svgIndexTable.getSelectedRow();
        if (row < 0 || svgsData == null) {
            svgDetailArea.setText("(no selection)");
            svgPreviewPanel.setFeatureName("");
            svgPreviewPanel.setSvgFragment("");
            updateCanvasNoteLabel("");
            return;
        }
        String feature = cellStr(svgIndexModel, row, 0);
        String id      = cellStr(svgIndexModel, row, 1);

        updateCanvasNoteLabel(feature);
        svgPreviewPanel.setFeatureName(feature);

        if (feature.isEmpty() || id.isEmpty()) {
            svgDetailArea.setText("(feature or id is empty)");
            svgPreviewPanel.setSvgFragment("");
            return;
        }

        if (svgsData.has(feature) && svgsData.getAsJsonObject(feature).has(id)) {
            String fragment = svgsData.getAsJsonObject(feature).get(id).getAsString();
            svgDetailArea.setText(fragment);
            svgDetailArea.setCaretPosition(0);
            svgPreviewPanel.setSvgFragment(fragment);
        } else {
            svgDetailArea.setText("(no entry found in svgs.json for "
                    + feature + "/" + id + ")");
            svgPreviewPanel.setSvgFragment("");
        }
    }

    // ── Selection handler (SVG Resource) ─────────────────────────────────────

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

    // ── Default file auto-loading ─────────────────────────────────────────────

    private void tryLoadDefaults() {
        File resourceDefault = new File(DEFAULT_RESOURCE_PATH);
        if (resourceDefault.exists()) loadSvgResourceFromFile(resourceDefault);
        File indexDefault = new File(DEFAULT_INDEX_PATH);
        if (indexDefault.exists()) loadSvgIndexFromFile(indexDefault);
        File svgsDataDefault = new File(DEFAULT_SVG_DATA_PATH);
        if (svgsDataDefault.exists()) loadSvgsDataFromFile(svgsDataDefault);
    }

    // ── File operations: svgs.json (detail data) ──────────────────────────────

    private void loadSvgsDataFromFile(File file) {
        try (Reader reader = new FileReader(file)) {
            svgsData = new Gson().fromJson(reader, JsonObject.class);
        } catch (Exception ex) {
            svgsData = null;
        }
    }

    // ── File operations: SVG Resource (svg_resource.json) ────────────────────

    private void openSvgResource() {
        File chosen = chooseOpenFile(resourceFile, "assets/text");
        if (chosen != null) loadSvgResourceFromFile(chosen);
    }

    private void loadSvgResourceFromFile(File file) {
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

            resourceFile = file;
            resourceFileField.setText(file.getAbsolutePath());
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

    private void saveSvgResource(boolean saveAs) {
        if (filePartsTable.isEditing()) {
            filePartsTable.getCellEditor().stopCellEditing();
        }
        flushItemsToCurrentPart();

        resourceFile = resolveTargetFile(resourceFile, saveAs);
        if (resourceFile == null) return;

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
            try (Writer writer = new FileWriter(resourceFile)) {
                gson.toJson(root, writer);
            }
            statusLabel.setText("SVG resources saved: " + resourceFile.getAbsolutePath());
            resourceFileField.setText(resourceFile.getAbsolutePath());
            JOptionPane.showMessageDialog(this,
                    "File saved successfully.",
                    "Saved", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Error saving file:\n" + ex.getMessage(),
                    "Save Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ── File operations: SVG Index (svgs-index.json) ──────────────────────────

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
                String    feature = featureEntry.getKey();
                JsonArray ids     = featureEntry.getValue().getAsJsonArray();
                JsonArray genders = gendersObj.has(feature)
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

    private static String str(Object v) {
        return v != null ? v.toString().trim() : "";
    }

    private static int intVal(Object v) {
        if (v == null) return 0;
        try { return Integer.parseInt(v.toString().trim()); } catch (NumberFormatException e) { return 0; }
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

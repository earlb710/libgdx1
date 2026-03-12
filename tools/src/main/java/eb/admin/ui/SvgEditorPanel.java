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
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.EventObject;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

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

    private static final String DEFAULT_RESOURCE_PATH   = "assets/text/svg_resource.json";
    private static final String DEFAULT_INDEX_PATH      = "assets/face/svgs-index.json";
    private static final String DEFAULT_SVG_DATA_PATH   = "assets/face/svgs.json";
    private static final String DEFAULT_FACE_RULES_PATH = "assets/face/facerules.json";

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
    /** File from which {@link #svgsData} was loaded; used by the Save Fragment button. */
    private File svgsDataFile;

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

    // ── Face Maker tab fields ──────────────────────────────────────────────────

    /**
     * Feature draw order for the face maker preview (back → front).
     * Mirrors the exact facesjs {@code featureInfos} layering convention.
     */
    private static final String[] FACE_DRAW_ORDER = {
        "hairBg", "body", "jersey", "ear", "head",
        "eyeLine", "smileLine", "miscLine", "facialHair",
        "eye", "eyebrow", "mouth", "nose",
        "hair", "glasses", "accessories"
    };

    /**
     * Pixel positions [x, y] for features that must be translated on the 400×600
     * canvas, mirroring {@code FaceSvgBuilder.FEATURE_INFOS}.
     * Bilateral features have two position pairs: index 0 = left, index 1 = right.
     * The right instance is also mirrored via {@code scale(-1 1)}.
     */
    private static final Map<String, int[][]> FACE_FEATURE_POSITIONS = new LinkedHashMap<>();
    static {
        FACE_FEATURE_POSITIONS.put("ear",       new int[][]{{55, 325}, {345, 325}});
        FACE_FEATURE_POSITIONS.put("eye",       new int[][]{{140, 310}, {260, 310}});
        FACE_FEATURE_POSITIONS.put("eyebrow",   new int[][]{{140, 270}, {260, 270}});
        FACE_FEATURE_POSITIONS.put("mouth",     new int[][]{{200, 440}});
        FACE_FEATURE_POSITIONS.put("nose",      new int[][]{{200, 370}});
        FACE_FEATURE_POSITIONS.put("smileLine", new int[][]{{150, 435}, {250, 435}});
    }

    /**
     * Features that expose color pickers in the face maker UI.
     * Each entry maps to: [placeholders…], [defaults…], [tooltips…].
     * The three inner arrays must all have the same length (1, 2, or 3).
     */
    private static final Map<String, String[][]> FEATURE_COLOR_DEFS = new LinkedHashMap<>();
    static {
        // hairBg:     1 color – hair fill ($[hairColor])
        FEATURE_COLOR_DEFS.put("hairBg",
                new String[][]{{"$[hairColor]"},
                                {"#272421"},
                                {"Hair"}});
        // head:       2 colors – skin fill ($[skinColor]),  shave stub ($[faceShave]/$[headShave])
        FEATURE_COLOR_DEFS.put("head",
                new String[][]{{"$[skinColor]",  "$[faceShave]"},
                                {"#f2d6cb",      "#ffffff"},
                                {"Skin",         "Shave"}});
        // ear:        1 color  – skin fill ($[skinColor])
        FEATURE_COLOR_DEFS.put("ear",
                new String[][]{{"$[skinColor]"},
                                {"#f2d6cb"},
                                {"Skin"}});
        // nose:       1 color  – skin fill ($[skinColor])
        FEATURE_COLOR_DEFS.put("nose",
                new String[][]{{"$[skinColor]"},
                                {"#f2d6cb"},
                                {"Skin"}});
        // hair:       2 colors – main ($[hairColor]),  secondary ($[hairColor2])
        FEATURE_COLOR_DEFS.put("hair",
                new String[][]{{"$[hairColor]",  "$[hairColor2]"},
                                {"#272421",      "#272421"},
                                {"Color 1",      "Color 2"}});
        // facialHair: 2 colors – hair ($[hairColor]),  accent ($[primary])
        FEATURE_COLOR_DEFS.put("facialHair",
                new String[][]{{"$[hairColor]",  "$[primary]"},
                                {"#272421",      "#89bfd3"},
                                {"Hair",         "Accent"}});
        // jersey:     3 colors – primary, secondary, accent
        FEATURE_COLOR_DEFS.put("jersey",
                new String[][]{{"$[primary]",    "$[secondary]",  "$[accent]"},
                                {"#89bfd3",      "#7a1319",       "#07364f"},
                                {"Primary",      "Secondary",     "Accent"}});
        // body:       1 color  – skin fill ($[skinColor])
        FEATURE_COLOR_DEFS.put("body",
                new String[][]{{"$[skinColor]"},
                                {"#f2d6cb"},
                                {"Skin"}});
        // glasses:    2 colors – lens/straps ($[primary]),  frame ($[secondary])
        FEATURE_COLOR_DEFS.put("glasses",
                new String[][]{{"$[primary]",    "$[secondary]"},
                                {"#89bfd3",      "#333333"},
                                {"Lens",         "Frame"}});
        // eyebrow:    1 color  – main ($[hairColor])
        FEATURE_COLOR_DEFS.put("eyebrow",
                new String[][]{{"$[hairColor]"},
                                {"#272421"},
                                {"Hair"}});
        // miscLine:   1 color  – line/accent ($[primary])
        FEATURE_COLOR_DEFS.put("miscLine",
                new String[][]{{"$[primary]"},
                                {"#000000"},
                                {"Color"}});
        // accessories: 3 colors – primary, secondary, accent
        FEATURE_COLOR_DEFS.put("accessories",
                new String[][]{{"$[primary]",    "$[secondary]",  "$[accent]"},
                                {"#89bfd3",      "#7a1319",       "#07364f"},
                                {"Primary",      "Secondary",     "Accent"}});
    }

    /** Body-width selector for the face maker (thin = −15%, normal = 0%, thick = +15%). */
    private final JComboBox<String> faceMakerBuildCombo =
            new JComboBox<>(new String[]{"normal", "thin", "thick"});

    /** One combo box per feature, keyed by feature name, in draw order. */
    private final Map<String, JComboBox<String>> faceMakerCombos = new LinkedHashMap<>();

    /**
     * Color-picker buttons per color-enabled feature (1, 2, or 3 buttons depending on the
     * feature).  Each button shows the current color as a solid fill; clicking opens a
     * {@link JColorChooser}.
     */
    private final Map<String, JButton[]> faceMakerColorBtns = new LinkedHashMap<>();

    /** Composite preview panel for the face maker. */
    private final SvgPreviewPanel faceMakerPreview = new SvgPreviewPanel();

    /** Debug text area showing bbox center and translate for eye and nose. */
    private final JTextArea faceMakerDebugArea = new JTextArea(4, 40);

    /**
     * Shared {@link JColorChooser} instance reused across all color-picker invocations so
     * that the built-in "Recent Colors" swatch panel accumulates a history across picks.
     * A new instance would start with an empty recent-colors list each time.
     */
    private final JColorChooser sharedColorChooser = new JColorChooser();

    // ── Face Rules tab fields ─────────────────────────────────────────────────

    /** Valid gender values offered as combo presets in the Face Rules table. */
    private static final String[] FACE_RULE_GENDER_PRESETS =
            {"", "male", "female"};

    /** Valid emotion values for face rules. */
    private static final String[] FACE_RULE_EMOTIONS =
            {"normal", "happy", "sad", "anxious", "angry"};

    /** Valid clothes-type values for face rules. */
    private static final String[] FACE_RULE_CLOTHES_TYPES =
            {"normal", "work", "sport", "gym"};

    private final JTextField faceRulesFileField = new JTextField();
    private File faceRulesFile;

    /**
     * Table model for face rules.
     * <p>Columns: Gender (0), Emotion (1), MinWealth (2), MinAge (3), ClothesType (4),
     * Percentage (5), Priority (6), Include (7), Exclude (8).
     * <ul>
     *   <li>Gender – one of {@code ""}, {@code "male"}, {@code "female"}; empty means any gender.
     *   <li>Emotion – one of {@code ""}, {@code "normal"}, {@code "happy"}, {@code "sad"},
     *       {@code "anxious"}, {@code "angry"}; empty means any emotion.
     *   <li>MinWealth – non-negative integer; 0 = no minimum.
     *   <li>MinAge – non-negative integer; 0 = no minimum.
     *   <li>ClothesType – one of {@code ""}, {@code "normal"}, {@code "work"},
     *       {@code "sport"}, {@code "gym"}; empty means any clothes type.
     *   <li>Percentage – integer 1–100; chance (%) the rule fires when its conditions match.
     *       100 means the rule always fires (default).
     *   <li>Priority – non-negative integer; rules are sorted ascending by priority before being
     *       applied so that higher-priority rules are processed last and overwrite earlier ones.
     *       0 = default (lowest priority).
     *   <li>Include – comma-separated list of {@code "feature.id"} pairs that are
     *       allowed when this rule's conditions are met.
     *   <li>Exclude – comma-separated list of {@code "feature.id"} pairs that are
     *       forbidden when this rule's conditions are met.
     * </ul>
     */
    private final DefaultTableModel faceRulesModel =
            new DefaultTableModel(
                    new String[]{"Gender", "Emotion", "MinWealth", "MinAge", "ClothesType", "Percentage", "Priority", "Include", "Exclude"}, 0) {
                @Override public boolean isCellEditable(int row, int col) { return true; }
            };

    private final JTable faceRulesTable = new JTable(faceRulesModel);

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
        subTabs.addTab("Face Maker",   buildFaceMakerTab());
        subTabs.addTab("Face Rules",   buildFaceRulesTab());

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

        svgDetailArea.setEditable(true);
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

        // ── Save Fragment button ──────────────────────────────────────────────
        JButton saveFragmentBtn = new JButton("Save Fragment");
        saveFragmentBtn.setToolTipText("Save the edited SVG fragment back to svgs.json");
        saveFragmentBtn.addActionListener((ActionEvent e) -> saveSvgFragment());

        JPanel detailSouth = new JPanel(new BorderLayout(4, 2));
        detailSouth.add(saveFragmentBtn, BorderLayout.WEST);
        detailSouth.add(canvasNoteLabel, BorderLayout.CENTER);

        JPanel detailPanel = new JPanel(new BorderLayout(0, 2));
        detailPanel.setBorder(BorderFactory.createTitledBorder("SVG Detail"));
        detailPanel.add(detailSplit,  BorderLayout.CENTER);
        detailPanel.add(detailSouth, BorderLayout.SOUTH);

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
        File faceRulesDefault = new File(DEFAULT_FACE_RULES_PATH);
        if (faceRulesDefault.exists()) loadFaceRulesFromFile(faceRulesDefault);
    }

    // ── File operations: svgs.json (detail data) ──────────────────────────────

    private void loadSvgsDataFromFile(File file) {
        try (Reader reader = new FileReader(file)) {
            svgsData     = new Gson().fromJson(reader, JsonObject.class);
            svgsDataFile = file;
        } catch (Exception ex) {
            svgsData     = null;
            svgsDataFile = null;
        }
        refreshFaceMakerCombos();
    }

    /**
     * Saves the currently edited SVG fragment (from {@link #svgDetailArea}) back
     * into {@link #svgsData} for the selected feature/id row, updates the live
     * preview, and writes the updated {@code svgs.json} file to disk.
     */
    private void saveSvgFragment() {
        int row = svgIndexTable.getSelectedRow();
        if (row < 0 || svgsData == null) {
            JOptionPane.showMessageDialog(this,
                    "No row selected or svgs.json not loaded.",
                    "Save Fragment", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String feature  = cellStr(svgIndexModel, row, 0).trim();
        String id       = cellStr(svgIndexModel, row, 1).trim();
        if (feature.isEmpty() || id.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "The selected row has no Feature or ID.",
                    "Save Fragment", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String newFragment = svgDetailArea.getText();

        // Update in-memory svgsData
        if (!svgsData.has(feature)) {
            svgsData.add(feature, new JsonObject());
        }
        svgsData.getAsJsonObject(feature).addProperty(id, newFragment);

        // Refresh live preview
        svgPreviewPanel.setSvgFragment(newFragment);

        // Persist to file (prompt for a file if none is known)
        if (svgsDataFile == null) {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(new FileNameExtensionFilter("JSON files (*.json)", "json"));
            File d = new File(DEFAULT_SVG_DATA_PATH);
            chooser.setCurrentDirectory(d.getParentFile().isDirectory()
                    ? d.getParentFile() : new File("."));
            if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
            svgsDataFile = ensureJsonExtension(chooser.getSelectedFile());
        }

        try (Writer writer = new FileWriter(svgsDataFile)) {
            new GsonBuilder().setPrettyPrinting().create().toJson(svgsData, writer);
            statusLabel.setText("SVG fragment saved: " + feature + "/" + id
                    + " → " + svgsDataFile.getAbsolutePath());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    "Error saving svgs.json:\n" + ex.getMessage(),
                    "Save Error", JOptionPane.ERROR_MESSAGE);
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
        refreshFaceMakerCombos();
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
        refreshFaceMakerCombos();
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
            return ensureJsonExtension(chooser.getSelectedFile());
        }
        return currentFile;
    }

    private static File ensureJsonExtension(File file) {
        return file.getName().endsWith(".json") ? file
                : new File(file.getAbsolutePath() + ".json");
    }

    // ── Face Maker tab ────────────────────────────────────────────────────────

    /**
     * Builds the Face Maker sub-tab.
     *
     * <p>Left side: a scrollable panel of (feature-label, combobox, [color buttons]) rows in
     * back-to-front draw order, plus Randomize and Clear All buttons.
     * Color-enabled features show 1–3 color-picker buttons (depending on the feature) that
     * let the user override the colors used when rendering those features in the preview.
     * Right side: a composite {@link SvgPreviewPanel} that renders all selected
     * fragments stacked on the shared 400×600 canvas.
     */
    @SuppressWarnings("unchecked")
    private JPanel buildFaceMakerTab() {
        JPanel selectorsPanel = new JPanel(new GridBagLayout());
        selectorsPanel.setBorder(BorderFactory.createTitledBorder("Feature Selections (back → front)"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets  = new Insets(3, 4, 3, 4);
        gbc.fill    = GridBagConstraints.HORIZONTAL;
        gbc.anchor  = GridBagConstraints.WEST;

        // Row 0: build (body-width) selector
        JLabel buildLabel = new JLabel("build:");
        buildLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        faceMakerBuildCombo.setToolTipText("thin = 15% narrower  |  normal = standard width  |  thick = 15% wider");
        faceMakerBuildCombo.addActionListener((ActionEvent e) -> updateFaceMakerPreview());
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0; gbc.gridwidth = 1;
        selectorsPanel.add(buildLabel, gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        selectorsPanel.add(faceMakerBuildCombo, gbc);

        for (int i = 0; i < FACE_DRAW_ORDER.length; i++) {
            String feature = FACE_DRAW_ORDER[i];

            JLabel label = new JLabel(feature + ":");
            label.setHorizontalAlignment(SwingConstants.RIGHT);

            JComboBox<String> combo = new JComboBox<>();
            combo.addItem("(none)");
            faceMakerCombos.put(feature, combo);
            combo.addActionListener((ActionEvent e) -> updateFaceMakerPreview());

            gbc.gridx = 0; gbc.gridy = 1 + i; gbc.weightx = 0; gbc.gridwidth = 1;
            selectorsPanel.add(label, gbc);
            gbc.gridx = 1; gbc.weightx = 1.0;
            selectorsPanel.add(combo, gbc);

            // Color pickers for color-enabled features (1, 2, or 3 buttons depending on feature)
            String[][] colorDef = FEATURE_COLOR_DEFS.get(feature);
            if (colorDef != null) {
                String[] placeholders = colorDef[0];
                String[] defaults     = colorDef[1];
                String[] tooltips     = colorDef[2];
                int numColors = placeholders.length;
                JButton[] btns = new JButton[numColors];
                for (int ci = 0; ci < numColors; ci++) {
                    btns[ci] = createColorButton(defaults[ci], tooltips[ci]);
                    final int idx = ci;
                    final JButton btn = btns[ci];
                    final String feat = feature;
                    btn.addActionListener((ActionEvent e) -> {
                        Color chosen = showColorPickerDialog(
                                SvgEditorPanel.this, tooltips[idx] + " – " + feat, btn.getBackground());
                        if (chosen != null) {
                            btn.setBackground(chosen);
                            btn.setToolTipText(colorToHex(chosen));
                            updateFaceMakerPreview();
                        }
                    });
                }
                faceMakerColorBtns.put(feature, btns);

                JPanel colorPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
                colorPanel.setOpaque(false);
                for (JButton btn : btns) colorPanel.add(btn);
                gbc.gridx = 2; gbc.weightx = 0;
                selectorsPanel.add(colorPanel, gbc);
            }
        }

        JButton randomizeBtn = new JButton("Randomize");
        JButton clearBtn     = new JButton("Clear All");
        randomizeBtn.setToolTipText("Pick a random variant for every feature");
        clearBtn.setToolTipText("Reset all feature selections to (none)");
        randomizeBtn.addActionListener((ActionEvent e) -> randomizeFaceMaker());
        clearBtn.addActionListener((ActionEvent e) -> clearFaceMaker());

        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        buttonsPanel.add(randomizeBtn);
        buttonsPanel.add(clearBtn);

        gbc.gridx = 0; gbc.gridy = FACE_DRAW_ORDER.length + 1;
        gbc.gridwidth = 3; gbc.weightx = 1.0;
        selectorsPanel.add(buttonsPanel, gbc);

        JScrollPane selectorsScroll = new JScrollPane(selectorsPanel,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        selectorsScroll.setPreferredSize(new Dimension(340, 100));
        selectorsScroll.setMinimumSize(new Dimension(260, 100));

        faceMakerPreview.setBorder(BorderFactory.createTitledBorder(
                "Preview (canvas: " + SvgPreviewPanel.SVG_W
                        + "\u00d7" + SvgPreviewPanel.SVG_H + ")"));
        faceMakerPreview.setPreferredSize(new Dimension(320, 480));
        faceMakerPreview.setMinimumSize(new Dimension(200, 300));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                selectorsScroll, faceMakerPreview);
        split.setDividerLocation(350);
        split.setOneTouchExpandable(true);

        faceMakerDebugArea.setEditable(false);
        faceMakerDebugArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        faceMakerDebugArea.setText("(select eye or nose to see position debug)");
        JScrollPane debugScroll = new JScrollPane(faceMakerDebugArea);
        debugScroll.setBorder(BorderFactory.createTitledBorder("Eye / Nose position debug"));

        JPanel tab = new JPanel(new BorderLayout());
        tab.add(split, BorderLayout.CENTER);
        tab.add(debugScroll, BorderLayout.SOUTH);
        return tab;
    }

    /**
     * Repopulates all face-maker combo boxes from the currently loaded
     * {@link #svgIndexModel} (i.e. the contents of {@code svgs-index.json}).
     * Using the index as the source means that only registered/curated parts
     * are offered, in exactly the order they appear in the index file.
     * Previously selected values are preserved when still available.
     */
    private void refreshFaceMakerCombos() {
        // Build feature → ordered IDs from the index table
        Map<String, List<String>> indexedIds = new LinkedHashMap<>();
        for (int r = 0; r < svgIndexModel.getRowCount(); r++) {
            String feature = cellStr(svgIndexModel, r, 0).trim();
            String id      = cellStr(svgIndexModel, r, 1).trim();
            if (!feature.isEmpty() && !id.isEmpty()) {
                indexedIds.computeIfAbsent(feature, k -> new ArrayList<>()).add(id);
            }
        }

        for (String feature : FACE_DRAW_ORDER) {
            JComboBox<String> combo = faceMakerCombos.get(feature);
            if (combo == null) continue;

            String currentSelection = (String) combo.getSelectedItem();
            combo.removeAllItems();
            combo.addItem("(none)");

            List<String> ids = indexedIds.get(feature);
            if (ids != null) {
                for (String id : ids) combo.addItem(id);
            }

            // Restore previous selection if still present in the new list
            if (currentSelection != null && !currentSelection.equals("(none)")) {
                for (int i = 0; i < combo.getItemCount(); i++) {
                    if (currentSelection.equals(combo.getItemAt(i))) {
                        combo.setSelectedItem(currentSelection);
                        break;
                    }
                }
            }
        }
        updateFaceMakerPreview();
    }

    /** Collects the selected fragments in draw order and pushes them to the preview. */
    private void updateFaceMakerPreview() {
        if (svgsData == null) {
            faceMakerPreview.setCompositeFragments(null);
            return;
        }
        List<String> fragments = new ArrayList<>();
        StringBuilder debugSb = new StringBuilder();

        for (String feature : FACE_DRAW_ORDER) {
            JComboBox<String> combo = faceMakerCombos.get(feature);
            if (combo == null) continue;
            String selected = (String) combo.getSelectedItem();
            if (selected == null || selected.equals("(none)")) continue;
            if (!svgsData.has(feature)) continue;
            JsonObject featureObj = svgsData.getAsJsonObject(feature);
            if (!featureObj.has(selected)) continue;
            String frag = featureObj.get(selected).getAsString();
            if (frag.isEmpty()) continue;

            // Apply per-feature color substitutions from the color-picker buttons,
            // then fall back to global defaults for any remaining placeholders.
            frag = applyFaceMakerColors(frag, feature);
            frag = applyDefaultColorSubstitutions(frag);

            int[][] positions = FACE_FEATURE_POSITIONS.get(feature);
            if (positions == null) {
                // Full-canvas feature (head, hair, etc.): draw as-is.
                fragments.add(frag);
                continue;
            }

            // Compute the original bounding-box center of the fragment in its local
            // coordinate space (mirrors how getBBox() works in a browser — the element's
            // own transform attribute is NOT included, only shape coordinates).
            java.awt.geom.Rectangle2D bounds = SvgPreviewPanel.computeFragmentBounds(frag);
            double cx = 0, cy = 0;
            if (bounds != null) {
                cx = bounds.getCenterX();
                cy = bounds.getCenterY();
            }

            // Debug output for eye and nose
            boolean isDebugFeature = "eye".equals(feature) || "nose".equals(feature);
            if (isDebugFeature) {
                debugSb.append(String.format(Locale.US, "%s (%s): bbox_center=(%.2f, %.2f)%n",
                        feature, selected, cx, cy));
            }

            for (int i = 0; i < positions.length; i++) {
                double px = positions[i][0];
                double py = positions[i][1];
                double tx = px - cx;
                double ty = py - cy;

                if (isDebugFeature) {
                    debugSb.append(String.format(Locale.US,
                            "  instance[%d]: target=(%.0f, %.0f)  translate=(%.2f, %.2f)%n",
                            i, px, py, tx, ty));
                }

                String transform;
                if (i == 0) {
                    // Left / single instance: translate center to target position.
                    transform = String.format(Locale.US, "translate(%.2f %.2f)", tx, ty);
                } else {
                    // Right bilateral: translate center to target, then mirror about
                    // that center.  Matches facesjs scaleCentered(-1, 1) which uses
                    // getBBox() in the LOCAL coordinate space (i.e. the original bbox
                    // center (cx, cy), not the post-translate center).
                    // Scale transform: scale(-1 1) translate(-2*cx 0)
                    double txMirror = -2.0 * cx;
                    transform = String.format(Locale.US,
                            "translate(%.2f %.2f) scale(-1 1) translate(%.2f 0)",
                            tx, ty, txMirror);
                }
                fragments.add("<g transform=\"" + transform + "\">" + frag + "</g>");
            }
        }

        String debugText = debugSb.toString().trim();
        faceMakerDebugArea.setText(debugText.isEmpty() ? "(select eye or nose to see position debug)" : debugText);

        // Apply horizontal width scale (build: thin=0.85, normal=1.0, thick=1.15).
        // Scale is about canvas center-x (200) so the face stays centred.
        String buildVal = (String) faceMakerBuildCombo.getSelectedItem();
        double widthScale = "thin".equals(buildVal) ? 0.85 : "thick".equals(buildVal) ? 1.15 : 1.0;
        if (widthScale != 1.0) {
            String wrapTransform = String.format(Locale.US,
                    "translate(200 0) scale(%.4f 1) translate(-200 0)", widthScale);
            List<String> scaled = new ArrayList<>(fragments.size());
            for (String frag : fragments) {
                scaled.add("<g transform=\"" + wrapTransform + "\">" + frag + "</g>");
            }
            fragments = scaled;
        }

        faceMakerPreview.setCompositeFragments(fragments.isEmpty() ? null : fragments);
    }

    /**
     * Applies the per-feature color substitutions driven by the color-picker buttons for
     * the given {@code feature}.  Only color-enabled features have buttons; all others are
     * returned unchanged.
     *
     * <p>For {@code head}, both {@code $[faceShave]} and {@code $[headShave]} are
     * replaced with color 2, since the SVG templates use either spelling.
     */
    private String applyFaceMakerColors(String frag, String feature) {
        JButton[] btns    = faceMakerColorBtns.get(feature);
        String[][] colDef = FEATURE_COLOR_DEFS.get(feature);
        if (btns == null || colDef == null) return frag;
        String[] placeholders = colDef[0];
        for (int i = 0; i < btns.length; i++) {
            if (btns[i] != null && i < placeholders.length && placeholders[i] != null) {
                frag = frag.replace(placeholders[i], colorToHex(btns[i].getBackground()));
            }
        }
        // head: $[faceShave] and $[headShave] are two spellings for the same concept (shave color)
        if ("head".equals(feature) && btns.length >= 2 && btns[1] != null) {
            frag = frag.replace("$[headShave]", colorToHex(btns[1].getBackground()));
        }
        return frag;
    }

    /**
     * Replaces any remaining {@code $[...]} color placeholders with sensible defaults
     * so that all SVG fragments render with valid color values.
     */
    private static String applyDefaultColorSubstitutions(String frag) {
        frag = frag.replace("$[skinColor]",   "#f2d6cb");
        frag = frag.replace("$[hairColor]",   "#272421");
        frag = frag.replace("$[hairColor2]",  "#272421");
        frag = frag.replace("$[primary]",     "#89bfd3");
        frag = frag.replace("$[secondary]",   "#7a1319");
        frag = frag.replace("$[accent]",      "#07364f");
        frag = frag.replace("$[faceShave]",   "rgba(0,0,0,0)");
        frag = frag.replace("$[headShave]",   "rgba(0,0,0,0)");
        return frag;
    }

    /** Randomly selects one variant for every feature that has data loaded. */
    private void randomizeFaceMaker() {
        if (svgsData == null) return;
        Random rnd = new Random();
        for (String feature : FACE_DRAW_ORDER) {
            JComboBox<String> combo = faceMakerCombos.get(feature);
            if (combo == null || combo.getItemCount() <= 1) continue;
            // item 0 is "(none)"; items 1+ are real variants
            combo.setSelectedIndex(1 + rnd.nextInt(combo.getItemCount() - 1));
        }
    }

    /** Resets all face maker combo boxes to {@code "(none)"}. */
    private void clearFaceMaker() {
        for (JComboBox<String> combo : faceMakerCombos.values()) {
            combo.setSelectedIndex(0);
        }
    }

    // ── Face Rules tab ────────────────────────────────────────────────────────

    /**
     * Builds the Face Rules sub-tab.
     *
     * <p>The tab presents a table where each row defines a single rule that the
     * face generator should consult when selecting parts.  A rule pins a specific
     * {@code feature/id} part to a set of conditions:
     * <ul>
     *   <li><b>Genders</b> – comma-separated list of genders the rule applies to
     *       ({@code "male"}, {@code "female"}, or both).
     *   <li><b>Emotions</b> – comma-separated list of emotions ({@code "normal"},
     *       {@code "happy"}, {@code "sad"}, {@code "anxious"}, {@code "angry"}).
     *   <li><b>MinWealth</b> – minimum wealth integer threshold (0 = unrestricted).
     *   <li><b>MinAge</b> – minimum age integer threshold (0 = unrestricted).
     *   <li><b>ClothesTypes</b> – comma-separated clothes-type values
     *       ({@code "normal"}, {@code "work"}, {@code "sport"}, {@code "gym"}).
     *   <li><b>Percentage</b> – integer 1–100; chance the rule fires when conditions match
     *       (100 = always; omitted/0 in JSON is treated as 100).
     *   <li><b>Mode</b> – {@code "include"} (part is eligible) or
     *       {@code "exclude"} (part is forbidden).
     * </ul>
     * Rows are serialised to / loaded from {@code assets/face/facerules.json}.
     */
    private JPanel buildFaceRulesTab() {
        faceRulesFileField.setEditable(false);

        // ── Configure table ───────────────────────────────────────────────────

        configureTable(faceRulesTable);
        faceRulesTable.getColumnModel().getColumn(0).setPreferredWidth(80);  // Gender
        faceRulesTable.getColumnModel().getColumn(1).setPreferredWidth(100); // Emotion
        faceRulesTable.getColumnModel().getColumn(2).setPreferredWidth(80);  // MinWealth
        faceRulesTable.getColumnModel().getColumn(3).setPreferredWidth(60);  // MinAge
        faceRulesTable.getColumnModel().getColumn(4).setPreferredWidth(100); // ClothesType
        faceRulesTable.getColumnModel().getColumn(5).setPreferredWidth(80);  // Percentage
        faceRulesTable.getColumnModel().getColumn(6).setPreferredWidth(70);  // Priority
        faceRulesTable.getColumnModel().getColumn(7).setPreferredWidth(280); // Include
        faceRulesTable.getColumnModel().getColumn(8).setPreferredWidth(280); // Exclude

        // Gender column – preset combo
        JComboBox<String> gendersCombo = new JComboBox<>(FACE_RULE_GENDER_PRESETS);
        gendersCombo.setEditable(true);
        faceRulesTable.getColumnModel().getColumn(0)
                .setCellEditor(new DefaultCellEditor(gendersCombo));

        // Emotion column – preset combo (single emotion or empty)
        JComboBox<String> emotionsCombo = new JComboBox<>();
        emotionsCombo.setEditable(true);
        emotionsCombo.addItem("");
        for (String e : FACE_RULE_EMOTIONS) emotionsCombo.addItem(e);
        faceRulesTable.getColumnModel().getColumn(1)
                .setCellEditor(new DefaultCellEditor(emotionsCombo));

        // ClothesType column – preset combo (single type or empty)
        JComboBox<String> clothesCombo = new JComboBox<>();
        clothesCombo.setEditable(true);
        clothesCombo.addItem("");
        for (String c : FACE_RULE_CLOTHES_TYPES) clothesCombo.addItem(c);
        faceRulesTable.getColumnModel().getColumn(4)
                .setCellEditor(new DefaultCellEditor(clothesCombo));

        // ── Custom cell editor / renderer for Include (col 7) and Exclude (col 8) ─
        //
        // Each cell renders as [  text label  ][▼].  A single click activates the
        // editor (text field + ▼ button).  Clicking ▼, or double-clicking anywhere
        // in the cell, opens the SVG-ID picker dialog.

        /** Renderer: shows the cell value followed by a drop-down button. */
        class SvgListCellRenderer implements TableCellRenderer {
            private final JPanel  panel = new JPanel(new BorderLayout());
            private final JLabel  label = new JLabel();
            private final JButton btn   = new JButton("\u25BC");
            SvgListCellRenderer() {
                btn.setPreferredSize(new Dimension(28, 0));
                btn.setMargin(new Insets(0, 2, 0, 2));
                btn.setFocusable(false);
                label.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 0));
                panel.add(label, BorderLayout.CENTER);
                panel.add(btn,   BorderLayout.EAST);
            }
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v,
                    boolean sel, boolean foc, int r, int c) {
                label.setText(v == null ? "" : v.toString());
                Color bg = sel ? t.getSelectionBackground() : t.getBackground();
                panel.setBackground(bg);
                label.setForeground(sel ? t.getSelectionForeground() : t.getForeground());
                label.setOpaque(false);
                return panel;
            }
        }

        /** Editor: text field + ▼ button.  ▼ and double-click open the picker. */
        class SvgListCellEditor extends AbstractCellEditor implements TableCellEditor {
            private final JPanel     panel = new JPanel(new BorderLayout());
            private final JTextField field = new JTextField();
            private final JButton    btn   = new JButton("\u25BC");
            private EventObject triggerEvent;
            private int editRow = -1, editCol = -1;
            SvgListCellEditor() {
                btn.setPreferredSize(new Dimension(28, 0));
                btn.setMargin(new Insets(0, 2, 0, 2));
                btn.setFocusable(false);
                field.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 0));
                panel.add(field, BorderLayout.CENTER);
                panel.add(btn,   BorderLayout.EAST);
                btn.addActionListener(ae -> {
                    int r = editRow, c = editCol;
                    faceRulesModel.setValueAt(field.getText(), r, c);
                    fireEditingCanceled();
                    showSvgPickerDialog(r, c);
                });
            }
            @Override public boolean isCellEditable(EventObject e) {
                triggerEvent = e; return true;
            }
            @Override public Object getCellEditorValue() { return field.getText(); }
            @Override public Component getTableCellEditorComponent(JTable t, Object v,
                    boolean sel, int row, int col) {
                editRow = row; editCol = col;
                field.setText(v == null ? "" : v.toString());
                if (triggerEvent instanceof MouseEvent
                        && ((MouseEvent) triggerEvent).getClickCount() >= 2) {
                    SwingUtilities.invokeLater(() -> {
                        faceRulesModel.setValueAt(field.getText(), editRow, editCol);
                        fireEditingCanceled();
                        showSvgPickerDialog(editRow, editCol);
                    });
                }
                return panel;
            }
        }

        SvgListCellRenderer svgRenderer = new SvgListCellRenderer();
        faceRulesTable.getColumnModel().getColumn(7).setCellRenderer(svgRenderer);
        faceRulesTable.getColumnModel().getColumn(8).setCellRenderer(svgRenderer);
        faceRulesTable.getColumnModel().getColumn(7).setCellEditor(new SvgListCellEditor());
        faceRulesTable.getColumnModel().getColumn(8).setCellEditor(new SvgListCellEditor());

        // ── Row toolbar ───────────────────────────────────────────────────────

        JButton addBtn    = new JButton("Add Rule");
        JButton deleteBtn = new JButton("Delete Rule");

        addBtn.addActionListener((ActionEvent e) -> {
            faceRulesModel.addRow(new Object[]{"", "normal", 0, 0, "", 100, 0, "", ""});
            int last = faceRulesModel.getRowCount() - 1;
            faceRulesTable.scrollRectToVisible(faceRulesTable.getCellRect(last, 0, true));
            faceRulesTable.setRowSelectionInterval(last, last);
            faceRulesTable.editCellAt(last, 0);
        });
        deleteBtn.addActionListener((ActionEvent e) -> deleteSelectedRow(faceRulesTable, faceRulesModel));

        JPanel rowToolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        rowToolbar.add(addBtn);
        rowToolbar.add(deleteBtn);

        // ── File toolbar ──────────────────────────────────────────────────────

        JPanel fileRow = new JPanel(new BorderLayout(4, 0));
        fileRow.setBorder(BorderFactory.createEmptyBorder(0, 8, 2, 8));
        fileRow.add(new JLabel("File:"), BorderLayout.WEST);
        fileRow.add(faceRulesFileField, BorderLayout.CENTER);

        JPanel fileMeta = new JPanel(new BorderLayout());
        fileMeta.setBorder(BorderFactory.createTitledBorder("File"));
        fileMeta.add(fileRow, BorderLayout.CENTER);

        JButton openBtn   = new JButton("Open\u2026");
        JButton saveBtn   = new JButton("Save");
        JButton saveAsBtn = new JButton("Save As\u2026");
        openBtn.addActionListener((ActionEvent e) -> openFaceRules());
        saveBtn.addActionListener((ActionEvent e) -> saveFaceRules(false));
        saveAsBtn.addActionListener((ActionEvent e) -> saveFaceRules(true));

        JPanel fileToolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        fileToolbar.add(openBtn);
        fileToolbar.add(saveBtn);
        fileToolbar.add(saveAsBtn);

        JPanel north = new JPanel(new BorderLayout());
        north.add(fileMeta,    BorderLayout.CENTER);
        north.add(fileToolbar, BorderLayout.SOUTH);

        // ── Assemble ──────────────────────────────────────────────────────────

        JPanel south = new JPanel(new BorderLayout());
        south.add(rowToolbar, BorderLayout.WEST);

        JPanel tab = new JPanel(new BorderLayout());
        tab.add(north,                          BorderLayout.NORTH);
        tab.add(new JScrollPane(faceRulesTable), BorderLayout.CENTER);
        tab.add(south,                          BorderLayout.SOUTH);
        return tab;
    }

    // ── SVG ID picker dialog ──────────────────────────────────────────────────

    /**
     * Opens a scrollable checkbox dialog listing all available {@code feature.id}
     * entries from the loaded {@code svgs.json}.  Items already present in the
     * target cell are pre-checked.  On confirmation the cell is updated with the
     * new comma-separated selection.
     *
     * @param row row index in {@link #faceRulesTable}
     * @param col column index – 7 (Include) or 8 (Exclude)
     */
    private void showSvgPickerDialog(int row, int col) {
        // Build a sorted map of feature → sorted list of ids from svgsData
        Map<String, List<String>> featureIds = new TreeMap<>();
        if (svgsData != null) {
            for (Map.Entry<String, JsonElement> featureEntry : svgsData.entrySet()) {
                String feature = featureEntry.getKey();
                if (!featureEntry.getValue().isJsonObject()) continue;
                List<String> ids = new ArrayList<>();
                for (Map.Entry<String, JsonElement> idEntry :
                        featureEntry.getValue().getAsJsonObject().entrySet()) {
                    ids.add(idEntry.getKey());
                }
                java.util.Collections.sort(ids);
                featureIds.put(feature, ids);
            }
        }

        if (featureIds.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "No SVG data loaded. Please load svgs.json first via the SVG Index tab.",
                    "No SVG Data", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Parse the current cell value into a set of already-selected entries
        String currentVal = cellStr(faceRulesModel, row, col).trim();
        java.util.Set<String> selected = new java.util.LinkedHashSet<>();
        if (!currentVal.isEmpty()) {
            for (String part : currentVal.split(",")) {
                String t = part.trim();
                if (!t.isEmpty()) selected.add(t);
            }
        }

        // ── Preview panel (top-right of dialog) ───────────────────────────────
        SvgPreviewPanel pickerPreview = new SvgPreviewPanel();
        pickerPreview.setPreferredSize(new Dimension(200, 200));
        pickerPreview.setMinimumSize(new Dimension(160, 160));
        pickerPreview.setBorder(BorderFactory.createTitledBorder("Preview"));

        // Helper: update preview from a "feature.id" key
        java.util.function.Consumer<String> updatePreview = key -> {
            if (key == null || key.isEmpty() || svgsData == null) {
                pickerPreview.setFeatureName("");
                pickerPreview.setSvgFragment("");
                return;
            }
            int dot = key.indexOf('.');
            if (dot <= 0 || dot == key.length() - 1) return;
            String feat = key.substring(0, dot);
            String id   = key.substring(dot + 1);
            pickerPreview.setFeatureName(feat);
            if (svgsData.has(feat) && svgsData.getAsJsonObject(feat).has(id)) {
                pickerPreview.setSvgFragment(
                        svgsData.getAsJsonObject(feat).get(id).getAsString());
            } else {
                pickerPreview.setSvgFragment("");
            }
        };

        // Seed the preview with the first already-selected item (if any)
        updatePreview.accept(selected.isEmpty() ? null : selected.iterator().next());

        // Build a feature.id → gender map from the SVG Index table (col 2)
        Map<String, String> genderMap = new LinkedHashMap<>();
        for (int r = 0; r < svgIndexModel.getRowCount(); r++) {
            String feat   = cellStr(svgIndexModel, r, 0);
            String id     = cellStr(svgIndexModel, r, 1);
            String gender = cellStr(svgIndexModel, r, 2);
            if (!feat.isEmpty() && !id.isEmpty()) {
                genderMap.put(feat + "." + id, gender);
            }
        }

        // Build the checkbox panel, one section per feature.
        // boxKeyMap maps each checkbox to the clean "feature.id" value (used when collecting results).
        JPanel checkPanel = new JPanel();
        checkPanel.setLayout(new BoxLayout(checkPanel, BoxLayout.Y_AXIS));
        Map<JCheckBox, String> boxKeyMap = new LinkedHashMap<>();

        for (Map.Entry<String, List<String>> entry : featureIds.entrySet()) {
            String feature = entry.getKey();

            // Feature header label
            JLabel header = new JLabel(feature);
            header.setFont(header.getFont().deriveFont(Font.BOLD));
            header.setBorder(BorderFactory.createEmptyBorder(6, 4, 2, 4));
            header.setAlignmentX(Component.LEFT_ALIGNMENT);
            checkPanel.add(header);

            for (String id : entry.getValue()) {
                String key = feature + "." + id;
                // Append [male] or [female] hint; omit for "both" / unknown
                String gender  = genderMap.getOrDefault(key, "");
                String label   = ("male".equals(gender) || "female".equals(gender))
                        ? key + " [" + gender + "]"
                        : key;
                JCheckBox cb = new JCheckBox(label, selected.contains(key));
                cb.setAlignmentX(Component.LEFT_ALIGNMENT);
                // Update preview when the checkbox is clicked or hovered
                cb.addItemListener(ie  -> updatePreview.accept(key));
                cb.addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                        updatePreview.accept(key);
                    }
                });
                checkPanel.add(cb);
                boxKeyMap.put(cb, key);
            }
        }

        JScrollPane scroll = new JScrollPane(checkPanel);
        scroll.setPreferredSize(new Dimension(400, 500));

        // Main dialog content: checkbox scroll pane on the left, preview on the right
        JPanel content = new JPanel(new BorderLayout(8, 0));
        content.add(scroll,        BorderLayout.CENTER);
        content.add(pickerPreview, BorderLayout.EAST);

        String colName = col == 7 ? "Include" : "Exclude";
        int result = JOptionPane.showConfirmDialog(
                this, content,
                "Select SVG IDs for " + colName,
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);

        if (result != JOptionPane.OK_OPTION) return;

        // Collect checked keys in order (feature-sorted, then id-sorted)
        List<String> newList = new ArrayList<>();
        for (Map.Entry<JCheckBox, String> entry : boxKeyMap.entrySet()) {
            if (entry.getKey().isSelected()) newList.add(entry.getValue());
        }
        faceRulesModel.setValueAt(String.join(",", newList), row, col);
    }

    // ── File operations: facerules.json ───────────────────────────────────────

    private void openFaceRules() {
        File chosen = chooseOpenFile(faceRulesFile, "assets/face");
        if (chosen != null) loadFaceRulesFromFile(chosen);
    }

    /**
     * Parses {@code facerules.json} and populates the face-rules table.
     *
     * <p>The JSON structure is:
     * <pre>
     * {
     *   "rules": [
     *     {
     *       "gender":     "female",
     *       "emotion":    "happy",
     *       "minWealth":  0,
     *       "minAge":     0,
     *       "clothesType":"",
     *       "percentage": 100,
     *       "priority":   0,
     *       "include":    ["mouth.mouth4", "mouth.mouth5", "eyes.female5", "eyes.female6"],
     *       "exclude":    []
     *     },
     *     ...
     *   ]
     * }
     * </pre>
     */
    private void loadFaceRulesFromFile(File file) {
        try (Reader reader = new FileReader(file)) {
            JsonObject root = new Gson().fromJson(reader, JsonObject.class);
            faceRulesModel.setRowCount(0);

            JsonArray rules = root.has("rules") ? root.getAsJsonArray("rules") : new JsonArray();

            // Collect raw row data so we can sort by priority before adding to the table
            List<Object[]> rows = new ArrayList<>();
            for (JsonElement ruleEl : rules) {
                JsonObject rule = ruleEl.getAsJsonObject();
                String gender      = rule.has("gender")      ? rule.get("gender").getAsString()      : "";
                String emotion     = rule.has("emotion")     ? rule.get("emotion").getAsString()     : "";
                int    minWealth   = rule.has("minWealth")   ? rule.get("minWealth").getAsInt()      : 0;
                int    minAge      = rule.has("minAge")      ? rule.get("minAge").getAsInt()         : 0;
                String clothesType = rule.has("clothesType") ? rule.get("clothesType").getAsString() : "";
                int    percentage  = rule.has("percentage")  ? rule.get("percentage").getAsInt()     : 100;
                int    priority    = rule.has("priority")    ? rule.get("priority").getAsInt()       : 0;

                String include = jsonArrayToString(rule, "include");
                String exclude = jsonArrayToString(rule, "exclude");

                rows.add(new Object[]{gender, emotion, minWealth, minAge, clothesType, percentage, priority, include, exclude});
            }

            // Sort ascending by priority so higher-priority rules appear later in the table
            rows.sort((a, b) -> Integer.compare((int) a[6], (int) b[6]));
            for (Object[] row : rows) {
                faceRulesModel.addRow(row);
            }

            faceRulesFile = file;
            faceRulesFileField.setText(file.getAbsolutePath());
            statusLabel.setText("Face rules loaded: " + file.getAbsolutePath()
                    + "  (" + faceRulesModel.getRowCount() + " rules)");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Error loading file:\n" + ex.getMessage(),
                    "Load Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Serialises the face-rules table back to {@code facerules.json} format.
     * Rows with both Include and Exclude empty are skipped with a warning dialog.
     */
    private void saveFaceRules(boolean saveAs) {
        faceRulesFile = resolveTargetFile(faceRulesFile, saveAs);
        if (faceRulesFile == null) return;

        JsonArray rules   = new JsonArray();
        List<String> skipped = new ArrayList<>();

        // Collect rows, then sort ascending by priority so the JSON reflects application order
        List<Object[]> rowData = new ArrayList<>();
        for (int r = 0; r < faceRulesModel.getRowCount(); r++) {
            String gender      = cellStr(faceRulesModel, r, 0).trim();
            String emotion     = cellStr(faceRulesModel, r, 1).trim();
            int    minWealth   = intVal(faceRulesModel.getValueAt(r, 2));
            int    minAge      = intVal(faceRulesModel.getValueAt(r, 3));
            String clothesType = cellStr(faceRulesModel, r, 4).trim();
            int    percentage  = intVal(faceRulesModel.getValueAt(r, 5));
            int    priority    = intVal(faceRulesModel.getValueAt(r, 6));
            String include     = cellStr(faceRulesModel, r, 7).trim();
            String exclude     = cellStr(faceRulesModel, r, 8).trim();

            if (include.isEmpty() && exclude.isEmpty()) {
                skipped.add("row " + (r + 1) + " (no include or exclude entries)");
                continue;
            }
            rowData.add(new Object[]{gender, emotion, minWealth, minAge, clothesType, percentage, priority, include, exclude});
        }

        rowData.sort((a, b) -> Integer.compare((int) a[6], (int) b[6]));

        for (Object[] row : rowData) {
            JsonObject rule = new JsonObject();
            rule.addProperty("gender",      (String) row[0]);
            rule.addProperty("emotion",     (String) row[1]);
            rule.addProperty("minWealth",   (int)    row[2]);
            rule.addProperty("minAge",      (int)    row[3]);
            rule.addProperty("clothesType", (String) row[4]);
            rule.addProperty("percentage",  Math.min(100, Math.max(1, (int) row[5])));
            rule.addProperty("priority",    Math.max(0, (int) row[6]));
            rule.add("include", stringToJsonArray((String) row[7]));
            rule.add("exclude", stringToJsonArray((String) row[8]));
            rules.add(rule);
        }

        if (!skipped.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "The following rows were skipped because both Include and Exclude are empty:\n"
                            + String.join("\n", skipped),
                    "Save Warning", JOptionPane.WARNING_MESSAGE);
        }

        JsonObject root = new JsonObject();
        root.add("rules", rules);

        try (Writer writer = new FileWriter(faceRulesFile)) {
            new GsonBuilder().setPrettyPrinting().create().toJson(root, writer);
            faceRulesFileField.setText(faceRulesFile.getAbsolutePath());
            statusLabel.setText("Face rules saved: " + faceRulesFile.getAbsolutePath()
                    + "  (" + rules.size() + " rules)");
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    "Error saving file:\n" + ex.getMessage(),
                    "Save Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Converts a JSON array of strings under {@code key} in {@code obj} to a
     * comma-separated string.  Returns an empty string when the key is absent.
     */
    private static String jsonArrayToString(JsonObject obj, String key) {
        if (!obj.has(key)) return "";
        JsonArray arr = obj.getAsJsonArray(key);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arr.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(arr.get(i).getAsString());
        }
        return sb.toString();
    }

    /**
     * Splits a comma-separated string into a {@link JsonArray} of trimmed strings,
     * omitting blank elements.
     */
    private static JsonArray stringToJsonArray(String csv) {
        JsonArray arr = new JsonArray();
        if (csv == null || csv.isBlank()) return arr;
        for (String part : csv.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) arr.add(trimmed);
        }
        return arr;
    }

    /**
     * Shows the shared {@link JColorChooser} dialog.  Because the same instance is reused
     * on every invocation, the built-in "Recent Colors" swatch panel accumulates a history
     * of previously confirmed picks so the user can quickly return to earlier colors.
     *
     * <p>The {@code initialColor} is pre-loaded as the chooser's current selection before
     * the dialog opens.  This means the button's <em>current</em> color always appears as
     * the selected swatch, making it easy to click OK without changing anything to revert
     * to the previous value.
     *
     * @param parent       parent component for dialog placement
     * @param title        dialog title bar text
     * @param initialColor the color to pre-select (typically the button's current color)
     * @return the confirmed {@link Color}, or {@code null} if the dialog was cancelled
     */
    private Color showColorPickerDialog(Component parent, String title, Color initialColor) {
        sharedColorChooser.setColor(initialColor);
        final Color[] result = {null};
        JDialog dialog = JColorChooser.createDialog(
                parent, title, true, sharedColorChooser,
                e -> result[0] = sharedColorChooser.getColor(),  // OK
                null);                                           // Cancel
        try {
            dialog.setVisible(true);
        } finally {
            dialog.dispose();
        }
        return result[0];
    }

    /**
     * Creates a small square color-swatch button.  The button paints itself as a solid
     * filled rectangle in its background color, which is reliable across all Swing
     * look-and-feels (including macOS Aqua, which ignores {@code setBackground()} in
     * the default button UI).  Clicking the button opens a {@link JColorChooser} (wired
     * up by the caller).
     *
     * @param hexDefault default hex color string, e.g. {@code "#f2d6cb"}
     * @param tooltip    tooltip text describing what the color controls
     */
    private static JButton createColorButton(String hexDefault, String tooltip) {
        JButton btn = new JButton() {
            @Override
            protected void paintComponent(java.awt.Graphics g) {
                g.setColor(getBackground());
                g.fillRect(0, 0, getWidth(), getHeight());
            }
            @Override
            protected void paintBorder(java.awt.Graphics g) {
                g.setColor(Color.DARK_GRAY);
                g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
            }
        };
        btn.setPreferredSize(new Dimension(28, 22));
        btn.setMinimumSize(new Dimension(28, 22));
        btn.setMaximumSize(new Dimension(28, 22));
        btn.setBackground(hexToColor(hexDefault));
        btn.setOpaque(true);
        btn.setFocusPainted(false);
        btn.setToolTipText(tooltip + " – " + hexDefault);
        return btn;
    }

    /** Converts a {@link Color} to a lowercase 6-digit hex string, e.g. {@code "#f2d6cb"}. */
    private static String colorToHex(Color c) {
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }

    /**
     * Parses a 6-digit hex color string (with or without leading {@code #}) into a
     * {@link Color}.  Returns {@link Color#LIGHT_GRAY} on any parse error.
     */
    private static Color hexToColor(String hex) {
        try {
            String s = hex.startsWith("#") ? hex.substring(1) : hex;
            int r = Integer.parseInt(s.substring(0, 2), 16);
            int g = Integer.parseInt(s.substring(2, 4), 16);
            int b = Integer.parseInt(s.substring(4, 6), 16);
            return new Color(r, g, b);
        } catch (Exception e) {
            System.err.println("[SvgEditorPanel] Invalid hex color string: '" + hex + "' – " + e.getMessage());
            return Color.LIGHT_GRAY;
        }
    }
}

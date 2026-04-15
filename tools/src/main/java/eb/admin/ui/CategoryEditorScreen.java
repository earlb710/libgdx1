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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Main AWT/Swing screen for the Game Admin tool.
 * Allows viewing and editing of all category sections inside category_en.json.
 *
 * Tabs:
 *   1. Building Categories      – code, description, color
 *   2. Item Categories          – code, description
 *   3. Evidence Categories      – code, description
 *   4. Case Types               – code, name, description
 *   5. Discovery Methods        – code, name, description
 *   6. Evidence Modifiers       – code, name, description
 *   7. Evidence Items           – code, name, description
 *   8. Improvement Categories   – code, description, color, function
 *   9. Skill Categories         – code, name
 *  10. Skin Tone Categories     – code, name, rgb
 *  11. Gender Categories        – code, name
 *  12. Buildings                – id, name, category, description, improvements (buildings_en.json)
 *  13. Improvements             – id, name, attribute_modifiers (improvements_en.json)
 *  14. Company Types            – id, name, description, buildings (company_types_en.json)
 *  15. Names                    – person first-names, surnames, company name templates
 *  16. SVG                      – SVG Resource (svg_resource.json) + SVG Index (svgs-index.json)
 *  17. Case                     – step-by-step case generation testing (case type, leads, story tree)
 *  18. Templates › Interview    – edit interview_templates_en.json pools and question strings
 */
public class CategoryEditorScreen extends JFrame {

    private static final String WINDOW_TITLE = "Game Admin – Category Editor";
    private static final String DEFAULT_JSON_PATH = "assets/text/category_en.json";
    private static final Color ANNOTATION_FOREGROUND = new Color(0, 0, 139);

    // Metadata fields
    private final JTextField versionField = new JTextField(8);
    private final JComboBox<String> languageCombo = new JComboBox<>(EditorUtils.LANGUAGES);
    private final JTextField fileField = new JTextField();
    /** When true, combo ActionListener does not trigger a language switch. */
    private boolean suppressLangListener = false;

    // Tab pane and table models
    private final JTabbedPane tabbedPane = new JTabbedPane();
    private final DefaultTableModel buildingModel     = createModel(new String[]{"Code", "Description", "Color"});
    private final DefaultTableModel itemModel         = createModel(new String[]{"Code", "Description"});
    private final DefaultTableModel evidenceModel     = createModel(new String[]{"Code", "Description"});
    private final DefaultTableModel caseModel         = createModel(new String[]{"Code", "Name", "Description"});
    private final DefaultTableModel discoveryMethodModel =
            createModel(new String[]{"Code", "Name", "Description"});
    private final DefaultTableModel evidenceModifierModel =
            createModel(new String[]{"Code", "Name", "Description"});
    private final DefaultTableModel evidenceItemModel =
            createModel(new String[]{"Code", "Name", "Description"});
    private final DefaultTableModel improvementCategoryModel =
            createModel(new String[]{"Code", "Description", "Color", "Actions"});
    private final DefaultTableModel skillCategoryModel =
            createModel(new String[]{"Code", "Name"});
    private final DefaultTableModel skinToneCategoryModel =
            createModel(new String[]{"Code", "Name", "RGB", "Percentage"});
    private final DefaultTableModel genderCategoryModel =
            createModel(new String[]{"Code", "Name"});
    private final DefaultTableModel motiveCategoryModel =
            createModel(new String[]{"Code", "Name", "Description"});

    /** Case type checkboxes shown beneath the motive categories table. */
    private final Map<String, JCheckBox> motiveCaseTypeCheckboxes = new java.util.LinkedHashMap<>();
    /** Parallel list storing the case-type codes for each row of motiveCategoryModel. */
    private final List<List<String>> motiveCaseTypesPerRow = new ArrayList<>();
    /** The motive table currently being edited — needed for selection listener. */
    private JTable motiveTable;
    /** Panel holding the case-type checkboxes for the motive tab. */
    private final JPanel motiveCaseTypeCheckboxPanel = new JPanel();

    // Status bar
    private final JLabel statusLabel = new JLabel("No file loaded – use File › Open to load a JSON file.");

    private final BuildingsEditorPanel      buildingsPanel     = new BuildingsEditorPanel(statusLabel);
    private final ImprovementsEditorPanel   improvementsPanel  = new ImprovementsEditorPanel(statusLabel);
    private final CompanyTypesEditorPanel   companyTypesPanel  = new CompanyTypesEditorPanel(statusLabel);
    private final NamesEditorPanel          namesPanel         = new NamesEditorPanel(statusLabel);
    private final SvgEditorPanel            svgPanel           = new SvgEditorPanel(statusLabel);
    private final CaseEditorPanel               casePanel              = new CaseEditorPanel(statusLabel);
    private final InterviewTemplateEditorPanel  interviewTemplatePanel = new InterviewTemplateEditorPanel(statusLabel);
    private final CaseDescriptionTemplatePanel  caseDescTemplatePanel  = new CaseDescriptionTemplatePanel(statusLabel);

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

        languageCombo.addActionListener(e -> {
            if (!suppressLangListener) {
                switchLanguage((String) languageCombo.getSelectedItem());
            }
        });

        setJMenuBar(buildMenuBar());

        // Inner category sub-tabs (Building / Item / Evidence / Case Types / Improvement Categories)
        JTabbedPane categoryTabs = new JTabbedPane();
        categoryTabs.addTab("Building Categories",     buildTabPanel(buildingModel,             true,  false));
        categoryTabs.addTab("Item Categories",         buildTabPanel(itemModel,                 false, false));
        categoryTabs.addTab("Evidence Categories",     buildTabPanel(evidenceModel,             false, false));
        categoryTabs.addTab("Case Types",              buildCodeNameDescTabPanel(caseModel));
        categoryTabs.addTab("Discovery Methods",      buildCodeNameDescTabPanel(discoveryMethodModel));
        categoryTabs.addTab("Evidence Modifiers",     buildCodeNameDescTabPanel(evidenceModifierModel));
        categoryTabs.addTab("Evidence Items",         buildCodeNameDescTabPanel(evidenceItemModel));
        categoryTabs.addTab("Improvement Categories",  buildTabPanel(improvementCategoryModel,  true,  true));
        categoryTabs.addTab("Skill Categories",        buildSkillCategoryTabPanel());
        categoryTabs.addTab("Skin Tone Categories",    buildSkinToneCategoryTabPanel());
        categoryTabs.addTab("Gender Categories",       buildGenderCategoryTabPanel());
        categoryTabs.addTab("Motive Categories",      buildMotiveCategoryTabPanel());

        // "Categories" top-level panel: meta (version/lang) + inner sub-tabs
        JPanel categoriesPanel = new JPanel(new BorderLayout(0, 4));
        categoriesPanel.add(buildMetaPanel(), BorderLayout.NORTH);
        categoriesPanel.add(categoryTabs,     BorderLayout.CENTER);

        // Outer tabs: "Categories", "Buildings", "Improvements", "Company Types", "Names", "SVG", and "Case"
        tabbedPane.addTab("Categories",    categoriesPanel);
        tabbedPane.addTab("Buildings",     buildingsPanel);
        tabbedPane.addTab("Improvements",  improvementsPanel);
        tabbedPane.addTab("Company Types", companyTypesPanel);
        tabbedPane.addTab("Names",         namesPanel);
        tabbedPane.addTab("SVG",           svgPanel);
        tabbedPane.addTab("Case",          casePanel);

        JTabbedPane templatesTabs = new JTabbedPane();
        templatesTabs.addTab("Interview", interviewTemplatePanel);
        templatesTabs.addTab("Case Description", caseDescTemplatePanel);
        tabbedPane.addTab("Templates",    templatesTabs);

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

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("File Metadata"));
        panel.add(metaTop, BorderLayout.NORTH);
        panel.add(fileRow, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Builds a panel containing an editable JTable and a button toolbar.
     *
     * @param model       the table model to use
     * @param hasColor    whether the table has a Color column (building/improvement categories)
     * @param hasFunction whether the table has a Function column (improvement categories only)
     */
    private JPanel buildTabPanel(DefaultTableModel model, boolean hasColor, boolean hasFunction) {
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

        if (hasColor && hasFunction) {
            // Improvement categories: Code, Description, Color, Function
            table.getColumnModel().getColumn(0).setPreferredWidth(130);
            table.getColumnModel().getColumn(1).setPreferredWidth(380);
            table.getColumnModel().getColumn(2).setPreferredWidth(100);
            table.getColumnModel().getColumn(3).setPreferredWidth(250);
            table.getColumnModel().getColumn(2).setCellRenderer(new ColorCellRenderer());
        } else if (hasColor) {
            // Building categories: Code, Description, Color
            table.getColumnModel().getColumn(0).setPreferredWidth(140);
            table.getColumnModel().getColumn(1).setPreferredWidth(520);
            table.getColumnModel().getColumn(2).setPreferredWidth(100);
            table.getColumnModel().getColumn(2).setCellRenderer(new ColorCellRenderer());
        } else {
            // Item / Evidence / Case Types: Code, Description
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
            if (hasColor && hasFunction) {
                model.addRow(new Object[]{"", "", "", ""});
            } else if (hasColor) {
                model.addRow(new Object[]{"", "", ""});
            } else {
                model.addRow(new Object[]{"", ""});
            }
            int last = model.getRowCount() - 1;
            table.scrollRectToVisible(table.getCellRect(last, 0, true));
            table.setRowSelectionInterval(last, last);
        });

        deleteBtn.addActionListener((ActionEvent e) -> {
            if (table.isEditing()) {
                table.getCellEditor().cancelCellEditing();
            }
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

    /**
     * Builds a tab panel for a three-column table: Code, Name, Description.
     * Used by Case Types, Discovery Methods, Evidence Modifiers, and Evidence Items.
     */
    private JPanel buildCodeNameDescTabPanel(DefaultTableModel model) {
        JTable table = new JTable(model) {
            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int col) {
                Component c = super.prepareRenderer(renderer, row, col);
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
                Color bg = getAnnotationColor((DefaultTableModel) getModel(), row, column);
                if (bg != null && editor instanceof DefaultCellEditor) {
                    ((DefaultCellEditor) editor).getComponent().setBackground(bg);
                }
                return editor;
            }
        };
        categoryTables.add(table);
        table.setRowHeight(26);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
        table.getColumnModel().getColumn(0).setPreferredWidth(160);
        table.getColumnModel().getColumn(1).setPreferredWidth(160);
        table.getColumnModel().getColumn(2).setPreferredWidth(500);

        JScrollPane scrollPane = new JScrollPane(table);

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

        JButton addBtn    = new JButton("Add Row");
        JButton deleteBtn = new JButton("Delete Row");
        JButton saveBtn   = new JButton("Save");

        addBtn.addActionListener((ActionEvent e) -> {
            model.addRow(new Object[]{"", "", ""});
            int last = model.getRowCount() - 1;
            table.scrollRectToVisible(table.getCellRect(last, 0, true));
            table.setRowSelectionInterval(last, last);
        });

        deleteBtn.addActionListener((ActionEvent e) -> {
            if (table.isEditing()) {
                table.getCellEditor().cancelCellEditing();
            }
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

    /**
     * Builds a custom tab panel for Motive Categories that includes the standard
     * Code/Name/Description table plus a row of case-type checkboxes below it.
     * When a motive row is selected, the checkboxes reflect which case types
     * that motive is associated with.
     */
    private JPanel buildMotiveCategoryTabPanel() {
        JTable table = new JTable(motiveCategoryModel) {
            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int col) {
                Component c = super.prepareRenderer(renderer, row, col);
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
                Color bg = getAnnotationColor((DefaultTableModel) getModel(), row, column);
                if (bg != null && editor instanceof DefaultCellEditor) {
                    ((DefaultCellEditor) editor).getComponent().setBackground(bg);
                }
                return editor;
            }
        };
        motiveTable = table;
        categoryTables.add(table);
        table.setRowHeight(26);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
        table.getColumnModel().getColumn(0).setPreferredWidth(160);
        table.getColumnModel().getColumn(1).setPreferredWidth(160);
        table.getColumnModel().getColumn(2).setPreferredWidth(500);

        JScrollPane motiveScrollPane = new JScrollPane(table);

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

        // --- Case type checkboxes panel ---
        motiveCaseTypeCheckboxPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 8, 2));
        motiveCaseTypeCheckboxPanel.setBorder(
                BorderFactory.createTitledBorder("Applicable Case Types (select a motive row)"));

        // When a row is selected, load its case-type checkboxes
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                loadMotiveCaseTypesForSelectedRow();
            }
        });

        JButton motiveAddBtn    = new JButton("Add Row");
        JButton motiveDeleteBtn = new JButton("Delete Row");
        JButton motiveSaveBtn   = new JButton("Save");

        motiveAddBtn.addActionListener((ActionEvent e) -> {
            motiveCategoryModel.addRow(new Object[]{"", "", ""});
            motiveCaseTypesPerRow.add(new ArrayList<>());
            int last = motiveCategoryModel.getRowCount() - 1;
            table.scrollRectToVisible(table.getCellRect(last, 0, true));
            table.setRowSelectionInterval(last, last);
        });

        motiveDeleteBtn.addActionListener((ActionEvent e) -> {
            if (table.isEditing()) {
                table.getCellEditor().cancelCellEditing();
            }
            int row = table.getSelectedRow();
            if (row >= 0) {
                motiveCategoryModel.removeRow(row);
                if (row < motiveCaseTypesPerRow.size()) {
                    motiveCaseTypesPerRow.remove(row);
                }
            } else {
                JOptionPane.showMessageDialog(this,
                        "Please select a row to delete.",
                        "No Selection", JOptionPane.WARNING_MESSAGE);
            }
        });

        motiveSaveBtn.addActionListener((ActionEvent e) -> saveFile(false));

        JPanel motiveButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        motiveButtons.add(motiveAddBtn);
        motiveButtons.add(motiveDeleteBtn);
        motiveButtons.add(Box.createHorizontalStrut(12));
        motiveButtons.add(motiveSaveBtn);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(motiveCaseTypeCheckboxPanel, BorderLayout.CENTER);
        bottomPanel.add(motiveButtons, BorderLayout.SOUTH);

        JPanel motivePanel = new JPanel(new BorderLayout());
        motivePanel.add(motiveScrollPane, BorderLayout.CENTER);
        motivePanel.add(bottomPanel,      BorderLayout.SOUTH);

        return motivePanel;
    }

    /**
     * Rebuilds the case-type checkboxes for the Motive Categories panel
     * using the current set of case types from the case type table model.
     */
    private void refreshMotiveCaseTypeCheckboxes() {
        motiveCaseTypeCheckboxPanel.removeAll();
        motiveCaseTypeCheckboxes.clear();

        for (int r = 0; r < caseModel.getRowCount(); r++) {
            String code = cellStr(caseModel, r, 0);
            String label = cellStr(caseModel, r, 1);
            if (code == null || code.isEmpty()) continue;
            if (label == null || label.isEmpty()) label = code;
            JCheckBox cb = new JCheckBox(label);
            cb.setActionCommand(code);
            cb.setEnabled(false); // disabled until a row is selected
            cb.addActionListener(e -> saveMotiveCaseTypesForSelectedRow());
            motiveCaseTypeCheckboxes.put(code, cb);
            motiveCaseTypeCheckboxPanel.add(cb);
        }
        motiveCaseTypeCheckboxPanel.revalidate();
        motiveCaseTypeCheckboxPanel.repaint();
    }

    /** Loads checkbox state from the parallel list for the currently selected motive row. */
    private void loadMotiveCaseTypesForSelectedRow() {
        int row = motiveTable != null ? motiveTable.getSelectedRow() : -1;
        for (JCheckBox cb : motiveCaseTypeCheckboxes.values()) {
            cb.setSelected(false);
            cb.setEnabled(row >= 0);
        }
        if (row >= 0 && row < motiveCaseTypesPerRow.size()) {
            List<String> codes = motiveCaseTypesPerRow.get(row);
            for (String code : codes) {
                JCheckBox cb = motiveCaseTypeCheckboxes.get(code);
                if (cb != null) {
                    cb.setSelected(true);
                }
            }
        }
    }

    /** Saves checkbox state back to the parallel list for the currently selected motive row. */
    private void saveMotiveCaseTypesForSelectedRow() {
        int row = motiveTable != null ? motiveTable.getSelectedRow() : -1;
        if (row < 0 || row >= motiveCaseTypesPerRow.size()) return;
        List<String> codes = new ArrayList<>();
        for (Map.Entry<String, JCheckBox> entry : motiveCaseTypeCheckboxes.entrySet()) {
            if (entry.getValue().isSelected()) {
                codes.add(entry.getKey());
            }
        }
        motiveCaseTypesPerRow.set(row, codes);
    }

    /**
     * Builds the tab panel for the Skill Categories table.
     * Skill categories use {@code code} and {@code name} columns (not {@code description}).
     */
    private JPanel buildSkillCategoryTabPanel() {
        JTable table = new JTable(skillCategoryModel) {
            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int col) {
                Component c = super.prepareRenderer(renderer, row, col);
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
                Color bg = getAnnotationColor((DefaultTableModel) getModel(), row, column);
                if (bg != null && editor instanceof DefaultCellEditor) {
                    ((DefaultCellEditor) editor).getComponent().setBackground(bg);
                }
                return editor;
            }
        };
        categoryTables.add(table);
        table.setRowHeight(26);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
        table.getColumnModel().getColumn(0).setPreferredWidth(180);
        table.getColumnModel().getColumn(1).setPreferredWidth(540);

        JScrollPane scrollPane = new JScrollPane(table);

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

        JButton addBtn    = new JButton("Add Row");
        JButton deleteBtn = new JButton("Delete Row");
        JButton saveBtn   = new JButton("Save");

        addBtn.addActionListener((ActionEvent e) -> {
            skillCategoryModel.addRow(new Object[]{"", ""});
            int last = skillCategoryModel.getRowCount() - 1;
            table.scrollRectToVisible(table.getCellRect(last, 0, true));
            table.setRowSelectionInterval(last, last);
        });

        deleteBtn.addActionListener((ActionEvent e) -> {
            if (table.isEditing()) {
                table.getCellEditor().cancelCellEditing();
            }
            int row = table.getSelectedRow();
            if (row >= 0) {
                skillCategoryModel.removeRow(row);
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

    /**
     * Builds the tab panel for the Skin Tone Categories table.
     * Skin tone categories use {@code code}, {@code name}, and {@code rgb} columns.
     */
    private JPanel buildSkinToneCategoryTabPanel() {
        JTable table = new JTable(skinToneCategoryModel) {
            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int col) {
                Component c = super.prepareRenderer(renderer, row, col);
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
                Color bg = getAnnotationColor((DefaultTableModel) getModel(), row, column);
                if (bg != null && editor instanceof DefaultCellEditor) {
                    ((DefaultCellEditor) editor).getComponent().setBackground(bg);
                }
                return editor;
            }
        };
        categoryTables.add(table);
        table.setRowHeight(26);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
        table.getColumnModel().getColumn(0).setPreferredWidth(180);
        table.getColumnModel().getColumn(1).setPreferredWidth(260);
        table.getColumnModel().getColumn(2).setPreferredWidth(120);
        table.getColumnModel().getColumn(3).setPreferredWidth(90);

        JScrollPane scrollPane = new JScrollPane(table);

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

        JButton addBtn    = new JButton("Add Row");
        JButton deleteBtn = new JButton("Delete Row");
        JButton saveBtn   = new JButton("Save");

        addBtn.addActionListener((ActionEvent e) -> {
            skinToneCategoryModel.addRow(new Object[]{"", "", "", 10});
            int last = skinToneCategoryModel.getRowCount() - 1;
            table.scrollRectToVisible(table.getCellRect(last, 0, true));
            table.setRowSelectionInterval(last, last);
        });

        deleteBtn.addActionListener((ActionEvent e) -> {
            int row = table.getSelectedRow();
            if (row >= 0) {
                skinToneCategoryModel.removeRow(row);
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
            suppressLangListener = true;
            languageCombo.setSelectedItem(data.getLanguage() != null ? data.getLanguage() : "en");
            suppressLangListener = false;

            populateModel(buildingModel,            data.getBuilding_categories(),     true,  false);
            populateModel(itemModel,                data.getItem_categories(),          false, false);
            populateModel(evidenceModel,            data.getEvidence_categories(),      false, false);
            populateCodeNameDescModel(caseModel,         data.getCase_types());
            populateCodeNameDescModel(discoveryMethodModel, data.getDiscovery_methods());
            populateCodeNameDescModel(evidenceModifierModel, data.getEvidence_modifiers());
            populateCodeNameDescModel(evidenceItemModel,    data.getEvidence_items());
            populateModel(improvementCategoryModel, data.getImprovement_categories(),   true,  true);
            populateSkillCategoryModel(skillCategoryModel, data.getSkill_categories());
            populateSkinToneCategoryModel(skinToneCategoryModel, data.getSkin_tone_categories());
            populateGenderCategoryModel(genderCategoryModel, data.getGender_categories());
            populateCodeNameDescModel(motiveCategoryModel, data.getMotive_categories());
            populateMotiveCaseTypes(data.getMotive_categories());
            refreshMotiveCaseTypeCheckboxes();

            casePanel.loadData(data);

            currentFile = file;
            fileField.setText(file.getAbsolutePath());
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
            fileField.setText(currentFile.getAbsolutePath());
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

    private void switchLanguage(String newLang) {
        if (currentFile == null) return;
        File newFile = EditorUtils.deriveFileForLanguage(currentFile, newLang);
        if (newFile == null) return;
        if (newFile.exists()) {
            loadFromFile(newFile);
        } else {
            buildingModel.setRowCount(0);
            itemModel.setRowCount(0);
            evidenceModel.setRowCount(0);
            caseModel.setRowCount(0);
            discoveryMethodModel.setRowCount(0);
            evidenceModifierModel.setRowCount(0);
            evidenceItemModel.setRowCount(0);
            improvementCategoryModel.setRowCount(0);
            skillCategoryModel.setRowCount(0);
            skinToneCategoryModel.setRowCount(0);
            genderCategoryModel.setRowCount(0);
            motiveCategoryModel.setRowCount(0);
            motiveCaseTypesPerRow.clear();
            casePanel.clearAll();
            versionField.setText("");
            currentFile = newFile;
            fileField.setText(newFile.getAbsolutePath());
            setTitle(WINDOW_TITLE + " – " + newFile.getName());
            statusLabel.setText("New file (not yet saved): " + newFile.getAbsolutePath());
        }
    }

    private static DefaultTableModel createModel(String[] columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return true;
            }
        };
    }

    private static void populateModel(DefaultTableModel model, List<CategoryEntry> entries,
                                      boolean hasColor, boolean hasFunction) {
        model.setRowCount(0);
        if (entries == null) return;
        List<CategoryEntry> sorted = new ArrayList<>(entries);
        sorted.sort(Comparator.comparing(e -> nvl(e.getCode())));
        for (CategoryEntry e : sorted) {
            if (hasColor && hasFunction) {
                // Improvement categories: Code, Description, Color, Actions
                model.addRow(new Object[]{
                        nvl(e.getCode()),
                        nvl(e.getDescription()),
                        nvl(e.getColor()),
                        nvl(e.getActions())
                });
            } else if (hasColor) {
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

    /**
     * Populates a three-column (Code, Name, Description) table model from a list
     * of {@link CategoryEntry} objects.  Used for case types, discovery methods,
     * evidence modifiers, and evidence items.
     */
    private static void populateCodeNameDescModel(DefaultTableModel model,
                                                   List<CategoryEntry> entries) {
        model.setRowCount(0);
        if (entries == null) return;
        List<CategoryEntry> sorted = new ArrayList<>(entries);
        sorted.sort(Comparator.comparing(e -> nvl(e.getCode())));
        for (CategoryEntry e : sorted) {
            model.addRow(new Object[]{
                    nvl(e.getCode()),
                    nvl(e.getName()),
                    nvl(e.getDescription())
            });
        }
    }

    /**
     * Populates the parallel case-type list for each motive category entry.
     * The entries must be in the same sorted order as {@link #populateCodeNameDescModel}.
     */
    private void populateMotiveCaseTypes(List<CategoryEntry> entries) {
        motiveCaseTypesPerRow.clear();
        if (entries == null) return;
        List<CategoryEntry> sorted = new ArrayList<>(entries);
        sorted.sort(Comparator.comparing(e -> nvl(e.getCode())));
        for (CategoryEntry e : sorted) {
            List<String> types = e.getCase_types() != null
                    ? new ArrayList<>(e.getCase_types())
                    : new ArrayList<>();
            motiveCaseTypesPerRow.add(types);
        }
    }

    /**
     * Converts a three-column (Code, Name, Description) table model back to a list
     * of {@link CategoryEntry} objects.
     */
    private static List<CategoryEntry> codeNameDescModelToEntries(DefaultTableModel model) {
        List<CategoryEntry> list = new ArrayList<>();
        for (int r = 0; r < model.getRowCount(); r++) {
            CategoryEntry entry = new CategoryEntry();
            entry.setCode(cellStr(model, r, 0));
            entry.setName(cellStr(model, r, 1));
            entry.setDescription(cellStr(model, r, 2));
            list.add(entry);
        }
        return list;
    }

    /**
     * Converts the motive category table model and the parallel case-type list
     * back to a list of {@link CategoryEntry} objects with {@code case_types} set.
     */
    private List<CategoryEntry> motiveCategoryModelToEntries() {
        List<CategoryEntry> list = new ArrayList<>();
        for (int r = 0; r < motiveCategoryModel.getRowCount(); r++) {
            CategoryEntry entry = new CategoryEntry();
            entry.setCode(cellStr(motiveCategoryModel, r, 0));
            entry.setName(cellStr(motiveCategoryModel, r, 1));
            entry.setDescription(cellStr(motiveCategoryModel, r, 2));
            if (r < motiveCaseTypesPerRow.size()) {
                List<String> types = motiveCaseTypesPerRow.get(r);
                if (types != null && !types.isEmpty()) {
                    entry.setCase_types(new ArrayList<>(types));
                }
            }
            list.add(entry);
        }
        return list;
    }

    /**
     * Populates the skill category model from a list of {@link CategoryEntry} objects.
     * Skill categories use the {@code name} field (not {@code description}).
     */
    private static void populateSkillCategoryModel(DefaultTableModel model,
                                                    List<CategoryEntry> entries) {
        model.setRowCount(0);
        if (entries == null) return;
        List<CategoryEntry> sorted = new ArrayList<>(entries);
        sorted.sort(Comparator.comparing(e -> nvl(e.getCode())));
        for (CategoryEntry e : sorted) {
            model.addRow(new Object[]{nvl(e.getCode()), nvl(e.getName())});
        }
    }

    /**
     * Converts the skill category table model back to a list of {@link CategoryEntry} objects.
     * Skill categories use the {@code name} field (not {@code description}).
     */
    private static List<CategoryEntry> skillCategoryModelToEntries(DefaultTableModel model) {
        List<CategoryEntry> list = new ArrayList<>();
        for (int r = 0; r < model.getRowCount(); r++) {
            CategoryEntry entry = new CategoryEntry();
            entry.setCode(cellStr(model, r, 0));
            entry.setName(cellStr(model, r, 1));
            list.add(entry);
        }
        return list;
    }

    /**
     * Populates the skin tone category model from a list of {@link CategoryEntry} objects.
     * Skin tone categories use {@code code}, {@code name}, {@code rgb}, and {@code percentage} fields.
     */
    private static void populateSkinToneCategoryModel(DefaultTableModel model,
                                                       List<CategoryEntry> entries) {
        model.setRowCount(0);
        if (entries == null) return;
        List<CategoryEntry> sorted = new ArrayList<>(entries);
        sorted.sort(Comparator.comparing(e -> nvl(e.getCode())));
        for (CategoryEntry e : sorted) {
            model.addRow(new Object[]{nvl(e.getCode()), nvl(e.getName()), nvl(e.getRgb()), e.getPercentage()});
        }
    }

    /**
     * Converts the skin tone category table model back to a list of {@link CategoryEntry} objects.
     * Skin tone categories use {@code code}, {@code name}, {@code rgb}, and {@code percentage} fields.
     */
    private static List<CategoryEntry> skinToneCategoryModelToEntries(DefaultTableModel model) {
        List<CategoryEntry> list = new ArrayList<>();
        for (int r = 0; r < model.getRowCount(); r++) {
            CategoryEntry entry = new CategoryEntry();
            entry.setCode(cellStr(model, r, 0));
            entry.setName(cellStr(model, r, 1));
            entry.setRgb(cellStr(model, r, 2));
            Object pctVal = model.getValueAt(r, 3);
            if (pctVal != null) {
                try {
                    entry.setPercentage(Integer.parseInt(pctVal.toString().trim()));
                } catch (NumberFormatException ignored) {
                    entry.setPercentage(10);
                }
            } else {
                entry.setPercentage(10);
            }
            list.add(entry);
        }
        return list;
    }

    /**
     * Builds the tab panel for the Gender Categories table.
     * Gender categories use {@code code} and {@code name} columns.
     */
    private JPanel buildGenderCategoryTabPanel() {
        JTable table = new JTable(genderCategoryModel) {
            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int col) {
                Component c = super.prepareRenderer(renderer, row, col);
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
                Color bg = getAnnotationColor((DefaultTableModel) getModel(), row, column);
                if (bg != null && editor instanceof DefaultCellEditor) {
                    ((DefaultCellEditor) editor).getComponent().setBackground(bg);
                }
                return editor;
            }
        };
        categoryTables.add(table);
        table.setRowHeight(26);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
        table.getColumnModel().getColumn(0).setPreferredWidth(180);
        table.getColumnModel().getColumn(1).setPreferredWidth(540);

        JScrollPane scrollPane = new JScrollPane(table);

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

        JButton addBtn    = new JButton("Add Row");
        JButton deleteBtn = new JButton("Delete Row");
        JButton saveBtn   = new JButton("Save");

        addBtn.addActionListener((ActionEvent e) -> {
            genderCategoryModel.addRow(new Object[]{"", ""});
            int last = genderCategoryModel.getRowCount() - 1;
            table.scrollRectToVisible(table.getCellRect(last, 0, true));
            table.setRowSelectionInterval(last, last);
        });

        deleteBtn.addActionListener((ActionEvent e) -> {
            int row = table.getSelectedRow();
            if (row >= 0) {
                genderCategoryModel.removeRow(row);
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

    /**
     * Populates the gender category model from a list of {@link CategoryEntry} objects.
     * Gender categories use {@code code} and {@code name} fields.
     */
    private static void populateGenderCategoryModel(DefaultTableModel model,
                                                     List<CategoryEntry> entries) {
        model.setRowCount(0);
        if (entries == null) return;
        List<CategoryEntry> sorted = new ArrayList<>(entries);
        sorted.sort(Comparator.comparing(e -> nvl(e.getCode())));
        for (CategoryEntry e : sorted) {
            model.addRow(new Object[]{nvl(e.getCode()), nvl(e.getName())});
        }
    }

    /**
     * Converts the gender category table model back to a list of {@link CategoryEntry} objects.
     * Gender categories use {@code code} and {@code name} fields.
     */
    private static List<CategoryEntry> genderCategoryModelToEntries(DefaultTableModel model) {
        List<CategoryEntry> list = new ArrayList<>();
        for (int r = 0; r < model.getRowCount(); r++) {
            CategoryEntry entry = new CategoryEntry();
            entry.setCode(cellStr(model, r, 0));
            entry.setName(cellStr(model, r, 1));
            list.add(entry);
        }
        return list;
    }

    private CategoryData buildCategoryData() {
        CategoryData data = new CategoryData();
        data.setVersion(versionField.getText().trim());
        data.setLanguage((String) languageCombo.getSelectedItem());
        data.setBuilding_categories(modelToEntries(buildingModel,            true,  false));
        data.setItem_categories(modelToEntries(itemModel,                    false, false));
        data.setEvidence_categories(modelToEntries(evidenceModel,            false, false));
        data.setCase_types(codeNameDescModelToEntries(caseModel));
        data.setDiscovery_methods(codeNameDescModelToEntries(discoveryMethodModel));
        data.setEvidence_modifiers(codeNameDescModelToEntries(evidenceModifierModel));
        data.setEvidence_items(codeNameDescModelToEntries(evidenceItemModel));
        data.setImprovement_categories(modelToEntries(improvementCategoryModel, true, true));
        data.setSkill_categories(skillCategoryModelToEntries(skillCategoryModel));
        data.setSkin_tone_categories(skinToneCategoryModelToEntries(skinToneCategoryModel));
        data.setGender_categories(genderCategoryModelToEntries(genderCategoryModel));
        data.setMotive_categories(motiveCategoryModelToEntries());
        return data;
    }

    private static List<CategoryEntry> modelToEntries(DefaultTableModel model,
                                                       boolean hasColor, boolean hasFunction) {
        List<CategoryEntry> list = new ArrayList<>();
        for (int r = 0; r < model.getRowCount(); r++) {
            CategoryEntry entry = new CategoryEntry();
            entry.setCode(cellStr(model, r, 0));
            entry.setDescription(cellStr(model, r, 1));
            if (hasColor) {
                entry.setColor(cellStr(model, r, 2));
            }
            // hasFunction=true is only valid when hasColor=true (improvement categories)
            if (hasFunction) {
                entry.setActions(cellStr(model, r, 3));
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

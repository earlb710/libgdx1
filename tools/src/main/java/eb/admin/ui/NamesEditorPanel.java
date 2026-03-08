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
 * Top-level panel for the "Names" tab in the Game Admin tool.
 *
 * <p>Contains three sub-tabs, each backed by its own JSON file:
 * <ul>
 *   <li><b>Person Names</b> – {@code person_names.json} (Name, Gender columns)</li>
 *   <li><b>Surnames</b>     – {@code person_surnames.json} (Surname column)</li>
 *   <li><b>Company Names</b>– {@code company_names.json} (Template, Type columns)</li>
 * </ul>
 *
 * <p>Each sub-tab supports full CRUD (add/delete rows), open/save file operations,
 * and right-click annotation ratings, consistent with the rest of the admin tool.
 */
public class NamesEditorPanel extends JPanel {

    private static final Color  ANNOTATION_FOREGROUND = new Color(0, 0, 139);
    private static final String[] RATINGS = {"Excellent", "Good", "Sufficient", "Bad", "Very Bad"};

    private static final String DEFAULT_PERSON_NAMES_PATH   = "assets/text/person_names.json";
    private static final String DEFAULT_SURNAMES_PATH        = "assets/text/person_surnames.json";
    private static final String DEFAULT_COMPANY_NAMES_PATH   = "assets/text/company_names.json";

    private final JLabel statusLabel;

    // ── Person Names ─────────────────────────────────────────────────────────
    private final JTextField personNamesVersionField = new JTextField(8);
    private final JTextField personNamesFileField    = new JTextField();
    private final DefaultTableModel personNamesModel =
            createModel(new String[]{"Name", "Gender"});
    private final Map<String, String> personNamesAnnotations = new HashMap<>();
    private final JTable personNamesTable = buildAnnotatedTable(personNamesModel,
            personNamesAnnotations);
    private File personNamesFile;

    // ── Surnames ─────────────────────────────────────────────────────────────
    private final JTextField surnamesVersionField = new JTextField(8);
    private final JTextField surnamesFileField    = new JTextField();
    private final DefaultTableModel surnamesModel =
            createModel(new String[]{"Surname"});
    private final Map<String, String> surnamesAnnotations = new HashMap<>();
    private final JTable surnamesTable = buildAnnotatedTable(surnamesModel,
            surnamesAnnotations);
    private File surnamesFile;

    // ── Company Names ─────────────────────────────────────────────────────────
    private final JTextField companyNamesVersionField = new JTextField(8);
    private final JTextField companyNamesFileField    = new JTextField();
    private final DefaultTableModel companyNamesModel =
            createModel(new String[]{"Template", "Type"});
    private final Map<String, String> companyNamesAnnotations = new HashMap<>();
    private final JTable companyNamesTable = buildAnnotatedTable(companyNamesModel,
            companyNamesAnnotations);
    private File companyNamesFile;

    // ─────────────────────────────────────────────────────────────────────────

    public NamesEditorPanel(JLabel statusLabel) {
        this.statusLabel = statusLabel;
        buildUI();
        tryLoadDefaults();
    }

    // ── UI construction ───────────────────────────────────────────────────────

    private void buildUI() {
        setLayout(new BorderLayout());

        JTabbedPane subTabs = new JTabbedPane();
        subTabs.addTab("Person Names",   buildPersonNamesTab());
        subTabs.addTab("Surnames",       buildSurnamesTab());
        subTabs.addTab("Company Names",  buildCompanyNamesTab());

        add(subTabs, BorderLayout.CENTER);
    }

    // ── Person Names tab ──────────────────────────────────────────────────────

    private JPanel buildPersonNamesTab() {
        personNamesFileField.setEditable(false);

        configureTable(personNamesTable);
        personNamesTable.getColumnModel().getColumn(0).setPreferredWidth(260);
        personNamesTable.getColumnModel().getColumn(1).setPreferredWidth(100);

        personNamesTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    int row = personNamesTable.rowAtPoint(e.getPoint());
                    int col = personNamesTable.columnAtPoint(e.getPoint());
                    if (row >= 0 && col >= 0) {
                        personNamesTable.setRowSelectionInterval(row, row);
                        showAnnotationMenu(personNamesTable, personNamesModel,
                                personNamesAnnotations,
                                () -> annotationFile(personNamesFile),
                                () -> annotationFilePath(personNamesFile),
                                row, col, e.getX(), e.getY());
                    }
                }
            }
        });

        JButton addBtn    = new JButton("Add Row");
        JButton deleteBtn = new JButton("Delete Row");
        JButton saveBtn   = new JButton("Save");

        addBtn.addActionListener((ActionEvent e) -> {
            personNamesModel.addRow(new Object[]{"", "M"});
            int last = personNamesModel.getRowCount() - 1;
            personNamesTable.scrollRectToVisible(personNamesTable.getCellRect(last, 0, true));
            personNamesTable.setRowSelectionInterval(last, last);
        });
        deleteBtn.addActionListener((ActionEvent e) -> deleteSelectedRow(personNamesTable, personNamesModel));
        saveBtn.addActionListener((ActionEvent e) -> savePersonNames(false));

        return buildTabLayout(personNamesVersionField, personNamesFileField,
                personNamesTable,
                addBtn, deleteBtn, saveBtn,
                () -> openPersonNames(),
                () -> savePersonNames(false),
                () -> savePersonNames(true));
    }

    // ── Surnames tab ──────────────────────────────────────────────────────────

    private JPanel buildSurnamesTab() {
        surnamesFileField.setEditable(false);

        configureTable(surnamesTable);
        surnamesTable.getColumnModel().getColumn(0).setPreferredWidth(400);

        surnamesTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    int row = surnamesTable.rowAtPoint(e.getPoint());
                    int col = surnamesTable.columnAtPoint(e.getPoint());
                    if (row >= 0 && col >= 0) {
                        surnamesTable.setRowSelectionInterval(row, row);
                        showAnnotationMenu(surnamesTable, surnamesModel,
                                surnamesAnnotations,
                                () -> annotationFile(surnamesFile),
                                () -> annotationFilePath(surnamesFile),
                                row, col, e.getX(), e.getY());
                    }
                }
            }
        });

        JButton addBtn    = new JButton("Add Row");
        JButton deleteBtn = new JButton("Delete Row");
        JButton saveBtn   = new JButton("Save");

        addBtn.addActionListener((ActionEvent e) -> {
            surnamesModel.addRow(new Object[]{""});
            int last = surnamesModel.getRowCount() - 1;
            surnamesTable.scrollRectToVisible(surnamesTable.getCellRect(last, 0, true));
            surnamesTable.setRowSelectionInterval(last, last);
        });
        deleteBtn.addActionListener((ActionEvent e) -> deleteSelectedRow(surnamesTable, surnamesModel));
        saveBtn.addActionListener((ActionEvent e) -> saveSurnames(false));

        return buildTabLayout(surnamesVersionField, surnamesFileField,
                surnamesTable,
                addBtn, deleteBtn, saveBtn,
                () -> openSurnames(),
                () -> saveSurnames(false),
                () -> saveSurnames(true));
    }

    // ── Company Names tab ─────────────────────────────────────────────────────

    private JPanel buildCompanyNamesTab() {
        companyNamesFileField.setEditable(false);

        configureTable(companyNamesTable);
        companyNamesTable.getColumnModel().getColumn(0).setPreferredWidth(520);
        companyNamesTable.getColumnModel().getColumn(1).setPreferredWidth(120);

        companyNamesTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    int row = companyNamesTable.rowAtPoint(e.getPoint());
                    int col = companyNamesTable.columnAtPoint(e.getPoint());
                    if (row >= 0 && col >= 0) {
                        companyNamesTable.setRowSelectionInterval(row, row);
                        showAnnotationMenu(companyNamesTable, companyNamesModel,
                                companyNamesAnnotations,
                                () -> annotationFile(companyNamesFile),
                                () -> annotationFilePath(companyNamesFile),
                                row, col, e.getX(), e.getY());
                    }
                }
            }
        });

        JButton addBtn    = new JButton("Add Row");
        JButton deleteBtn = new JButton("Delete Row");
        JButton saveBtn   = new JButton("Save");

        addBtn.addActionListener((ActionEvent e) -> {
            companyNamesModel.addRow(new Object[]{"", "G"});
            int last = companyNamesModel.getRowCount() - 1;
            companyNamesTable.scrollRectToVisible(companyNamesTable.getCellRect(last, 0, true));
            companyNamesTable.setRowSelectionInterval(last, last);
        });
        deleteBtn.addActionListener((ActionEvent e) -> deleteSelectedRow(companyNamesTable, companyNamesModel));
        saveBtn.addActionListener((ActionEvent e) -> saveCompanyNames(false));

        return buildTabLayout(companyNamesVersionField, companyNamesFileField,
                companyNamesTable,
                addBtn, deleteBtn, saveBtn,
                () -> openCompanyNames(),
                () -> saveCompanyNames(false),
                () -> saveCompanyNames(true));
    }

    // ── Generic layout builder ────────────────────────────────────────────────

    /**
     * Assembles the common BorderLayout panel structure used by all three sub-tabs:
     * a metadata/toolbar strip at the top, a scrollable table in the center, and
     * Add/Delete/Save buttons along the bottom.
     */
    private JPanel buildTabLayout(JTextField versionField, JTextField fileField,
                                   JTable table,
                                   JButton addBtn, JButton deleteBtn, JButton saveRowBtn,
                                   Runnable openAction, Runnable saveAction, Runnable saveAsAction) {

        // Metadata strip
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

        // File toolbar
        JButton openBtn   = new JButton("Open…");
        JButton saveBtn   = new JButton("Save");
        JButton saveAsBtn = new JButton("Save As…");
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

    // ── File operations: Person Names ─────────────────────────────────────────

    private void openPersonNames() {
        File chosen = chooseOpenFile(personNamesFile, "assets");
        if (chosen != null) loadPersonNamesFromFile(chosen);
    }

    private void loadPersonNamesFromFile(File file) {
        try (Reader reader = new FileReader(file)) {
            JsonObject root = new Gson().fromJson(reader, JsonObject.class);
            personNamesVersionField.setText(jsonStr(root, "version"));
            personNamesModel.setRowCount(0);
            if (root.has("names")) {
                List<Object[]> rows = new ArrayList<>();
                for (JsonElement el : root.getAsJsonArray("names")) {
                    JsonObject entry = el.getAsJsonObject();
                    rows.add(new Object[]{jsonStr(entry, "name"), jsonStr(entry, "gender")});
                }
                rows.sort(Comparator.comparing(r -> r[0].toString()));
                for (Object[] row : rows) personNamesModel.addRow(row);
            }
            personNamesFile = file;
            personNamesFileField.setText(file.getAbsolutePath());
            statusLabel.setText("Person names loaded: " + file.getAbsolutePath());
            loadAnnotationColors(personNamesAnnotations, annotationFile(personNamesFile),
                    annotationFilePath(personNamesFile), personNamesTable);
        } catch (Exception ex) {
            showLoadError(ex);
        }
    }

    private void savePersonNames(boolean saveAs) {
        personNamesFile = resolveTargetFile(personNamesFile, saveAs);
        if (personNamesFile == null) return;
        try {
            JsonObject root = new JsonObject();
            root.addProperty("version", personNamesVersionField.getText().trim());
            JsonArray names = new JsonArray();
            for (int r = 0; r < personNamesModel.getRowCount(); r++) {
                JsonObject entry = new JsonObject();
                entry.addProperty("name",   cellStr(personNamesModel, r, 0));
                entry.addProperty("gender", cellStr(personNamesModel, r, 1));
                names.add(entry);
            }
            root.add("names", names);
            writeJson(root, personNamesFile);
            personNamesFileField.setText(personNamesFile.getAbsolutePath());
            statusLabel.setText("Person names saved: " + personNamesFile.getAbsolutePath());
        } catch (Exception ex) {
            showSaveError(ex);
        }
    }

    // ── File operations: Surnames ─────────────────────────────────────────────

    private void openSurnames() {
        File chosen = chooseOpenFile(surnamesFile, "assets");
        if (chosen != null) loadSurnamesFromFile(chosen);
    }

    private void loadSurnamesFromFile(File file) {
        try (Reader reader = new FileReader(file)) {
            JsonObject root = new Gson().fromJson(reader, JsonObject.class);
            surnamesVersionField.setText(jsonStr(root, "version"));
            surnamesModel.setRowCount(0);
            if (root.has("surnames")) {
                List<String> items = new ArrayList<>();
                for (JsonElement el : root.getAsJsonArray("surnames")) {
                    items.add(el.getAsString());
                }
                items.sort(String::compareToIgnoreCase);
                for (String s : items) surnamesModel.addRow(new Object[]{s});
            }
            surnamesFile = file;
            surnamesFileField.setText(file.getAbsolutePath());
            statusLabel.setText("Surnames loaded: " + file.getAbsolutePath());
            loadAnnotationColors(surnamesAnnotations, annotationFile(surnamesFile),
                    annotationFilePath(surnamesFile), surnamesTable);
        } catch (Exception ex) {
            showLoadError(ex);
        }
    }

    private void saveSurnames(boolean saveAs) {
        surnamesFile = resolveTargetFile(surnamesFile, saveAs);
        if (surnamesFile == null) return;
        try {
            JsonObject root = new JsonObject();
            root.addProperty("version", surnamesVersionField.getText().trim());
            JsonArray surnames = new JsonArray();
            for (int r = 0; r < surnamesModel.getRowCount(); r++) {
                surnames.add(cellStr(surnamesModel, r, 0));
            }
            root.add("surnames", surnames);
            writeJson(root, surnamesFile);
            surnamesFileField.setText(surnamesFile.getAbsolutePath());
            statusLabel.setText("Surnames saved: " + surnamesFile.getAbsolutePath());
        } catch (Exception ex) {
            showSaveError(ex);
        }
    }

    // ── File operations: Company Names ────────────────────────────────────────

    private void openCompanyNames() {
        File chosen = chooseOpenFile(companyNamesFile, "assets");
        if (chosen != null) loadCompanyNamesFromFile(chosen);
    }

    private void loadCompanyNamesFromFile(File file) {
        try (Reader reader = new FileReader(file)) {
            JsonObject root = new Gson().fromJson(reader, JsonObject.class);
            companyNamesVersionField.setText(jsonStr(root, "version"));
            companyNamesModel.setRowCount(0);
            if (root.has("names")) {
                List<Object[]> rows = new ArrayList<>();
                for (JsonElement el : root.getAsJsonArray("names")) {
                    JsonObject entry = el.getAsJsonObject();
                    rows.add(new Object[]{jsonStr(entry, "template"), jsonStr(entry, "type")});
                }
                rows.sort(Comparator.comparing(r -> r[0].toString()));
                for (Object[] row : rows) companyNamesModel.addRow(row);
            }
            companyNamesFile = file;
            companyNamesFileField.setText(file.getAbsolutePath());
            statusLabel.setText("Company names loaded: " + file.getAbsolutePath());
            loadAnnotationColors(companyNamesAnnotations, annotationFile(companyNamesFile),
                    annotationFilePath(companyNamesFile), companyNamesTable);
        } catch (Exception ex) {
            showLoadError(ex);
        }
    }

    private void saveCompanyNames(boolean saveAs) {
        companyNamesFile = resolveTargetFile(companyNamesFile, saveAs);
        if (companyNamesFile == null) return;
        try {
            JsonObject root = new JsonObject();
            root.addProperty("version", companyNamesVersionField.getText().trim());
            JsonArray names = new JsonArray();
            for (int r = 0; r < companyNamesModel.getRowCount(); r++) {
                JsonObject entry = new JsonObject();
                entry.addProperty("template", cellStr(companyNamesModel, r, 0));
                entry.addProperty("type",     cellStr(companyNamesModel, r, 1));
                names.add(entry);
            }
            root.add("names", names);
            writeJson(root, companyNamesFile);
            companyNamesFileField.setText(companyNamesFile.getAbsolutePath());
            statusLabel.setText("Company names saved: " + companyNamesFile.getAbsolutePath());
        } catch (Exception ex) {
            showSaveError(ex);
        }
    }

    // ── Default-file auto-loading ─────────────────────────────────────────────

    private void tryLoadDefaults() {
        File pn = new File(DEFAULT_PERSON_NAMES_PATH);
        if (pn.exists()) loadPersonNamesFromFile(pn);

        File sn = new File(DEFAULT_SURNAMES_PATH);
        if (sn.exists()) loadSurnamesFromFile(sn);

        File cn = new File(DEFAULT_COMPANY_NAMES_PATH);
        if (cn.exists()) loadCompanyNamesFromFile(cn);
    }

    // ── Annotation support ────────────────────────────────────────────────────

    private void showAnnotationMenu(JTable table, DefaultTableModel model,
                                     Map<String, String> annotations,
                                     java.util.function.Supplier<File> annotationFileSupplier,
                                     java.util.function.Supplier<String> annotationPathSupplier,
                                     int row, int col, int x, int y) {
        String key    = cellStr(model, row, 0);
        String column = model.getColumnName(col);
        String existingRating = findExistingRating(key, column,
                annotationFileSupplier.get(), annotationPathSupplier.get());
        JPopupMenu menu = new JPopupMenu("Rate");
        for (String rating : RATINGS) {
            String ratingId = rating.toLowerCase().replace(' ', '_');
            JCheckBoxMenuItem item = new JCheckBoxMenuItem(rating, ratingId.equals(existingRating));
            item.addActionListener(e -> promptAndSaveAnnotation(table, model, annotations,
                    annotationFileSupplier, annotationPathSupplier, row, col, ratingId));
            menu.add(item);
        }
        menu.addSeparator();
        JMenuItem clearItem = new JMenuItem("Clear");
        clearItem.addActionListener(e -> clearAnnotationsForItem(key, annotations,
                annotationFileSupplier.get(), annotationPathSupplier.get(), table));
        menu.add(clearItem);
        menu.show(table, x, y);
    }

    private void promptAndSaveAnnotation(JTable table, DefaultTableModel model,
                                          Map<String, String> annotations,
                                          java.util.function.Supplier<File>   annotationFileSupplier,
                                          java.util.function.Supplier<String> annotationPathSupplier,
                                          int row, int col, String rating) {
        String key    = cellStr(model, row, 0);
        String column = model.getColumnName(col);
        String existingComment = findExistingComment(key, column,
                annotationFileSupplier.get(), annotationPathSupplier.get());

        JTextArea textArea = new JTextArea(existingComment, 6, 50);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        JScrollPane scroll = new JScrollPane(textArea);
        scroll.setPreferredSize(new Dimension(520, 160));

        int result = JOptionPane.showConfirmDialog(
                this, scroll, "Annotation \u2013 " + rating + " (clear comment to remove)",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;
        saveAnnotation(key, column, rating, textArea.getText().trim(),
                annotations, annotationFileSupplier.get(),
                annotationPathSupplier.get(), table);
    }

    private void saveAnnotation(String key, String column, String rating, String comment,
                                 Map<String, String> annotations,
                                 File annotationFile, String filePath, JTable table) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonArray arr = new JsonArray();

        if (annotationFile.exists()) {
            try (Reader r = Files.newBufferedReader(annotationFile.toPath(), StandardCharsets.UTF_8)) {
                JsonObject existing = gson.fromJson(r, JsonObject.class);
                if (existing != null && existing.has("annotations")) {
                    for (JsonElement el : existing.getAsJsonArray("annotations")) {
                        JsonObject e = el.getAsJsonObject();
                        JsonElement fileEl = e.get("file"), keyEl = e.get("key"), colEl = e.get("column");
                        if (fileEl == null || keyEl == null || colEl == null) { arr.add(e); continue; }
                        if (filePath.equals(fileEl.getAsString())
                                && key.equals(keyEl.getAsString())
                                && column.equals(colEl.getAsString())) continue;
                        arr.add(e);
                    }
                }
            } catch (IOException | RuntimeException ex) {
                System.err.println("Could not read annotation.json, starting fresh: " + ex.getMessage());
            }
        }

        if (!comment.isEmpty()) {
            JsonObject entry = new JsonObject();
            entry.addProperty("file",    filePath);
            entry.addProperty("key",     key);
            entry.addProperty("column",  column);
            entry.addProperty("rating",  rating);
            entry.addProperty("comment", comment);
            arr.add(entry);
        }

        JsonObject root = new JsonObject();
        root.add("annotations", arr);
        try (Writer w = Files.newBufferedWriter(annotationFile.toPath(), StandardCharsets.UTF_8)) {
            gson.toJson(root, w);
            w.flush();
            statusLabel.setText("Annotation saved: " + annotationFile.getAbsolutePath());
            loadAnnotationColors(annotations, annotationFile, filePath, table);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Error saving annotation:\n" + ex.getMessage(),
                    "Annotation Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void clearAnnotationsForItem(String key, Map<String, String> annotations,
                                          File annotationFile, String filePath, JTable table) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonArray arr = new JsonArray();

        if (annotationFile.exists()) {
            try (Reader r = Files.newBufferedReader(annotationFile.toPath(), StandardCharsets.UTF_8)) {
                JsonObject existing = gson.fromJson(r, JsonObject.class);
                if (existing != null && existing.has("annotations")) {
                    for (JsonElement el : existing.getAsJsonArray("annotations")) {
                        JsonObject e = el.getAsJsonObject();
                        JsonElement fileEl = e.get("file"), keyEl = e.get("key");
                        if (fileEl == null || keyEl == null) { arr.add(e); continue; }
                        if (filePath.equals(fileEl.getAsString()) && key.equals(keyEl.getAsString())) continue;
                        arr.add(e);
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
        root.add("annotations", arr);
        try (Writer w = Files.newBufferedWriter(annotationFile.toPath(), StandardCharsets.UTF_8)) {
            gson.toJson(root, w);
            w.flush();
            statusLabel.setText("Annotations cleared: " + annotationFile.getAbsolutePath());
            loadAnnotationColors(annotations, annotationFile, filePath, table);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Error clearing annotation:\n" + ex.getMessage(),
                    "Annotation Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String findExistingRating(String key, String column, File annotationFile, String filePath) {
        if (!annotationFile.exists()) return null;
        try (Reader r = Files.newBufferedReader(annotationFile.toPath(), StandardCharsets.UTF_8)) {
            JsonObject existing = new Gson().fromJson(r, JsonObject.class);
            if (existing == null || !existing.has("annotations")) return null;
            String last = null;
            for (JsonElement el : existing.getAsJsonArray("annotations")) {
                JsonObject e = el.getAsJsonObject();
                JsonElement fileEl = e.get("file"), keyEl = e.get("key"), colEl = e.get("column");
                if (fileEl == null || keyEl == null || colEl == null) continue;
                if (filePath.equals(fileEl.getAsString()) && key.equals(keyEl.getAsString())
                        && column.equals(colEl.getAsString())) {
                    JsonElement re = e.get("rating");
                    last = re != null ? re.getAsString() : null;
                }
            }
            return last;
        } catch (IOException | RuntimeException ex) {
            return null;
        }
    }

    private String findExistingComment(String key, String column, File annotationFile, String filePath) {
        if (!annotationFile.exists()) return "";
        try (Reader r = Files.newBufferedReader(annotationFile.toPath(), StandardCharsets.UTF_8)) {
            JsonObject existing = new Gson().fromJson(r, JsonObject.class);
            if (existing == null || !existing.has("annotations")) return "";
            String last = "";
            for (JsonElement el : existing.getAsJsonArray("annotations")) {
                JsonObject e = el.getAsJsonObject();
                JsonElement fileEl = e.get("file"), keyEl = e.get("key"), colEl = e.get("column");
                if (fileEl == null || keyEl == null || colEl == null) continue;
                if (filePath.equals(fileEl.getAsString()) && key.equals(keyEl.getAsString())
                        && column.equals(colEl.getAsString())) {
                    JsonElement ce = e.get("comment");
                    last = ce != null ? ce.getAsString() : "";
                }
            }
            return last;
        } catch (IOException | RuntimeException ex) {
            return "";
        }
    }

    private void loadAnnotationColors(Map<String, String> annotations,
                                       File annotationFile, String filePath, JTable table) {
        annotations.clear();
        if (annotationFile.exists()) {
            try (Reader r = Files.newBufferedReader(annotationFile.toPath(), StandardCharsets.UTF_8)) {
                JsonObject existing = new Gson().fromJson(r, JsonObject.class);
                if (existing != null && existing.has("annotations")) {
                    for (JsonElement el : existing.getAsJsonArray("annotations")) {
                        JsonObject e = el.getAsJsonObject();
                        JsonElement fileEl  = e.get("file");
                        JsonElement keyEl   = e.get("key");
                        JsonElement colEl   = e.get("column");
                        JsonElement ratingEl = e.get("rating");
                        if (fileEl == null || keyEl == null || colEl == null || ratingEl == null) continue;
                        if (filePath.equals(fileEl.getAsString())) {
                            annotations.put(
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

    // ── Shared helpers ────────────────────────────────────────────────────────

    private static File annotationFile(File currentFile) {
        return currentFile != null
                ? new File(currentFile.getParent(), "annotation.json")
                : new File("annotation.json");
    }

    private static String annotationFilePath(File currentFile) {
        return currentFile != null ? currentFile.getAbsolutePath() : "";
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

    private static void writeJson(JsonObject root, File target) throws IOException {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (Writer writer = new FileWriter(target)) {
            gson.toJson(root, writer);
        }
    }

    private static void configureTable(JTable table) {
        table.setRowHeight(26);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
    }

    private static DefaultTableModel createModel(String[] columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return true; }
        };
    }

    /**
     * Creates a JTable whose renderer highlights annotated cells using
     * {@link DescriptionEditorPanel#ratingToColor(String)}.
     */
    private static JTable buildAnnotatedTable(DefaultTableModel model,
                                               Map<String, String> annotations) {
        return new JTable(model) {
            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int col) {
                Component c = super.prepareRenderer(renderer, row, col);
                Object keyVal = model.getValueAt(row, 0);
                String key = keyVal != null ? keyVal.toString() : "";
                String colName = model.getColumnName(col);
                String ratingId = annotations.get(key + "\0" + colName);
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

    private static String jsonStr(JsonObject obj, String field) {
        return obj.has(field) ? obj.get(field).getAsString() : "";
    }

    private void showLoadError(Exception ex) {
        JOptionPane.showMessageDialog(this,
                "Error loading file:\n" + ex.getMessage(),
                "Load Error", JOptionPane.ERROR_MESSAGE);
    }

    private void showSaveError(Exception ex) {
        JOptionPane.showMessageDialog(this,
                "Error saving file:\n" + ex.getMessage(),
                "Save Error", JOptionPane.ERROR_MESSAGE);
    }
}

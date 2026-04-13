package eb.admin.ui;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * A reusable dialog that lets the user pick SVG feature.id values via
 * checkboxes, with live single-feature preview and full-face composite preview.
 *
 * <p>Extracted from {@link SvgEditorPanel#showSvgPickerDialog(int, int)} to
 * reduce that class's size and isolate the picker's UI/logic.
 */
class SvgPickerDialog {

    private final Component parent;
    private final JsonObject svgsData;
    private final DefaultTableModel svgIndexModel;
    private final DefaultTableModel faceRulesModel;
    private final String[] faceDrawOrder;
    private final Map<String, int[][]> faceFeaturePositions;

    /**
     * @param parent              parent component for dialog positioning
     * @param svgsData            parsed svgs.json data (feature → id → fragment)
     * @param svgIndexModel       SVG Index table model (cols: feature, id, gender)
     * @param faceRulesModel      Face Rules table model being edited
     * @param faceDrawOrder       ordered feature names for face composition
     * @param faceFeaturePositions per-feature placement positions for preview
     */
    SvgPickerDialog(Component parent, JsonObject svgsData,
                    DefaultTableModel svgIndexModel,
                    DefaultTableModel faceRulesModel,
                    String[] faceDrawOrder,
                    Map<String, int[][]> faceFeaturePositions) {
        this.parent = parent;
        this.svgsData = svgsData;
        this.svgIndexModel = svgIndexModel;
        this.faceRulesModel = faceRulesModel;
        this.faceDrawOrder = faceDrawOrder;
        this.faceFeaturePositions = faceFeaturePositions;
    }

    /**
     * Shows the picker dialog for the given cell in the face-rules table.
     *
     * @param row row index in faceRulesModel
     * @param col column index in faceRulesModel (8=Include, 9=Additional, 10=Exclude)
     */
    void show(int row, int col) {
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
                Collections.sort(ids);
                featureIds.put(feature, ids);
            }
        }

        if (featureIds.isEmpty()) {
            JOptionPane.showMessageDialog(parent,
                    "No SVG data loaded. Please load svgs.json first via the SVG Index tab.",
                    "No SVG Data", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Parse the current cell value into a set of already-selected entries
        String currentVal = cellStr(faceRulesModel, row, col).trim();
        Set<String> selected = new LinkedHashSet<>();
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
        JPanel checkPanel = new JPanel();
        checkPanel.setLayout(new BoxLayout(checkPanel, BoxLayout.Y_AXIS));
        Map<JCheckBox, String> boxKeyMap = new LinkedHashMap<>();

        for (Map.Entry<String, List<String>> entry : featureIds.entrySet()) {
            String feature = entry.getKey();
            List<String> ids = entry.getValue();

            List<JCheckBox> itemBoxes = new ArrayList<>();

            boolean allSelected = !ids.isEmpty()
                    && ids.stream().allMatch(id -> selected.contains(feature + "." + id));
            JCheckBox featureHeader = new JCheckBox(feature, allSelected);
            featureHeader.setFont(featureHeader.getFont().deriveFont(Font.BOLD));
            featureHeader.setBorder(BorderFactory.createEmptyBorder(6, 4, 2, 4));
            featureHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
            checkPanel.add(featureHeader);

            for (String id : ids) {
                String key = feature + "." + id;
                String gender  = genderMap.getOrDefault(key, "");
                String label   = ("male".equals(gender) || "female".equals(gender))
                        ? key + " [" + gender + "]"
                        : key;
                JCheckBox cb = new JCheckBox(label, selected.contains(key));
                cb.setAlignmentX(Component.LEFT_ALIGNMENT);
                cb.addItemListener(ie -> {
                    boolean all = itemBoxes.stream().allMatch(JCheckBox::isSelected);
                    featureHeader.setSelected(all);
                    updatePreview.accept(key);
                });
                cb.addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                        updatePreview.accept(key);
                    }
                });
                checkPanel.add(cb);
                boxKeyMap.put(cb, key);
                itemBoxes.add(cb);
            }

            featureHeader.addActionListener(ae -> {
                boolean sel = featureHeader.isSelected();
                for (JCheckBox itemBox : itemBoxes) {
                    itemBox.setSelected(sel);
                }
            });
        }

        JScrollPane scroll = new JScrollPane(checkPanel);
        scroll.setPreferredSize(new Dimension(400, 500));

        // Full-face composite preview inside the dialog
        SvgPreviewPanel facePreviewInDialog = new SvgPreviewPanel();
        facePreviewInDialog.setPreferredSize(new Dimension(200, 300));
        facePreviewInDialog.setMinimumSize(new Dimension(160, 240));
        facePreviewInDialog.setBorder(BorderFactory.createTitledBorder("Face Preview"));

        JButton randomViewBtn = new JButton("Random View");
        randomViewBtn.setToolTipText("Generate a random face using only the checked items");
        randomViewBtn.addActionListener(ae -> buildRandomFacePreview(
                boxKeyMap, facePreviewInDialog));

        // Right panel: individual feature preview on top, full-face preview below
        JPanel rightPanel = new JPanel(new BorderLayout(0, 4));
        rightPanel.add(pickerPreview,       BorderLayout.NORTH);
        rightPanel.add(facePreviewInDialog, BorderLayout.CENTER);
        rightPanel.add(randomViewBtn,       BorderLayout.SOUTH);

        // Main dialog content
        JPanel content = new JPanel(new BorderLayout(8, 0));
        content.add(scroll,     BorderLayout.CENTER);
        content.add(rightPanel, BorderLayout.EAST);

        String colName = col == 8 ? "Include" : col == 9 ? "Additional" : "Exclude";
        int result = JOptionPane.showConfirmDialog(
                parent, content,
                "Select SVG IDs for " + colName,
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);

        if (result != JOptionPane.OK_OPTION) return;

        // Collect checked keys in order
        List<String> newList = new ArrayList<>();
        for (Map.Entry<JCheckBox, String> entry : boxKeyMap.entrySet()) {
            if (entry.getKey().isSelected()) newList.add(entry.getValue());
        }
        if (row < faceRulesModel.getRowCount() && col < faceRulesModel.getColumnCount()) {
            faceRulesModel.setValueAt(String.join(",", newList), row, col);
        }
    }

    /**
     * Builds a composite face preview from the currently-checked checkboxes.
     */
    private void buildRandomFacePreview(Map<JCheckBox, String> boxKeyMap,
                                        SvgPreviewPanel facePreview) {
        if (svgsData == null) return;

        Map<String, List<String>> checkedByFeature = new LinkedHashMap<>();
        for (Map.Entry<JCheckBox, String> entry : boxKeyMap.entrySet()) {
            if (!entry.getKey().isSelected()) continue;
            String key = entry.getValue();
            int dot = key.indexOf('.');
            if (dot <= 0 || dot == key.length() - 1) continue;
            String feat = key.substring(0, dot);
            String id   = key.substring(dot + 1);
            checkedByFeature.computeIfAbsent(feat, k -> new ArrayList<>()).add(id);
        }

        if (checkedByFeature.isEmpty()) {
            facePreview.setCompositeFragments(null);
            return;
        }

        Random rndFace = new Random();
        List<String> fragments = new ArrayList<>();
        for (String feature : faceDrawOrder) {
            List<String> ids = checkedByFeature.get(feature);
            if (ids == null || ids.isEmpty()) continue;
            if (!svgsData.has(feature)) continue;
            JsonObject featureObj = svgsData.getAsJsonObject(feature);
            String selectedId = ids.get(rndFace.nextInt(ids.size()));
            if (!featureObj.has(selectedId)) continue;
            String frag = featureObj.get(selectedId).getAsString();
            if (frag.isEmpty()) continue;
            frag = applyDefaultColorSubstitutions(frag);
            int[][] positions = faceFeaturePositions.get(feature);
            if (positions == null) {
                fragments.add(frag);
            } else {
                java.awt.geom.Rectangle2D bounds =
                        SvgPreviewPanel.computeFragmentBounds(frag);
                double cx = bounds != null ? bounds.getCenterX() : 0;
                double cy = bounds != null ? bounds.getCenterY() : 0;
                for (int i = 0; i < positions.length; i++) {
                    double tx = positions[i][0] - cx;
                    double ty = positions[i][1] - cy;
                    String transform;
                    if (i == 0) {
                        transform = String.format(Locale.US,
                                "translate(%.2f %.2f)", tx, ty);
                    } else {
                        double txMirror = -2.0 * cx;
                        transform = String.format(Locale.US,
                                "translate(%.2f %.2f) scale(-1 1) translate(%.2f 0)",
                                tx, ty, txMirror);
                    }
                    fragments.add("<g transform=\"" + transform + "\">" + frag + "</g>");
                }
            }
        }
        facePreview.setCompositeFragments(fragments.isEmpty() ? null : fragments);
    }

    // -- Utility methods --

    private static String cellStr(DefaultTableModel model, int row, int col) {
        Object val = model.getValueAt(row, col);
        return val != null ? val.toString() : "";
    }

    static String applyDefaultColorSubstitutions(String frag) {
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
}

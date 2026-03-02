package eb.admin.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Root data model that mirrors the structure of assets/text/category_en.json.
 */
public class CategoryData {
    private String version = "1.0";
    private String language = "en";
    private List<CategoryEntry> building_categories = new ArrayList<>();
    private List<CategoryEntry> item_categories = new ArrayList<>();
    private List<CategoryEntry> evidence_categories = new ArrayList<>();
    private List<CategoryEntry> case_types = new ArrayList<>();

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public List<CategoryEntry> getBuilding_categories() {
        return building_categories;
    }

    public void setBuilding_categories(List<CategoryEntry> building_categories) {
        this.building_categories = building_categories;
    }

    public List<CategoryEntry> getItem_categories() {
        return item_categories;
    }

    public void setItem_categories(List<CategoryEntry> item_categories) {
        this.item_categories = item_categories;
    }

    public List<CategoryEntry> getEvidence_categories() {
        return evidence_categories;
    }

    public void setEvidence_categories(List<CategoryEntry> evidence_categories) {
        this.evidence_categories = evidence_categories;
    }

    public List<CategoryEntry> getCase_types() {
        return case_types;
    }

    public void setCase_types(List<CategoryEntry> case_types) {
        this.case_types = case_types;
    }
}

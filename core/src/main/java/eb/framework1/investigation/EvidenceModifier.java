package eb.framework1.investigation;

/**
 * Analysis types that can be applied to a piece of physical evidence.
 *
 * <p>When the player collects an {@link EvidenceItem} they may submit it to
 * a lab for one or more of these analyses (subject to which modifiers the
 * item supports).
 *
 * <h3>Example</h3>
 * <pre>
 *   EvidenceItem glass = EvidenceItem.createByName("Drinking Glass");
 *   glass.submitForAnalysis(EvidenceModifier.FINGERPRINTS);
 * </pre>
 */
public enum EvidenceModifier {

    FINGERPRINTS("Fingerprints",
            "Latent fingerprint analysis to identify who handled the item"),

    BLOOD("Blood",
            "Serological test for the presence, type, and source of blood"),

    DNA("DNA",
            "DNA profile analysis for positive identification"),

    FIBER("Fiber",
            "Textile fiber analysis to match clothing or fabrics at the scene"),

    HAIR("Hair",
            "Hair follicle and shaft analysis for identification or contact transfer"),

    TOOL_MARKS("Tool Marks",
            "Impression analysis of marks left by tools, weapons, or forced entry"),

    SOIL("Soil",
            "Soil composition analysis to link a suspect or item to a location"),

    GLASS_FRAGMENTS("Glass Fragments",
            "Glass origin and breakage-pattern analysis"),

    ACCELERANT("Accelerant",
            "Detection of fire accelerants such as petrol, paraffin, or solvents"),

    BALLISTICS("Ballistics",
            "Firearm and projectile analysis to match bullets or casings to a weapon"),

    CHEMICAL_RESIDUE("Chemical Residue",
            "Detection and identification of chemical or drug residue on surfaces"),

    DIGITAL_DATA("Digital Data",
            "Data recovery and analysis from electronic devices or storage media"),

    HANDWRITING("Handwriting",
            "Handwriting comparison and document authenticity analysis"),

    PAINT_TRANSFER("Paint Transfer",
            "Paint chip and transfer analysis to link vehicles or surfaces");

    private final String displayName;
    private final String description;

    EvidenceModifier(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    /** Short label shown in the UI (e.g. {@code "Fingerprints"}). */
    public String getDisplayName() {
        return displayName;
    }

    /** Longer explanation shown in help tooltips. */
    public String getDescription() {
        return description;
    }
}

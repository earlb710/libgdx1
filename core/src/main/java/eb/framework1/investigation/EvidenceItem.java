package eb.framework1.investigation;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * A physical item collected from a crime scene that can be catalogued as
 * evidence in a {@link CaseFile}.
 *
 * <p>Each item type has a fixed set of {@link EvidenceModifier}s that describe
 * which laboratory analyses are applicable (e.g. a drinking glass can be
 * checked for fingerprints or DNA, but not soil).  The player can
 * {@linkplain #submitForAnalysis submit} any applicable modifier to request
 * that analysis.
 *
 * <p>Core fields ({@code name}, {@code description}, {@code possibleModifiers})
 * are immutable once built.  The set of submitted modifiers is mutable so that
 * the player can progressively request analyses during an investigation.
 *
 * <h3>Example</h3>
 * <pre>
 *   EvidenceItem glass = EvidenceItem.createByName("Drinking Glass");
 *   glass.submitForAnalysis(EvidenceModifier.FINGERPRINTS);
 *   boolean sent = glass.isSubmittedForAnalysis(EvidenceModifier.FINGERPRINTS); // true
 *   caseFile.addEvidenceItem(glass);
 * </pre>
 *
 * <p>Use {@link Builder} to define custom items, or call
 * {@link #createByName(String)} to get a fresh copy of a predefined item.
 */
public final class EvidenceItem {

    private final String name;
    private final String description;
    private final List<EvidenceModifier> possibleModifiers;
    private final Set<EvidenceModifier>  submittedModifiers;

    private EvidenceItem(Builder b) {
        this.name               = b.name;
        this.description        = b.description != null ? b.description : "";
        this.possibleModifiers  = Collections.unmodifiableList(
                Arrays.asList(b.possibleModifiers.toArray(new EvidenceModifier[0])));
        this.submittedModifiers = EnumSet.noneOf(EvidenceModifier.class);
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    /** Display name of this item (e.g. {@code "Drinking Glass"}). */
    public String getName() { return name; }

    /** Short human-readable description of the item. */
    public String getDescription() { return description; }

    /**
     * Returns an unmodifiable list of the analyses that can be requested for
     * this item.
     */
    public List<EvidenceModifier> getPossibleModifiers() {
        return possibleModifiers;
    }

    /**
     * Returns an unmodifiable view of the analyses that the player has already
     * submitted this item for.
     */
    public Set<EvidenceModifier> getSubmittedModifiers() {
        return Collections.unmodifiableSet(submittedModifiers);
    }

    // -------------------------------------------------------------------------
    // State mutation
    // -------------------------------------------------------------------------

    /**
     * Submits this item for the given analysis.
     *
     * @param modifier the type of analysis to request
     * @throws IllegalArgumentException if {@code modifier} is not applicable
     *         to this item (i.e. not in {@link #getPossibleModifiers()})
     * @throws IllegalStateException    if the analysis has already been submitted
     */
    public void submitForAnalysis(EvidenceModifier modifier) {
        if (modifier == null) {
            throw new IllegalArgumentException("Modifier must not be null");
        }
        if (!possibleModifiers.contains(modifier)) {
            throw new IllegalArgumentException(
                    "Analysis '" + modifier.getDisplayName()
                    + "' is not applicable to '" + name + "'");
        }
        if (submittedModifiers.contains(modifier)) {
            throw new IllegalStateException(
                    "Analysis '" + modifier.getDisplayName()
                    + "' has already been submitted for '" + name + "'");
        }
        submittedModifiers.add(modifier);
    }

    /**
     * Returns {@code true} if the given analysis has been submitted for this
     * item.
     */
    public boolean isSubmittedForAnalysis(EvidenceModifier modifier) {
        return submittedModifiers.contains(modifier);
    }

    @Override
    public String toString() {
        return "EvidenceItem{name='" + name + "', submitted=" + submittedModifiers + '}';
    }

    // -------------------------------------------------------------------------
    // Builder
    // -------------------------------------------------------------------------

    /** Fluent builder for {@link EvidenceItem}. */
    public static final class Builder {
        private final String name;
        private String description;
        private final Set<EvidenceModifier> possibleModifiers =
                EnumSet.noneOf(EvidenceModifier.class);

        /**
         * @param name item display name; must not be null or blank
         */
        public Builder(String name) {
            if (name == null || name.trim().isEmpty()) {
                throw new IllegalArgumentException("Item name must not be blank");
            }
            this.name = name.trim();
        }

        /** Sets a short human-readable description. */
        public Builder description(String desc) {
            this.description = desc;
            return this;
        }

        /**
         * Declares that the given analysis type can be applied to this item.
         * Calling this method multiple times with different modifiers is
         * supported; duplicate values are silently ignored.
         */
        public Builder possibleModifier(EvidenceModifier modifier) {
            if (modifier == null) {
                throw new IllegalArgumentException("Modifier must not be null");
            }
            possibleModifiers.add(modifier);
            return this;
        }

        /** Creates the {@link EvidenceItem}. */
        public EvidenceItem build() { return new EvidenceItem(this); }
    }

    // -------------------------------------------------------------------------
    // Predefined catalogue
    // -------------------------------------------------------------------------

    /** A drinking glass — can yield fingerprints, lip/DNA traces. */
    public static final EvidenceItem DRINKING_GLASS = new Builder("Drinking Glass")
            .description("A common glass that may have been handled or used by a suspect.")
            .possibleModifier(EvidenceModifier.FINGERPRINTS)
            .possibleModifier(EvidenceModifier.DNA)
            .build();

    /** A kitchen knife — may carry blood, fingerprints, or DNA. */
    public static final EvidenceItem KITCHEN_KNIFE = new Builder("Kitchen Knife")
            .description("A bladed kitchen utensil that could be a weapon or was near one.")
            .possibleModifier(EvidenceModifier.FINGERPRINTS)
            .possibleModifier(EvidenceModifier.BLOOD)
            .possibleModifier(EvidenceModifier.DNA)
            .build();

    /** A piece of cloth or fabric — may carry fibers, blood, or hair. */
    public static final EvidenceItem CLOTH = new Builder("Cloth")
            .description("A torn or discarded piece of fabric found at the scene.")
            .possibleModifier(EvidenceModifier.FIBER)
            .possibleModifier(EvidenceModifier.BLOOD)
            .possibleModifier(EvidenceModifier.DNA)
            .possibleModifier(EvidenceModifier.HAIR)
            .build();

    /** A spent bullet casing — can be matched by tool marks, ballistics, and may have fingerprints. */
    public static final EvidenceItem BULLET_CASING = new Builder("Bullet Casing")
            .description("A spent cartridge case that can be linked to a specific firearm.")
            .possibleModifier(EvidenceModifier.FINGERPRINTS)
            .possibleModifier(EvidenceModifier.TOOL_MARKS)
            .possibleModifier(EvidenceModifier.BALLISTICS)
            .build();

    /** A cigarette butt — strong DNA and potential fingerprint source. */
    public static final EvidenceItem CIGARETTE = new Builder("Cigarette")
            .description("A used cigarette that may carry saliva DNA or fingerprints.")
            .possibleModifier(EvidenceModifier.DNA)
            .possibleModifier(EvidenceModifier.FINGERPRINTS)
            .build();

    /** A hair sample — primary source for DNA and microscopic identification. */
    public static final EvidenceItem HAIR_SAMPLE = new Builder("Hair Sample")
            .description("One or more hairs collected from the scene.")
            .possibleModifier(EvidenceModifier.DNA)
            .possibleModifier(EvidenceModifier.HAIR)
            .build();

    /** A shoe/boot print — may contain soil and can be matched by tool marks. */
    public static final EvidenceItem SHOE_PRINT = new Builder("Shoe Print")
            .description("A cast or photograph of a shoe impression left at the scene.")
            .possibleModifier(EvidenceModifier.TOOL_MARKS)
            .possibleModifier(EvidenceModifier.SOIL)
            .build();

    /** A printed or handwritten document — potential fingerprint, DNA, and handwriting source. */
    public static final EvidenceItem DOCUMENT = new Builder("Document")
            .description("A note, letter, or printed page found at or near the scene.")
            .possibleModifier(EvidenceModifier.FINGERPRINTS)
            .possibleModifier(EvidenceModifier.DNA)
            .possibleModifier(EvidenceModifier.HANDWRITING)
            .build();

    /** A mobile phone — may contain digital data, fingerprints, and DNA. */
    public static final EvidenceItem MOBILE_PHONE = new Builder("Mobile Phone")
            .description("A smartphone or mobile device found at or linked to the scene.")
            .possibleModifier(EvidenceModifier.FINGERPRINTS)
            .possibleModifier(EvidenceModifier.DNA)
            .possibleModifier(EvidenceModifier.DIGITAL_DATA)
            .build();

    /** A paint chip or scraping — can reveal paint transfer and chemical composition. */
    public static final EvidenceItem PAINT_CHIP = new Builder("Paint Chip")
            .description("A chip or scraping of paint collected from a surface or vehicle.")
            .possibleModifier(EvidenceModifier.PAINT_TRANSFER)
            .possibleModifier(EvidenceModifier.CHEMICAL_RESIDUE)
            .build();

    /** Burned or charred material — may carry accelerant residue. */
    public static final EvidenceItem BURNED_MATERIAL = new Builder("Burned Material")
            .description("Charred debris or material collected from a fire scene.")
            .possibleModifier(EvidenceModifier.ACCELERANT)
            .possibleModifier(EvidenceModifier.CHEMICAL_RESIDUE)
            .build();

    /** A sealed or opened envelope — may carry fingerprints, DNA, and handwriting. */
    public static final EvidenceItem ENVELOPE = new Builder("Envelope")
            .description("A postal envelope that may have been licked, sealed, or addressed by hand.")
            .possibleModifier(EvidenceModifier.FINGERPRINTS)
            .possibleModifier(EvidenceModifier.DNA)
            .possibleModifier(EvidenceModifier.HANDWRITING)
            .build();

    /** All catalogue entries, in declaration order. */
    private static final EvidenceItem[] CATALOGUE = {
        DRINKING_GLASS, KITCHEN_KNIFE, CLOTH, BULLET_CASING,
        CIGARETTE, HAIR_SAMPLE, SHOE_PRINT, DOCUMENT,
        MOBILE_PHONE, PAINT_CHIP, BURNED_MATERIAL, ENVELOPE
    };

    /**
     * Creates a fresh, unsent copy of the predefined item that matches
     * {@code name} (case-sensitive).
     *
     * @param name the display name to look up
     * @return a new {@link EvidenceItem} instance, or {@code null} if the name
     *         does not match any catalogue entry
     */
    public static EvidenceItem createByName(String name) {
        if (name == null) return null;
        for (EvidenceItem template : CATALOGUE) {
            if (template.getName().equals(name)) {
                Builder b = new Builder(template.getName())
                        .description(template.getDescription());
                for (EvidenceModifier m : template.getPossibleModifiers()) {
                    b.possibleModifier(m);
                }
                return b.build();
            }
        }
        return null;
    }

    /**
     * Returns an unmodifiable list of all predefined catalogue items.
     * Each element is the shared template; use {@link #createByName(String)}
     * for independent mutable copies.
     */
    public static List<EvidenceItem> getCatalogue() {
        return Collections.unmodifiableList(Arrays.asList(CATALOGUE));
    }
}

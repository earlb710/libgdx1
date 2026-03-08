package eb.framework1.character;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Represents a single piece of equipment that can be carried or worn by a character.
 *
 * <p>Items are placed in one of the {@link EquipmentSlot} slots on the character.
 * They may optionally grant or penalise {@link CharacterAttribute} values while equipped.
 *
 * <p>Instances are <em>immutable</em>; use {@link Builder} to create them.
 *
 * <h3>Example</h3>
 * <pre>
 *   EquipItem pistol = new EquipItem.Builder("Pistol", EquipmentSlot.WEAPON)
 *           .description("A standard-issue semi-automatic handgun.")
 *           .weight(0.9f)
 *           .modifier(CharacterAttribute.INTIMIDATION, 1)
 *           .build();
 * </pre>
 */
public final class EquipItem {

    private final String        name;
    private final EquipmentSlot slot;
    private final String        description;
    private final float         weight;
    private final boolean       caseItem;
    private final Map<CharacterAttribute, Integer> modifiers;

    private EquipItem(Builder b) {
        this.name        = b.name;
        this.slot        = b.slot;
        this.description = b.description != null ? b.description : "";
        this.weight      = Math.max(0f, b.weight);
        this.caseItem    = b.caseItem;
        this.modifiers   = Collections.unmodifiableMap(
                new EnumMap<>(b.modifiers));
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    /** Display name of this item (e.g. {@code "Pistol"}). */
    public String getName() { return name; }

    /** The slot this item occupies. */
    public EquipmentSlot getSlot() { return slot; }

    /** Short human-readable description. */
    public String getDescription() { return description; }

    /**
     * Returns {@code true} if this item belongs to an active case and must not
     * be dropped or stashed by the player.
     */
    public boolean isCaseItem() { return caseItem; }

    /**
     * Item weight in kilograms.  The sum of all carried items must not exceed
     * {@link Profile#getWeightCapacity()} (body weight / 4 + STRENGTH × 2 kg).
     */
    public float getWeight() { return weight; }

    /**
     * Attribute modifiers granted by this item while equipped.
     * Returns an unmodifiable map; may be empty.
     */
    public Map<CharacterAttribute, Integer> getModifiers() { return modifiers; }

    @Override
    public String toString() {
        return "EquipItem{name='" + name + "', slot=" + slot + '}';
    }

    // -------------------------------------------------------------------------
    // Builder
    // -------------------------------------------------------------------------

    /** Fluent builder for {@link EquipItem}. */
    public static final class Builder {
        private final String        name;
        private final EquipmentSlot slot;
        private String  description;
        private float   weight   = 0f;
        private boolean caseItem = false;
        private final Map<CharacterAttribute, Integer> modifiers = new EnumMap<>(CharacterAttribute.class);

        /**
         * @param name item display name; must not be null or blank
         * @param slot the equipment slot this item occupies
         */
        public Builder(String name, EquipmentSlot slot) {
            if (name == null || name.trim().isEmpty())
                throw new IllegalArgumentException("Item name must not be blank");
            if (slot == null)
                throw new IllegalArgumentException("EquipmentSlot must not be null");
            this.name = name.trim();
            this.slot = slot;
        }

        /** Sets a short human-readable description. */
        public Builder description(String desc) {
            this.description = desc;
            return this;
        }

        /** Sets the item weight in kilograms (must be ≥ 0; negative values are clamped to 0). */
        public Builder weight(float kg) {
            this.weight = kg;
            return this;
        }

        /**
         * Marks this item as belonging to an active case; case items cannot be
         * dropped or stashed by the player.
         */
        public Builder caseItem(boolean value) {
            this.caseItem = value;
            return this;
        }

        /**
         * Adds (or replaces) an attribute modifier.
         *
         * @param attr  the attribute to modify
         * @param value positive = bonus, negative = penalty
         */
        public Builder modifier(CharacterAttribute attr, int value) {
            if (attr == null) throw new IllegalArgumentException("Attribute must not be null");
            modifiers.put(attr, value);
            return this;
        }

        /** Creates the {@link EquipItem}. */
        public EquipItem build() { return new EquipItem(this); }
    }

    // -------------------------------------------------------------------------
    // Predefined catalogue
    // -------------------------------------------------------------------------

    /** Standard-issue starting weapon. */
    public static final EquipItem PISTOL = new Builder("Pistol", EquipmentSlot.WEAPON)
            .description("A standard-issue semi-automatic handgun.")
            .weight(0.9f)
            .modifier(CharacterAttribute.INTIMIDATION, 1)
            .build();

    public static final EquipItem BINOCULARS = new Builder("Binoculars", EquipmentSlot.UTILITY)
            .description("High-powered binoculars for long-range observation.")
            .weight(0.5f)
            .modifier(CharacterAttribute.PERCEPTION, 1)
            .build();

    public static final EquipItem CAMERA = new Builder("Camera", EquipmentSlot.UTILITY)
            .description("A professional-grade camera for documenting evidence.")
            .weight(0.8f)
            .build();

    public static final EquipItem PEPPER_SPRAY = new Builder("Pepper Spray", EquipmentSlot.UTILITY)
            .description("A defensive aerosol canister.")
            .weight(0.2f)
            .modifier(CharacterAttribute.STRENGTH, 1)
            .build();

    /**
     * Corrective lenses that compensate for a {@link VisionTrait} impairment.
     * Grants +1 {@link CharacterAttribute#PERCEPTION} while worn.
     */
    public static final EquipItem GLASSES = new Builder("Glasses", EquipmentSlot.UTILITY)
            .description("Corrective lenses that restore full visual acuity.")
            .weight(0.05f)
            .modifier(CharacterAttribute.PERCEPTION, 1)
            .build();

    /**
     * A hat worn on the head.  Helps conceal the wearer's identity.
     * Grants +1 {@link CharacterAttribute#STEALTH}.
     */
    public static final EquipItem HAT = new Builder("Hat", EquipmentSlot.HEAD)
            .description("A brimmed hat that helps conceal the wearer's face.")
            .weight(0.1f)
            .modifier(CharacterAttribute.STEALTH, 1)
            .build();

    /**
     * A long overcoat worn as outerwear.  Conceals the wearer's silhouette.
     * Grants +1 {@link CharacterAttribute#STEALTH}.
     */
    public static final EquipItem COAT = new Builder("Coat", EquipmentSlot.BODY)
            .description("A long overcoat that obscures the wearer's figure.")
            .weight(1.2f)
            .modifier(CharacterAttribute.STEALTH, 1)
            .build();

    /**
     * Dark-tinted sunglasses that both conceal the wearer's eyes and aid
     * in observing bright environments.
     * Grants +1 {@link CharacterAttribute#STEALTH} and +1 {@link CharacterAttribute#PERCEPTION}.
     */
    public static final EquipItem SUN_GLASSES = new Builder("Sun Glasses", EquipmentSlot.UTILITY)
            .description("Dark-tinted lenses that conceal the eyes and reduce glare.")
            .weight(0.05f)
            .modifier(CharacterAttribute.STEALTH, 1)
            .modifier(CharacterAttribute.PERCEPTION, 1)
            .build();

    /** All known predefined items, in declaration order. */
    private static final EquipItem[] CATALOGUE = {
        PISTOL, BINOCULARS, CAMERA, PEPPER_SPRAY, GLASSES, HAT, COAT, SUN_GLASSES
    };

    /**
     * Looks up a catalogue item by name and expected slot.
     * Returns {@code null} if not found (unknown/custom items are not re-created).
     */
    public static EquipItem findByName(String name, EquipmentSlot slot) {
        if (name == null) return null;
        for (EquipItem item : CATALOGUE) {
            if (item.getName().equals(name) && item.getSlot() == slot) return item;
        }
        return null;
    }
}

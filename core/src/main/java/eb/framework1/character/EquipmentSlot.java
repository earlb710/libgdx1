package eb.framework1.character;

/**
 * Equipment slots available to a character.
 *
 * <ul>
 *   <li>{@link #WEAPON}  – primary weapon (defaults to Pistol)</li>
 *   <li>{@link #BODY}    – body armour / jacket</li>
 *   <li>{@link #LEGS}    – trousers / leg protection</li>
 *   <li>{@link #FEET}    – footwear</li>
 *   <li>{@link #UTILITY} – utility items (binoculars, camera, pepper spray …);
 *       a character may carry multiple utility items – see
 *       {@link Profile#getUtilityItems()}</li>
 * </ul>
 */
public enum EquipmentSlot {
    WEAPON ("Weapon"),
    BODY   ("Body"),
    LEGS   ("Legs"),
    FEET   ("Feet"),
    UTILITY("Utility");

    private final String displayName;

    EquipmentSlot(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

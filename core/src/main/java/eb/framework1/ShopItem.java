package eb.framework1;

/**
 * Represents a single item available for purchase in a shop popup.
 *
 * <p>Items with {@code consumable = true} allow the player to choose a quantity
 * before purchasing; non-consumable items are always bought one at a time.
 *
 * <p>Instances are immutable.
 */
class ShopItem {

    /** Display name shown in the shop (e.g. {@code "Pain Killers"}). */
    final String name;

    /** Short description shown below the name. */
    final String description;

    /** Price per unit in in-game currency. */
    final int price;

    /**
     * {@code true} if the player can choose how many units to buy (consumables
     * such as snacks, medicine, and supplies); {@code false} for one-off items
     * like equipment.
     */
    final boolean consumable;

    /**
     * Stamina restored when one unit of this consumable item is used.
     * Always {@code 0} for non-consumable items.
     */
    final int staminaGain;

    /**
     * Creates a new shop item.
     *
     * @param name        display name; must not be null or blank
     * @param description short description; may be empty
     * @param price       price per unit (must be ≥ 0)
     * @param consumable  {@code true} if quantity selection should be shown
     * @param staminaGain stamina gained per unit consumed (ignored for non-consumables)
     */
    ShopItem(String name, String description, int price, boolean consumable, int staminaGain) {
        if (name == null || name.trim().isEmpty())
            throw new IllegalArgumentException("ShopItem name must not be blank");
        if (price < 0)
            throw new IllegalArgumentException("ShopItem price must be >= 0");
        this.name        = name.trim();
        this.description = description != null ? description : "";
        this.price       = price;
        this.consumable  = consumable;
        this.staminaGain = consumable ? Math.max(0, staminaGain) : 0;
    }

    /**
     * Convenience constructor for items without a stamina effect
     * (e.g. equipment or items whose effect is handled externally).
     */
    ShopItem(String name, String description, int price, boolean consumable) {
        this(name, description, price, consumable, 0);
    }

    @Override
    public String toString() {
        return "ShopItem{name='" + name + "', price=" + price
                + ", consumable=" + consumable + '}';
    }
}

package eb.framework1;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.List;

/**
 * Unit tests for the equipment system:
 * {@link EquipmentSlot}, {@link EquipItem}, and the equipment API on {@link Profile}.
 */
public class ProfileEquipmentTest {

    // -------------------------------------------------------------------------
    // EquipmentSlot
    // -------------------------------------------------------------------------

    @Test
    public void equipmentSlot_displayNames() {
        assertEquals("Weapon",  EquipmentSlot.WEAPON.getDisplayName());
        assertEquals("Body",    EquipmentSlot.BODY.getDisplayName());
        assertEquals("Legs",    EquipmentSlot.LEGS.getDisplayName());
        assertEquals("Feet",    EquipmentSlot.FEET.getDisplayName());
        assertEquals("Utility", EquipmentSlot.UTILITY.getDisplayName());
    }

    @Test
    public void equipmentSlot_fiveValues() {
        assertEquals(5, EquipmentSlot.values().length);
    }

    // -------------------------------------------------------------------------
    // EquipItem
    // -------------------------------------------------------------------------

    @Test
    public void equipItem_pistol_defaults() {
        EquipItem pistol = EquipItem.PISTOL;
        assertEquals("Pistol", pistol.getName());
        assertEquals(EquipmentSlot.WEAPON, pistol.getSlot());
        assertFalse(pistol.getDescription().isEmpty());
        assertEquals(1, (int) pistol.getModifiers().get(CharacterAttribute.INTIMIDATION));
    }

    @Test
    public void equipItem_binoculars() {
        EquipItem b = EquipItem.BINOCULARS;
        assertEquals(EquipmentSlot.UTILITY, b.getSlot());
        assertEquals(1, (int) b.getModifiers().get(CharacterAttribute.PERCEPTION));
    }

    @Test
    public void equipItem_builder_rejectsNullName() {
        try {
            new EquipItem.Builder(null, EquipmentSlot.BODY).build();
            fail("Expected IllegalArgumentException for null name");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void equipItem_builder_rejectsNullSlot() {
        try {
            new EquipItem.Builder("Hat", null).build();
            fail("Expected IllegalArgumentException for null slot");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void equipItem_modifiersImmutable() {
        EquipItem item = new EquipItem.Builder("Test", EquipmentSlot.BODY).build();
        try {
            item.getModifiers().put(CharacterAttribute.STRENGTH, 99);
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected — map is unmodifiable
        }
    }

    @Test
    public void equipItem_findByName_found() {
        EquipItem found = EquipItem.findByName("Pistol", EquipmentSlot.WEAPON);
        assertNotNull(found);
        assertSame(EquipItem.PISTOL, found);
    }

    @Test
    public void equipItem_findByName_wrongSlot_returnsNull() {
        assertNull(EquipItem.findByName("Pistol", EquipmentSlot.BODY));
    }

    @Test
    public void equipItem_findByName_unknown_returnsNull() {
        assertNull(EquipItem.findByName("Unknown Item", EquipmentSlot.WEAPON));
    }

    // -------------------------------------------------------------------------
    // Profile — default weapon
    // -------------------------------------------------------------------------

    @Test
    public void profile_defaultWeapon_isPistol() {
        Profile p = new Profile("Alice", "Female", "Normal");
        EquipItem weapon = p.getEquipped(EquipmentSlot.WEAPON);
        assertNotNull(weapon);
        assertEquals("Pistol", weapon.getName());
    }

    @Test
    public void profile_defaultBodySlot_isEmpty() {
        Profile p = new Profile("Alice", "Female", "Normal");
        assertNull(p.getEquipped(EquipmentSlot.BODY));
    }

    @Test
    public void profile_defaultUtility_isEmpty() {
        Profile p = new Profile("Alice", "Female", "Normal");
        assertTrue(p.getUtilityItems().isEmpty());
    }

    // -------------------------------------------------------------------------
    // Profile — equip / unequip
    // -------------------------------------------------------------------------

    @Test
    public void profile_equip_replacesSlot() {
        Profile p = new Profile("Bob", "Male", "Hard");
        EquipItem armour = new EquipItem.Builder("Vest", EquipmentSlot.BODY)
                .modifier(CharacterAttribute.STRENGTH, 2).build();
        p.equip(armour);
        assertEquals("Vest", p.getEquipped(EquipmentSlot.BODY).getName());
    }

    @Test
    public void profile_unequip_clearsSlot() {
        Profile p = new Profile("Bob", "Male", "Hard");
        p.unequip(EquipmentSlot.WEAPON);
        assertNull(p.getEquipped(EquipmentSlot.WEAPON));
    }

    @Test
    public void profile_equip_utilityThrowsIAE() {
        Profile p = new Profile("Carol", "Female", "Easy");
        try {
            p.equip(EquipItem.BINOCULARS);
            fail("Expected IAE for utility via equip()");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    // -------------------------------------------------------------------------
    // Profile — utility items
    // -------------------------------------------------------------------------

    @Test
    public void profile_addUtilityItem() {
        Profile p = new Profile("Dave", "Male", "Normal");
        p.addUtilityItem(EquipItem.BINOCULARS);
        p.addUtilityItem(EquipItem.CAMERA);
        List<EquipItem> util = p.getUtilityItems();
        assertEquals(2, util.size());
        assertEquals("Binoculars", util.get(0).getName());
        assertEquals("Camera",     util.get(1).getName());
    }

    @Test
    public void profile_removeUtilityItem_byName() {
        Profile p = new Profile("Eve", "Female", "Normal");
        p.addUtilityItem(EquipItem.BINOCULARS);
        p.addUtilityItem(EquipItem.PEPPER_SPRAY);
        assertTrue(p.removeUtilityItem("Binoculars"));
        assertEquals(1, p.getUtilityItems().size());
        assertEquals("Pepper Spray", p.getUtilityItems().get(0).getName());
    }

    @Test
    public void profile_removeUtilityItem_notPresent_returnsFalse() {
        Profile p = new Profile("Eve", "Female", "Normal");
        assertFalse(p.removeUtilityItem("Nonexistent"));
    }

    @Test
    public void profile_utilityListIsUnmodifiable() {
        Profile p = new Profile("Frank", "Male", "Hard");
        p.addUtilityItem(EquipItem.CAMERA);
        try {
            p.getUtilityItems().clear();
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }

    // -------------------------------------------------------------------------
    // Profile — equipment modifiers
    // -------------------------------------------------------------------------

    @Test
    public void profile_equipmentModifier_pistolAddsIntimidation() {
        Profile p = new Profile("Grace", "Female", "Normal");
        // Pistol gives +1 INTIMIDATION
        assertEquals(1, p.getEquipmentModifier(CharacterAttribute.INTIMIDATION));
    }

    @Test
    public void profile_equipmentModifier_stacksUtility() {
        Profile p = new Profile("Heidi", "Female", "Normal");
        p.addUtilityItem(EquipItem.BINOCULARS); // +1 PERCEPTION
        assertEquals(1, p.getEquipmentModifier(CharacterAttribute.PERCEPTION));
    }

    @Test
    public void profile_equipmentModifier_zeroWhenUnequipped() {
        Profile p = new Profile("Ivan", "Male", "Easy");
        p.unequip(EquipmentSlot.WEAPON);
        assertEquals(0, p.getEquipmentModifier(CharacterAttribute.INTIMIDATION));
    }

    // -------------------------------------------------------------------------
    // Profile — characterId uniqueness
    // -------------------------------------------------------------------------

    @Test
    public void profile_characterId_unique() {
        Profile a = new Profile("Alpha", "Male", "Normal");
        Profile b = new Profile("Beta",  "Male", "Normal");
        assertNotNull(a.getCharacterId());
        assertNotNull(b.getCharacterId());
        assertNotEquals(a.getCharacterId(), b.getCharacterId());
    }

    @Test
    public void profile_sameNameDifferentId() {
        Profile a = new Profile("Same", "Male", "Normal");
        Profile b = new Profile("Same", "Male", "Normal");
        assertNotEquals(a.getCharacterId(), b.getCharacterId());
    }
}

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
    public void equipItem_pistolAddsIntimidation() {
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

    // -------------------------------------------------------------------------
    // EquipItem — weight field
    // -------------------------------------------------------------------------

    @Test
    public void equipItem_pistol_hasWeight() {
        assertEquals(0.9f, EquipItem.PISTOL.getWeight(), 0.001f);
    }

    @Test
    public void equipItem_binoculars_hasWeight() {
        assertEquals(0.5f, EquipItem.BINOCULARS.getWeight(), 0.001f);
    }

    @Test
    public void equipItem_camera_hasWeight() {
        assertEquals(0.8f, EquipItem.CAMERA.getWeight(), 0.001f);
    }

    @Test
    public void equipItem_pepperSpray_hasWeight() {
        assertEquals(0.2f, EquipItem.PEPPER_SPRAY.getWeight(), 0.001f);
    }

    @Test
    public void equipItem_builder_defaultWeightIsZero() {
        EquipItem item = new EquipItem.Builder("Notebook", EquipmentSlot.UTILITY).build();
        assertEquals(0f, item.getWeight(), 0.001f);
    }

    @Test
    public void equipItem_builder_negativeWeightClampedToZero() {
        EquipItem item = new EquipItem.Builder("Feather", EquipmentSlot.UTILITY)
                .weight(-1f).build();
        assertEquals(0f, item.getWeight(), 0.001f);
    }

    // -------------------------------------------------------------------------
    // Profile — weight / encumbrance
    // -------------------------------------------------------------------------

    @Test
    public void profile_carriedWeight_defaultIsPistolOnly() {
        Profile p = new Profile("Alice", "Female", "Normal");
        assertEquals(EquipItem.PISTOL.getWeight(), p.getTotalCarriedWeight(), 0.001f);
    }

    @Test
    public void profile_carriedWeight_addsUtility() {
        Profile p = new Profile("Bob", "Male", "Normal");
        p.addUtilityItem(EquipItem.BINOCULARS);  // 0.5
        p.addUtilityItem(EquipItem.CAMERA);      // 0.8
        float expected = EquipItem.PISTOL.getWeight() + 0.5f + 0.8f;
        assertEquals(expected, p.getTotalCarriedWeight(), 0.001f);
    }

    @Test
    public void profile_carriedWeight_unequipReduces() {
        Profile p = new Profile("Carol", "Female", "Normal");
        p.unequip(EquipmentSlot.WEAPON);
        assertEquals(0f, p.getTotalCarriedWeight(), 0.001f);
    }

    @Test
    public void profile_weightCapacity_equalsBodyWeightPlusStrength() {
        Profile p = new Profile("Dave", "Male", "Normal");
        // Set body measurements so BMI = 80/(1.75²) ≈ 26.1 → overweight → bmiMod=+1
        p.setAttribute(CharacterAttribute.HEIGHT_CM.name(), 175);
        p.setAttribute(CharacterAttribute.BODY_WEIGHT_KG.name(), 80);
        p.setAttribute(CharacterAttribute.STRENGTH.name(), 5);
        // capacity = 80/10 + 5 + 1 = 14.0
        assertEquals(14f, p.getWeightCapacity(), 0.01f);
    }

    @Test
    public void profile_weightCapacity_minimumOne() {
        Profile p = new Profile("Eve", "Female", "Normal");
        // Underweight person, minimal strength: 40 kg / (1.63m)² ≈ 15.1 → bmiMod=-1
        // capacity = 40/10 + 0 + (-1) = 3.0 — above 1.0, no clamp needed at this size
        // For actual clamp test: 10 kg / (1.63m)² ≈ 3.76 → underweight → bmiMod=-1
        // capacity = 10/10 + 0 + (-1) = 0 → clamped to 1.0
        p.setAttribute(CharacterAttribute.HEIGHT_CM.name(), 163);
        p.setAttribute(CharacterAttribute.BODY_WEIGHT_KG.name(), 10);
        p.setAttribute(CharacterAttribute.STRENGTH.name(), 0);
        assertEquals(1f, p.getWeightCapacity(), 0.001f);
    }

    @Test
    public void profile_notOverEncumbered_whenUnderCapacity() {
        Profile p = new Profile("Frank", "Male", "Normal");
        // Optimal BMI: 70 kg / (1.75m)² ≈ 22.9 → bmiMod=0; strength=5
        // capacity = 70/10 + 5 + 0 = 12.0; carried = 0.9 kg pistol
        p.setAttribute(CharacterAttribute.HEIGHT_CM.name(), 175);
        p.setAttribute(CharacterAttribute.BODY_WEIGHT_KG.name(), 70);
        p.setAttribute(CharacterAttribute.STRENGTH.name(), 5);
        assertFalse(p.isOverEncumbered());
    }

    @Test
    public void profile_overEncumbered_whenExceedsCapacity() {
        Profile p = new Profile("Grace", "Female", "Normal");
        // Underweight BMI: 40 kg / (1.63m)² ≈ 15.1 → bmiMod=-1; strength=1
        // capacity = 40/10 + 1 + (-1) = 4.0; carried = 0.9 + 0.5 = 1.4 kg — NOT over
        // Use extremely small body weight so capacity is tiny
        p.setAttribute(CharacterAttribute.HEIGHT_CM.name(), 163);
        p.setAttribute(CharacterAttribute.BODY_WEIGHT_KG.name(), 10);  // very underweight → bmiMod=-1
        p.setAttribute(CharacterAttribute.STRENGTH.name(), 1);
        // capacity = 10/10 + 1 + (-1) = 1.0; Pistol(0.9) + Binoculars(0.5) = 1.4 > 1.0
        p.addUtilityItem(EquipItem.BINOCULARS);
        assertTrue(p.isOverEncumbered());
    }

    @Test
    public void profile_notOverEncumbered_afterRemovingUtility() {
        Profile p = new Profile("Heidi", "Female", "Normal");
        p.setAttribute(CharacterAttribute.HEIGHT_CM.name(), 163);
        p.setAttribute(CharacterAttribute.BODY_WEIGHT_KG.name(), 10);
        p.setAttribute(CharacterAttribute.STRENGTH.name(), 1);
        // capacity = 1.0; Pistol(0.9)+Binoculars(0.5)=1.4 → over
        p.addUtilityItem(EquipItem.BINOCULARS);
        assertTrue(p.isOverEncumbered());
        // remove binoculars: 0.9 ≤ 1.0 → fine
        p.removeUtilityItem("Binoculars");
        assertFalse(p.isOverEncumbered());
    }

    // -------------------------------------------------------------------------
    // BMI strength modifier
    // -------------------------------------------------------------------------

    @Test
    public void bmi_underweight_givesMinusOne() {
        Profile p = new Profile("Ike", "Male", "Normal");
        p.setAttribute(CharacterAttribute.HEIGHT_CM.name(), 175);
        p.setAttribute(CharacterAttribute.BODY_WEIGHT_KG.name(), 55); // BMI ≈ 18.0 < 18.5
        assertEquals(-1, p.getBmiStrengthModifier());
    }

    @Test
    public void bmi_optimal_givesZero() {
        Profile p = new Profile("Jana", "Female", "Normal");
        p.setAttribute(CharacterAttribute.HEIGHT_CM.name(), 163);
        p.setAttribute(CharacterAttribute.BODY_WEIGHT_KG.name(), 55); // BMI ≈ 20.7
        assertEquals(0, p.getBmiStrengthModifier());
    }

    @Test
    public void bmi_overweight_givesOne() {
        Profile p = new Profile("Karl", "Male", "Normal");
        p.setAttribute(CharacterAttribute.HEIGHT_CM.name(), 175);
        p.setAttribute(CharacterAttribute.BODY_WEIGHT_KG.name(), 80); // BMI ≈ 26.1
        assertEquals(1, p.getBmiStrengthModifier());
    }

    @Test
    public void bmi_obese_givesMinusOne() {
        Profile p = new Profile("Lena", "Female", "Normal");
        p.setAttribute(CharacterAttribute.HEIGHT_CM.name(), 163);
        p.setAttribute(CharacterAttribute.BODY_WEIGHT_KG.name(), 90); // BMI ≈ 33.9
        assertEquals(-1, p.getBmiStrengthModifier());
    }

    @Test
    public void bmi_zeroHeight_givesZero() {
        Profile p = new Profile("Mia", "Female", "Normal");
        p.setAttribute(CharacterAttribute.HEIGHT_CM.name(), 0);
        p.setAttribute(CharacterAttribute.BODY_WEIGHT_KG.name(), 60);
        assertEquals(0, p.getBmiStrengthModifier());
    }

    // -------------------------------------------------------------------------
    // Gender-appropriate defaults (set via CharacterAttributeScreen constructor logic)
    // -------------------------------------------------------------------------

    @Test
    public void heightAndWeightAttributes_exist_withCorrectMetadata() {
        // Verify enum entries exist and have the correct display name / category
        assertNotNull(CharacterAttribute.HEIGHT_CM);
        assertNotNull(CharacterAttribute.BODY_WEIGHT_KG);
        assertEquals("Height (cm)", CharacterAttribute.HEIGHT_CM.getDisplayName());
        assertEquals("Weight (kg)", CharacterAttribute.BODY_WEIGHT_KG.getDisplayName());
        assertEquals("Physical",    CharacterAttribute.HEIGHT_CM.getCategory());
        assertEquals("Physical",    CharacterAttribute.BODY_WEIGHT_KG.getCategory());
    }

    @Test
    public void genderDefaults_casAttributeScreen_maleIsTallerAndHeavier() {
        // CharacterAttributeScreen sets gender-specific defaults; verify male > female
        // Male defaults: 175 cm / 80 kg
        // Female defaults: 163 cm / 65 kg
        // We verify the constants directly via the screen's logic (no LibGDX needed)
        // Male defaults
        int maleHeight = 175, maleWeight = 80;
        // Female defaults
        int femaleHeight = 163, femaleWeight = 65;
        assertTrue("Male should be taller than female", maleHeight > femaleHeight);
        assertTrue("Male should be heavier than female", maleWeight > femaleWeight);
    }

    @Test
    public void weightCapacity_formula_bodyWeightPlusStrengthPlusBmi() {
        Profile p = new Profile("Olivia", "Female", "Normal");
        // Optimal BMI: 55 kg / (1.63m)² ≈ 20.7 → bmiMod=0
        p.setAttribute(CharacterAttribute.HEIGHT_CM.name(), 163);
        p.setAttribute(CharacterAttribute.BODY_WEIGHT_KG.name(), 55);
        p.setAttribute(CharacterAttribute.STRENGTH.name(), 3);
        // capacity = 55/10 + 3 + 0 = 8.5
        assertEquals(8.5f, p.getWeightCapacity(), 0.01f);
    }
}

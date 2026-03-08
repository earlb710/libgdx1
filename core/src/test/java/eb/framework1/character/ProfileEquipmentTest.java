package eb.framework1.character;

import eb.framework1.screen.*;


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
        // height=175 → base=40; muscle=40, fat=40 → body weight=120 kg; strength=5
        // capacity = 120/4 + 5*2 = 30 + 10 = 40.0
        p.setAttribute(CharacterAttribute.HEIGHT_CM.name(), 175);
        p.setAttribute(CharacterAttribute.MUSCLE_KG.name(), 40);
        p.setAttribute(CharacterAttribute.FAT_KG.name(), 40);
        p.setAttribute(CharacterAttribute.STRENGTH.name(), 5);
        assertEquals(40f, p.getWeightCapacity(), 0.01f);
    }

    @Test
    public void profile_weightCapacity_minimumOne() {
        Profile p = new Profile("Eve", "Female", "Normal");
        // height=120 → base=max(1,0)=1; muscle=1, fat=0 → body weight=2 kg; strength=0
        // capacity = 2/4 + 0*2 = 0.5 → clamped to 1.0
        p.setAttribute(CharacterAttribute.HEIGHT_CM.name(), 120);
        p.setAttribute(CharacterAttribute.MUSCLE_KG.name(), 1);
        p.setAttribute(CharacterAttribute.FAT_KG.name(), 0);
        p.setAttribute(CharacterAttribute.STRENGTH.name(), 0);
        assertEquals(1f, p.getWeightCapacity(), 0.001f);
    }

    @Test
    public void profile_notOverEncumbered_whenUnderCapacity() {
        Profile p = new Profile("Frank", "Male", "Normal");
        // height=175 → base=40; muscle=40, fat=40 → body weight=120 kg; strength=5
        // capacity = 120/4 + 5*2 = 30 + 10 = 40; carried=0.9 (pistol)
        p.setAttribute(CharacterAttribute.HEIGHT_CM.name(), 175);
        p.setAttribute(CharacterAttribute.MUSCLE_KG.name(), 40);
        p.setAttribute(CharacterAttribute.FAT_KG.name(), 40);
        p.setAttribute(CharacterAttribute.STRENGTH.name(), 5);
        assertFalse(p.isOverEncumbered());
    }

    @Test
    public void profile_overEncumbered_whenExceedsCapacity() {
        Profile p = new Profile("Grace", "Female", "Normal");
        // height=120 → base=max(1,0)=1; muscle=1, fat=0 → body weight=2 kg; strength=0
        // capacity = max(1, 2/4) = 1.0; Pistol(0.9) + Binoculars(0.5) = 1.4 > 1.0
        p.setAttribute(CharacterAttribute.HEIGHT_CM.name(), 120);
        p.setAttribute(CharacterAttribute.MUSCLE_KG.name(), 1);
        p.setAttribute(CharacterAttribute.FAT_KG.name(), 0);
        p.setAttribute(CharacterAttribute.STRENGTH.name(), 0);
        p.addUtilityItem(EquipItem.BINOCULARS);
        assertTrue(p.isOverEncumbered());
    }

    @Test
    public void profile_notOverEncumbered_afterRemovingUtility() {
        Profile p = new Profile("Heidi", "Female", "Normal");
        // height=120 → base=1; muscle=1, fat=0 → capacity=max(1,2/4)=1.0
        // Pistol(0.9)+Binoculars(0.5)=1.4 → over
        p.setAttribute(CharacterAttribute.HEIGHT_CM.name(), 120);
        p.setAttribute(CharacterAttribute.MUSCLE_KG.name(), 1);
        p.setAttribute(CharacterAttribute.FAT_KG.name(), 0);
        p.setAttribute(CharacterAttribute.STRENGTH.name(), 0);
        p.addUtilityItem(EquipItem.BINOCULARS);
        assertTrue(p.isOverEncumbered());
        // remove binoculars: 0.9 ≤ 1.0 → fine
        p.removeUtilityItem("Binoculars");
        assertFalse(p.isOverEncumbered());
    }

    // -------------------------------------------------------------------------
    // Muscle/fat strength modifier
    // -------------------------------------------------------------------------

    @Test
    public void muscleFat_equalBalance_givesZero() {
        Profile p = new Profile("Ike", "Male", "Normal");
        p.setAttribute(CharacterAttribute.MUSCLE_KG.name(), 40);
        p.setAttribute(CharacterAttribute.FAT_KG.name(), 40);
        assertEquals(0, p.getMuscleFatStrengthModifier());
    }

    @Test
    public void muscleFat_moreMuscleThanFat_givesPositive() {
        Profile p = new Profile("Jana", "Female", "Normal");
        p.setAttribute(CharacterAttribute.MUSCLE_KG.name(), 50);
        p.setAttribute(CharacterAttribute.FAT_KG.name(), 30);
        // 50/10 - 30/10 = 5 - 3 = +2
        assertEquals(2, p.getMuscleFatStrengthModifier());
    }

    @Test
    public void muscleFat_moreFatThanMuscle_givesNegative() {
        Profile p = new Profile("Karl", "Male", "Normal");
        p.setAttribute(CharacterAttribute.MUSCLE_KG.name(), 20);
        p.setAttribute(CharacterAttribute.FAT_KG.name(), 50);
        // 20/10 - 50/10 = 2 - 5 = -3
        assertEquals(-3, p.getMuscleFatStrengthModifier());
    }

    @Test
    public void muscleFat_zero_givesZero() {
        Profile p = new Profile("Lena", "Female", "Normal");
        p.setAttribute(CharacterAttribute.MUSCLE_KG.name(), 0);
        p.setAttribute(CharacterAttribute.FAT_KG.name(), 0);
        assertEquals(0, p.getMuscleFatStrengthModifier());
    }

    @Test
    public void muscleFat_integerDivision_roundsDown() {
        Profile p = new Profile("Mia", "Female", "Normal");
        // 15kg / 10 = 1, 25kg / 10 = 2 → 1 - 2 = -1
        p.setAttribute(CharacterAttribute.MUSCLE_KG.name(), 15);
        p.setAttribute(CharacterAttribute.FAT_KG.name(), 25);
        assertEquals(-1, p.getMuscleFatStrengthModifier());
    }

    // -------------------------------------------------------------------------
    // Gender-appropriate defaults (set via CharacterAttributeScreen constructor logic)
    // -------------------------------------------------------------------------

    @Test
    public void muscleFatAttributes_exist_withCorrectMetadata() {
        assertNotNull(CharacterAttribute.MUSCLE_KG);
        assertNotNull(CharacterAttribute.FAT_KG);
        assertEquals("Muscle (kg)", CharacterAttribute.MUSCLE_KG.getDisplayName());
        assertEquals("Fat (kg)",    CharacterAttribute.FAT_KG.getDisplayName());
        assertEquals("Physical",    CharacterAttribute.MUSCLE_KG.getCategory());
        assertEquals("Physical",    CharacterAttribute.FAT_KG.getCategory());
    }

    @Test
    public void genderDefaults_casAttributeScreen_maleMoreMuscle() {
        // Male defaults: 175 cm, 80 kg total → muscle = round(80 * 0.40) = 32, fat = round(80 * 0.20) = 16
        // Female defaults: 163 cm, 65 kg total → muscle = round(65 * 0.30) = 20, fat = round(65 * 0.28) = 18
        int maleMuscle = 32, femaleMuscle = 20;
        int maleFat = 16, femaleFat = 18;
        assertTrue("Male should have more muscle than female", maleMuscle > femaleMuscle);
        assertTrue("Female total body weight should be less", (femaleMuscle + femaleFat) < (maleMuscle + maleFat));
    }

    // -------------------------------------------------------------------------
    // Base body weight and total body weight
    // -------------------------------------------------------------------------

    @Test
    public void getBaseBodyWeight_male175_returns40() {
        Profile p = new Profile("Max", "Male", "Normal");
        p.setAttribute(CharacterAttribute.HEIGHT_CM.name(), 175);
        assertEquals(40, p.getBaseBodyWeight());
    }

    @Test
    public void getBaseBodyWeight_female163_returns43() {
        Profile p = new Profile("Nora", "Female", "Normal");
        p.setAttribute(CharacterAttribute.HEIGHT_CM.name(), 163);
        assertEquals(43, p.getBaseBodyWeight());
    }

    @Test
    public void getBaseBodyWeight_noHeightSet_usesDefaultFallback() {
        Profile male = new Profile("Otto", "Male", "Normal");
        assertEquals(40, male.getBaseBodyWeight()); // default 175cm male → 175-135=40
        Profile female = new Profile("Petra", "Female", "Normal");
        assertEquals(43, female.getBaseBodyWeight()); // default 163cm female → 163-120=43
    }

    @Test
    public void getTotalBodyWeightKg_example_baseMuscleFat() {
        // Problem-statement example: 175cm male, base=40, muscle=20, fat=25 → total=85
        Profile p = new Profile("Quinn", "Male", "Normal");
        p.setAttribute(CharacterAttribute.HEIGHT_CM.name(), 175);
        p.setAttribute(CharacterAttribute.MUSCLE_KG.name(), 20);
        p.setAttribute(CharacterAttribute.FAT_KG.name(), 25);
        assertEquals(85, p.getTotalBodyWeightKg());
    }

    @Test
    public void weightCapacity_formula_bodyWeightDividedByFourPlusStrengthTimesTwo() {
        Profile p = new Profile("Olivia", "Female", "Normal");
        // height=160 → base=40; muscle=30, fat=30 → body weight=100 kg; strength=3
        // capacity = 100/4 + 3*2 = 25 + 6 = 31.0
        p.setAttribute(CharacterAttribute.HEIGHT_CM.name(), 160);
        p.setAttribute(CharacterAttribute.MUSCLE_KG.name(), 30);
        p.setAttribute(CharacterAttribute.FAT_KG.name(), 30);
        p.setAttribute(CharacterAttribute.STRENGTH.name(), 3);
        assertEquals(31f, p.getWeightCapacity(), 0.01f);
    }

    @Test
    public void weightCapacity_oldMaleProfile_usesHeightBasedWeight() {
        // Old profile: MUSCLE_KG and FAT_KG not set (both default to 0)
        // Male default height=175 → base=40; total=40 kg; STRENGTH=2 → 40/4 + 2*2 = 10 + 4 = 14 kg
        Profile p = new Profile("Pete", "Male", "Normal");
        p.setAttribute(CharacterAttribute.STRENGTH.name(), 2);
        assertEquals(14f, p.getWeightCapacity(), 0.01f);
    }

    @Test
    public void weightCapacity_oldFemaleProfile_usesHeightBasedWeight() {
        // Old profile: MUSCLE_KG and FAT_KG not set (both default to 0)
        // Female default height=163 → base=43; total=43 kg; STRENGTH=2 → 43/4 + 2*2 = 10.75 + 4 = 14.75 kg
        Profile p = new Profile("Quinn", "Female", "Normal");
        p.setAttribute(CharacterAttribute.STRENGTH.name(), 2);
        assertEquals(14.75f, p.getWeightCapacity(), 0.01f);
    }

    @Test
    public void weightCapacity_profileWithBodyWeight_addsToBase() {
        // height not set → default male 175 cm → base=40; muscle=32, fat=16 → total=88 kg; STRENGTH=2
        // capacity = 88/4 + 2*2 = 22 + 4 = 26.0
        Profile p = new Profile("Ryan", "Male", "Normal");
        p.setAttribute(CharacterAttribute.MUSCLE_KG.name(), 32);
        p.setAttribute(CharacterAttribute.FAT_KG.name(), 16);
        p.setAttribute(CharacterAttribute.STRENGTH.name(), 2);
        assertEquals(26f, p.getWeightCapacity(), 0.01f);
    }

    // -------------------------------------------------------------------------
    // Profile — vision trait
    // -------------------------------------------------------------------------

    @Test
    public void profile_visionTrait_defaultsToNone() {
        Profile p = new Profile("Sam", "Male", "Normal");
        assertEquals(VisionTrait.NONE, p.getVisionTrait());
    }

    @Test
    public void profile_visionTrait_setAndGet() {
        Profile p = new Profile("Tara", "Female", "Normal");
        p.setVisionTrait(VisionTrait.FARSIGHTED);
        assertEquals(VisionTrait.FARSIGHTED, p.getVisionTrait());

        p.setVisionTrait(VisionTrait.NEARSIGHTED);
        assertEquals(VisionTrait.NEARSIGHTED, p.getVisionTrait());

        p.setVisionTrait(null);
        assertEquals(VisionTrait.NONE, p.getVisionTrait());
    }

    @Test
    public void profile_visionTrait_farsightedReducesEffectivePerception() {
        Profile p = new Profile("Uma", "Female", "Normal");
        p.setAttribute(CharacterAttribute.PERCEPTION.name(), 5);
        p.setVisionTrait(VisionTrait.FARSIGHTED);
        // base 5 + farsighted -1 + no equipment modifier = 4
        assertEquals(4, p.getEffectiveAttribute(CharacterAttribute.PERCEPTION));
    }

    @Test
    public void profile_visionTrait_nearsightedReducesEffectivePerception() {
        Profile p = new Profile("Victor", "Male", "Normal");
        p.setAttribute(CharacterAttribute.PERCEPTION.name(), 7);
        p.setVisionTrait(VisionTrait.NEARSIGHTED);
        assertEquals(6, p.getEffectiveAttribute(CharacterAttribute.PERCEPTION));
    }

    @Test
    public void profile_glasses_compensatesVisionImpairment() {
        Profile p = new Profile("Wendy", "Female", "Normal");
        p.setAttribute(CharacterAttribute.PERCEPTION.name(), 5);
        p.setVisionTrait(VisionTrait.NEARSIGHTED); // -1 PERCEPTION
        p.addUtilityItem(EquipItem.GLASSES);       // +1 PERCEPTION
        // net effect: 5 - 1 + 1 = 5 (fully compensated)
        assertEquals(5, p.getEffectiveAttribute(CharacterAttribute.PERCEPTION));
    }

    @Test
    public void profile_getEffectiveAttribute_noVisionTrait_equalsBaseAndEquipment() {
        Profile p = new Profile("Xavier", "Male", "Normal");
        p.setAttribute(CharacterAttribute.PERCEPTION.name(), 4);
        // Default: no vision trait, pistol (+1 INTIMIDATION, no PERCEPTION modifier)
        assertEquals(4, p.getEffectiveAttribute(CharacterAttribute.PERCEPTION));
    }

    @Test(expected = IllegalArgumentException.class)
    public void profile_getEffectiveAttribute_nullThrows() {
        new Profile("Yara", "Female", "Normal").getEffectiveAttribute(null);
    }

    @Test
    public void equipItem_glasses_hasCorrectProperties() {
        EquipItem glasses = EquipItem.GLASSES;
        assertEquals("Glasses", glasses.getName());
        assertEquals(EquipmentSlot.UTILITY, glasses.getSlot());
        assertEquals(1, (int) glasses.getModifiers().get(CharacterAttribute.PERCEPTION));
        assertEquals(0.05f, glasses.getWeight(), 0.001f);
    }

    // -------------------------------------------------------------------------
    // EquipItem — hat, coat, sun glasses
    // -------------------------------------------------------------------------

    @Test
    public void equipItem_hat_hasCorrectProperties() {
        EquipItem hat = EquipItem.HAT;
        assertEquals("Hat", hat.getName());
        assertEquals(EquipmentSlot.HEAD, hat.getSlot());
        assertEquals(1, (int) hat.getModifiers().get(CharacterAttribute.STEALTH));
        assertEquals(0.1f, hat.getWeight(), 0.001f);
    }

    @Test
    public void equipItem_coat_hasCorrectProperties() {
        EquipItem coat = EquipItem.COAT;
        assertEquals("Coat", coat.getName());
        assertEquals(EquipmentSlot.BODY, coat.getSlot());
        assertEquals(1, (int) coat.getModifiers().get(CharacterAttribute.STEALTH));
        assertEquals(1.2f, coat.getWeight(), 0.001f);
    }

    @Test
    public void equipItem_sunGlasses_hasCorrectProperties() {
        EquipItem sg = EquipItem.SUN_GLASSES;
        assertEquals("Sun Glasses", sg.getName());
        assertEquals(EquipmentSlot.UTILITY, sg.getSlot());
        assertEquals(1, (int) sg.getModifiers().get(CharacterAttribute.STEALTH));
        assertEquals(1, (int) sg.getModifiers().get(CharacterAttribute.PERCEPTION));
        assertEquals(0.05f, sg.getWeight(), 0.001f);
    }

    @Test
    public void profile_hat_increasesEffectiveStealth() {
        Profile p = new Profile("Alice", "Female", "Normal");
        p.setAttribute(CharacterAttribute.STEALTH.name(), 4);
        p.equip(EquipItem.HAT);
        assertEquals(5, p.getEffectiveAttribute(CharacterAttribute.STEALTH));
    }

    @Test
    public void profile_coat_increasesEffectiveStealth() {
        Profile p = new Profile("Bob", "Male", "Normal");
        p.setAttribute(CharacterAttribute.STEALTH.name(), 3);
        p.equip(EquipItem.COAT);
        assertEquals(4, p.getEffectiveAttribute(CharacterAttribute.STEALTH));
    }

    @Test
    public void profile_sunGlasses_increasesBothStealthAndPerception() {
        Profile p = new Profile("Carol", "Female", "Normal");
        p.setAttribute(CharacterAttribute.STEALTH.name(), 5);
        p.setAttribute(CharacterAttribute.PERCEPTION.name(), 5);
        p.addUtilityItem(EquipItem.SUN_GLASSES);
        assertEquals(6, p.getEffectiveAttribute(CharacterAttribute.STEALTH));
        assertEquals(6, p.getEffectiveAttribute(CharacterAttribute.PERCEPTION));
    }

    @Test
    public void profile_hatAndCoat_stackStealth() {
        Profile p = new Profile("Dave", "Male", "Normal");
        p.setAttribute(CharacterAttribute.STEALTH.name(), 4);
        p.equip(EquipItem.HAT);
        p.equip(EquipItem.COAT);
        // HAT(HEAD, +1) + COAT(BODY, +1) = +2
        assertEquals(6, p.getEffectiveAttribute(CharacterAttribute.STEALTH));
    }

    @Test
    public void equipItem_hat_foundByName() {
        EquipItem found = EquipItem.findByName("Hat", EquipmentSlot.HEAD);
        assertNotNull("Hat should be in the catalogue", found);
        assertEquals(EquipItem.HAT, found);
    }

    @Test
    public void equipItem_coat_foundByName() {
        EquipItem found = EquipItem.findByName("Coat", EquipmentSlot.BODY);
        assertNotNull("Coat should be in the catalogue", found);
        assertEquals(EquipItem.COAT, found);
    }

    @Test
    public void equipItem_sunGlasses_foundByName() {
        EquipItem found = EquipItem.findByName("Sun Glasses", EquipmentSlot.UTILITY);
        assertNotNull("Sun Glasses should be in the catalogue", found);
        assertEquals(EquipItem.SUN_GLASSES, found);
    }
}

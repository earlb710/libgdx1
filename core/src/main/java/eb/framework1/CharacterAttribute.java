package eb.framework1;

public enum CharacterAttribute {
    // Mental Attributes
    INTELLIGENCE("Intelligence", "Mental", "Ability to solve puzzles, connect clues, and draw logical conclusions"),
    PERCEPTION("Perception", "Mental", "Noticing small details, finding hidden objects, reading crime scenes"),
    MEMORY("Memory", "Mental", "Remembering facts, witness statements, and case details"),
    INTUITION("Intuition", "Mental", "Making hunches, reading people, sensing when something's off"),
    
    // Physical Attributes
    AGILITY("Agility", "Physical", "Chasing suspects, sneaking, breaking and entering"),
    STAMINA("Stamina", "Physical", "Long stakeouts, extended investigations, chasing, running"),
    STRENGTH("Strength", "Physical", "Physical confrontations, moving obstacles, carrying stuff"),
    HEIGHT_CM("Height (cm)", "Physical", "Character's height in centimetres"),
    WEIGHT_KG("Weight (kg)", "Physical", "Total body weight in kg"),
    MUSCLE_KG("Muscle (kg)", "Physical", "Muscle mass in kg; each 10 kg adds +1 Strength"),
    FAT_KG("Fat (kg)",    "Physical", "Body fat in kg; each 10 kg subtracts 1 Strength"),
    
    // Social Attributes
    CHARISMA("Charisma", "Social", "Getting people to talk, gaining trust"),
    INTIMIDATION("Intimidation", "Social", "Pressuring suspects during interrogation"),
    EMPATHY("Empathy", "Social", "Understanding motives, connecting with victims/witnesses"),
    STEALTH("Stealth", "Social", "Going undercover, bluffing during interrogations");
    
    private final String displayName;
    private final String category;
    private final String description;
    
    CharacterAttribute(String displayName, String category, String description) {
        this.displayName = displayName;
        this.category = category;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getCategory() {
        return category;
    }
    
    public String getDescription() {
        return description;
    }
    
    public static CharacterAttribute[] getMentalAttributes() {
        return new CharacterAttribute[]{INTELLIGENCE, PERCEPTION, MEMORY, INTUITION};
    }
    
    public static CharacterAttribute[] getPhysicalAttributes() {
        return new CharacterAttribute[]{AGILITY, STAMINA, STRENGTH};
    }

    /** Returns {@code true} for attributes that represent physical body measurements
     *  (entered directly, not via the point-allocation system). */
    public boolean isBodyMeasurement() {
        return this == HEIGHT_CM || this == WEIGHT_KG || this == MUSCLE_KG || this == FAT_KG;
    }
    
    public static CharacterAttribute[] getSocialAttributes() {
        return new CharacterAttribute[]{CHARISMA, INTIMIDATION, EMPATHY, STEALTH};
    }
}

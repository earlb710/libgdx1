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
    
    public static CharacterAttribute[] getSocialAttributes() {
        return new CharacterAttribute[]{CHARISMA, INTIMIDATION, EMPATHY, STEALTH};
    }
}

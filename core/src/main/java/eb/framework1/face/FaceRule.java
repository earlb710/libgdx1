package eb.framework1.face;

import java.util.Collections;
import java.util.List;

/**
 * Immutable data model for a single face-rule entry from {@code facerules.json}.
 *
 * <p>A face rule specifies which SVG feature parts are eligible ({@link #include}),
 * additionally allowed on top of an existing eligible set ({@link #additional}),
 * or forbidden ({@link #exclude}) for a character that matches the rule's
 * conditions (gender, age, etc.).
 *
 * <p>Each entry in {@code include}, {@code additional}, and {@code exclude} is a
 * {@code "feature.id"} string, e.g. {@code "eye.female1"} or {@code "hair.bald"}.
 *
 * <p>Instances are produced by {@link FaceRuleLoader}.
 */
public final class FaceRule {

    /** Free-text label; not used during face generation. */
    public final String name;

    /**
     * Gender filter: {@code ""} (any), {@code "male"}, or {@code "female"}.
     * A rule with an empty gender matches every character.
     */
    public final String gender;

    /**
     * Emotion filter: {@code ""} (any), {@code "normal"}, {@code "happy"},
     * {@code "sad"}, {@code "anxious"}, or {@code "angry"}.
     * A rule with an empty emotion matches every emotion.
     */
    public final String emotion;

    /** Minimum wealth level (inclusive); 0 = no minimum. */
    public final int minWealth;

    /** Minimum age (inclusive); 0 = no minimum. */
    public final int minAge;

    /**
     * Clothes-type filter: {@code ""} (any), {@code "normal"}, {@code "work"},
     * {@code "sport"}, or {@code "gym"}.
     * A rule with an empty clothesType matches every clothes type.
     */
    public final String clothesType;

    /**
     * Probability (1–100) that this rule fires when its conditions are met.
     * 100 = always fires.
     */
    public final int percentage;

    /**
     * Sort key: rules are processed ascending by priority, so that lower-priority
     * rules are applied first and can be overwritten by higher-priority rules.
     */
    public final int priority;

    /**
     * SVG {@code "feature.id"} entries that define the <em>eligible</em> set for
     * each feature type named in this list.  When this rule fires, any previous
     * include list for the same feature type (from a lower-priority rule) is
     * <strong>replaced</strong> (include is "unique per type").
     */
    public final List<String> include;

    /**
     * SVG {@code "feature.id"} entries that are <em>added on top of</em> the
     * eligible set without replacing it.  Multiple rules may contribute additional
     * entries for the same feature type — they accumulate (additional is
     * "not unique per type").
     */
    public final List<String> additional;

    /**
     * SVG {@code "feature.id"} entries that are <em>forbidden</em> when this rule
     * fires.  Excluded entries are removed from the final eligible set even if
     * they were added by an include or additional list.
     */
    public final List<String> exclude;

    // -------------------------------------------------------------------------

    private FaceRule(Builder b) {
        this.name        = b.name        != null ? b.name        : "";
        this.gender      = b.gender      != null ? b.gender      : "";
        this.emotion     = b.emotion     != null ? b.emotion     : "";
        this.minWealth   = b.minWealth;
        this.minAge      = b.minAge;
        this.clothesType = b.clothesType != null ? b.clothesType : "";
        this.percentage  = Math.min(100, Math.max(1, b.percentage));
        this.priority    = Math.max(0, b.priority);
        this.include     = Collections.unmodifiableList(b.include);
        this.additional  = Collections.unmodifiableList(b.additional);
        this.exclude     = Collections.unmodifiableList(b.exclude);
    }

    // -------------------------------------------------------------------------
    // Builder
    // -------------------------------------------------------------------------

    /** Fluent builder for {@link FaceRule}. */
    public static final class Builder {
        private String       name        = "";
        private String       gender      = "";
        private String       emotion     = "";
        private int          minWealth   = 0;
        private int          minAge      = 0;
        private String       clothesType = "";
        private int          percentage  = 100;
        private int          priority    = 0;
        private List<String> include     = Collections.emptyList();
        private List<String> additional  = Collections.emptyList();
        private List<String> exclude     = Collections.emptyList();

        public Builder name(String v)            { this.name        = v; return this; }
        public Builder gender(String v)          { this.gender      = v; return this; }
        public Builder emotion(String v)         { this.emotion     = v; return this; }
        public Builder minWealth(int v)          { this.minWealth   = v; return this; }
        public Builder minAge(int v)             { this.minAge      = v; return this; }
        public Builder clothesType(String v)     { this.clothesType = v; return this; }
        public Builder percentage(int v)         { this.percentage  = v; return this; }
        public Builder priority(int v)           { this.priority    = v; return this; }
        public Builder include(List<String> v)   { this.include     = v != null ? v : Collections.emptyList(); return this; }
        public Builder additional(List<String> v){ this.additional  = v != null ? v : Collections.emptyList(); return this; }
        public Builder exclude(List<String> v)   { this.exclude     = v != null ? v : Collections.emptyList(); return this; }

        /** Builds and returns the immutable {@link FaceRule}. */
        public FaceRule build() { return new FaceRule(this); }
    }

    @Override
    public String toString() {
        return "FaceRule{name='" + name + "', priority=" + priority
                + ", gender='" + gender + "', minAge=" + minAge
                + ", include=" + include.size()
                + ", additional=" + additional.size()
                + ", exclude=" + exclude.size() + '}';
    }
}

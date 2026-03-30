package eb.framework1.face;

/**
 * Data model for a generated cartoon face, ported from the
 * <a href="https://github.com/zengm-games/facesjs">facesjs</a> JavaScript library.
 *
 * <p>A {@code FaceConfig} captures all feature selections needed to render a
 * vector-based cartoon face as an SVG image.  Instances are produced by
 * {@link FaceGenerator} and converted to SVG by {@link FaceSvgBuilder}.
 *
 * <p>Instances are created through {@link FaceConfig.Builder}.
 */
public final class FaceConfig {

    // -------------------------------------------------------------------------
    // Nested value types (mirror JS objects per feature)
    // -------------------------------------------------------------------------

    /** A feature that has only an {@code id} selector. */
    public static final class SimpleFeature {
        public final String id;
        public SimpleFeature(String id) { this.id = id != null ? id : "none"; }
        @Override public String toString() { return "SimpleFeature{id='" + id + "'}"; }
    }

    /** Body feature: id, skin-colour hex, and body size scale. */
    public static final class BodyFeature {
        public final String id;
        public final String color;
        public final double size;
        public BodyFeature(String id, String color, double size) {
            this.id    = id    != null ? id    : "body";
            this.color = color != null ? color : "#f2d6cb";
            this.size  = size;
        }
    }

    /** Ear feature: id and size scale. */
    public static final class EarFeature {
        public final String id;
        public final double size;
        public EarFeature(String id, double size) {
            this.id   = id != null ? id : "ear1";
            this.size = size;
        }
    }

    /** Head feature: id and optional shave colour (rgba string). */
    public static final class HeadFeature {
        public final String id;
        public final String shave;
        public HeadFeature(String id, String shave) {
            this.id    = id    != null ? id    : "head1";
            this.shave = shave != null ? shave : "rgba(0,0,0,0)";
        }
    }

    /** Smile-line feature: id and size scale. */
    public static final class SmileLineFeature {
        public final String id;
        public final double size;
        public SmileLineFeature(String id, double size) {
            this.id   = id != null ? id : "none";
            this.size = size;
        }
    }

    /** Eye feature: id and rotation angle in degrees. */
    public static final class EyeFeature {
        public final String id;
        public final int    angle;
        public EyeFeature(String id, int angle) {
            this.id    = id != null ? id : "eye1";
            this.angle = angle;
        }
    }

    /** Eyebrow feature: id and rotation angle in degrees. */
    public static final class EyebrowFeature {
        public final String id;
        public final int    angle;
        public EyebrowFeature(String id, int angle) {
            this.id    = id != null ? id : "eyebrow1";
            this.angle = angle;
        }
    }

    /** Hair feature: id, colour hex, and horizontal flip flag. */
    public static final class HairFeature {
        public final String  id;
        public final String  color;
        public final boolean flip;
        public HairFeature(String id, String color, boolean flip) {
            this.id    = id    != null ? id    : "short";
            this.color = color != null ? color : "#272421";
            this.flip  = flip;
        }
    }

    /** Mouth feature: id and horizontal flip flag. */
    public static final class MouthFeature {
        public final String  id;
        public final boolean flip;
        public MouthFeature(String id, boolean flip) {
            this.id   = id != null ? id : "mouth";
            this.flip = flip;
        }
    }

    /** Nose feature: id, horizontal flip flag, and size scale. */
    public static final class NoseFeature {
        public final String  id;
        public final boolean flip;
        public final double  size;
        public NoseFeature(String id, boolean flip, double size) {
            this.id   = id != null ? id : "nose1";
            this.flip = flip;
            this.size = size;
        }
    }

    // -------------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------------

    /** Body-width fatness scale: 0.0 (thin) → 1.0 (fat). */
    public final double fatness;

    /** Three team colours used to tint jersey and some accessories: [primary, secondary, accent]. */
    public final String[] teamColors;

    public final SimpleFeature    hairBg;
    public final BodyFeature      body;
    public final SimpleFeature    jersey;
    public final EarFeature       ear;
    public final HeadFeature      head;
    public final SimpleFeature    eyeLine;
    public final SmileLineFeature smileLine;
    public final SimpleFeature    miscLine;
    public final SimpleFeature    facialHair;
    public final EyeFeature       eye;
    public final EyebrowFeature   eyebrow;
    public final HairFeature      hair;
    public final MouthFeature     mouth;
    public final NoseFeature      nose;
    public final SimpleFeature    glasses;
    public final SimpleFeature    accessories;

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------

    private FaceConfig(Builder b) {
        this.fatness     = b.fatness;
        this.teamColors  = b.teamColors != null ? b.teamColors
                         : new String[]{"#89bfd3", "#7a1319", "#07364f"};
        this.hairBg      = b.hairBg;
        this.body        = b.body;
        this.jersey      = b.jersey;
        this.ear         = b.ear;
        this.head        = b.head;
        this.eyeLine     = b.eyeLine;
        this.smileLine   = b.smileLine;
        this.miscLine    = b.miscLine;
        this.facialHair  = b.facialHair;
        this.eye         = b.eye;
        this.eyebrow     = b.eyebrow;
        this.hair        = b.hair;
        this.mouth       = b.mouth;
        this.nose        = b.nose;
        this.glasses     = b.glasses;
        this.accessories = b.accessories;
    }

    /**
     * Returns a copy of this {@code FaceConfig} with the body skin colour
     * replaced by {@code newColor}.  All other fields are shared (safe because
     * they are immutable).
     *
     * @param newColor hex colour string, e.g. {@code "#F5D0C5"}
     * @return a new {@code FaceConfig} with the given skin colour
     */
    public FaceConfig withSkinColor(String newColor) {
        return new Builder()
                .fatness(this.fatness)
                .teamColors(this.teamColors)
                .hairBg(this.hairBg)
                .body(new BodyFeature(this.body.id, newColor, this.body.size))
                .jersey(this.jersey)
                .ear(this.ear)
                .head(this.head)
                .eyeLine(this.eyeLine)
                .smileLine(this.smileLine)
                .miscLine(this.miscLine)
                .facialHair(this.facialHair)
                .eye(this.eye)
                .eyebrow(this.eyebrow)
                .hair(this.hair)
                .mouth(this.mouth)
                .nose(this.nose)
                .glasses(this.glasses)
                .accessories(this.accessories)
                .build();
    }

    // -------------------------------------------------------------------------
    // Builder
    // -------------------------------------------------------------------------

    /** Fluent builder for {@link FaceConfig}. */
    public static final class Builder {
        private double fatness = 0.5;
        private String[] teamColors = {"#89bfd3", "#7a1319", "#07364f"};
        private SimpleFeature    hairBg     = new SimpleFeature("none");
        private BodyFeature      body       = new BodyFeature("body", "#f2d6cb", 1.0);
        private SimpleFeature    jersey     = new SimpleFeature("none");
        private EarFeature       ear        = new EarFeature("ear1", 1.0);
        private HeadFeature      head       = new HeadFeature("head1", "rgba(0,0,0,0)");
        private SimpleFeature    eyeLine    = new SimpleFeature("none");
        private SmileLineFeature smileLine  = new SmileLineFeature("none", 1.0);
        private SimpleFeature    miscLine   = new SimpleFeature("none");
        private SimpleFeature    facialHair = new SimpleFeature("none");
        private EyeFeature       eye        = new EyeFeature("eye1", 0);
        private EyebrowFeature   eyebrow    = new EyebrowFeature("eyebrow1", 0);
        private HairFeature      hair       = new HairFeature("short", "#272421", false);
        private MouthFeature     mouth      = new MouthFeature("mouth", false);
        private NoseFeature      nose       = new NoseFeature("nose1", false, 1.0);
        private SimpleFeature    glasses    = new SimpleFeature("none");
        private SimpleFeature    accessories = new SimpleFeature("none");

        public Builder fatness(double v)              { fatness = v; return this; }
        public Builder teamColors(String[] v)         { teamColors = v; return this; }
        public Builder hairBg(SimpleFeature v)        { hairBg = v; return this; }
        public Builder body(BodyFeature v)            { body = v; return this; }
        public Builder jersey(SimpleFeature v)        { jersey = v; return this; }
        public Builder ear(EarFeature v)              { ear = v; return this; }
        public Builder head(HeadFeature v)            { head = v; return this; }
        public Builder eyeLine(SimpleFeature v)       { eyeLine = v; return this; }
        public Builder smileLine(SmileLineFeature v)  { smileLine = v; return this; }
        public Builder miscLine(SimpleFeature v)      { miscLine = v; return this; }
        public Builder facialHair(SimpleFeature v)    { facialHair = v; return this; }
        public Builder eye(EyeFeature v)              { eye = v; return this; }
        public Builder eyebrow(EyebrowFeature v)      { eyebrow = v; return this; }
        public Builder hair(HairFeature v)            { hair = v; return this; }
        public Builder mouth(MouthFeature v)          { mouth = v; return this; }
        public Builder nose(NoseFeature v)            { nose = v; return this; }
        public Builder glasses(SimpleFeature v)       { glasses = v; return this; }
        public Builder accessories(SimpleFeature v)   { accessories = v; return this; }

        /** Builds and returns the immutable {@link FaceConfig}. */
        public FaceConfig build() { return new FaceConfig(this); }
    }

    @Override
    public String toString() {
        return "FaceConfig{fatness=" + fatness
                + ", body.id='" + body.id + '\''
                + ", hair.id='" + hair.id + '\''
                + ", eye.id='" + eye.id + '\''
                + '}';
    }
}

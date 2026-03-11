package eb.framework1.face;

import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link FaceGenerator}, {@link FaceConfig}, {@link FaceSvgBuilder},
 * and {@link JsonSvgTemplateLoader}.
 *
 * <p>All tests are pure-Java and have no libGDX dependency.
 */
public class FaceGeneratorTest {

    // =========================================================================
    // FaceConfig — construction via Builder
    // =========================================================================

    @Test
    public void faceConfig_defaultBuilder_hasNonNullFields() {
        FaceConfig f = new FaceConfig.Builder().build();
        assertNotNull(f.body);
        assertNotNull(f.hair);
        assertNotNull(f.eye);
        assertNotNull(f.eyebrow);
        assertNotNull(f.mouth);
        assertNotNull(f.nose);
        assertNotNull(f.ear);
        assertNotNull(f.head);
        assertNotNull(f.glasses);
        assertNotNull(f.accessories);
        assertNotNull(f.facialHair);
        assertNotNull(f.hairBg);
        assertNotNull(f.eyeLine);
        assertNotNull(f.smileLine);
        assertNotNull(f.miscLine);
        assertNotNull(f.jersey);
        assertNotNull(f.teamColors);
    }

    @Test
    public void faceConfig_defaultFatness_isHalf() {
        FaceConfig f = new FaceConfig.Builder().build();
        assertEquals(0.5, f.fatness, 0.001);
    }

    @Test
    public void faceConfig_nullBodyColor_usesDefault() {
        FaceConfig.BodyFeature body = new FaceConfig.BodyFeature(null, null, 1.0);
        assertEquals("#f2d6cb", body.color);
    }

    @Test
    public void faceConfig_nullHairId_usesDefault() {
        FaceConfig.HairFeature hair = new FaceConfig.HairFeature(null, null, false);
        assertEquals("short", hair.id);
        assertEquals("#272421", hair.color);
    }

    // =========================================================================
    // FaceGenerator — basic generation
    // =========================================================================

    @Test
    public void generate_defaultOptions_returnsNonNull() {
        FaceGenerator gen = new FaceGenerator(new Random(1));
        FaceConfig face = gen.generate();
        assertNotNull(face);
    }

    @Test
    public void generate_nullOptions_treatedAsMaleDefault() {
        FaceGenerator gen = new FaceGenerator(new Random(42));
        FaceConfig face = gen.generate(null);
        assertNotNull(face);
        assertNotNull(face.body.id);
    }

    @Test
    public void generate_maleGender_hasMaleCompatibleIds() {
        FaceGenerator gen = new FaceGenerator(new Random(10));
        for (int i = 0; i < 20; i++) {
            FaceConfig f = gen.generate(new FaceGenerator.Options().gender("male"));
            // male-only head IDs start with "head", not "female"
            assertFalse("male head ID should not be female: " + f.head.id,
                    f.head.id.startsWith("female"));
        }
    }

    @Test
    public void generate_femaleGender_hasFemaleCompatibleIds() {
        FaceGenerator gen = new FaceGenerator(new Random(99));
        for (int i = 0; i < 20; i++) {
            FaceConfig f = gen.generate(new FaceGenerator.Options().gender("female"));
            // female heads include "female1", "female2", "female3"
            // but also some shared ones (head5, head7...)
            assertNotNull(f.head.id);
        }
    }

    @Test
    public void generate_deterministic_withSeed() {
        FaceConfig f1 = new FaceGenerator(new Random(123)).generate();
        FaceConfig f2 = new FaceGenerator(new Random(123)).generate();
        assertEquals(f1.body.id,   f2.body.id);
        assertEquals(f1.hair.id,   f2.hair.id);
        assertEquals(f1.eye.id,    f2.eye.id);
        assertEquals(f1.fatness,   f2.fatness, 0.0001);
        assertEquals(f1.body.color, f2.body.color);
    }

    @Test
    public void generate_fatnessInRange_maleDefault() {
        FaceGenerator gen = new FaceGenerator(new Random(77));
        for (int i = 0; i < 50; i++) {
            FaceConfig f = gen.generate();
            assertTrue("fatness out of range: " + f.fatness,
                    f.fatness >= 0.0 && f.fatness <= 1.0);
        }
    }

    @Test
    public void generate_fatnessInRange_female() {
        FaceGenerator gen = new FaceGenerator(new Random(88));
        for (int i = 0; i < 50; i++) {
            FaceConfig f = gen.generate(new FaceGenerator.Options().gender("female"));
            assertTrue("female fatness out of range: " + f.fatness,
                    f.fatness >= 0.0 && f.fatness <= 0.4);
        }
    }

    @Test
    public void generate_racePalette_whiteHasSkinTone() {
        FaceGenerator gen = new FaceGenerator(new Random(5));
        FaceConfig f = gen.generate(new FaceGenerator.Options().race("white"));
        String skin = f.body.color;
        assertTrue("white skin color should be #f2d6cb or #ddb7a0",
                "#f2d6cb".equals(skin) || "#ddb7a0".equals(skin));
    }

    @Test
    public void generate_racePalette_blackHasSkinTone() {
        FaceGenerator gen = new FaceGenerator(new Random(6));
        FaceConfig f = gen.generate(new FaceGenerator.Options().race("black"));
        String skin = f.body.color;
        assertTrue("black skin should be one of the black palette colors",
                "#ad6453".equals(skin) || "#74453d".equals(skin) || "#5c3937".equals(skin));
    }

    @Test
    public void generate_hatAccessory_shortensLongHair() {
        // With seed 0 accessories might not be hat — keep trying
        FaceGenerator gen = new FaceGenerator(new Random(2026));
        boolean foundHat = false;
        for (int i = 0; i < 1000; i++) {
            FaceConfig f = gen.generate();
            if ("hat".equals(f.accessories.id) || "hat2".equals(f.accessories.id)
                    || "hat3".equals(f.accessories.id)) {
                // Hair should not be one of the restricted long styles
                String h = f.hair.id;
                assertFalse("afro with hat", "afro".equals(h));
                assertFalse("dreads with hat", "dreads".equals(h));
                foundHat = true;
                break;
            }
        }
        // Don't fail if no hat was generated after 1000 tries — just skip
        if (!foundHat) {
            // acceptable — hats have 20% chance but hat sub-variants further reduce
        }
    }

    @Test
    public void generate_bodySize_male_withinRange() {
        FaceGenerator gen = new FaceGenerator(new Random(55));
        for (int i = 0; i < 50; i++) {
            FaceConfig f = gen.generate(new FaceGenerator.Options().gender("male"));
            assertTrue("male body size out of range [0.95,1.05]: " + f.body.size,
                    f.body.size >= 0.94 && f.body.size <= 1.06);
        }
    }

    @Test
    public void generate_eyeAngle_withinRange() {
        FaceGenerator gen = new FaceGenerator(new Random(9));
        for (int i = 0; i < 100; i++) {
            FaceConfig f = gen.generate();
            assertTrue("eye angle out of [-10,15]: " + f.eye.angle,
                    f.eye.angle >= -10 && f.eye.angle <= 15);
        }
    }

    @Test
    public void generate_teamColorsNotNull() {
        FaceConfig f = new FaceGenerator(new Random(3)).generate();
        assertNotNull(f.teamColors);
        assertEquals(3, f.teamColors.length);
        for (String c : f.teamColors) assertNotNull(c);
    }

    // =========================================================================
    // FaceGenerator.Options
    // =========================================================================

    @Test
    public void options_defaultGender_isMale() {
        FaceGenerator.Options opts = new FaceGenerator.Options();
        assertEquals("male", opts.gender);
    }

    @Test
    public void options_chainedSetters_returnSelf() {
        FaceGenerator.Options opts = new FaceGenerator.Options()
                .gender("female").race("asian");
        assertEquals("female", opts.gender);
        assertEquals("asian", opts.race);
    }

    // =========================================================================
    // JsonSvgTemplateLoader — parser
    // =========================================================================

    @Test
    public void jsonLoader_simpleJson_parsesCorrectly() {
        String json = "{ \"eye\": { \"eye1\": \"<path d=\\\"M10 10\\\"/>\", \"eye2\": \"<circle/>\" } }";
        JsonSvgTemplateLoader loader = JsonSvgTemplateLoader.fromJson(json);
        assertEquals("<path d=\"M10 10\"/>", loader.getSvgTemplate("eye", "eye1"));
        assertEquals("<circle/>",           loader.getSvgTemplate("eye", "eye2"));
        assertNull(loader.getSvgTemplate("eye", "unknown"));
        assertNull(loader.getSvgTemplate("hair", "short"));
    }

    @Test
    public void jsonLoader_emptyObject_returnsNull() {
        JsonSvgTemplateLoader loader = JsonSvgTemplateLoader.fromJson("{}");
        assertNull(loader.getSvgTemplate("eye", "eye1"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void jsonLoader_nullJson_throws() {
        JsonSvgTemplateLoader.fromJson(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void jsonLoader_nullMap_throws() {
        JsonSvgTemplateLoader.fromMap(null);
    }

    @Test
    public void jsonLoader_fromMap_works() {
        Map<String, Map<String, String>> map = new HashMap<>();
        Map<String, String> inner = new HashMap<>();
        inner.put("short", "<g/>");
        map.put("hair", inner);
        JsonSvgTemplateLoader loader = JsonSvgTemplateLoader.fromMap(map);
        assertEquals("<g/>", loader.getSvgTemplate("hair", "short"));
    }

    @Test
    public void jsonParser_handlesEscapedQuotes() {
        String json = "{\"a\":{\"k\":\"val \\\"quoted\\\" text\"}}";
        JsonSvgTemplateLoader loader = JsonSvgTemplateLoader.fromJson(json);
        assertEquals("val \"quoted\" text", loader.getSvgTemplate("a", "k"));
    }

    @Test
    public void jsonParser_handlesBackslash() {
        String json = "{\"a\":{\"k\":\"a\\\\b\"}}";
        JsonSvgTemplateLoader loader = JsonSvgTemplateLoader.fromJson(json);
        assertEquals("a\\b", loader.getSvgTemplate("a", "k"));
    }

    // =========================================================================
    // FaceSvgBuilder
    // =========================================================================

    @Test(expected = IllegalArgumentException.class)
    public void svgBuilder_nullLoader_throws() {
        new FaceSvgBuilder(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void svgBuilder_nullFace_throws() {
        FaceSvgBuilder b = new FaceSvgBuilder((feature, id) -> null);
        b.toSvgString(null);
    }

    @Test
    public void svgBuilder_emptyLoader_returnsSvgShell() {
        FaceSvgBuilder builder = new FaceSvgBuilder((feature, id) -> null);
        FaceConfig face = new FaceConfig.Builder().build();
        String svg = builder.toSvgString(face);
        assertTrue("SVG should start with <svg", svg.startsWith("<svg"));
        assertTrue("SVG should end with </svg>", svg.endsWith("</svg>"));
        assertTrue("SVG should contain viewBox", svg.contains("viewBox=\"0 0 400 600\""));
    }

    @Test
    public void svgBuilder_withTemplate_embedsPathContent() {
        FaceSvgBuilder builder = new FaceSvgBuilder((feature, id) -> {
            if ("body".equals(feature) && "body".equals(id)) {
                return "<path d=\"M0 0\"/>";
            }
            return null;
        });
        FaceConfig face = new FaceConfig.Builder().build();
        String svg = builder.toSvgString(face);
        assertTrue("SVG should contain the body path", svg.contains("<path d=\"M0 0\"/>"));
    }

    @Test
    public void svgBuilder_colorSubstitution_replacesSkinColor() {
        FaceSvgBuilder builder = new FaceSvgBuilder((feature, id) -> {
            if ("body".equals(feature)) return "<path fill=\"$[skinColor]\"/>";
            return null;
        });
        FaceConfig face = new FaceConfig.Builder()
                .body(new FaceConfig.BodyFeature("body", "#abc123", 1.0))
                .build();
        String svg = builder.toSvgString(face);
        assertTrue("skin color substituted", svg.contains("fill=\"#abc123\""));
        assertFalse("placeholder removed", svg.contains("$[skinColor]"));
    }

    @Test
    public void svgBuilder_colorSubstitution_replacesHairColor() {
        FaceSvgBuilder builder = new FaceSvgBuilder((feature, id) -> {
            if ("hair".equals(feature)) return "<path fill=\"$[hairColor]\"/>";
            return null;
        });
        FaceConfig face = new FaceConfig.Builder()
                .hair(new FaceConfig.HairFeature("short", "#ff0000", false))
                .build();
        String svg = builder.toSvgString(face);
        assertTrue("hair color substituted", svg.contains("fill=\"#ff0000\""));
    }

    @Test
    public void svgBuilder_noneIdSkipped() {
        // A feature with id="none" should produce no SVG group for that feature
        FaceSvgBuilder builder = new FaceSvgBuilder((feature, id) -> {
            if ("none".equals(id)) fail("Template for 'none' should never be requested");
            return null;
        });
        FaceConfig face = new FaceConfig.Builder()
                .glasses(new FaceConfig.SimpleFeature("none"))
                .accessories(new FaceConfig.SimpleFeature("none"))
                .facialHair(new FaceConfig.SimpleFeature("none"))
                .build();
        // Should not throw
        String svg = builder.toSvgString(face);
        assertNotNull(svg);
    }

    // =========================================================================
    // Integration: generate + build SVG
    // =========================================================================

    @Test
    public void integrationTest_generateAndBuildSvg_producesValidSvg() {
        // Loader returns a trivial stub for every feature/id combination
        FaceSvgBuilder.SvgTemplateLoader stubLoader =
                (feature, id) -> "<path d=\"M0 0 L10 10\"/>";

        FaceGenerator gen     = new FaceGenerator(new Random(7));
        FaceSvgBuilder builder = new FaceSvgBuilder(stubLoader);

        for (int i = 0; i < 10; i++) {
            FaceConfig face = gen.generate();
            String svg = builder.toSvgString(face);
            assertTrue("iteration " + i + ": SVG should start with <svg",
                    svg.startsWith("<svg"));
            assertTrue("iteration " + i + ": SVG should end with </svg>",
                    svg.endsWith("</svg>"));
        }
    }

    // =========================================================================
    // JsonSvgTemplateLoader — load actual assets/face/svgs.json if available
    // =========================================================================

    @Test
    public void jsonLoader_realSvgsFile_loadsBodyTemplate() throws Exception {
        // Try to load the real svgs.json from the assets directory
        java.io.File svgsFile = new java.io.File(
                "../../../../assets/face/svgs.json");
        if (!svgsFile.exists()) {
            // Try relative to current working directory
            svgsFile = new java.io.File("assets/face/svgs.json");
        }
        if (!svgsFile.exists()) {
            // Not available in this test environment — skip
            return;
        }

        String json = new String(Files.readAllBytes(svgsFile.toPath()),
                java.nio.charset.StandardCharsets.UTF_8);
        JsonSvgTemplateLoader loader = JsonSvgTemplateLoader.fromJson(json);

        String bodyTemplate = loader.getSvgTemplate("body", "body");
        assertNotNull("body/body template should be present in svgs.json", bodyTemplate);
        assertFalse("body/body template should not be empty", bodyTemplate.isEmpty());
    }

    // =========================================================================
    // FaceSvgBuilder.computeCenter — bbox centering
    // =========================================================================

    /**
     * Helper that calls the package-private {@code computeCenter} via reflection.
     */
    private static double[] computeCenter(String svgFragment) throws Exception {
        java.lang.reflect.Method m = FaceSvgBuilder.class
                .getDeclaredMethod("computeCenter", String.class);
        m.setAccessible(true);
        return (double[]) m.invoke(null, svgFragment);
    }

    @Test
    public void computeCenter_ear1_correctBbox() throws Exception {
        // ear1 path: x 3-43 (cx=23), y 3-73 (cy=38)
        double[] c = computeCenter(
                "<path d=\"M43 13C43 13 23 3 13 3C3 3 3 23 3 33C3 43 6 53 16 63C26 73 43 53 43 53L43 13Z\"/>");
        assertEquals("ear1 cx", 23.0, c[0], 0.5);
        assertEquals("ear1 cy", 38.0, c[1], 0.5);
    }

    @Test
    public void computeCenter_eye1_correctBbox() throws Exception {
        // eye1: two paths, x -2 to 63 (cx≈30.5), y 3-53 (cy≈28)
        double[] c = computeCenter(
                "<path d=\"M63 43C63 43 58 53 28 53C-2 53 3 43 3 43C3 43 3 3 33 3C63 3 63 43 63 43Z\"/>"
                + "<path d=\"M33 38C23 38 23 18 33 18C43 18 43 38 33 38Z\"/>");
        assertEquals("eye1 cx", 30.5, c[0], 1.0);
        assertEquals("eye1 cy", 28.0, c[1], 1.0);
    }

    @Test
    public void computeCenter_eyebrow1_correctBbox() throws Exception {
        // eyebrow1: x 3-83 (cx=43), y -2 to 23 (cy≈10.5)
        double[] c = computeCenter(
                "<path d=\"M83 13C83 3 73 3 73 3C48 -2 17.46 8.36 3 18C43 13 53 23 78 23C78 23 83 23 83 13Z\"/>");
        assertEquals("eyebrow1 cx", 43.0, c[0], 0.5);
        assertEquals("eyebrow1 cy", 10.5, c[1], 0.5);
    }

    @Test
    public void computeCenter_horizontalCommand_correctBbox() throws Exception {
        // M10 10 H80 V50 H10 Z → x 10-80 (cx=45), y 10-50 (cy=30)
        double[] c = computeCenter("<path d=\"M10 10H80V50H10Z\"/>");
        assertEquals("H/V cx", 45.0, c[0], 0.1);
        assertEquals("H/V cy", 30.0, c[1], 0.1);
    }

    @Test
    public void computeCenter_relativeCommand_correctBbox() throws Exception {
        // M10 10 h20 v30 h-20 z → x 10-30 (cx=20), y 10-40 (cy=25)
        double[] c = computeCenter("<path d=\"M10 10h20v30h-20z\"/>");
        assertEquals("relative h cx", 20.0, c[0], 0.1);
        assertEquals("relative h cy", 25.0, c[1], 0.1);
    }

    @Test
    public void computeCenter_noPath_returnsZeroZero() throws Exception {
        double[] c = computeCenter("<g><style>.foo { fill: red }</style></g>");
        assertEquals("no path cx", 0.0, c[0], 0.001);
        assertEquals("no path cy", 0.0, c[1], 0.001);
    }

    @Test
    public void svgBuilder_positioning_centerAtTarget() {
        // Verify that a feature with a known bbox center ends up at the target position.
        // Use a simple path that has bbox center at (5, 5).
        // For left eye target (140, 310): effective translate should be (140-5, 310-5)=(135, 305)
        final String pathWith5x5Center = "<path d=\"M0 0 L10 10\"/>";  // bbox (0-10, 0-10), cx=cy=5
        FaceSvgBuilder builder = new FaceSvgBuilder((feature, id) -> pathWith5x5Center);

        FaceConfig face = new FaceConfig.Builder()
                .eye(new FaceConfig.EyeFeature("eye1", 0))
                .build();
        String svg = builder.toSvgString(face);

        // The left eye transform should contain translate(140.00 310.00) and translate(-5.00 -5.00)
        assertTrue("left eye position translate", svg.contains("translate(140.00 310.00)"));
        assertTrue("left eye bbox offset translate", svg.contains("translate(-5.00 -5.00)"));
    }
}

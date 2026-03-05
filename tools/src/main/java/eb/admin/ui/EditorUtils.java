package eb.admin.ui;

import java.io.File;

/**
 * Shared constants and utilities for the Game Admin editor panels.
 */
final class EditorUtils {

    /** ISO 639-1 language codes shown in the Language dropdown. */
    static final String[] LANGUAGES = {
        "af", "ar", "az", "be", "bg", "bn", "bs", "ca", "cs", "cy",
        "da", "de", "el", "en", "eo", "es", "et", "eu", "fa", "fi",
        "fr", "ga", "gl", "gu", "he", "hi", "hr", "hu", "hy", "id",
        "is", "it", "ja", "ka", "kk", "km", "kn", "ko", "lt", "lv",
        "mk", "ml", "mn", "mr", "ms", "mt", "my", "nl", "no", "pa",
        "pl", "pt", "ro", "ru", "sk", "sl", "sq", "sr", "sv", "sw",
        "ta", "te", "th", "tl", "tr", "uk", "ur", "uz", "vi", "zh"
    };

    private EditorUtils() {}

    /**
     * Derives the sibling file for {@code newLang} by replacing the language
     * suffix in {@code base}'s name (e.g. {@code buildings_en.json} with
     * {@code newLang="fr"} → {@code buildings_fr.json}).
     *
     * @return the derived file, or {@code null} if the pattern is not found
     */
    static File deriveFileForLanguage(File base, String newLang) {
        if (base == null) return null;
        String name = base.getName();
        int dot = name.lastIndexOf('.');
        if (dot < 0) return null;
        String stem = name.substring(0, dot);       // e.g. "buildings_en"
        int under = stem.lastIndexOf('_');
        if (under < 0) return null;
        String prefix = stem.substring(0, under);   // e.g. "buildings"
        return new File(base.getParentFile(), prefix + "_" + newLang + ".json");
    }
}

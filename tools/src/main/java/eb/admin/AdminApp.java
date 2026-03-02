package eb.admin;

import eb.admin.ui.CategoryEditorScreen;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Entry point for the Game Admin / Mod tool.
 *
 * Launch with:
 *   cd tools && ./gradlew run
 *
 * The first screen ({@link CategoryEditorScreen}) lets you open, edit, and save
 * the game's category JSON file (assets/text/category_en.json).
 */
public class AdminApp {

    public static void main(String[] args) {
        // Use the system look-and-feel so the window feels native
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // Fallback to default L&F
        }

        SwingUtilities.invokeLater(() -> {
            CategoryEditorScreen screen = new CategoryEditorScreen();
            screen.setVisible(true);
        });
    }
}

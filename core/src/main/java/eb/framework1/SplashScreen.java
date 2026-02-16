package eb.framework1;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.ScreenUtils;

public class SplashScreen implements Screen {
    private Main game;
    private SpriteBatch batch;
    private BitmapFont titleFont;
    private BitmapFont buttonFont;
    private ShapeRenderer shapeRenderer;
    
    // Button dimensions and positions
    private Rectangle playButton;
    private Rectangle quitButton;
    private static final int BUTTON_WIDTH = 200;
    private static final int BUTTON_HEIGHT = 60;
    
    // Colors
    private Color buttonColor = new Color(0.3f, 0.3f, 0.4f, 1f);
    private Color buttonHoverColor = new Color(0.4f, 0.4f, 0.5f, 1f);
    private Color buttonTextColor = Color.WHITE;
    
    public SplashScreen(Main game) {
        this.game = game;
    }
    
    @Override
    public void show() {
        this.batch = new SpriteBatch();
        this.shapeRenderer = new ShapeRenderer();
        
        // Create fonts
        this.titleFont = new BitmapFont();
        this.titleFont.setColor(Color.GOLD);
        this.titleFont.getData().setScale(3.0f);
        
        this.buttonFont = new BitmapFont();
        this.buttonFont.setColor(buttonTextColor);
        this.buttonFont.getData().setScale(2.0f);
        
        // Initialize button positions (will be properly set in resize)
        int centerX = Gdx.graphics.getWidth() / 2;
        int centerY = Gdx.graphics.getHeight() / 2;
        
        playButton = new Rectangle(
            centerX - BUTTON_WIDTH / 2,
            centerY - 20,
            BUTTON_WIDTH,
            BUTTON_HEIGHT
        );
        
        quitButton = new Rectangle(
            centerX - BUTTON_WIDTH / 2,
            centerY - 100,
            BUTTON_WIDTH,
            BUTTON_HEIGHT
        );
    }
    
    @Override
    public void render(float delta) {
        // Handle input
        handleInput();
        
        // Clear screen with dark background
        ScreenUtils.clear(0.1f, 0.1f, 0.15f, 1f);
        
        // Draw title
        batch.begin();
        
        // Title: "Veritas Detegere"
        String title = "Veritas Detegere";
        float titleX = Gdx.graphics.getWidth() / 2 - 200;
        float titleY = Gdx.graphics.getHeight() / 2 + 150;
        titleFont.draw(batch, title, titleX, titleY);
        
        // Subtitle: "A Detective Game"
        titleFont.getData().setScale(1.5f);
        titleFont.setColor(Color.LIGHT_GRAY);
        String subtitle = "A Detective Game";
        float subtitleX = Gdx.graphics.getWidth() / 2 - 100;
        float subtitleY = Gdx.graphics.getHeight() / 2 + 100;
        titleFont.draw(batch, subtitle, subtitleX, subtitleY);
        titleFont.getData().setScale(3.0f);
        titleFont.setColor(Color.GOLD);
        
        batch.end();
        
        // Draw buttons
        drawButtons();
    }
    
    private void drawButtons() {
        int mouseX = Gdx.input.getX();
        int mouseY = Gdx.graphics.getHeight() - Gdx.input.getY(); // Flip Y coordinate
        
        // Draw Play button
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        if (playButton.contains(mouseX, mouseY)) {
            shapeRenderer.setColor(buttonHoverColor);
        } else {
            shapeRenderer.setColor(buttonColor);
        }
        shapeRenderer.rect(playButton.x, playButton.y, playButton.width, playButton.height);
        shapeRenderer.end();
        
        // Draw Play button border
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.WHITE);
        shapeRenderer.rect(playButton.x, playButton.y, playButton.width, playButton.height);
        shapeRenderer.end();
        
        // Draw Quit button
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        if (quitButton.contains(mouseX, mouseY)) {
            shapeRenderer.setColor(buttonHoverColor);
        } else {
            shapeRenderer.setColor(buttonColor);
        }
        shapeRenderer.rect(quitButton.x, quitButton.y, quitButton.width, quitButton.height);
        shapeRenderer.end();
        
        // Draw Quit button border
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.WHITE);
        shapeRenderer.rect(quitButton.x, quitButton.y, quitButton.width, quitButton.height);
        shapeRenderer.end();
        
        // Draw button text
        batch.begin();
        
        // Play button text
        String playText = "PLAY";
        float playTextX = playButton.x + BUTTON_WIDTH / 2 - 35;
        float playTextY = playButton.y + BUTTON_HEIGHT / 2 + 10;
        buttonFont.draw(batch, playText, playTextX, playTextY);
        
        // Quit button text
        String quitText = "QUIT";
        float quitTextX = quitButton.x + BUTTON_WIDTH / 2 - 35;
        float quitTextY = quitButton.y + BUTTON_HEIGHT / 2 + 10;
        buttonFont.draw(batch, quitText, quitTextX, quitTextY);
        
        batch.end();
    }
    
    private void handleInput() {
        if (Gdx.input.justTouched()) {
            int mouseX = Gdx.input.getX();
            int mouseY = Gdx.graphics.getHeight() - Gdx.input.getY(); // Flip Y coordinate
            
            if (playButton.contains(mouseX, mouseY)) {
                // Start the game
                game.setScreen(new MainScreen(game));
            } else if (quitButton.contains(mouseX, mouseY)) {
                // Quit the application
                Gdx.app.exit();
            }
        }
    }
    
    @Override
    public void resize(int width, int height) {
        // Recalculate button positions when window is resized
        int centerX = width / 2;
        int centerY = height / 2;
        
        playButton.setPosition(centerX - BUTTON_WIDTH / 2, centerY - 20);
        quitButton.setPosition(centerX - BUTTON_WIDTH / 2, centerY - 100);
    }
    
    @Override
    public void pause() {
    }
    
    @Override
    public void resume() {
    }
    
    @Override
    public void hide() {
    }
    
    @Override
    public void dispose() {
        if (batch != null) {
            batch.dispose();
        }
        if (titleFont != null) {
            titleFont.dispose();
        }
        if (buttonFont != null) {
            buttonFont.dispose();
        }
        if (shapeRenderer != null) {
            shapeRenderer.dispose();
        }
    }
}

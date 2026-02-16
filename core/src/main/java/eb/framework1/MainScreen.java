package eb.framework1;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;

public class MainScreen implements Screen {
    private Main game;
    private SpriteBatch batch;
    private Texture image;
    private boolean initialized = false;
    
    public MainScreen(Main game) {
        this.game = game;
    }
    
    @Override
    public void show() {
        this.batch = new SpriteBatch();
        this.image = new Texture("libgdx.png");
        initialized = true;
    }
    
    @Override
    public void render(float delta) {
        // Skip rendering if not initialized yet
        if (!initialized) {
            ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f);
            return;
        }
        
        ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f);
        batch.begin();
        batch.draw(image, 140, 210);
        batch.end();
    }
    
    @Override
    public void resize(int width, int height) {
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
        if (image != null) {
            image.dispose();
        }
    }
}

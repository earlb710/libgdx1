package eb.framework1;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;

public class MainScreen implements Screen {
    private Main game;
    private SpriteBatch batch;
    private Texture image;
    
    public MainScreen(Main game) {
        this.game = game;
        this.batch = new SpriteBatch();
        this.image = new Texture("libgdx.png");
    }
    
    @Override
    public void show() {
    }
    
    @Override
    public void render(float delta) {
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
        batch.dispose();
        image.dispose();
    }
}

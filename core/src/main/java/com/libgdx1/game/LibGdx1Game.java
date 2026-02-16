package com.libgdx1.game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class LibGdx1Game extends ApplicationAdapter {
	SpriteBatch batch;
	
	@Override
	public void create () {
		batch = new SpriteBatch();
		// Set the clear color to blue
		Gdx.gl.glClearColor(0.2f, 0.3f, 0.8f, 1);
	}

	@Override
	public void render () {
		// Clear the screen with the blue color
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		
		batch.begin();
		// Game rendering logic goes here
		batch.end();
	}
	
	@Override
	public void dispose () {
		batch.dispose();
	}
}

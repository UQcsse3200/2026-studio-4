package com.csse3200.game.screens;

import com.badlogic.gdx.ScreenAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.csse3200.game.GdxGame;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.factories.RenderFactory;
import com.csse3200.game.input.InputService;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.rendering.Renderer;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;

public class SplashScreen extends ScreenAdapter{
    private static final Logger logger = LoggerFactory.getLogger(SplashScreen.class);

    private final GdxGame game;
    private final Renderer renderer;
    
    public SplashScreen(GdxGame game) {
        this.game = game;

        logger.debug("Initialising splash screen services");
        ServiceLocator.registerInputService(new InputService());
        ServiceLocator.registerResourceService(new ResourceService());
        ServiceLocator.registerEntityService(new EntityService());
        ServiceLocator.registerRenderService(new RenderService());

        renderer = RenderFactory.createRenderer();
    }

    @Override 
    public void render(float delta) {
        
    }

    @Override
    public void resize(int width, int height) {

    }

    @Override
    public void dispose() {

    }
}

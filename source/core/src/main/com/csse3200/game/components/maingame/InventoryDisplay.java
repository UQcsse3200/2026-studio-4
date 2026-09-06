package com.csse3200.game.components.maingame;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.csse3200.game.ui.UIComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;

/** Displays a button to exit the Main Game screen to the Main Menu screen. */
public class InventoryDisplay extends UIComponent {
    private static final Logger logger = LoggerFactory.getLogger(MainGameExitDisplay.class);
    private static final float Z_INDEX = 2f;
    private Table table;
    @Override
    public void create() {
        super.create();
        addActors();
    }

    private void addActors() {
        table = new Table();
        table.top().right();
        table.setFillParent(true);
        TextButton mainMenuBtn = new TextButton("INVENTORY TEST", skin);
        table.add(mainMenuBtn).padTop(100f).padRight(100f);
        stage.addActor(table);
        table.setVisible(false);
    }

    @Override
    public void draw(SpriteBatch batch) {
        // draw is handled by the stage
    }

    @Override
    public float getZIndex() {
        return Z_INDEX;
    }

    @Override
    public void dispose() {
        table.clear();
        super.dispose();
    }

    public void setVisible(boolean set){
        table.setVisible(set);
    }
}

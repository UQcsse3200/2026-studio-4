package com.csse3200.game.components.maingame;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.csse3200.game.ui.UIComponent;
import java.awt.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    table.setFillParent(true);
    stage.addActor(table);

    Table gridTable = new Table();
    int columns = 5;
    int totalSlots = 20;
    int slotSize = 64;

    for (int i = 0; i < totalSlots; i++) {
      TextButton slotBackground = new TextButton("PlaceHOLDER", skin);

      gridTable.add(slotBackground).size(slotSize).pad(5);

      // Break to a new row after reaching the column limit
      if ((i + 1) % columns == 0) {
        gridTable.row();
      }
    }
    table.add(gridTable);
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

  public void setVisible(boolean set) {
    table.setVisible(set);
  }
}

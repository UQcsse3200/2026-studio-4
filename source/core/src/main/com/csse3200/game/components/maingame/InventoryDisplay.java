package com.csse3200.game.components.maingame;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
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

    // Right side of inventory
    Table charmsTable = new Table();
    Table consumablesTable = new Table();

    // Left side of inventory
    Table goldCount = new Table();
    Table weaponDisplayTable = new Table();

    Table rightStack = new Table().background(inventory.getDrawable("UI_TravelBook_BookPageLeft01a"));
    Table leftStack = new Table().background(inventory.getDrawable("UI_TravelBook_BookPageRight01a"));

    // Gold display building
    goldCount.add(new Image(skin, "button-c")).left();
    goldCount.add(new Label("52", skin)).expand();

    // Weapon Display Table building
    weaponDisplayTable.add(new Image(inventory, "UI_TravelBook_CommandDirection01a"));
    weaponDisplayTable.row();
    weaponDisplayTable.add(new Label("Sword", skin));

    charmsTable.add(new Label("Charms", skin)).colspan(3);
    charmsTable.row();
    tableDraw(charmsTable);

    consumablesTable.add(new Label("Consumables", skin)).colspan(3);
    consumablesTable.row();
    tableDraw(consumablesTable);

    // Created to have the two row on left column and one row on right column
    leftStack.add(goldCount).fill();
    leftStack.row();
    leftStack.add(weaponDisplayTable).expand().fill();

    rightStack.add(charmsTable).padLeft(20f).fill();
    rightStack.add(consumablesTable).padLeft(20f).fill();

    table.add(leftStack).fill();
    table.add(rightStack).fill();

    leftStack.setDebug(true);
    rightStack.setDebug(true);
    charmsTable.setDebug(true);
    consumablesTable.setDebug(true);
    goldCount.setDebug(true);
    weaponDisplayTable.setDebug(true);
    table.setDebug(true);

    table.setVisible(false);
  }

  private void tableDraw(Table table) {
    // Grid Table building
    int columns = 3;
    int totalSlots = 21;
    int slotSize = 64;

    for (int i = 0; i < totalSlots; i++) {
      ImageButton slotBackground = new ImageButton(inventory, "inventory-box");

      table.add(slotBackground).size(slotSize).pad(5);

      // Break to a new row after reaching the column limit
      if ((i + 1) % columns == 0) {
        table.row();
      }
    }
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

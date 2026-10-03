package com.csse3200.game.ui.minimap;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.csse3200.game.ui.UIComponent;
import com.csse3200.game.utils.shapes.Rectangle;

/** Minimap */
public class Minimap extends UIComponent {
  private Table table;

  private static final int WIDTH = 220;
  private static final int HEIGHT = 220;

  @Override
  public void create() {
    super.create();
    stage.addActor(table);
  }

  public Minimap() {
    this.table = buildTable();
  }

  private Table buildTable() {
    Table table = new Table();
    table.setFillParent(true);
    table.center().left();
    table.padLeft(20f);

    Table panel = new Table();
    setTableBackground(panel, new Color(0, 0, 0, 0.4f));

    var centerRoom = new Rectangle(Color.WHITE);
    panel.add(centerRoom).size(40, 30).center();

    table.add(panel).size(WIDTH, HEIGHT).top().left();

    return table;
  }

  private static void setTableBackground(Table table, Color color) {
    Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
    pixmap.setColor(color);
    pixmap.fill();
    TextureRegionDrawable textureRegion =
        new TextureRegionDrawable(new TextureRegion(new Texture(pixmap)));
    table.setBackground(textureRegion);
  }

  @Override
  protected void draw(SpriteBatch batch) {}
}

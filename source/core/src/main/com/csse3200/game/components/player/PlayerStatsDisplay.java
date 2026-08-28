package com.csse3200.game.components.player;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.items.Charm;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.ui.UIComponent;

/** A ui component for displaying player stats, e.g. health. */
public class PlayerStatsDisplay extends UIComponent {
  Table table;
  private Image heartImage;
  private Label healthLabel;
  private Label strengthLabel;
  private Label charmCountLabel;

  /** Creates reusable ui styles and adds actors to the stage. */
  @Override
  public void create() {
    super.create();
    addActors();

    entity.getEvents().addListener("updateHealth", this::updatePlayerHealthUI);
    entity.getEvents().addListener("updateStrength", this::updatePlayerStrengthUI);
    entity.getEvents().addListener("charmAdded", this::updateCharmCountUI);
    entity.getEvents().addListener("charmRemoved", this::updateCharmCountUI);
  }

  /**
   * Creates actors and positions them on the stage using a table.
   *
   * @see Table for positioning options
   */
  private void addActors() {
    table = new Table();
    table.top().left();
    table.setFillParent(true);
    table.padTop(45f).padLeft(5f);

    // Heart image
    float heartSideLength = 30f;
    heartImage =
        new Image(ServiceLocator.getResourceService().getAsset("images/heart.png", Texture.class));

    // Health text
    int health = entity.getComponent(CombatStatsComponent.class).getHealth();
    CharSequence healthText = String.format("Health: %d", health);
    healthLabel = new Label(healthText, skin, "large");

    int strength = entity.getComponent(CombatStatsComponent.class).getStrength();
    CharSequence strengthText = String.format("Strength: %d", strength);
    strengthLabel = new Label(strengthText, skin, "large");

    int charmCount = entity.getComponent(InventoryComponent.class).getCharmCount();
    CharSequence charmCountText = String.format("Strength Charms: %d", charmCount);
    charmCountLabel = new Label(charmCountText, skin, "large");

    table.add(heartImage).size(heartSideLength).pad(5);
    table.add(healthLabel);
    table.row();
    table.add(strengthLabel).colspan(2).left();
    table.row();
    table.add(charmCountLabel).colspan(2).left();
    stage.addActor(table);
  }

  @Override
  public void draw(SpriteBatch batch) {
    // draw is handled by the stage
  }

  /**
   * Updates the player's health on the ui.
   *
   * @param health player health
   */
  public void updatePlayerHealthUI(int health) {
    CharSequence text = String.format("Health: %d", health);
    healthLabel.setText(text);
  }

  /**
   * Updates the player's strength on the ui.
   *
   * @param strength player strength
   */
  public void updatePlayerStrengthUI(int strength) {
    CharSequence text = String.format("Strength: %d", strength);
    strengthLabel.setText(text);
  }

  /** Updates the displayed charm count after a charm is added to or removed from the inventory. */
  public void updateCharmCountUI(Charm charm) {
    int charmCount = entity.getComponent(InventoryComponent.class).getCharmCount();
    CharSequence text = String.format("Strength Charms: %d", charmCount);
    charmCountLabel.setText(text);
  }

  @Override
  public void dispose() {
    super.dispose();
    heartImage.remove();
    healthLabel.remove();
    strengthLabel.remove();
    charmCountLabel.remove();
  }
}

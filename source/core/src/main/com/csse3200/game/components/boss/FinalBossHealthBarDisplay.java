package com.csse3200.game.components.boss;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.ui.UIComponent;

/** Displays the Final Boss health bar at the bottom centre of the screen. */
public class FinalBossHealthBarDisplay extends UIComponent {
  private static final float BAR_WIDTH = 520f;
  private static final float BAR_HEIGHT = 24f;

  private Table table;
  private Label healthLabel;
  private ProgressBar healthBar;
  private int health;
  private int maxHealth;

  @Override
  public void create() {
    super.create();

    CombatStatsComponent stats = entity.getComponent(CombatStatsComponent.class);

    if (stats == null) {
      throw new IllegalStateException("FinalBossHealthBarDisplay requires CombatStatsComponent");
    }

    health = stats.getHealth();
    maxHealth = stats.getMaxHealth();

    createActors();

    entity.getEvents().addListener("updateHealth", this::updateBossHealthUI);
    entity.getEvents().addListener("updateMaxHealth", this::updateBossMaxHealthUI);
  }

  private void createActors() {
    table = new Table();
    table.bottom();
    table.setFillParent(true);
    table.padBottom(25f);

    Label title = new Label("FINAL BOSS: GRANDPA", skin, "statDisplay");

    ProgressBar.ProgressBarStyle barStyle = skin.get("fancy", ProgressBar.ProgressBarStyle.class);

    healthBar = new ProgressBar(0, maxHealth, 1, false, barStyle);
    healthBar.setValue(health);
    healthBar.setAnimateDuration(0.3f);

    healthLabel = new Label(String.format("%d / %d", health, maxHealth), skin, "statDisplay");

    table.add(title).padBottom(5f);
    table.row();
    table.add(healthBar).width(BAR_WIDTH).height(BAR_HEIGHT);
    table.row();
    table.add(healthLabel).padTop(4f);

    stage.addActor(table);
  }

  /** Updates the displayed current health. */
  public void updateBossHealthUI(int newHealth) {
    health = newHealth;
    healthBar.setValue(newHealth);
    updateHealthLabel();
  }

  /** Updates the displayed maximum health. */
  public void updateBossMaxHealthUI(int newMaxHealth) {
    maxHealth = newMaxHealth;
    healthBar.setRange(0, newMaxHealth);
    updateHealthLabel();
  }

  private void updateHealthLabel() {
    healthLabel.setText(String.format("%d / %d", health, maxHealth));
  }

  @Override
  public void draw(SpriteBatch batch) {
    // Rendering is handled by the shared UI stage.
  }

  @Override
  public void dispose() {
    if (table != null) {
      table.remove();
    }
    super.dispose();
  }
}

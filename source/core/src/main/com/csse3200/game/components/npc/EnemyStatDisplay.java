package com.csse3200.game.components.npc;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.ui.UIComponent;

public class EnemyStatDisplay extends UIComponent {

  private Table table;
  private ProgressBar healthBar;
  private int maxHealth;
  private int health;
  private float scale;

  /** Vertical offset (in world units) above the entity's position to draw the bar. */
  private static final float Y_OFFSET = 0.2f;

  public EnemyStatDisplay(float sc) {
    this.scale = sc;
  }

  /** Creates reusable ui styles and adds actors to the stage. */
  @Override
  public void create() {
    super.create();
    addActors();

    entity.getEvents().addListener("updateHealth", this::updateEnemyHealthUI);
    entity.getEvents().addListener("updateMaxHealth", this::updateEnemyMaxHealthUI);
    //        entity.getEvents().addListener("setPosition", this::updateHealthBar);
  }

  /**
   * Creates actors and positions them on the stage using a table.
   *
   * @see Table for positioning options
   */
  private void addActors() {
    CombatStatsComponent stats = entity.getComponent(CombatStatsComponent.class);
    maxHealth = stats.getMaxHealth();
    health = stats.getHealth();

    table = new Table();
    table.setSkin(skin);

    ProgressBar.ProgressBarStyle barStyle =
        skin.get("enemy-health-bar", ProgressBar.ProgressBarStyle.class);
    healthBar = new ProgressBar(0, maxHealth, 1, false, barStyle);
    healthBar.setValue(health);
    healthBar.setAnimateDuration(0.3f);

    table.add(healthBar);
    table.setTransform(true);
    table.setScale(scale);

    if (stage == null) {
      return;
    }
    stage.addActor(table);

    // Place it above the enemy's head right away, using its spawn position
    updateHealthBar(entity.getPosition());
  }

  @Override
  public void update() {
    updateHealthBar(entity.getPosition());
  }

  @Override
  public void draw(SpriteBatch batch) {
    // draw is handled by the stage
  }

  /**
   * Repositions the health bar above the entity whenever it moves.
   *
   * @param newPosition entity's new world position
   */
  public void updateHealthBar(Vector2 newPosition) {
    if (table == null) {
      return;
    }

    Camera camera = ServiceLocator.getWorldCamera();
    if (camera == null) {
      return; // camera not registered yet
    }

    Vector3 worldPos =
        new Vector3(
            newPosition.x + entity.getScale().x / 2f,
            newPosition.y + entity.getScale().y + Y_OFFSET,
            0);

    camera.project(worldPos, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

    table.setPosition(worldPos.x - table.getWidth() / 2f, worldPos.y);
  }

  /**
   * Updates the enemy's health on the ui.
   *
   * @param health enemy health
   */
  public void updateEnemyHealthUI(int health) {
    healthBar.setValue(health);
    this.health = health;
  }

  /**
   * Updates the enemy's max health on the ui.
   *
   * @param maxHealth enemy max health
   */
  public void updateEnemyMaxHealthUI(int maxHealth) {
    this.maxHealth = maxHealth;
    healthBar.setRange(0, maxHealth);
  }

  @Override
  public void dispose() {
    super.dispose();
    if (table != null) {
      table.remove();
    }
  }
}

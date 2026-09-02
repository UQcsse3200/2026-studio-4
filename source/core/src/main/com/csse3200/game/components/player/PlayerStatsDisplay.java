package com.csse3200.game.components.player;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.items.Charm;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.ui.UIComponent;

/** A ui component for displaying player stats, e.g. health. */
public class PlayerStatsDisplay extends UIComponent {
  Table table;
  private Label healthLabel;
  private Label strengthLabel;
  private Label charmCountLabel;
  private Label movementSpeedLabel;
  private Label attackSpeedLabel;
  private ProgressBar healthBar;
  private int maxHealth;
  private int health;

  private static final String LABEL_STYLE = "statDisplay";

  /** Creates reusable ui styles and adds actors to the stage. */
  @Override
  public void create() {
    super.create();
    addActors();

    entity.getEvents().addListener("updateHealth", this::updatePlayerHealthUI);
    entity.getEvents().addListener("updateBaseAttack", this::updatePlayerStrengthUI);
    entity.getEvents().addListener("updateMovementSpeed", this::updatePlayerMovementSpeedUI);
    entity.getEvents().addListener("updateAttackSpeed", this::updatePlayerAttackSpeedUI);
    entity.getEvents().addListener("updateMaxHealth", this::updatePlayerMaxHealthUI);
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

    CombatStatsComponent stats = entity.getComponent(CombatStatsComponent.class);
    maxHealth = stats.getMaxHealth();
    health = stats.getHealth();

    ProgressBar.ProgressBarStyle barStyle = skin.get("fancy", ProgressBar.ProgressBarStyle.class);
    healthBar = new ProgressBar(0, maxHealth, 1, false, barStyle);
    healthBar.setValue(health);
    healthBar.setAnimateDuration(0.3f);

    int charmCount = entity.getComponent(InventoryComponent.class).getCharmCount();

    // Labels
    healthLabel =
        new Label(
            String.format("Health: %d / %d", stats.getHealth(), stats.getMaxHealth()),
            skin,
            LABEL_STYLE);
    strengthLabel = new Label(
            String.format("Strength: %d", stats.getBaseAttack()), skin, LABEL_STYLE);
    movementSpeedLabel =
        new Label(
            String.format("Movement Speed: %.2f", stats.getMovementSpeed()), skin, LABEL_STYLE);
    attackSpeedLabel =
        new Label(String.format("Attack Speed: %.2f", stats.getAttackSpeed()), skin, LABEL_STYLE);
    charmCountLabel = new Label(String.format("Strength Charms: %d", charmCount), skin, LABEL_STYLE);

    table.add(healthLabel).left();
    table.row();
    table.add(healthBar).left();
    table.row();
    table.add(movementSpeedLabel).left();
    table.row();
    table.add(attackSpeedLabel).left();
    table.row();
    table.add(strengthLabel).left();
    table.row();
    table.add(charmCountLabel).left();

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
    healthBar.setValue(health);
    this.health = health;
  }

  /**
   * Updates the player's movement speed on the ui.
   *
   * @param movementSpeed player movement speed
   */
  public void updatePlayerMovementSpeedUI(float movementSpeed) {
    CharSequence text = String.format("Movement Speed: %.2f", movementSpeed);
    movementSpeedLabel.setText(text);
  }

  /**
   * Updates the player's Attack Speed on the ui.
   *
   * @param attackSpeed player attack speed
   */
  public void updatePlayerAttackSpeedUI(float attackSpeed) {
    CharSequence text = String.format("Attack Speed: %.2f", attackSpeed);
    attackSpeedLabel.setText(text);
  }

  /**
   * Updates the player's max Health on the ui.
   *
   * @param maxHealth player attack speed
   */
  public void updatePlayerMaxHealthUI(int maxHealth) {
    this.maxHealth = maxHealth;
    healthBar.setRange(0, maxHealth);
  }

  /**
   * Updates the player's strength on the ui.
   *
   * @param strength player strength, represented by base attack damage
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
    table.remove();
  }
}

package com.csse3200.game.components.player;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.csse3200.game.components.CombatStatsComponent;
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
  private CombatStatsComponent stats;
  private int maxHealth;
  private int health;

  private static final String LABEL_STYLE = "statDisplay";

  /** Creates reusable ui styles and adds actors to the stage. */
  @Override
  public void create() {
    super.create();
    addActors();

    entity.getEvents().addListener("updateHealth", this::updatePlayerHealthUI);
    entity.getEvents().addListener("updateMaxHealth", this::updatePlayerMaxHealthUI);
    // The amplified lines are read back from the component, so the raw value carried by these
    // events is ignored; Last Stand starting or expiring changes them without any raw stat change.
    entity
        .getEvents()
        .addListener("updateBaseAttack", (Integer attack) -> updateAmplifiedStatsUI());
    entity
        .getEvents()
        .addListener("updateMovementSpeed", (Float speed) -> updateAmplifiedStatsUI());
    entity.getEvents().addListener("updateAttackSpeed", (Float speed) -> updateAmplifiedStatsUI());
    entity.getEvents().addListener("abilityUsed", (String ability) -> updateAmplifiedStatsUI());
    entity.getEvents().addListener("abilityEnded", (String ability) -> updateAmplifiedStatsUI());
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

    stats = entity.getComponent(CombatStatsComponent.class);
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
    strengthLabel = new Label("", skin, LABEL_STYLE);
    movementSpeedLabel = new Label("", skin, LABEL_STYLE);
    attackSpeedLabel = new Label("", skin, LABEL_STYLE);
    updateAmplifiedStatsUI();
    charmCountLabel =
        new Label(String.format("Strength Charms: %d", charmCount), skin, LABEL_STYLE);

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
   * Refreshes every stat line that Last Stand amplifies, showing the values the player currently
   * fights and moves at rather than the raw stored stats. Called both when a raw stat changes and
   * when the passive starts or expires, so the display tracks the buff live.
   */
  public void updateAmplifiedStatsUI() {
    strengthLabel.setText(String.format("Strength: %d", stats.getEffectiveBaseAttack()));
    movementSpeedLabel.setText(
        String.format("Movement Speed: %.2f", stats.getEffectiveMovementSpeed()));
    attackSpeedLabel.setText(String.format("Attack Speed: %.2f", stats.getEffectiveAttackSpeed()));
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

  /** Updates the displayed charm count after a charm is added to or removed from the inventory. */
  public void updateCharmCountUI() {
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

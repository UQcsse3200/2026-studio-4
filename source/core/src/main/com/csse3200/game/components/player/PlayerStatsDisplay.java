package com.csse3200.game.components.player;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.ui.UIComponent;

/** Player combat HUD: health in the top-left corner, other stats kept off that slot. */
public class PlayerStatsDisplay extends UIComponent {
  static final float LOW_HEALTH_FRACTION = 0.25f;
  private static final Color HEALTH_OK = Color.WHITE;
  private static final Color HEALTH_LOW = Color.RED;
  private static final String LABEL_STYLE = "statDisplay";

  private Table table;
  private Label healthValueLabel;
  private Label strengthLabel;
  private Label invisibilityLabel;
  private Label invisibilityCooldownLabel;
  private Label movementSpeedLabel;
  private Label attackSpeedLabel;
  private ProgressBar healthBar;
  private int maxHealth;
  private int health;
  private Label shieldLabel;
  private ProgressBar shieldBar;
  private Table shieldTable;
  private boolean lowHealthWarning;

  /**
   * Updates the shield value and maximum shown in the player's HUD.
   *
   * @param current the shield's current number of points
   * @param max the shield's maximum number of points
   */
  public void updatePlayerShieldUI(int current, int max) {
    shieldLabel.setText(String.format("Shield: %d / %d", current, max));
    shieldBar.setRange(0, max);
    shieldBar.setValue(current);
  }

  /** True when current health is at most 25% of max health. */
  public boolean isLowHealthWarning() {
    return lowHealthWarning;
  }

  @Override
  public void create() {
    super.create();
    addActors();

    entity.getEvents().addListener("updateHealth", this::updatePlayerHealthUI);
    entity.getEvents().addListener("updateBaseAttack", this::updatePlayerStrengthUI);
    entity.getEvents().addListener("updateMovementSpeed", this::updatePlayerMovementSpeedUI);
    entity.getEvents().addListener("updateAttackSpeed", this::updatePlayerAttackSpeedUI);
    entity.getEvents().addListener("updateMaxHealth", this::updatePlayerMaxHealthUI);
    entity.getEvents().addListener("updateShield", this::updatePlayerShieldUI);
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
    table.padTop(20f).padLeft(20f);

    CombatStatsComponent stats = entity.getComponent(CombatStatsComponent.class);
    maxHealth = stats.getMaxHealth();
    health = stats.getHealth();

    float panelWidth = 280f;
    float panelHeight = 90f;

    Table panel = new Table();
    panel.setBackground(hud.getDrawable("scroll-background"));

    Image heartIcon = new Image(hud.getDrawable("heart-icon"));
    float iconSize = 20f;

    ProgressBar.ProgressBarStyle barStyle =
        hud.get("player-health-bar", ProgressBar.ProgressBarStyle.class);
    healthBar = new ProgressBar(0, maxHealth, 1, false, barStyle);
    healthBar.setValue(health);
    healthBar.setAnimateDuration(0.3f);
    shieldBar = new ProgressBar(0, 20, 1, false, barStyle);
    shieldBar.setValue(20);
    shieldBar.setAnimateDuration(0.2f);

    shieldLabel = new Label("Shield: 20 / 20", skin, LABEL_STYLE);

    float barWidth = 100f;
    float barHeight = barWidth / (32f / 5f);

    healthValueLabel = new Label(healthText(), skin, LABEL_STYLE);
    healthValueLabel.setFontScale(0.4f);

    Stack healthStack = new Stack();
    healthStack.add(healthBar);
    Table labelWrap = new Table();
    labelWrap.add(healthValueLabel);
    healthStack.add(labelWrap);

    Table healthBarRow = new Table();
    healthBarRow.add(heartIcon).size(iconSize);
    healthBarRow.add(healthStack).size(barWidth, barHeight);
    healthBarRow.setTransform(true);
    healthBarRow.setScale(2.5f);

    movementSpeedLabel =
        new Label(String.format("%.1fx", stats.getMovementSpeed()), skin, LABEL_STYLE);
    attackSpeedLabel = new Label(String.format("%.1f", stats.getAttackSpeed()), skin, LABEL_STYLE);
    strengthLabel = new Label(String.valueOf(stats.getBaseAttack()), skin, LABEL_STYLE);
    movementSpeedLabel.setVisible(false);
    attackSpeedLabel.setVisible(false);
    strengthLabel.setVisible(false);

    InvisibilityPotionComponent invisibility =
        entity.getComponent(InvisibilityPotionComponent.class);
    invisibilityLabel =
        new Label(
            invisibility == null ? "Invisibility: Ready" : invisibility.getDurationHudText(),
            skin,
            LABEL_STYLE);
    invisibilityCooldownLabel =
        new Label(
            invisibility == null ? "Invis CD: Ready" : invisibility.getCooldownHudText(),
            skin,
            LABEL_STYLE);

    Table content = new Table();
    content.add(healthBarRow).left();

    panel.add(content).expand().top().center();
    table.add(panel).size(panelWidth, panelHeight).top().left().padTop(20f);

    shieldTable = new Table();
    shieldTable.bottom().left();
    shieldTable.setFillParent(true);
    shieldTable.padBottom(25f).padLeft(5f);
    shieldTable.add(shieldLabel).left();
    shieldTable.row();
    shieldTable.add(shieldBar).left();
    shieldTable.row();
    shieldTable.add(invisibilityLabel).left();
    shieldTable.row();
    shieldTable.add(invisibilityCooldownLabel).left();

    refreshLowHealthWarning();

    if (stage == null) {
      return;
    }
    stage.addActor(table);
    stage.addActor(shieldTable);
  }

  @Override
  public void update() {
    InvisibilityPotionComponent invisibility =
        entity.getComponent(InvisibilityPotionComponent.class);
    if (invisibility != null) {
      invisibilityLabel.setText(invisibility.getDurationHudText());
      invisibilityCooldownLabel.setText(invisibility.getCooldownHudText());
    }
    if (lowHealthWarning && healthValueLabel != null) {
      float pulse = 0.65f + 0.35f * (0.5f + 0.5f * (float) Math.sin(System.nanoTime() / 1.5e8));
      healthValueLabel.getColor().a = pulse;
    }
  }

  /**
   * Updates the player's health on the ui.
   *
   * @param health player health
   */
  public void updatePlayerHealthUI(int health) {
    this.health = health;
    healthBar.setValue(health);
    healthValueLabel.setText(healthText());
    refreshLowHealthWarning();
  }

  /**
   * Updates the player's movement speed on the ui.
   *
   * @param movementSpeed player movement speed
   */
  public void updatePlayerMovementSpeedUI(float movementSpeed) {
    movementSpeedLabel.setText(String.format("%.1fx", movementSpeed));
  }

  /**
   * Updates the player's Attack Speed on the ui.
   *
   * @param attackSpeed player attack speed
   */
  public void updatePlayerAttackSpeedUI(float attackSpeed) {
    attackSpeedLabel.setText(String.format("%.1f", attackSpeed));
  }

  /**
   * Updates the player's max Health on the ui.
   *
   * @param maxHealth player max health
   */
  public void updatePlayerMaxHealthUI(int maxHealth) {
    this.maxHealth = maxHealth;
    healthBar.setRange(0, maxHealth);
    healthValueLabel.setText(healthText());
    refreshLowHealthWarning();
  }

  /**
   * Updates the player's strength on the ui.
   *
   * @param strength player strength, represented by base attack damage
   */
  public void updatePlayerStrengthUI(int strength) {
    strengthLabel.setText(String.format("%d", strength));
  }

  @Override
  public void draw(SpriteBatch batch) {
    // draw is handled by the stage
  }

  @Override
  public void dispose() {
    super.dispose();
    table.remove();
    shieldTable.remove();
  }

  private String healthText() {
    return String.format("Health: %d / %d", health, maxHealth);
  }

  private void refreshLowHealthWarning() {
    lowHealthWarning = maxHealth > 0 && health <= maxHealth * LOW_HEALTH_FRACTION;
    Color colour = lowHealthWarning ? HEALTH_LOW : HEALTH_OK;
    if (healthValueLabel != null) {
      healthValueLabel.setColor(colour);
      healthValueLabel.getColor().a = 1f;
    }
    if (healthBar != null) {
      healthBar.setColor(colour);
    }
  }
}

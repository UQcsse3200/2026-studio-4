package com.csse3200.game.components.player;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.items.Charm;
import com.csse3200.game.ui.UIComponent;

/** A ui component for displaying player stats as an icon-driven HUD panel. */
public class PlayerStatsDisplay extends UIComponent {
  private Table table;
  private Label healthValueLabel;
  private Label strengthLabel;
  private Label charmCountLabel;
  private Label movementSpeedLabel;
  private Label attackSpeedLabel;
  private ProgressBar healthBar;
  private int maxHealth;
  private int health;

  private static final String LABEL_STYLE = "statDisplay";
  private static final float SCALE = 3f;

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
    table.padTop(20f).padLeft(20f);

    CombatStatsComponent stats = entity.getComponent(CombatStatsComponent.class);
    maxHealth = stats.getMaxHealth();
    health = stats.getHealth();

    // Background
    float panelWidth = 420f;
    float panelHeight = panelWidth / (1683f / 794f); // ≈ 245f

    Table panel = new Table();
    panel.setBackground(skin.getDrawable("scroll-background"));

    // Icon heart
    Image heartIcon = new Image(skin.getDrawable("heart-icon")); // native 123x127, ~1:1
    float iconSize = 20f;

    // Health bar
    ProgressBar.ProgressBarStyle barStyle =
        skin.get("player-health-bar", ProgressBar.ProgressBarStyle.class);
    healthBar = new ProgressBar(0, maxHealth, 1, false, barStyle);
    healthBar.setValue(health);
    healthBar.setAnimateDuration(0.3f);

    float barWidth = 100f;
    float barHeight = barWidth / (32f / 5f); // ≈ 40.6f

    healthValueLabel = new Label(health + "/" + maxHealth, skin, LABEL_STYLE);
    healthValueLabel.setFontScale(0.4f);

    // Bar with numbers
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

    // MS + AS
    Image speedIcon = new Image(skin.getDrawable("ms-icon")); // placeholder
    movementSpeedLabel =
        new Label(String.format("%.1fx", stats.getMovementSpeed()), skin, LABEL_STYLE);
    movementSpeedLabel.setFontScale(1f);

    Image attackSpeedIcon = new Image(skin.getDrawable("as-icon")); // placeholder
    attackSpeedLabel = new Label(String.format("%.1f", stats.getAttackSpeed()), skin, LABEL_STYLE);
    attackSpeedLabel.setFontScale(1f);

    Table msBlock = new Table();
    msBlock.add(new Label("MS:", skin, LABEL_STYLE));
    msBlock.row().padTop(2f);
    msBlock.add(speedIcon).size(28f, 28f);
    msBlock.row().padTop(2f);
    msBlock.add(movementSpeedLabel);

    Table asBlock = new Table();
    asBlock.add(new Label("AS:", skin, LABEL_STYLE));
    asBlock.row().padTop(2f);
    asBlock.add(attackSpeedIcon).size(28f, 28f);
    asBlock.row().padTop(2f);
    asBlock.add(attackSpeedLabel);

    // Strength box
    int charmCount = entity.getComponent(InventoryComponent.class).getCharmCount();
    charmCountLabel = new Label(String.format("#: %d", charmCount), skin, LABEL_STYLE);

    Image strengthIcon = new Image(skin.getDrawable("strength-icon")); // placeholder
    strengthLabel = new Label(String.valueOf(stats.getBaseAttack()), skin, LABEL_STYLE);
    strengthLabel.setFontScale(1f);

    Table strengthBox = new Table();
    strengthBox.add(new Label("STRENGTH", skin, LABEL_STYLE));
    strengthBox.row().padTop(2f);
    strengthBox.add(strengthIcon).size(28f, 28f);
    strengthBox.row().padTop(2f);
    strengthBox.add(strengthLabel);
    strengthBox.row().padTop(2f);
    strengthBox.add(charmCountLabel);

    // Row below health bar
    Table subStatsRow = new Table();
    subStatsRow.add(msBlock).padRight(50f);
    subStatsRow.add(asBlock).padRight(50f);
    subStatsRow.add(strengthBox);

    // Assemble
    Table content = new Table();
    content.add(healthBarRow).left();
    content.row();
    content.add(subStatsRow);

    panel.add(content).expand().top().left().padTop(39f).padLeft(52f);
    table.add(panel).size(panelWidth, panelHeight).top().left();

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
    healthBar.setValue(health);
    healthValueLabel.setText(health + " / " + this.maxHealth);
    this.health = health;
  }

  /**
   * Updates the player's movement speed on the ui.
   *
   * @param movementSpeed player movement speed
   */
  public void updatePlayerMovementSpeedUI(float movementSpeed) {
    CharSequence text = String.format("%.1fx", movementSpeed);
    movementSpeedLabel.setText(text);
  }

  /**
   * Updates the player's Attack Speed on the ui.
   *
   * @param attackSpeed player attack speed
   */
  public void updatePlayerAttackSpeedUI(float attackSpeed) {
    CharSequence text = String.format("%.1f", attackSpeed);
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
    healthValueLabel.setText(this.health + " / " + maxHealth);
  }

  /**
   * Updates the player's strength on the ui.
   *
   * @param strength player strength, represented by base attack damage
   */
  public void updatePlayerStrengthUI(int strength) {
    CharSequence text = String.format("%d", strength);
    strengthLabel.setText(text);
  }

  /** Updates the displayed charm count after a charm is added to or removed from the inventory. */
  public void updateCharmCountUI(Charm charm) {
    int charmCount = entity.getComponent(InventoryComponent.class).getCharmCount();
    CharSequence text = String.format("#: %d", charmCount);
    charmCountLabel.setText(text);
  }

  @Override
  public void dispose() {
    super.dispose();
    table.remove();
  }
}

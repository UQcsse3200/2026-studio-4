package com.csse3200.game.components.maingame;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.ui.UIComponent;
import java.util.function.BooleanSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Displays a "double or nothing" coin flip booth: stake a chosen amount of the player's Gold, flip
 * a coin, and either double the stake back or lose it.
 *
 * <p>Unlike {@link ShopDisplay}, this component owns both presentation and the Gold transaction
 * itself — there is no separate team-owned settlement system for a self-contained wager like this.
 * It still fires a {@code coinFlipResolved} event after every flip (carrying whether the player won
 * and the stake wagered) so other systems, such as achievements, can listen without this component
 * needing to know they exist.
 *
 * <p>Not yet wired to a Casino/Gambler NPC interaction; in the meantime it can be opened for
 * testing via the {@code coinflip} terminal command, the same way {@code shop} lets QA skip the
 * Merchant NPC.
 */
public class CoinFlipDisplay extends UIComponent {
  private static final Logger logger = LoggerFactory.getLogger(CoinFlipDisplay.class);
  private static final float Z_INDEX = 2f;
  private static final String COIN_TEXTURE = "images/gold_coin_pixel.png";
  private static final int MIN_STAKE = 10;
  private static final int STAKE_STEP = 10;
  private static final float FLIP_ANIMATION_SECONDS = 0.6f;

  private final InventoryComponent inventory;
  private final BooleanSupplier coinToss;

  private Table table;
  private Label goldLabel;
  private Label stakeLabel;
  private Label resultLabel;
  private TextButton flipButton;
  private Image coinImage;
  private Texture coinTexture;
  private int stake = MIN_STAKE;

  /**
   * @param inventory the player's Gold source and sink
   */
  public CoinFlipDisplay(InventoryComponent inventory) {
    this(inventory, MathUtils::randomBoolean);
  }

  /**
   * @param inventory the player's Gold source and sink
   * @param coinToss supplies the outcome of a flip; {@code true} means the player wins. Exposed so
   *     tests can inject a deterministic result instead of real randomness.
   */
  public CoinFlipDisplay(InventoryComponent inventory, BooleanSupplier coinToss) {
    if (inventory == null) {
      throw new IllegalArgumentException("inventory must not be null");
    }
    if (coinToss == null) {
      throw new IllegalArgumentException("coinToss must not be null");
    }
    this.inventory = inventory;
    this.coinToss = coinToss;
  }

  @Override
  public void create() {
    super.create();
    buildTable();
    table.setVisible(false);
  }

  private void buildTable() {
    table = new Table();
    table.setName("coin-flip-display");
    table.setFillParent(true);
    table.center();
    if (stage != null) {
      stage.addActor(table);
    }

    Table panel = new Table();
    panel.setBackground(skin.getDrawable("window-w"));
    panel.pad(20f);

    panel.add(new Label("Coin Flip - Double or Nothing", skin)).colspan(3).padBottom(12f);
    panel.row();

    goldLabel = new Label("", skin);
    panel.add(goldLabel).colspan(3).left().padBottom(10f);
    panel.row();

    ResourceService resourceService = ServiceLocator.getResourceService();
    resourceService.loadTextures(new String[] {COIN_TEXTURE});
    resourceService.loadAll();
    coinTexture = resourceService.getAsset(COIN_TEXTURE, Texture.class);
    coinImage = new Image(coinTexture);
    coinImage.setOrigin(Align.center);
    panel.add(coinImage).colspan(3).size(64f, 64f).padBottom(10f);
    panel.row();

    TextButton stakeDown = new TextButton("-" + STAKE_STEP, skin);
    stakeDown.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            adjustStake(-STAKE_STEP);
          }
        });

    stakeLabel = new Label("", skin);

    TextButton stakeUp = new TextButton("+" + STAKE_STEP, skin);
    stakeUp.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            adjustStake(STAKE_STEP);
          }
        });

    panel.add(stakeDown).pad(4f);
    panel.add(stakeLabel).pad(4f);
    panel.add(stakeUp).pad(4f);
    panel.row();

    flipButton = new TextButton("Flip", skin);
    flipButton.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            flip();
          }
        });
    panel.add(flipButton).colspan(3).padTop(6f);
    panel.row();

    resultLabel = new Label("", skin);
    panel.add(resultLabel).colspan(3).padTop(6f);
    panel.row();

    TextButton close = new TextButton("Close", skin);
    close.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            close();
          }
        });
    panel.add(close).colspan(3).padTop(10f);

    table.add(panel);
  }

  /**
   * Clamps the stake to the player's current Gold. The floor is normally {@link #MIN_STAKE}, but
   * drops to whatever Gold the player actually has if that is less than the usual minimum, so a
   * player with e.g. 5 Gold can still stake all of it rather than being locked out entirely.
   */
  private void adjustStake(int delta) {
    int maxStake = inventory.getGold();
    int minStake = Math.min(MIN_STAKE, maxStake);
    stake = Math.min(Math.max(stake + delta, minStake), maxStake);
    refreshStakeLabel();
  }

  /**
   * Resolves a coin flip immediately: validates the stake, updates Gold, updates the result label,
   * and fires {@code coinFlipResolved}. A purely cosmetic spin animation runs in parallel but does
   * not gate the outcome, so this method is safe to call directly from tests.
   */
  private void flip() {
    if (!Boolean.TRUE.equals(inventory.hasGold(stake)) || stake <= 0) {
      resultLabel.setText("Not enough Gold");
      return;
    }

    boolean won = coinToss.getAsBoolean();
    inventory.addGold(won ? stake : -stake);

    resultLabel.setText(won ? "You won " + stake + " Gold!" : "You lost " + stake + " Gold.");
    refresh();
    animateFlip();

    logger.debug("Coin flip resolved: won={}, stake={}", won, stake);
    entity.getEvents().trigger("coinFlipResolved", won, stake);
  }

  /** Purely visual: a quick squash-and-spin to sell the flip. Never gates game state. */
  private void animateFlip() {
    if (coinImage == null) {
      return;
    }
    coinImage.clearActions();
    coinImage.addAction(
        Actions.sequence(
            Actions.scaleTo(0f, 1f, FLIP_ANIMATION_SECONDS / 2f, Interpolation.pow2In),
            Actions.scaleTo(1f, 1f, FLIP_ANIMATION_SECONDS / 2f, Interpolation.pow2Out)));
  }

  private void refreshStakeLabel() {
    if (stakeLabel != null) {
      stakeLabel.setText("Bet: " + stake + "g");
    }
  }

  /** Re-reads Gold from the inventory and re-clamps the stake. Call after open and after a flip. */
  public void refresh() {
    if (goldLabel != null) {
      goldLabel.setText("Gold: " + inventory.getGold());
    }
    adjustStake(0);
  }

  /** Shows the booth and refreshes Gold/stake. */
  public void open() {
    resultLabel.setText("");
    refresh();
    table.setVisible(true);
    table.toFront();
  }

  /** Hides the booth. */
  public void close() {
    table.setVisible(false);
  }

  public boolean isOpen() {
    return table.isVisible();
  }

  /**
   * @return the currently staked Gold amount
   */
  public int getStake() {
    return stake;
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
    table.remove();
    ResourceService resourceService = ServiceLocator.getResourceService();
    if (resourceService != null) {
      resourceService.unloadAssets(new String[] {COIN_TEXTURE});
    }
    super.dispose();
  }
}

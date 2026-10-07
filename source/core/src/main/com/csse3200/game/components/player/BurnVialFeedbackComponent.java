package com.csse3200.game.components.player;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.items.ItemIds;
import com.csse3200.game.items.consumables.VialBurning;
import com.csse3200.game.rendering.RenderComponent;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A short, unmissable orange flame icon drawn above the player's head whenever a Burn Vial is used,
 * together with animated flames following enemies affected by that vial.
 *
 * <p>Mirrors {@link HealingPotionFeedbackComponent}'s pattern: listen for {@link
 * ConsumableEffectComponent#USED}, then draw a simple tinted shape for a fixed duration.
 */
public class BurnVialFeedbackComponent extends RenderComponent {
  private static final float DURATION = 1.2f;
  private static final float TILE_SIZE = 0.22f;
  private Texture pixel;
  private float remaining;
  private Texture flameTexture;
  private TextureRegion[] flameFrames;
  private float flameTime;
  private boolean disposed;
  private final Map<Entity, List<VialBurning>> burningEnemies = new LinkedHashMap<>();

  /** Track only an effect actually applied by the item, without adding live enemy components. */
  public void trackBurn(Entity enemy, VialBurning burn) {
    if (!disposed) {
      burningEnemies.computeIfAbsent(enemy, ignored -> new ArrayList<>()).add(burn);
    }
  }

  @Override
  public void create() {
    super.create();
    entity.getEvents().addListener(ConsumableEffectComponent.USED, this::onItemUsed);
  }

  private void onItemUsed(String id) {
    if (ItemIds.BURN_VIAL.equals(id)) {
      remaining = DURATION;
    }
  }

  @Override
  public void update() {
    float delta = ServiceLocator.getTimeSource().getDeltaTime();
    if (Float.isFinite(delta) && delta > 0f) {
      remaining = Math.max(0f, remaining - delta);
      flameTime = (flameTime + delta) % 0.4f;
    }
    removeEndedBurns();
  }

  @Override
  protected void draw(SpriteBatch batch) {
    if (disposed || isRepeatPass()) return;
    removeEndedBurns();
    if (!burningEnemies.isEmpty()) drawEnemyFlames(batch);
    if (remaining <= 0f) return;
    if (pixel == null) {
      Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
      pixmap.setColor(Color.WHITE);
      pixmap.fill();
      pixel = new Texture(pixmap);
      pixmap.dispose();
    }

    float alpha = remaining / DURATION;
    Vector2 centre = entity.getCenterPosition();
    float x = centre.x - TILE_SIZE / 2f;
    float originalColour = batch.getPackedColor();

    // Three stacked tiles, widest at the base, mimicking a simple flame silhouette.
    batch.setColor(0.85f, 0.15f, 0.05f, alpha);
    batch.draw(pixel, x - TILE_SIZE * 0.3f, 0.9f + centre.y, TILE_SIZE * 1.6f, TILE_SIZE);
    batch.setColor(1f, 0.5f, 0f, alpha);
    batch.draw(pixel, x - TILE_SIZE * 0.15f, 1.05f + centre.y, TILE_SIZE * 1.3f, TILE_SIZE);
    batch.setColor(1f, 0.85f, 0.2f, alpha);
    batch.draw(pixel, x, 1.2f + centre.y, TILE_SIZE, TILE_SIZE);
    batch.setPackedColor(originalColour);
  }

  private void removeEndedBurns() {
    burningEnemies
        .entrySet()
        .removeIf(
            entry -> {
              Entity enemy = entry.getKey();
              StatusEffectsControllerComponent effects =
                  enemy.getComponent(StatusEffectsControllerComponent.class);
              CombatStatsComponent stats = enemy.getComponent(CombatStatsComponent.class);
              if (effects == null || effects.isDisposed() || stats == null || stats.isDead())
                return true;
              entry.getValue().removeIf(burn -> !effects.hasStatusEffect(burn));
              return entry.getValue().isEmpty();
            });
  }

  private void drawEnemyFlames(SpriteBatch batch) {
    if (flameTexture == null) {
      // Reuse Team 4's existing artwork; their trap and original resources remain untouched.
      flameTexture = new Texture(Gdx.files.internal("images/traps/fire-start.png"));
      flameTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
      // The last two ignition frames retain the bright yellow core; the loop strip is dark embers.
      flameFrames = new TextureRegion[2];
      for (int frame = 0; frame < flameFrames.length; frame++) {
        flameFrames[frame] = new TextureRegion(flameTexture, (frame + 2) * 18, 0, 18, 59);
      }
    }
    float originalColour = batch.getPackedColor();
    try {
      batch.setColor(Color.WHITE);
      for (Map.Entry<Entity, List<VialBurning>> entry : burningEnemies.entrySet()) {
        Entity enemy = entry.getKey();
        Vector2 position = enemy.getPosition();
        Vector2 size = enemy.getScale();
        float pulse = 0f;
        for (VialBurning burn : entry.getValue()) {
          pulse = Math.max(pulse, burn.getPulseStrength());
        }
        float width = MathUtils.clamp(size.x * 0.22f, 0.18f, 0.45f) * (1f + pulse * 0.5f);
        float height = width * 59f / 18f;
        for (int flame = 0; flame < 3; flame++) {
          int frame = ((int) (flameTime / 0.1f) + flame) % flameFrames.length;
          float x = position.x + size.x * (0.25f + flame * 0.25f) - width / 2f;
          float y = position.y + size.y * (flame == 1 ? 0.42f : 0.18f);
          batch.draw(flameFrames[frame], x, y, width, height);
        }
      }
    } finally {
      batch.setPackedColor(originalColour);
    }
  }

  @Override
  public int getLayer() {
    return 2; // Draw attached flames after character sprites, before the Scene2D HUD.
  }

  @Override
  public float getZIndex() {
    return super.getZIndex() + 0.1f;
  }

  @Override
  public void dispose() {
    disposed = true;
    burningEnemies.clear();
    remaining = 0f;
    if (flameTexture != null) flameTexture.dispose();
    if (pixel != null) pixel.dispose();
    super.dispose();
  }
}

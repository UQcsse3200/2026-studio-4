package com.csse3200.game.components.tutorial;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.ui.UIComponent;

/**
 * Floats the damage of each hit up from a {@link TrainingDummyComponent}, so a player can see what
 * their weapon, a heavy attack or a status effect actually does.
 */
public class DamagePopupDisplay extends UIComponent {
  private static final float RISE_PIXELS = 40f;
  private static final float DURATION_SECONDS = 0.8f;
  private static final float JITTER_WORLD_UNITS = 0.25f;
  private static final Color COLOUR = new Color(1f, 0.85f, 0.3f, 1f);

  @Override
  public void create() {
    super.create();
    entity
        .getEvents()
        .<String, Integer, Integer>addListener(TrainingDummyComponent.HIT, this::onHit);
  }

  private void onHit(String label, Integer damage, Integer totalHits) {
    Camera camera = ServiceLocator.getWorldCamera();
    if (stage == null || camera == null) {
      return;
    }
    Vector3 point =
        new Vector3(
            entity.getPosition().x
                + entity.getScale().x / 2f
                + MathUtils.random(-JITTER_WORLD_UNITS, JITTER_WORLD_UNITS),
            entity.getPosition().y + entity.getScale().y,
            0f);
    camera.project(point, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

    Label popup = new Label(String.valueOf(damage), skin, "large");
    popup.setColor(COLOUR);
    popup.pack();
    popup.setPosition(point.x - popup.getWidth() / 2f, point.y);
    popup.addAction(
        Actions.sequence(
            Actions.parallel(
                Actions.moveBy(0f, RISE_PIXELS, DURATION_SECONDS),
                Actions.fadeOut(DURATION_SECONDS)),
            Actions.removeActor()));
    stage.addActor(popup);
  }

  @Override
  public void draw(SpriteBatch batch) {
    // Drawn by the stage.
  }
}

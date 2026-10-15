package com.csse3200.game.components.gamearea;

import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.TimeUtils;
import com.csse3200.game.files.AudioLevels;
import com.csse3200.game.services.AchievementService;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.ui.UIComponent;
import java.util.ArrayDeque;
import java.util.Queue;

/**
 * Displays achievement-unlocked notifications independently from room or game-area displays.
 *
 * <p>The component listens directly to {@link AchievementService}, so achievement notifications
 * continue working when rooms are refactored or replaced.
 */
public class AchievementDisplay extends UIComponent {
  private static final long DISPLAY_DURATION_MILLIS = 2_000L;
  private static final float TOP_PADDING = 96f;

  private final Queue<String> achievementQueue = new ArrayDeque<>();

  private Table rootTable;
  private Label achievementLabel;
  private long expiryMillis;

  @Override
  public void create() {
    super.create();

    addActors();

    AchievementService achievementService = ServiceLocator.getAchievementService();
    if (achievementService != null) {
      achievementService.getEvents().addListener("achievementUnlocked", this::showAchievement);
    }
  }

  private void addActors() {
    achievementLabel = new Label("", skin, "large");
    achievementLabel.setWrap(true);
    achievementLabel.setVisible(false);

    rootTable = new Table();
    rootTable.setFillParent(true);
    rootTable.center().top().padTop(TOP_PADDING);
    rootTable.add(achievementLabel).width(800f);

    stage.addActor(rootTable);
    rootTable.toFront();
  }

  /**
   * Adds an achievement to the notification queue.
   *
   * @param name the unlocked achievement name
   */
  public void showAchievement(String name) {
    if (name == null || name.isBlank() || achievementLabel == null) {
      return;
    }

    achievementQueue.add(name);

    if (expiryMillis == 0L) {
      showNextAchievement();
    }
  }

  private void showNextAchievement() {
    String next = achievementQueue.poll();

    if (next == null) {
      achievementLabel.setText("");
      achievementLabel.setVisible(false);
      return;
    }

    achievementLabel.setText("Achievement Unlocked: " + next);
    achievementLabel.setVisible(true);
    expiryMillis = TimeUtils.millis() + DISPLAY_DURATION_MILLIS;
    playAchievementSound();
  }

  private void playAchievementSound() {
    Sound achievementSound =
        ServiceLocator.getResourceService().getAsset("sounds/achievementSound.mp3", Sound.class);
    AudioLevels.play(achievementSound);
  }

  @Override
  public void update() {
    if (achievementLabel == null || expiryMillis == 0L) {
      return;
    }

    if (TimeUtils.millis() >= expiryMillis) {
      expiryMillis = 0L;
      showNextAchievement();
    }
  }

  @Override
  protected void draw(SpriteBatch batch) {
    // The shared Scene2D stage draws this component.
  }

  @Override
  public float getZIndex() {
    return 2.5f;
  }

  @Override
  public void dispose() {
    achievementQueue.clear();

    if (rootTable != null) {
      rootTable.remove();
    }

    super.dispose();
  }
}

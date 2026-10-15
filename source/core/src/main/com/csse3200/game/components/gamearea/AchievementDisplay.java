package com.csse3200.game.components.gamearea;

import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.TimeUtils;
import com.csse3200.game.files.AudioLevels;
import com.csse3200.game.services.AchievementService;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.ui.UIComponent;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.StringTokenizer;

/**
 * Displays achievement-unlocked notifications independently from room or game-area displays.
 *
 * <p>The component listens directly to {@link AchievementService}, so achievement notifications
 * continue working when rooms are refactored or replaced.
 */
public class AchievementDisplay extends UIComponent {
  private static final long DISPLAY_DURATION_MILLIS = 2_000L;
  private static final float TOP_PADDING = 75f;
  private static final float TOAST_WIDTH = 450f;

  private static final String ACHIEVEMENT_BACK = "achievement";
  private static final String ACHIEVEMENT_SOUND = "sounds/achievementSound.mp3";


  private final Queue<String> achievementQueue = new ArrayDeque<>();

  private Table rootTable;
  private Table toast;
  private Label titleLabel;
  private Label nameLabel;
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
    titleLabel = new Label("Achievement Unlocked:", skin);

    nameLabel = new Label("", skin);
    nameLabel.setWrap(true);

    toast = new Table();
    toast.align(0);
    toast.add(titleLabel).center().row();
    toast.add(nameLabel).center().width(TOAST_WIDTH).padTop(2f);
    toast.setBackground(hud.getDrawable(ACHIEVEMENT_BACK));
    toast.setVisible(false);

    rootTable = new Table();
    rootTable.setFillParent(true);
    rootTable.center().top().padTop(TOP_PADDING);
    rootTable.add(toast);

    stage.addActor(rootTable);
    rootTable.toFront();
  }

  /**
   * Adds an achievement to the notification queue.
   *
   * @param name the unlocked achievement name
   */
  public void showAchievement(String name) {
    if (name == null || name.isBlank() || nameLabel == null) {
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
      nameLabel.setText("");
      toast.setVisible(false);
      return;
    }

    nameLabel.setText(next);
    toast.setVisible(true);
    expiryMillis = TimeUtils.millis() + DISPLAY_DURATION_MILLIS;
    playAchievementSound();
  }

  private void playAchievementSound() {
    Sound achievementSound =
            ServiceLocator.getResourceService().getAsset(ACHIEVEMENT_SOUND, Sound.class);
    AudioLevels.play(achievementSound);
  }

  @Override
  public void update() {
    if (nameLabel == null || expiryMillis == 0L) {
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
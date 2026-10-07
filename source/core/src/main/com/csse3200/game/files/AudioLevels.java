package com.csse3200.game.files;

import com.badlogic.gdx.audio.Sound;

/** Volumes chosen in the settings menu, clamped to 0..1. */
public final class AudioLevels {
  private AudioLevels() {
    throw new IllegalStateException("Instantiating static util class");
  }

  public static float clamp(float value) {
    if (!Float.isFinite(value)) {
      return 0f;
    }
    return Math.min(1f, Math.max(0f, value));
  }

  public static float music() {
    return clamp(UserSettings.get().musicVolume);
  }

  public static float effects() {
    return clamp(UserSettings.get().soundVolume);
  }

  /** Plays a sound at the saved effects volume. A missing sound is ignored. */
  public static void play(Sound sound) {
    if (sound == null) {
      return;
    }
    sound.play(effects());
  }
}

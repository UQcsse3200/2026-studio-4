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
    return Math.clamp(value, 0f, 1f);
  }

  public static float music() {
    UserSettings.Settings settings = UserSettings.get();
    return audible(settings.musicVolume, settings.muteUnfocused, WindowFocus.focused());
  }

  public static float effects() {
    UserSettings.Settings settings = UserSettings.get();
    return audible(settings.soundVolume, settings.muteUnfocused, WindowFocus.focused());
  }

  /**
   * Volume after the mute-when-unfocused rule. An unfocused window with that option on is silent.
   */
  public static float audible(float volume, boolean muteWhenUnfocused, boolean windowFocused) {
    if (muteWhenUnfocused && !windowFocused) {
      return 0f;
    }
    return clamp(volume);
  }

  /** Plays a sound at the saved effects volume. A missing sound is ignored. */
  public static void play(Sound sound) {
    if (sound == null) {
      return;
    }
    sound.play(effects());
  }
}

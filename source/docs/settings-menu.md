# Settings menu

Players open Settings from the main menu, or from the Settings button beside Exit during a run. Apply writes `settings.json` through `UserSettings` and applies the display options immediately. Exit on the main-menu page returns to the main menu. Exit during a run closes the menu and returns to the same run. Either Exit drops unsaved edits. The run pauses while the in-game menu is open.

The menu has Display, Audio, Gameplay and Run history sections. Display includes the fullscreen resolution, the window size used when fullscreen is off, and a UI scale. The UI scale slider stays on the bottom bar, next to Apply and Exit, so those controls stay on screen at any size. Dragging it resizes the rest of this menu from the top. During a run each HUD panel grows from the edge it sits on, instead of the whole screen scaling around the centre. Apply stores it. Gameplay controls the in-run timer and a small FPS counter in the bottom-right corner. Audio stores music and effects volume, and can mute both while the window is in the background. Run history lists last run and best run on this settings page, and can reset those times. Achievements, online play and the win screen are not part of this menu.

## Class diagram

```mermaid
classDiagram
  class SettingsMenuDisplay {
    +applyChanges()
    +exitMenu()
    -refreshProgressLabel()
  }
  class UserSettings {
    +get() Settings
    +set(Settings, boolean)
    +applySettings(Settings)
  }
  class Settings {
    +int fps
    +boolean fullscreen
    +boolean vsync
    +float uiScale
    +boolean showTimer
    +boolean showFps
    +float musicVolume
    +float soundVolume
    +int windowWidth
    +int windowHeight
    +boolean onlinePlay
    +String displayName
    +boolean muteUnfocused
    +DisplaySettings displayMode
  }
  class WindowSize {
    +matching(int, int) Preset
    +width(int) int
    +height(int) int
  }
  class PlayMode {
    +cleanName(String) String
    +summary(boolean, String) String
  }
  class GameProgress {
    +get() SaveData
    +recordRun(long)
    +clearSave()
    +clearAchievements()
    +formatTime(long) String
  }
  class SaveData {
    +long lastRunMs
    +long bestRunMs
    +List achievements
  }
  class MainGameScreen {
    +dispose()
  }
  class TimerDisplay {
    +create()
    +toggle()
  }
  class RunTimer {
    +startRun()
    +getTotalTime() float
  }
  class FpsOverlay {
    +create()
  }

  SettingsMenuDisplay --> UserSettings : read and apply
  SettingsMenuDisplay --> WindowSize : window size list
  SettingsMenuDisplay --> GameProgress : summary and reset
  UserSettings --> WindowSize : windowed mode size
  UserSettings --> Settings : persists settings.json
  GameProgress --> SaveData : persists game-save.json
  MainGameScreen --> RunTimer : starts and reads the run
  MainGameScreen --> TimerDisplay : adds the HUD
  MainGameScreen --> GameProgress : recordRun on dispose
  TimerDisplay --> UserSettings : showTimer
  TimerDisplay --> RunTimer : label text
  FpsOverlay --> UserSettings : showFps
  MainGameScreen --> FpsOverlay : adds the counter
```

## Sequence diagram

Apply, then a finished run:

```mermaid
sequenceDiagram
  participant Player
  participant Menu as SettingsMenuDisplay
  participant Settings as UserSettings
  participant Run as MainGameScreen
  participant Timer as TimerDisplay
  participant Clock as RunTimer
  participant Save as GameProgress

  Player->>Menu: Apply
  Menu->>Settings: set(settings, true)
  Settings->>Settings: write settings.json and apply display options
  Player->>Run: start a run
  Run->>Clock: startRun
  Run->>Timer: create
  Timer->>Settings: read showTimer
  Timer->>Timer: show or hide the panel
  Run->>Clock: update each frame
  Run->>Save: dispose records total time
  Save->>Save: write last and best to game-save.json
  Player->>Menu: open Settings
  Menu->>Save: read last and best
  Menu->>Player: show them in Run history
```

Reset from the settings menu:

```mermaid
sequenceDiagram
  participant Player
  participant Menu as SettingsMenuDisplay
  participant Save as GameProgress

  Player->>Menu: Reset times
  Menu->>Save: clearSave
  Save->>Save: zero last and best
  Menu->>Player: refresh summary
```

## Tests

JUnit coverage lives next to the code:

- `UserSettingsTest` checks the gameplay options default to on, window size is applied when fullscreen is off, and the new options round-trip through `settings.json` without touching the display.
- `WindowSizeTest` checks listed sizes, the default window, and clamping.
- `PlayModeTest` checks the display name and the online status line.
- `GameProgressTest` checks time formatting and best-run updates. The settings page does not list or reset achievements.
- `TimerDisplayTest` checks the HUD panel stays hidden when Show run timer is off.
- `AudioLevelsTest` checks music and effects volumes stay inside 0 to 1, and that mute-in-background silences an unfocused window.

From `source/`:

```sh
./gradlew test --tests com.csse3200.game.files.UserSettingsTest --tests com.csse3200.game.files.WindowSizeTest --tests com.csse3200.game.files.PlayModeTest --tests com.csse3200.game.files.GameProgressTest --tests com.csse3200.game.files.AudioLevelsTest --tests com.csse3200.game.components.gamearea.TimerDisplayTest
```

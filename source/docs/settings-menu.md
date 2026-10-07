# Settings menu

Players open Settings from the main menu, change display and gameplay options, then press Apply or Exit. Apply writes `settings.json` through `UserSettings` and applies the display options immediately. Exit returns to the main menu and drops unsaved edits.

The menu has Display, Audio, Gameplay and Run history sections. Gameplay controls the in-run timer and a small FPS counter. Audio stores music and effects volume. Run history lists last run, best run and achievement count on this settings page, and can reset those values separately. The win screen is not part of this menu.

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
    +DisplaySettings displayMode
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
  SettingsMenuDisplay --> GameProgress : summary and reset
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
  Menu->>Save: read last, best and achievement count
  Menu->>Player: show them in Run history
```

Reset from the settings menu:

```mermaid
sequenceDiagram
  participant Player
  participant Menu as SettingsMenuDisplay
  participant Save as GameProgress

  Player->>Menu: Reset save
  Menu->>Save: clearSave
  Save->>Save: zero last and best, keep achievements
  Menu->>Player: refresh summary
  Player->>Menu: Reset achievements
  Menu->>Save: clearAchievements
  Save->>Save: clear the list, keep times
  Menu->>Player: refresh summary
```

## Tests

JUnit coverage lives next to the code:

- `UserSettingsTest` checks the gameplay options default to on and round-trip through `settings.json` without touching the display.
- `GameProgressTest` checks time formatting, best-run updates, one-time achievement unlocks, and that the two reset actions do not clear each other's data.
- `TimerDisplayTest` checks the HUD panel stays hidden when Show run timer is off.
- `AudioLevelsTest` checks music and effects volumes stay inside 0 to 1.

From `source/`:

```sh
./gradlew test --tests com.csse3200.game.files.UserSettingsTest --tests com.csse3200.game.files.GameProgressTest --tests com.csse3200.game.files.AudioLevelsTest --tests com.csse3200.game.components.gamearea.TimerDisplayTest
```

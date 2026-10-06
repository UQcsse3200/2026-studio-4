# Settings menu

Players open Settings from the main menu, change display and gameplay options, then press Apply or Exit. Apply writes `settings.json` through `UserSettings` and applies the display options immediately. Exit returns to the main menu and drops unsaved edits.

The gameplay section controls whether the in-run timer is shown, whether the victory screen lists last run, best run and achievement count, and lets the player reset those saved values. Reset save clears last and best times only. Reset achievements clears the achievement list only. The summary line on the settings screen updates after either reset.

The timer checkbox is read when `TimerDisplay` is created, so it takes effect on the next run. The victory checkbox is read when the victory screen is created.

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
    +boolean showVictoryStats
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
  class VictoryScreen {
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
  VictoryScreen --> UserSettings : showVictoryStats
  VictoryScreen --> GameProgress : last, best, count
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
  participant Victory as VictoryScreen

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
  Run->>Victory: open victory screen
  Victory->>Settings: read showVictoryStats
  alt stats enabled
    Victory->>Save: read last, best and achievement count
    Victory->>Player: show those lines
  else stats disabled
    Victory->>Player: show Victory only
  end
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
- `VictoryScreenTest` checks the victory lines appear only when Show victory stats is on.

From `source/`:

```sh
./gradlew test --tests com.csse3200.game.files.UserSettingsTest --tests com.csse3200.game.files.GameProgressTest --tests com.csse3200.game.components.gamearea.TimerDisplayTest --tests com.csse3200.game.screens.VictoryScreenTest
```

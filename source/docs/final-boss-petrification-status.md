# Stage 1 Wave 2 petrification status integration

This integration connects the Boss's existing petrification request events to the player's shared
`StatusEffectsControllerComponent`. It builds on Boss `6d6a76e` and main `4cebf67`, including the
Status Effects and weapon changes in main.

## Behaviour

- The flashing red warning records the player's position once and applies no penalty while active.
- Only the player's position at warning expiry decides the hit. Leaving its radius before that
  moment avoids the penalty; returning to the old position after resolution does not trigger it.
- On a hit, movement speed is multiplied by `petrificationSlowMultiplier` for
  `petrificationSlowDuration` seconds. Current defaults are 0.5 and 2 seconds.
- A new warning waits until any previous petrification effect has expired, even if the cooldown
  has already elapsed. This prevents the previous penalty from overlapping the next red warning.
- Repeated direct effect requests still refresh the duration instead of stacking penalties.
- Expiry, the end of Wave 2, Boss disposal, player death and player disposal remove the penalty.
- Other effects and changes to base speed remain in place. Attack speed, damage and control locks
  are unaffected by this movement penalty.
- The existing ring under the player is visible only while the actual status effect is active.
- While petrified, the player's sprite also alternates between a red tint and its normal appearance
  every 250 milliseconds. The tint uses the shared effect clock and stops on expiry or removal.
- Warning duration (0.5 seconds), radius (1.2) and penalty duration (2 seconds) are unchanged.
  The cooldown after warning resolution is now 2.5 seconds, for about 3 seconds between warning
  starts. A successful hit leaves about 0.5 seconds of recovery after the penalty ends; a miss
  still waits the full cooldown. Longer effects also hold the next warning until they expire.

## Components

`PlayerFactory` installs `PlayerPetrificationComponent`. The adapter listens for
`PETRIFICATION_EFFECT_REQUESTED` and `PETRIFICATION_EFFECT_CLEAR_REQUESTED`, retaining only the
effect it owns. It notifies the existing stat UI on application and removal.

`PetrificationEffect` extends the team's `TimedStatusEffect` and answers
`Stat.MOVEMENT_SPEED` through the controller's multiplier interface. The controller owns the clock,
expiry queries and removal lifecycle. No base-speed subtraction or snapshot restoration is needed.
The generic `Slow` class currently modifies raw speed and has no early-removal restoration, so this
Boss effect uses the shared multiplier interface rather than changing that class's behaviour.

The main-to-Boss merge preserves Stage 3 jumping and freeze/dialogue control locks in
`PlayerActions`, and adds the same lock check to the new heavy-attack entry point. Cancellation of
knife combo strikes already started before a freeze remains a separate weapon integration item.

## Validation

For the initial integration, all 222 core main sources and 129 test sources compiled with JDK 21.
The full core JUnit suite
passed: 755 tests, zero failures or skips. This includes ten new petrification integration tests
and a heavy-attack control-lock regression test. Google Java Format 1.28.0 reported no changes
for the core Java sources and tests.

For the follow-up warning timing fix, 18 petrification tests passed with JDK 21/JUnit,
including four additional tests covering harmless warning time, previous-penalty expiry,
leaving before resolution and returning before or after resolution. The overlap test failed
against the original implementation and passed after the fix. The changed Java files passed
Google Java Format 1.28.0. The full core suite was not rerun for this follow-up.

The local Gradle offline attempt stopped during configuration because the Sonar Gradle plugin
was not cached; it did not run the Gradle build tasks. Run `./gradlew build` in `source` locally,
then play through Wave 2 to check the feel of the existing warning and cooldown settings.

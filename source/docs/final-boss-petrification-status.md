# Stage 1 Wave 2 petrification status integration

This integration connects the Boss's existing petrification request events to the player's shared
`StatusEffectsControllerComponent`. It builds on Boss `6d6a76e` and main `4cebf67`, including the
Status Effects and weapon changes in main.

## Behaviour

- The warning still records the player's position once; leaving its radius avoids the penalty.
- On a hit, movement speed is multiplied by `petrificationSlowMultiplier` for
  `petrificationSlowDuration` seconds. Current defaults are 0.5 and 2 seconds.
- Repeated hits replace this one effect and restart its duration instead of stacking penalties.
- Expiry, the end of Wave 2, Boss disposal, player death and player disposal remove the penalty.
- Other effects and changes to base speed remain in place. Attack speed, damage and control locks
  are unaffected by this movement penalty.
- The existing ring under the player is visible only while the actual status effect is active.
- Existing warning timing (0.5 seconds), radius (1.2) and cooldown (0.5 seconds) are unchanged.

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

All 222 core main sources and 129 test sources compiled with JDK 21. The full core JUnit suite
passed: 755 tests, zero failures or skips. This includes ten new petrification integration tests
and a heavy-attack control-lock regression test. Google Java Format 1.28.0 reported no changes
for the core Java sources and tests.

The local Gradle offline attempt stopped during configuration because the Sonar Gradle plugin
was not cached; it did not run the Gradle build tasks. Run `./gradlew build` in `source` locally,
then play through Wave 2 to check the feel of the existing warning and cooldown settings.

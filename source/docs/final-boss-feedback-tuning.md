# Final Boss feedback and pacing

The statue timing table below records the previous feedback patch. The later tornado-finale update
supersedes its statue cadence and adds clustering; see `final-boss-tornado-finale.md` for current tuning.

This change follows the petrification warning fix. It keeps warning-time targeting and the shared
status-effect integration, and adjusts three areas following local playtest feedback.

## Stage 1 Wave 2: breathing room and a visible penalty

`FinalBossStageOneConfig.petrificationCooldown` is now 2.5 seconds, measured after the previous
warning resolves. The warning itself is still 0.5 seconds, so successive warnings start about
3 seconds apart. The first warning still starts when Wave 2 begins.

The player is slowed to 50% speed for 2 seconds only if inside the fixed circle at warning expiry.
The new default leaves about 0.5 seconds at normal speed before the next warning. A longer active
petrification effect still holds the next warning until that penalty expires.

The actual petrification effect also tints the player red for 250 ms, then returns to normal for
250 ms, repeating for the penalty's lifetime. It does not tint the player during the warning.
Expiry, stage cleanup, death and disposal remove this appearance with the effect.

## Stage 1 Wave 1: feedback for actual health loss

`PlayerDamageFlashComponent` listens for decreases in health and requests a 600 ms red blink.
This covers the Boss proximity damage and other sources that actually reduce player health.
Healing and fully blocked damage do not start a flash. A later hit refreshes the brief cue.

Both cues use the existing status-effect tint rendering. They leave movement, damage and attack
values unchanged, and preserve other effects' transparency and the sprite batch's original colour.
The existing underfoot petrification ring is retained as an additional status indicator.

## Stage 3 Wave 2: a faster rhythm as statues break

Statues take turns preparing their slam. The encounter-wide target interval between attacks is:

| Living statues | Interval between successive attack starts |
| --- | --- |
| 5 | 3.0 seconds |
| 4 | 2.8 seconds |
| 3 | 2.6 seconds |
| 2 | 2.4 seconds |
| 1 | 2.2 seconds |

These are intervals across the encounter, not one statue's personal cooldown. Every attack retains
the 0.6-second warning and 0.9-second jump. At the final pace, the last statue has about 0.7 seconds
on the ground after landing before its next warning begins. Frame timing, a statue breaking and
hit-triggered evades can delay or cancel an individual attack.

Changing the living count does not speed up an already visible warning or airborne jump. Attack
starts remain separated so several statues do not suddenly slam together after one breaks.

## Manual playtest

- Stage 1 Wave 1: approach the Boss; each actual health reduction should produce a red blink.
  Leave the damage radius and check that the brief feedback ends.
- Stage 1 Wave 2: dodge the warning and confirm there is no penalty blink; remain inside on a later
  warning and confirm that red blinking lasts for the actual slow, then stops as movement recovers.
- Stage 3 Wave 2: reduce the statues from five to one. Confirm a progressively tighter slam rhythm,
  readable warnings and continued opportunity to jump over shockwaves and attack between slams.

## Automated validation

All 224 core main sources and 131 test sources compiled with JDK 21. The full core JUnit suite
passed: 780 tests, zero failures or skips. The run used the project's existing Byte Buddy
experimental flag and a startup agent, with dependencies cached locally. Tests cover the actual
animation draw tint, shield/invulnerability, effect expiry and cleanup, the longer petrification
cooldown, repeated landing intervals at all five living-statue counts and long-frame protection
against overlapping statue attacks. All changed Java files passed Google Java Format 1.28.0.

This is direct Java/JUnit validation, not a complete Gradle build. The environment lacks the cached
Sonar Gradle plugin needed for the project's offline build. Run `./gradlew build` locally.
A headless test run cannot judge final visual readability or difficulty in the running game.

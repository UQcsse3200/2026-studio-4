# Burn Vial enemy visual feedback

## Behavior and reuse

A successful fire bomb use ignites the existing StrategyOnscreen target set. Each affected enemy
shows three small flames anchored to its current position and scale. They animate at 100 ms per
frame using the two bright-core frames of Team 4's existing `images/traps/fire-start.png`. No trap
code, enemy factories, status controller, or original fire assets are changed.

`VialBurning` extends the existing `Burning`, calls its original update, and compares health directly
before and after that call. Actual burn HP loss starts a 180 ms warm additive sprite glow and flame
surge. Ordinary attacks and fully blocked burn ticks do not start this pulse. The damage value,
cooldown, duration, mitigation, targeting, and independent damage stacks remain the original item
behavior. This is appearance around an existing damage implementation, not a second DOT system.

The existing BurnVialFeedbackComponent draws one group per enemy even with multiple stacks. It
checks the actual controller's membership and expiry before drawing, and removes groups when the
status ends, is cleared, the enemy dies, or its controller is disposed. Its layer places attached
flames after character sprites and before Scene2D. The existing player-head feedback is retained.
Owned textures are released when the player feedback component is disposed.

The existing Team 5 screen-flash hook now accepts the fire bomb's warm orange colour and 65% peak
opacity. The rise/fall are still 100/250 ms. It runs once per use, rather than once per target.
FreezeBomb's existing `startWhiteFlash()` resets the colour to white and the peak to 100%.

## Contribution preservation

Aarash Mehta's eight original commits, culminating in `1ff83e8070ec76f3fc0cbfe7661451364b513fa7`,
remain ancestors of this task and consumable-items. No history is rebased, squashed or rewritten.
His Burn Vial class, catalog entry, icon, command, original feedback silhouette, original test
coverage and design brief remain. BurnVial.use receives only the visual subclass/tracking hookup
and changes the flash entry point. The original BurnVialTest changes only its expected flash
method. The new visual class and new coverage are integration additions, not a replacement of
his contribution.

## Verification and limits (2026-10-07)

- Regression coverage includes real damage timing, non-burn damage, blocked burns, movement,
  stacked uses, all visible targets, offscreen exclusion, one flash per use, rejected reuse,
  pulse growth/fade, clearing, death, expiry, disposal and batch colour restoration.
- Existing four-slot input/inventory, player-head feedback and freeze flash tests remain.
- Run `./gradlew core:test spotlessCheck desktop:classes` for full verification.
- Real LibGDX/OpenGL isolated render check captured cast, steady fire, actual 2 HP burn pulse and
  expiry. Its temporary harness and PNGs live outside the repository in
  `/Users/yuri/CSSE3200/output/burn-vial-vfx-rendercheck/`. This verifies actual drawing but is not
  a full gameplay screenshot or a complete level playtest. Runtime exited normally; LWJGL emitted
  its existing unsupported JNI-version warning.
- SonarCloud baseline query for consumable-items head `62e2ff62` returned no branch analysis (404);
  the issues query returned an empty list. Without a matching analysis that does not establish a
  passing gate or zero warnings. This local change has no new remote scan and no source upload.
- No remote writes and no integration into items/main are part of this task.

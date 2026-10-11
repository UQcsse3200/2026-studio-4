# Team 5 HUD and Magnet integration — 7 October 2026

## Sources and scope

- Team 5 baseline: `items@7aba55d93f62086d6ff1ab8a3d61d42f64dea3d8` (clean worktree).
- Existing consumables: `consumable-items@e456c69b` with the Burn Vial and enemy flame feedback.
- Jeremy: `origin/task/197-consumable-hud@bc08f9e6`, four-slot inventory equipment,
  drag/swap actions, Charm equip/unequip, tooltips, HUD layering, and fountain/rocking drops.
- Sumith: `origin/task/197-magnet-potion@35d3172b`, based on the existing consumables head.
- Integration work: `task/197-hud-magnet-integration`, created from `items` and fast-forwarded
  to the consumable head before merging the two contributor branches. Original commits and authors
  are retained. This task does not integrate unrelated latest-main changes.

The user requested these existing Team 5 implementations. Jeremy's inventory changes are retained
as supplied; no new visual direction or changes to other teams' implementations are introduced.
Shared screen/asset changes add only Magnet's command and texture registration. No loot table,
shop, Wiki, remote branch, PR, issue or review request is changed by this integration.

## Compatibility adaptation

Jeremy's fountain drops start with physical outward momentum. Sumith's magnet moves an item towards
its owner using the existing position event, which also synchronises its body. Without adaptation,
that body's original velocity moves it again during the next physics update.

The new real-physics regression test failed before the fix: after the 0.3-unit magnetic pull,
the item drifted again at the next physics step. `MagnetEffect` now clears the pulled item's
linear velocity before moving it, so magnetic movement takes precedence. This change is confined
to Sumith's Team 5 effect; the physics engine and Jeremy's visual drop animation are unchanged.

The magnet still lasts 10 seconds, pulls Gold/consumables within 4 world units at 6 units/second,
collects within 0.4 units, and leaves Charms alone. It uses the existing inventory and status-effect
lifecycle. Its stock uses the existing four slots; excess item types remain in the backpack and
can be equipped through Jeremy's inventory interactions. Re-use refreshes one effect.

The original brief claimed a Magnet HUD countdown ring. The current ring supports Speed only.
The brief is corrected to describe the available remaining-duration API and existing golden glow;
this integration does not add a new ring.

## Verification

From `source`, with JDK 21:

```sh
./gradlew core:test spotlessCheck desktop:classes --offline --no-daemon
```

- Jeremy integration checkpoint: 1,955 tests, no failures/errors/skips; format and desktop classes pass.
- Combined integration: 1,970 tests across 252 suites, no failures/errors/skips;
  format and desktop classes pass.
- Sumith's 9 tests are retained. Added 5 real entity/physics integration tests and 1 real Scene2D
  HUD test for Magnet's icon/count and fourth-slot Tab/Q use.
- Coverage includes fountain-motion takeover, one-time real-world collection and disposal,
  ignored Charms and distant items, fourth-slot use, fifth-type backpack storage/equipment,
  repeated-use refresh without doubled pull, expiry and explicit status-effect removal.
- Existing Burn Vial, Freeze Bomb, inventory dragging/equipment, HUD layering and drop tests pass.

The full game was not launched during this integration. Headless Scene2D/Box2D tests do not
establish the final visual appearance or frame rate. For windowed playtesting, press F1 and enter
`magnet 3`, use Tab/Q, and test enemy fountain drops and inventory equipment swaps. Magnet is
available through that QA command and item creation; it is not added to enemy drops or a shop.

## SonarCloud boundary

Concrete unresolved-issue lists were checked at the start and end, alongside analysis metadata:

- PR #198: analysed `1c9ad811` on 5 October 2026 at 19:57:53 Brisbane time; gate OK,
  0 unresolved bugs, vulnerabilities or code smells.
- PR #221: analysed `ed8ba209` on 4 October 2026 at 17:37:01 Brisbane time; gate OK,
  0 unresolved bugs, vulnerabilities or code smells.

These are historical heads, not the current contributor or integration heads. No current scan
is available for this local integration, and no source-uploading scan was triggered.
Local integration and verification are complete; the new SonarCloud scan remains unverified.

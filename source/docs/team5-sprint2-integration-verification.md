# Team 5 Sprint 2 integration verification — 2026-09-16

## Scope and revisions

Tested implementation: `96de98e` (the following documentation commit changes no code).

- Previous local items: `5937b52`.
- Integrated main: `521e21b`, including merged PR #161 (final Boss) and #166 (random item factory).
- Remote items incorporated: `03e8986`; no remote items commits were missing before this integration.
- HUD source: `dbb3076`; input source: `71f53ea`; both original histories are retained.
- Production policy: `fc017e7`; original integration: `62c46f1`.

All integration remains local. No remote branch, PR, review, issue or chat was modified.

## Locked interface

Four fixed direct-use keys: 8 Health, 9 Shield, 0 Speed, minus Strength. Weapons retain 1–3 and K; inventory retains I. The user explicitly confirmed the four-key interface in this task. Other teams' acceptance is not implied.

`TypedItem extends Item` preserves the shared item abstraction while `ItemType` identifies Team 5 quantities and requests. Input invokes `useSlot` / `tryUse`, or `useConsumable(ItemType)`; only the effects component removes stock. `itemUsed(ItemType)` is emitted after successful use. The direct-use HUD does not depend on `selectedConsumableChanged`.

## Main compatibility decisions

- Resolved conflicts in EnemyManagerComponent, ItemFactory and ItemFactoryTest.
- Preserved #166's `createRandomDrop` and factory regression test. Its current random pool contains Strength Charm; it does not replace #152's required currency/consumable policy.
- Enemy rewards use EnemyDropPolicy: exactly one Gold drop and at most one consumable. Defaults remain configurable: 5 Gold and 35% consumable probability.
- Preserved #161's `finalBossEncounterCompleted` listener, shared with death processing. A new regression triggers completion and death in both orders and verifies one reward per enemy and one room-clear notification.
- Preserved captured death positions, deferred registration, duplicate-event guards and room disposal cancellation.
- Retained the reviewed main changes, including PlayerFactory's Boss-related player components, alongside Team 5 components.
- The latest HUD remains top-right. A regression creates the real Team 4 hotbar and Team 5 panel together, lays them out at 1920x1080, 1280x720 and 906x600, and verifies vertical separation and the panel's upper viewport boundary.
- Unmerged cross-team feature branches were not imported. Observed open work includes PR #173 enemy scaling, #165 spells, #153 minibosses, #142 popups and #140 invisibility potion.

## Validation

From `source/`:

```sh
./gradlew --offline spotlessApply core:test core:javadoc
./gradlew --offline spotlessCheck
```

Both commands succeeded. **875 tests, 0 failures, 0 errors, 0 skipped**, counted from Gradle JUnit XML. Formatting passes. JavaDoc succeeds with existing documentation warnings; this is not a warning-free claim. JavaDoc was generated successfully earlier in the same integration run and was up-to-date in the final run, whose subsequent code edits were tests only.

Logs: `/tmp/team5-main-compatibility.log`, `/tmp/team5-main-format-check.log`.

The full suite retains real-factory enemy reward → pickup → inventory → HUD → keyboard → effect regressions, fixed-key use, empty/full-health rejection, damage blocking, timed expiry, permanent Charm interaction and room lifecycle coverage.

## JaCoCo from the final full suite

| Class | Lines | Branches |
|---|---:|---:|
| EnemyDropPolicy | 100.0% | 100.0% |
| ItemDropSpec | 100.0% | 100.0% |
| ItemFactory | 78.4% | 100.0% |
| ItemComponent | 96.6% | 83.3% |
| TypedItem | 66.7% | 33.3% |
| ConsumableEffectComponent | 95.7% | 81.6% |
| ConsumableLoadoutComponent | 100.0% | 87.5% |
| Team5CombatHudDisplay | 98.1% | 76.5% |
| EnemyManagerComponent (also owns other teams' spawning) | 47.7% | 51.4% |

Source: `core/build/reports/jacoco/test/jacocoTestReport.xml`. Whole-class figures, not SonarCloud new-code coverage or proof of exhaustive gameplay testing.

## Remaining limits

No full manual gameplay playthrough or visual approval of this merged revision is claimed. Automated Scene2D layout verification does not replace a screenshot or user playtest. The earlier pre-merge game launch only reached MAIN_MENU. Final gameplay evidence and Sprint 2 SAF submission remain separate work.

Original inventory, effects, HUD and input authorship is preserved in Git history. No demo branch was merged and no existing branch was deleted. Codex assisted with compatibility resolution, regression tests and this evidence under Yuezhou's direction.

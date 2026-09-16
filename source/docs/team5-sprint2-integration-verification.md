# Team 5 Sprint 2 integration verification — 2026-09-16

## Scope and revisions

Tested implementation: `a110647` (the following documentation commit changes no code).

- Previous local items: `c30a1d5`; earlier four-key implementation: `5937b52`.
- Integrated local and remote main: `9625e56`, including merged PR #153 (minibosses), #161 (final Boss) and #166 (random item factory). Local main was fast-forwarded from `8ba827c`; its two untracked Sprint 1 documents were preserved.
- Remote items incorporated: `03e8986`; no remote items commits were missing before this integration.
- HUD source: `dbb3076`; input source: `26f7caf` (including `8910780` and `71f53ea`); both original histories are retained.
- Production policy: `fc017e7`; original integration: `62c46f1`.

All integration remains local. No remote branch, PR, review, issue or chat was modified.

## Locked interface

Four fixed direct-use keys: 7 Health, 8 Shield, 9 Speed, 0 Strength. Weapons retain 1–3 and K; inventory retains I. The user explicitly confirmed the four-key interface in this task. Other teams' acceptance is not implied.

`TypedItem extends Item` preserves the shared item abstraction while `ItemType` identifies Team 5 quantities and requests. Input invokes `useSlot` / `tryUse`, or `useConsumable(ItemType)`; only the effects component removes stock. `itemUsed(ItemType)` is emitted after successful use. The direct-use HUD does not depend on `selectedConsumableChanged`.

## Main compatibility decisions

- Resolved conflicts in EnemyManagerComponent, ItemFactory and ItemFactoryTest.
- Preserved #166's `createRandomDrop` and factory regression test. Its current random pool contains Strength Charm; it does not replace #152's required currency/consumable policy.
- Enemy rewards use EnemyDropPolicy: exactly one Gold drop and at most one consumable. Defaults remain configurable: 5 Gold and 35% consumable probability.
- Preserved #161's `finalBossEncounterCompleted` listener, shared with death processing. A new regression triggers completion and death in both orders and verifies one reward per enemy and one room-clear notification.
- Preserved captured death positions, deferred registration, duplicate-event guards and room disposal cancellation.
- Retained the reviewed main changes, including PlayerFactory's Boss-related player components, alongside Team 5 components.
- The latest HUD remains top-right. A regression creates the real Team 4 hotbar and Team 5 panel together, lays them out at 1920x1080, 1280x720 and 906x600, and verifies vertical separation and the panel's upper viewport boundary.
- Incorporated the 31 additional main commits since `521e21b`, including Cerberus phases, snake miniboss, wasp assets and player mist debuffs. The only textual merge conflict in this pass was EnemyManagerComponent imports; Team 5 drop policy imports and all new enemy spawning behavior were retained.
- Added a regression using the actual PlayerCerberusMistDebuffComponent with the fixed-key Speed Potion: entering/leaving mist and potion expiry remove only their own modifiers; two uses consume exactly two potions and restore the original speed.
- Unmerged cross-team feature branches were not imported.

## Validation

From `source/`:

```sh
./gradlew --offline spotlessCheck core:test core:javadoc
```

The command succeeded. **946 tests, 0 failures, 0 errors, 0 skipped**, counted from Gradle JUnit XML. Formatting passes. JavaDoc succeeds with existing documentation warnings; this is not a warning-free claim. JavaDoc was generated successfully earlier in the same integration run and was up-to-date in the final run, whose subsequent code edits were tests only.

Latest validation log: `/tmp/team5-miniboss-main-sync.log`. Earlier compatibility logs remain historical evidence.

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
| EnemyManagerComponent (also owns other teams' spawning) | 44.1% | 48.7% |

Source: `core/build/reports/jacoco/test/jacocoTestReport.xml`. Whole-class figures, not SonarCloud new-code coverage or proof of exhaustive gameplay testing.

## Remaining limits

No full manual gameplay playthrough or visual approval of this merged revision is claimed. Automated Scene2D layout verification does not replace a screenshot or user playtest. The earlier pre-merge game launch only reached MAIN_MENU. Final gameplay evidence and Sprint 2 SAF submission remain separate work.

Original inventory, effects, HUD and input authorship is preserved in Git history. No demo branch was merged and no existing branch was deleted. Codex assisted with compatibility resolution, regression tests and this evidence under Yuezhou's direction.

## DV key alignment — 2026-09-17

Fetched and merged DV’s latest `origin/task/consumable-use-input` at `26f7caf` using a history-preserving merge. Yuezhou requested adopting DV’s 7/8/9/0 keys. Retained the integrated direct-use effect pipeline (rather than reverting to select-then-U); updated HUD labels, integration tests and Wiki draft together. Original commits `8910780`, `71f53ea` and `26f7caf` remain ancestors with unchanged authorship. `./gradlew --offline spotlessCheck core:test` passed: 946 tests, zero failures/errors/skips. Log: `/tmp/team5-dv-key-alignment.log`. This update is local only.

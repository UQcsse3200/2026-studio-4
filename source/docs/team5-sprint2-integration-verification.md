# Team 5 Sprint 2 integration verification — 2026-09-16

## Scope and revisions

Implementation and test revision: `b4ba3af` (the subsequent evidence-only commit does not change code).

- Main baseline: `4cebf67`.
- Latest items incorporated before work: `03e8986`.
- HUD source: `dbb3076`; input source: `71f53ea`; original commits retained by merges.
- Unified consumable implementation: `62c46f1`.
- Production drop policy: `fc017e7`.
- End-to-end drop/pickup/HUD/use regression and current Wiki draft: `b4ba3af`.

All integration is local. No push, PR creation, issue modification, review submission or approval was performed.

## Validation

From the local items worktree, `source/`:

```sh
./gradlew --offline spotlessCheck core:test core:javadoc
```

Result: **BUILD SUCCESSFUL**. **720 tests, 0 failures, 0 errors, 0 skipped**, counted from Gradle JUnit XML. Spotless passed. JavaDoc passed; the project still emits documentation warnings (missing tags/comments and an external link redirect), so this is not a warning-free claim. The previous effects component had an invalid `&` in JavaDoc; its replacement documentation no longer contains that error.

Targeted runs independently passed the consumable, HUD, input, drop-policy and room-lifecycle tests. The final suite includes the real-factory end-to-end test `enemyRewardFlowsThroughPickupHudKeyboardAndEffect` and click-driven reassignment test `changeButtonMakesFourthTypeUsableAndSurvivesUpdates`.

Logs on this machine: `/tmp/team5-final-validation.log`, `/tmp/team5-e2e.log`, `/tmp/team5-integration-tests.log`.

## JaCoCo from final full test suite

| Class | Lines | Branches |
|---|---:|---:|
| EnemyDropPolicy | 100.0% | 100.0% |
| ItemDropSpec | 100.0% | 100.0% |
| ItemFactory | 72.7% | 100.0% |
| ItemComponent | 96.6% | 83.3% |
| TypedItem | 66.7% | 33.3% |
| ConsumableEffectComponent | 95.5% | 77.8% |
| ConsumableLoadoutComponent | 92.0% | 77.8% |
| Team5CombatHudDisplay | 95.8% | 71.4% |
| EnemyManagerComponent (includes other teams' enemy spawning) | 47.2% | 51.4% |

Source: `core/build/reports/jacoco/test/jacocoTestReport.xml`. The HTML report is under `core/build/reports/jacoco/test/html/`. These are whole-class figures, not a claim that all enemy/boss spawning was tested.

## Manual/runtime limits

`desktop:run` launched the game to MAIN_MENU. Computer Use did not expose the Java game application as a controllable app, so no manual visual approval, gameplay recording, or full in-game playthrough is claimed. Scene2D actors, assignment button events, physics pickup fixtures, keyboard handlers, real damage and effect expiry are exercised automatically. The remaining manual check is visual layout alongside other HUD panels and ordinary gameplay at the user's chosen resolution.

## Review of retained work

- Retained Team 5 inventory, assets, typed factory, spin animation, HUD and input history.
- Replaced production demo sequencing instead of merging additional demo branches.
- Preserved main's weapon 1–3, heavy attack K and inventory I input.
- Reused the shared status/damage interfaces; did not implement unrelated Team 1/2/3/4 features.
- Kept separate implementation branches; no old task/demo branches were deleted.
- Three configurable slots with 8/9/0 are the local implementation choice, aligned with Team 4's public proposal. A fourth item type is available via Change, not a fourth slot.
- Five Gold / 35% consumable chance are configurable initial defaults. No unconfirmed boss-specific payout was introduced.

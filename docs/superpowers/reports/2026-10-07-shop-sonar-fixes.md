# Shop PR #259 Sonar findings

The first PR analysis covered `1df11aceeaa55fe6ea868c21fe0ced283ae5123f` at 2026-10-07 12:09:30 UTC. Its quality gate passed, but the concrete unresolved list contained 45 findings: 3 bugs and 42 code smells. These findings concern this PR's Team 5 shop/inventory code and tests; they are separate from main's historical findings.

## Local corrections

| Findings | Correction |
| --- | --- |
| 2 possible null dereferences | Weapon descriptions now return a complete descriptor or reject missing/unsupported product IDs. The purchase adapter's nullable weapon lookup remains unchanged. |
| 1 allegedly constant callback condition | Purchase-result handling rechecks whether the panel is active after synchronous settlement callbacks. Closing/disposal protection is retained. |
| 7 duplicated literals | Extract unchanged event, texture, font and UI text constants. |
| 2 clamp suggestions | Use `Math.clamp` while retaining long arithmetic and the inventory's stake limits. |
| 3 complexity findings | Separate asset loading, UI construction and weapon state refresh into focused methods; flatten casino selection's inactive path. |
| 1 constructor parameter count | Group weapon-panel visual resources in `Assets`, retaining all inputs with seven constructor parameters. |
| 9 nested conditional expressions | Use ordered status, caption and feedback helpers with the same precedence. |
| 1 redundant inactive expression | Remove the duplicate condition only from the private payment refresh reached after the active guard. |
| 2 method-reference suggestions | Use typed references for inventory event collection. |
| 13 exception-assertion findings | Construct fixtures before `assertThrows` so only the operation under test can satisfy each assertion. |
| 4 missing override annotations | Annotate existing test callbacks. |

No quality rule, suppression, remote issue status, gameplay balance, transaction ordering or combat implementation was changed. The UI keeps its actor order, sizes, text, focus transitions and texture lifecycle. Existing tests for inventory observers, gambling settlement, actual weapon damage and callback-driven closure/disposal remain in place.

## Validation

The new missing/unsupported-description contract test failed with the old nullable implementation and passes with the correction. A second test verifies complete descriptors for the actual upgrade catalog.

`./gradlew spotlessApply core:test spotlessCheck desktop:classes --rerun-tasks` passed with **2165 tests, zero failures, errors or skips**. Formatting, desktop compilation and `git diff --check` passed. Independent review found no behavioral or layout regression in the extracted shop helpers.

These are local corrections against all 45 reported findings. A new SonarCloud scan of the fix commit has not yet run; the remote gate and unresolved count still describe `1df11ace` until an authorized push produces a new analysis. No claim of remotely resolved findings is made here.

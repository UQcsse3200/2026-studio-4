# Shared drop pool adaptation — 2026-09-17

Requested by Yuezhou following Team 4's Discord feedback. Based on local items `0c7f3b0` (including the Charm restoration and main `9182adb`).

EnemyManager now invokes Team 4's existing ItemFactory.createRandomDrop path once per valid defeat. DropTypes registers Strength Charm, Health Potion, Shield, Speed Potion, Strength Potion and a 5-Gold stack. All six entries use the existing uniform selection rule (1/6 each). Every supplier produces a fresh Item; createItem retains shared entity setup. TypedItem extends the shared Item abstraction and routes pickup to real inventory quantities.

This intentionally replaces the previous fixed 5 Gold plus 35% consumable reward and extra Charm draw. The old EnemyDropPolicy and its obsolete tests are removed. Unused per-type entity helper methods from the reviewer screenshot are removed; explicit createDrop APIs remain for caller-selected drops and existing consumers/tests.

Duplicate defeat guards, captured positions, queued spawning, room-disposal cancellation and item disposal are retained. No HUD, keyboard mapping, status-effect behaviour or inventory UI is changed.

Validation covers every pool entry, distinct item instances, Gold stack quantity, enemy death → shared factory → Charm/Gold/potion pickup → HUD → use, duplicate defeat events and disposal. Full suite: 989 tests, 0 failures, 0 errors, 0 skipped. Spotless and desktop compilation passed. Logs: `/tmp/team5-shared-pool-validation.log` and `/tmp/team5-shared-pool-format.log`.

Local adaptation only; publication and cross-team approval are not claimed.

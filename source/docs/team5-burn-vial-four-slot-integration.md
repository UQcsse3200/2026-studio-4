# Burn Vial four-slot integration and contribution record

Integration date: October 7, 2026 (Australia/Brisbane).

## Original contribution

Aarash Mehta (Team 5) implemented Burn Vial for #197. This integration merges his branch with
its original commits and author metadata intact. It does not squash or re-author those commits.

| Original commit | Aarash's contribution |
| --- | --- |
| `114b78b7` | Burn Vial item and reuse of the existing Burning status effect |
| `f4149db7` | Item design brief |
| `92996f15` | Burn Vial test timing assertions |
| `cfbbd678` | Immediate-use `burnvial` QA command and tests |
| `0200bc16` | Player flame feedback and tests |
| `73afa478` | Original hotbar integration and item icon |
| `b0bb64bc` | Design brief updated to the implemented state |
| `1ff83e80` | Burn Vial hotbar texture region registration |

The integrated item class, item ID/catalog registration, image, player feedback component,
resource preload, player/terminal wiring and original Burn Vial tests retain Aarash's work.
His original sixth-slot implementation remains visible in its original commit, while the
merge resolution preserves the target branch's existing four-slot design. Formatting changes
to incoming files do not change their behavior or authorship history.

## Integration contribution

Yuezhou Wang directed this integration, with OpenAI Codex assistance. A separate integration
commit records the additional command mode, four-slot/input/HUD regression tests, deterministic
tests of the existing Burning cadence and documentation corrections. These adaptations do not
reassign authorship of Aarash's original feature.

- Burn Vial uses the same pickup-order inventory slots as other consumables. There are still
  four physical slots; adding an item type does not add a fifth or sixth slot.
- Existing Tab/Q selection and use, inventory notifications, generic catalog-driven icons and
  successful-use feedback are reused. The Burn Vial class is retained rather than rewritten.
- `burnvial` retains the original grant-and-immediately-request-use behavior. `burnvial give`
  grants one vial without consuming it, allowing the normal inventory/HUD/input path to be tested.
- The command's return value reports granting/request dispatch, not a guarantee that the use
  succeeded. Normal consumable validation still determines successful use.
- Existing default drop weights and shop availability are unchanged. This integration does not
  choose new loot probabilities, boss exemptions or alternative artwork.
- Aarash's existing placeholder icon and player flame feedback remain for this integration;
  retaining them is not a new visual design approval. No shared Burning implementation or
  status-effect defaults are modified.

## Gameplay and verification boundaries

The item configures Burning with 2 damage, a 1000ms cooldown and a 6000ms duration. Existing
Burning uses a strict cooldown comparison and applies damage before checking expiry. Its
clock is created internally. The new tests control that clock externally through Mockito
construction interception without changing production status-effect code.

With updates at 1001ms increments, one vial deals six ticks of 2 damage, and two independently
applied stacks deal six ticks of 4 damage. After the expiration update the controller removes
both stacks and further updates deal no damage. This is evidence for that update cadence,
not a promise of an exact total under every frame schedule. The prior design brief's claim
of three ticks / six total damage has been corrected.

New tests cover real inventory pickup, slot identity, Tab/Q use, icon/count updates, consumption
of exactly one item, no-stock repeated input, failed use without camera/entity service, visible
versus offscreen targets, independent burn stacks, expiration and four-slot wraparound. Aarash's
original item, command and feedback tests remain. Test logs and overall validation are recorded
in the local integration completion report. No new manual playthrough is claimed by this record.

SonarCloud has no analysis for the original Burn Vial or consumable-items branch heads at the
time of integration. Local tests and compilation cannot establish a remote Quality Gate result
or zero warnings. No remote scan or repository write is part of this local integration.

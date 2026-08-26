# AI use summary — issue #9

**Issue:** [#9](https://github.com/UQcsse3200/2026-studio-4/issues/9)  
**Pull request:** [#25 Add weapon stats, WeaponComponent, hitbox factory, and follow-owner.](https://github.com/UQcsse3200/2026-studio-4/pull/25)  
**Branch:** `task/9-weapon-base`  
**Author:** Benjamin Chau (`chaubenn`)  

This discloses generative AI use on issue #9 / PR #25, as required by the CSSE3200 course profile.

The session log this summary refers to is:

[cursor_branching_strategy_discussion.md](cursor_branching_strategy_discussion.md)

The file is a 1:1 export of `cursor_branching_strategy_discussion.md`, the conversation I had with Cursor to fulfill this ticket.

## How the tool was used

Cursor was a coding assistant, using mainly the model Cursor Grok 4.6 (high). I specified the design, asked about intrusion and existing engine constraints, reviewed every change, and ran git, tests, and PR updates myself. AI output was not accepted blindly.

Examples of review / challenge that changed the result:

- Dropped the `EntityService.update()` snapshot after asking how intrusive it was
- Scoped this ticket to the shared foundation + follow-owner; knife/sword/bow and Space wiring stayed later `task/` tickets
- Reverted the `Entity.dispose()` edit after the review that `Entity` must not be modified; hitbox expiry moved into `LifetimeComponent`
- After a teammate asked whether weapon `damage` was standalone or a multiplier, I walked the attack chain and chose **B**: hitbox damage = `round(player.baseAttack * weapon.multiplier)`

The assistant drafted Java types, unit tests, JavaDoc, wiki wording, and Sonar/test follow-ups against existing engine patterns (`CombatStatsComponent`, `TouchAttackComponent`, factory style).

## What is my work vs AI-assisted

| Area | Who decided / did it |
| --- | --- |
| Shared stats + template-method `attack()`, hitbox factory, melee follow-owner | My design |
| No Space / `PlayerActions` wiring in this PR; later `task/` tickets | My scope |
| Do not modify `Entity`; expire hitboxes in `LifetimeComponent` | Review feedback I agreed with; I had the change reverted |
| Hitbox damage = `round(player.baseAttack * weapon.multiplier)` | My choice after the design options (standalone vs multiplier vs additive) |
| Java types, tests, JavaDoc, wiki draft, Sonar/test cleanups | Assistant drafted; I reviewed and kept or changed |
| Git commands, commits, PR body on GitHub | I ran / edited |
| Comments to teammates on the PR thread | Written by me after reviewing the facts |

## Parts of the assessment that used AI

- `WeaponStatsComponent`, `WeaponComponent`, `FollowComponent`, `LifetimeComponent`
- `HitboxFactory` / `HitboxSpec`
- `PhysicsLayer.WEAPON`
- Unit tests next to those types
- JavaDoc / package docs
- Wiki page [Weapons](https://github.com/UQcsse3200/2026-studio-4/wiki/Weapons)
- This summary (drafted with the same assistant, then edited)

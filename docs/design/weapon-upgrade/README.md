# Weapon upgrade purchase page — design review

Status: two reviewed visual concepts ready; Yuezhou Wang’s selection and game implementation are pending.

Yuezhou Wang requested an isolated local task branch and immediate visual design on 2026-10-07. The task branch `task/weapon-upgrade-page` starts at clean local `items` commit `7aba55d93f62086d6ff1ab8a3d61d42f64dea3d8`. No main merge or shop branch integration is part of this design step.

## Player outcome

At the merchant, choose one of the existing weapons, understand the light-attack bonus and newly unlocked heavy attack, pay a one-time gold price and see the purchased upgrade persist for the run. Existing Team 3 combat behavior remains the source of truth. This page supplies the missing purchase presentation; the mockups do not implement a real transaction.

## Confirmed contract

| Weapon | Light attack after upgrading | Heavy attack unlocked on K |
| --- | --- | --- |
| Sword | +20% damage | 360-degree sweep; 1.35x base weapon damage; 2x cooldown |
| Knife | +20% damage | Two 0.6x slashes, then 1.2x finishing stab; 3x cooldown |
| Bow / ranged weapon | +20% damage | Three projectiles, centre and +/-15 degrees; base damage per projectile; 2x cooldown |

- Each weapon has a boolean upgrade state, not levels or an upgrade tree.
- J is light attack; K is heavy attack. Heavy attacks are unavailable before upgrading.
- The current Bow component uses `throwing_knife.png` and `throwing_knife_upgraded.png`; mockup wording does not rename the game item.
- The local items baseline attaches all three weapon components and starts with Sword selected. Do not infer new weapon ownership rules from visual concepts.
- Purchase price is not yet configured. A 60-gold price and 90-gold starting balance in the concepts are illustrative values only, selected to demonstrate a successful purchase followed by insufficient funds. The game baseline starts with 50 gold.
- `setUpgraded(..., true)` is idempotent and returns success for an already-upgraded weapon. A later real transaction must reject repeat purchase before charging.
- Current `shop` work provides consumable purchase and session UI on another active task. Read only; do not merge or modify that task as part of visual design.

## Scope and ownership

This local step adds design artifacts only. Combat upgrades were delivered by Team 3 in PR #151 and #177; merchant NPC base by Wouter in #216. The earlier investigation found no ordinary gameplay purchase caller for weapon upgrades. Discord's 2026-09-25 Team 3 draft assigns weapon-upgrade Merchant to Wouter. On 2026-10-07, the visible Team 3 update confirms dialogue/cutscenes #231 merged; no new upgrade purchase delivery was visible in that channel.

Later implementation should reuse `WeaponUpgradeComponent`, inventory gold and the existing shop extension boundary, not rewrite weapons. Changes to Team 3 NPC configuration or shared integration files need their precise scope established before modification under the project AGENTS.md.

## Review checks

- Show real base/upgraded sprites, clear selection, price and balance.
- Distinguish ready, insufficient funds, already upgraded and success.
- Keep upgraded status visible after the success message ends.
- Display the actual K ability; do not invent increased ordinary hitbox size or permanent multi-run progression.
- Preserve merchant visual continuity, readable English and Leave/Esc.
- Fit 1280x800 and 960x600, keeping action and close controls available.

## Evidence boundary

Previous read-only investigation checked SonarCloud issue lists for PR #151 (head `2321d562`, analysis 2026-09-16 03:01:17 UTC), #177 (`1b0c51f1`, 2026-09-17 03:32:28 UTC) and #216 (`5c00cc5a`, 2026-10-06 03:50:30 UTC): gate OK, 0 unresolved bugs/vulnerabilities/code smells for each. Those historical PR scans do not validate this local design branch or current main. No new scan or production-code test is claimed for these design artifacts.

## Delivered candidates and verification

- [A: selected weapon comparison](concepts/candidate-a.html), Gemini initial design revised by GPT-6.1 after the 240-second Gemini timeout.
- [B: three-weapon catalogue](concepts/candidate-b.html), GPT-6.1 fallback design; recommended for comparing the three fixed upgrades.
- [Design notes](concepts/DESIGN.md), with sample economy and asset mapping.
- Both rendered at 1280x800 and 960x600 with no script errors, broken images or page overflow.
- Main agent exercised both prototypes through the browser: select weapon, buy once (90 to 30 gold), persistent upgraded state and disabled repeat purchase. B additionally verified insufficient funds and close/reopen state retention. This is mock interaction verification, not game runtime validation.
- Source, references and preview PNGs are saved together for portable local review. The original Gemini stdout/stderr and permissions cleanup evidence remain under /Users/yuri/CSSE3200/output/weapon-upgrade-design-20261007/logs and are not committed.

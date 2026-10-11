# Weapon upgrade purchase concepts

These are local visual prototypes, awaiting Yuezhou Wang's selection. No game code has been implemented. Sample economy: each upgrade costs 60 gold; the initial purse is 90, so the first purchase leaves 30. These values are illustrative, not a balancing decision.

## A — Selected weapon comparison
Gemini produced the initial A HTML before the CLI timed out. GPT-6.1 reviewed and revised it: existing merchant identity, actual gold art, corrected weapon names, dynamic shortfalls, semantic disabled buttons, keyboard purchase isolation, close/reopen interaction and compact 960×600 layout. It pairs a vertical weapon selector with a large base/upgraded comparison and full selected attack detail. Best for inspecting one upgrade closely.

## B — Workshop catalogue
Designed by GPT-6.1 after Gemini timeout. Three parchment cards show all weapons, base-to-upgraded art, +20% light damage and distinct K abilities together. A shared bottom selection strip contains exact selected attack details and a single purchase action. This is my recommendation: only three products exist, so a simultaneous comparison gives a quicker choice and keeps affordability visible on every card.

## Gameplay and assets
Both use existing 16×16 weapon sprites with nearest-neighbor scaling, current merchant crop x64/y192/32×32 from rogues.png, and real gold_coin_pixel.png. Wood, parchment, brass borders and restrained green purchasing continue the approved consumable shop. Historical gameplay behind B is a dimmed style reference, not a current game capture.

Sword: +20% light damage; K 360° sweep at 1.35× base damage, heavy cooldown 2× normal. Knife: +20% light damage; K two slashes at 0.6× each and a 1.2× finishing stab, cooldown 3× normal. Throwing Knife: +20% light damage; K three projectiles in ±15° fan, each at base damage, cooldown 2× normal. “Throwing Knife” maps to the code's BowWeaponComponent and existing throwing_knife sprites. This player-facing naming needs confirmation before product implementation.

No further upgrade levels, percentage completion, attack-speed improvement, materials, elemental effects or progression between runs. Selection does not auto-equip. All three are owned in sample state. For an unowned weapon, exclude its purchase offer or show “Weapon not owned” with disabled action; upgrading must never unlock ownership implicitly.

## Interaction and states
Click a weapon or press 1/2/3. U purchases the selected upgrade if available. Tab focus is visible; focused controls use their normal keyboard activation. Escape or Leave closes the prototype. Both expose ?state=poor (30 gold) and ?state=upgraded (Sword upgraded, 30 gold), plus ?gold=NUMBER. B also supports ?weapon=knife or ?weapon=ranged.

A successful upgrade updates purse 90→30 and leaves the selected weapon persistently UPGRADED with K unlocked. Other cards show a 30-gold shortfall. Upgraded and insufficient-funds buttons are disabled. Upgraded weapons cannot be repurchased. Close remains visible at 1280×800 and 960×600. Reopening either candidate preserves mock state. Reload resets either mockup. No gameplay exists behind either HTML modal.

## Provenance and limits
A: Gemini initial visual design + GPT-6.1 corrections. B: GPT-6.1 visual design. Gemini's timeout logs remain in logs/. Runtime rendering and final visual review are performed by the main agent; these prototypes do not establish actual game purchase atomicity or combat correctness.

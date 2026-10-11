# Weapon Upgrade Page — visual design brief

The user Yuezhou Wang urgently requests visual designs for the missing weapon-upgrade purchase page in a pixel-art dungeon game. Create TWO polished candidate visuals now, for the user to choose. This is visual design only, not product implementation. Do not modify repository source, NPC config, wiki, branches or remote state.

## Workspace and deliverables
- Repository reference: /Users/yuri/CSSE3200/worktrees/weapon-upgrade-page (new task/weapon-upgrade-page branch from clean local items 7aba55d9).
- Write only to /Users/yuri/CSSE3200/output/weapon-upgrade-design-20261007/concepts.
- Deliver candidate-a.html, candidate-b.html and DESIGN.md. Self-contained HTML with inline CSS/JS, no packages, network, fonts download or commands. Use relative paths ../references/ for provided images. Main agent will render PNGs and review. Actual visual composition is your responsibility as the designer.
- Target 1280x800 window; fit 960x600 without losing purchase/close controls. Screenshot composition itself should contain no design toolbar or developer labels. An optional separate ?state=poor or ?state=upgraded parameter can present states, or keyboard controls documented in DESIGN.md. A simple clickable prototype is welcome; it remains a mockup with sample data.

## References that you must inspect
../references/approved-consumable-shop.png is the user's selected previous shop concept: warm dark wood, parchment, pixel sprites, restrained gold trim. It establishes visual continuity, not a requirement to copy all content.
../references/historical-gameplay.png is a historical actual game screenshot, for game/UI style and scale, not a claim of current build.
../references/weapons/{sword.png,sword_upgraded.png,knife.png,knife_upgraded.png,throwing_knife.png,throwing_knife_upgraded.png} are real current weapon sprites. Use nearest-neighbor scaling. Do not replace them with emoji or invent weapons. The code calls ranged weapon Bow, but actual art is a throwing knife: player-facing mockup may say Throwing Knife / Ranged; note mapping to BowWeaponComponent in DESIGN.md.
../references/rogues.png and shopkeeper.atlas provide the current merchant, region default x64 y192 width32 height32 on 224x224. No new NPC or art asset is required.

## Agreed purpose / assumptions
Player reaches this page from the existing merchant/shop flow, chooses a weapon, sees what the one-time upgrade changes, spends inventory gold and immediately understands the unlocked attack. This page is Team 5 purchase/presentation work building on Team 3's existing combat upgrade API. No new upgrade combat logic or multi-level skill tree.
Use English game UI, concise player-friendly wording. Art direction and layout are yours. Two genuinely different usable compositions in the same established style, e.g. one can emphasize selected-weapon before/after comparison, another an at-a-glance workshop catalogue. Pick your own visual treatment and recommend one in notes. Avoid dense dashboards, arbitrary rarity/stat bars, futuristic/glass UI, giant empty backgrounds, excessive paragraphs, and developer terminology.

## Exact gameplay contract
Three weapons, each either BASE or UPGRADED; once per run, no further tiers or repeat purchases.
- Sword: light attack +20% damage; unlock K heavy attack, 360-degree sweep, 1.35x base damage, twice normal cooldown.
- Knife: light attack +20% damage; unlock K heavy attack, two slashes (0.6x each) followed by finishing stab (1.2x); triple normal cooldown.
- Ranged / Bow / Throwing Knife: light attack +20% damage; unlock K heavy attack, three projectiles in a +/-15-degree spread, each at base shot damage; twice normal cooldown.
J is light attack, K is heavy. Do not claim the ordinary hitbox grows, permanent progression across runs, improved attack speed, new elemental effects, or extra upgrade levels.
Show real base and upgraded sprites, clear new ability, price, current gold, action, current upgrade status. An owned/equipped tag can be shown as SAMPLE context but don't imply purchase auto-equips. Define treatment for unowned weapons in notes; preferably exclude/disable upgrade action if not owned and clearly explain it.
Prices are DESIGN SAMPLE VALUES ONLY (no balancing decision). Use all three at 60 gold, purse 90 gold initially, enabling a 90 -> 30 successful purchase and insufficient-funds state for the remaining upgrades. State in notes/caption outside product UI that values are illustrative, no technical disclaimer inside game UI.
Include ready-to-upgrade, selected/focused, upgraded (non-purchasable), insufficient gold (need 30 more if balance30/price60), immediate purchase success. Show a plausible page initial state, and provide mock interaction to select weapons and buy. Upgrade feedback must show persistent UPGRADED and unlock K, not only a disappearing toast. Esc/close affordance visible, keyboard/mouse focus clear, no gameplay input behind modal.

## Execution
Time is important: around four minutes for Gemini. Write actual complete files early. Do not spend time on a large plan. No shell needed; only read images/files and write the 3 deliverables. Be truthful about tool errors and missing deliverables. Visual approval remains with Yuezhou Wang after main-agent review.

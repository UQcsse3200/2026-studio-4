# Longbao dragon assets

Source: https://github.com/HashZard/codex-longbao-pet
Author: HashZard. Copyright (c) 2026 HashZard.
License: MIT; the complete original notice is in LICENSE.txt.
The original author states that artwork and animations were created with AI-assisted image generation and human-directed QA.

Adaptation: lossless decoded-pixel conversion from WebP to PNG and generation of a libGDX atlas. No artwork was redrawn. Original 192 x 208 frame cells and transparent padding are preserved.

Animations: idle (6), moveRight (8), moveLeft (8), wave (4), jump (5), collapse (8), waiting (6), working (6), review (6).
The collapse sequence is the original failed/blocked action, not an authored death animation.
look contains 16 directional poses, not an attack animation. default aliases the first idle frame for scaleEntity().
Unused cells and the additional neutral pose are not included in the animation sequences.
preview.png is a labelled reference image, not a game texture.

# Final Boss Stage 1 — Visual and Gameplay Integration

This document describes the current implementation of Final Boss (Grandpa) Stage 1, including gameplay flow, visual feedback, configurable values, and cross-team integration.

Stage 1 is part of Feature #93 — Final Boss (Grandpa).

## Stage 1 Flow

The current Stage 1 state flow is:

```text
INTRO
  ↓
TRANSFORMING
  ↓
SUMMONING_ONE
  ↓
WAVE_ONE
  ↓
BREAK_WINDOW
  ↓
SUMMONING_TWO
  ↓
WAVE_TWO
  ↓
COMPLETE
  ↓
Stage 2
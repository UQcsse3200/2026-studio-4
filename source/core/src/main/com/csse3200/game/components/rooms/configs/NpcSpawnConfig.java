package com.csse3200.game.components.rooms.configs;

/**
 * Places a friendly NPC defined in configs/friendlyNpcs.json at a tile in a room. Kept
 * separate from EnemySpawnConfig so friendly NPCs never count towards clearing a room.
 */
public class NpcSpawnConfig extends PositionConfig {
  /** Id of an entry in configs/friendlyNpcs.json. */
  public String npcId;
}

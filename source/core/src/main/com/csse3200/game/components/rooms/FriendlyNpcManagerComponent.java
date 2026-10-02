package com.csse3200.game.components.rooms;

import com.badlogic.gdx.math.GridPoint2;
import com.csse3200.game.components.friendlynpc.NpcInteractableComponent;
import com.csse3200.game.components.friendlynpc.NpcInteractableComponent.Availability;
import com.csse3200.game.components.rooms.configs.NpcSpawnConfig;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.configs.InteractableNpcConfig;
import com.csse3200.game.entities.configs.InteractableNpcConfigs;
import com.csse3200.game.entities.factories.FriendlyNpcFactory;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Spawns a room's friendly NPCs and finds the one the player can interact with.
 *
 * The friendly counterpart to EnemyManagerComponent. Friendly NPCs are tracked here and
 * never there, so they do not block a room from being cleared.
 *
 * Each frame it shows the interaction indicator on the nearest in-range NPC that is available.
 * RoomManager asks it for the HUD prompt and routes the E key to it.
 */
public class FriendlyNpcManagerComponent extends EntityManagerComponent {
  private final NpcSpawnConfig[] spawns;
  private final InteractableNpcConfig[] spawnDefinitions;
  private final String[] atlases;
  private final String[] textures;
  private final List<NpcInteractableComponent> npcs = new ArrayList<>();
  private Entity player;

  /** Creates a manager for a room without friendly NPCs */
  public FriendlyNpcManagerComponent() {
    this(new NpcSpawnConfig[0], new InteractableNpcConfigs());
  }

  public FriendlyNpcManagerComponent(NpcSpawnConfig[] spawns, InteractableNpcConfigs definitions) {
    this.spawns = spawns;
    this.spawnDefinitions = new InteractableNpcConfig[spawns.length];
    Set<String> atlasPaths = new LinkedHashSet<>();
    Set<String> texturePaths = new LinkedHashSet<>();
    for (int i = 0; i < spawns.length; i++) {
      InteractableNpcConfig definition = definitions.get(spawns[i].npcId);
      if (definition == null) {
        throw new IllegalArgumentException("Unknown friendly NPC: " + spawns[i].npcId);
      }
      spawnDefinitions[i] = definition;
      if (definition.atlas != null) {
        atlasPaths.add(definition.atlas);
      } else {
        texturePaths.add(definition.texture);
      }
    }
    atlases = atlasPaths.toArray(new String[0]);
    textures = texturePaths.toArray(new String[0]);
    if (spawns.length > 0) {
      ResourceService resources = ServiceLocator.getResourceService();
      resources.loadTextureAtlases(atlases);
      resources.loadTextures(textures);
      resources.loadAll();
    }
  }

  @Override
  public void create() {
    entity.getEvents().addListener("RoomCreated", this::spawnNpcs);
  }

  public void spawnNpcs(Entity player) {
    this.player = player;
    for (int i = 0; i < spawns.length; i++) {
      Entity npc = FriendlyNpcFactory.createFriendlyNpc(spawnDefinitions[i]);
      NpcInteractableComponent interactable = npc.getComponent(NpcInteractableComponent.class);
      interactable.setRoomClearedSupplier(this::isRoomCleared);
      npcs.add(interactable);
      spawnEntityAt(npc, new GridPoint2(spawns[i].x, spawns[i].y), true, true);
    }
  }

  @Override
  public void update() {
    if (player != null) {
      updateIndicators(player);
    }
  }

  public void updateIndicators(Entity player) {
    NpcInteractableComponent nearest = findNearestInRange(player);
    for (NpcInteractableComponent npc : npcs) {
      npc.setIndicatorVisible(
          npc == nearest && npc.getAvailability(player) == Availability.AVAILABLE);
    }
  }

  public NpcInteractableComponent findNearestInRange(Entity player) {
    NpcInteractableComponent nearest = null;
    float nearestDistance = Float.MAX_VALUE;
    for (NpcInteractableComponent npc : npcs) {
      float distance = npc.distanceTo(player);
      if (distance <= npc.getConfig().interactionRange && distance < nearestDistance) {
        nearest = npc;
        nearestDistance = distance;
      }
    }
    return nearest;
  }

  public String getPrompt(Entity player) {
    NpcInteractableComponent nearest = findNearestInRange(player);
    return nearest == null ? null : nearest.getPrompt(player);
  }

  public List<NpcInteractableComponent> getNpcs() {
    return Collections.unmodifiableList(npcs);
  }

  @Override
  public void dispose() {
    npcs.clear();
    player = null;
    super.dispose();
    if (spawns.length > 0) {
      ResourceService resources = ServiceLocator.getResourceService();
      resources.unloadAssets(atlases);
      resources.unloadAssets(textures);
    }
  }

  private boolean isRoomCleared() {
    EnemyManagerComponent enemies = entity.getComponent(EnemyManagerComponent.class);
    return enemies == null || enemies.isCleared();
  }
}
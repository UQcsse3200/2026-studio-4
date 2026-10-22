package com.csse3200.game.components.rooms;

import com.badlogic.gdx.math.GridPoint2;
import com.csse3200.game.components.friendlynpc.NpcInteractableComponent;
import com.csse3200.game.components.friendlynpc.NpcInteractableComponent.Availability;
import com.csse3200.game.components.gamearea.GameAreaDisplay;
import com.csse3200.game.components.maingame.InteractionPromptDisplay;
import com.csse3200.game.components.player.PlayerActions;
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
 * <p>The friendly counterpart to EnemyManagerComponent. Friendly NPCs are tracked here and never
 * there, so they do not block a room from being cleared.
 *
 * <p>Each frame it shows the interaction indicator on the nearest in-range NPC that is available,
 * and puts that NPC's "Press E" sentence on the HUD. The room interaction system also asks for
 * that sentence and routes the E key here. A door or pickup prompt is left as it is.
 */
public class FriendlyNpcManagerComponent extends EntityManagerComponent {
  private final NpcSpawnConfig[] spawns;
  private final InteractableNpcConfig[] spawnDefinitions;
  private final String[] atlases;
  private final String[] textures;
  private final List<NpcInteractableComponent> npcs = new ArrayList<>();
  private Entity player;
  private String shownPrompt;

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
    entity.getEvents().addListener("npcInteract", this::onInteract);
  }

  public void onInteract(Entity player) {
    // Passing player through to here is kind of not great ...
    var npc = findNearestInRange(player);
    if (npc != null) {
      npc.interact(player);
    }

    var prompt = getPrompt(player);
    if (prompt == null) return;
    entity.getEvents().trigger(GameAreaDisplay.DISPLAY, prompt);
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

  /**
   * Shows the nearest NPC's sentence while the player is in range, and hides that sentence after
   * they walk away or the conversation starts. A door or pickup prompt is left as it is.
   */
  @Override
  public void update() {
    if (player == null) {
      return;
    }
    updateIndicators(player);
    refreshNpcPrompt(player.getComponent(InteractionPromptDisplay.class));
  }

  /**
   * Puts the current NPC sentence on the HUD.
   *
   * @param display the player's prompt, or null when this entity has none
   */
  void refreshNpcPrompt(InteractionPromptDisplay display) {
    if (display == null) {
      return;
    }
    String prompt = promptToShow();
    if (prompt != null) {
      display.setPrompt(prompt);
      shownPrompt = prompt;
      return;
    }
    if (shownPrompt != null && shownPrompt.equals(display.getPrompt())) {
      display.clearPrompt();
    }
    shownPrompt = null;
  }

  /**
   * Sentence for the NPC the player is standing near, or null when no prompt should be shown.
   *
   * @return the HUD sentence, or null while the player is away, busy, or unable to act
   */
  private String promptToShow() {
    PlayerActions actions = player.getComponent(PlayerActions.class);
    if (actions != null && actions.areControlsLocked()) {
      return null;
    }
    return getPrompt(player);
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

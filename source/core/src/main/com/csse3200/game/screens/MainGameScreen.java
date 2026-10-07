package com.csse3200.game.screens;

import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.csse3200.game.GdxGame;
import com.csse3200.game.GdxGame.ScreenType;
import com.csse3200.game.components.achievements.AchievementConfig;
import com.csse3200.game.components.achievements.AchievementContext;
import com.csse3200.game.components.achievements.AchievementsFactory;
import com.csse3200.game.components.gamearea.PerformanceDisplay;
import com.csse3200.game.components.gamearea.TimerDisplay;
import com.csse3200.game.components.maingame.*;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.components.rooms.RoomAssets;
import com.csse3200.game.components.rooms.RoomCommand;
import com.csse3200.game.components.rooms.RoomManager;
import com.csse3200.game.components.rooms.configs.WorldConfig;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.factories.NarrativeFactory;
import com.csse3200.game.entities.factories.PlayerFactory;
import com.csse3200.game.entities.factories.RenderFactory;
import com.csse3200.game.files.FileLoader;
import com.csse3200.game.files.GameSaveData;
import com.csse3200.game.files.GameSaveMapper;
import com.csse3200.game.input.InputComponent;
import com.csse3200.game.input.InputDecorator;
import com.csse3200.game.input.InputService;
import com.csse3200.game.physics.PhysicsEngine;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.rendering.Renderer;
import com.csse3200.game.services.*;
import com.csse3200.game.ui.terminal.Terminal;
import com.csse3200.game.ui.terminal.TerminalDisplay;
import com.csse3200.game.ui.terminal.commands.AbilityCommand;
import com.csse3200.game.ui.terminal.commands.CutsceneCommand;
import com.csse3200.game.ui.terminal.commands.DialogueCommand;
import com.csse3200.game.ui.terminal.commands.SpellCommand;
import com.csse3200.game.ui.terminal.commands.StatusEffectCommand;
import com.csse3200.game.ui.terminal.commands.UpgradeCommand;
import com.csse3200.game.ui.terminal.commands.WeaponCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The game screen containing the main game.
 *
 * <p>Details on libGDX screens: https://happycoding.io/tutorials/libgdx/game-screens
 */
public class MainGameScreen extends ScreenAdapter {
  private static final Logger logger = LoggerFactory.getLogger(MainGameScreen.class);

  private final GdxGame game;
  private final Renderer renderer;
  private final PhysicsEngine physicsEngine;
  private RoomManager roomManager;
  private Entity player;
  private final Terminal terminal;
  private final int saveSlot;
  private final GameSaveData loadedSave;
  private boolean runSaved;
  private final RoomAssets roomAssets = new RoomAssets();
  private final RunTimer runTimer;
  private boolean winScreenRequested;
  private boolean saveOnDispose = true;

  public MainGameScreen(GdxGame game) {
    this(game, null, 1);
  }

  public MainGameScreen(GdxGame game, GameSaveData save, int saveSlot) {
    this(game, save, saveSlot, false);
  }

  public MainGameScreen(GdxGame game, GameSaveData save, int saveSlot, boolean loadAtCheckpoint) {
    this.game = game;
    this.loadedSave = save;
    this.saveSlot = saveSlot;

    terminal = new Terminal();
    GameTime gameTime = new GameTime();
    ServiceLocator.registerTimeSource(gameTime);
    runTimer = new RunTimer(gameTime);
    if (loadedSave == null) {
      runTimer.startRun();
    } else {
      runTimer.restoreRun(loadedSave.playTimeSeconds, loadedSave.dungeonTimesSeconds);
    }

    // load all game services
    logger.debug("Initialising main game screen services");
    ServiceLocator.registerTimeSource(new GameTime());
    PhysicsService physicsService = new PhysicsService();
    ServiceLocator.registerPhysicsService(physicsService);
    physicsEngine = physicsService.getPhysics();
    ServiceLocator.registerInputService(new InputService());
    ServiceLocator.registerResourceService(new ResourceService());
    ServiceLocator.registerEntityService(new EntityService());
    ServiceLocator.registerRenderService(new RenderService());
    renderer = RenderFactory.createRenderer();
    renderer.getDebug().renderPhysicsWorld(physicsEngine.getWorld());
    ServiceLocator.registerRunTimer(runTimer);

    ServiceLocator.registerAchievementService(createAchievementService());

    loadAssets();

    player = PlayerFactory.createPlayer();
    // trigger death screen via entity died event
    player.getEvents().addListener("entityDied", this::scheduleDeathScreen);
    // trigger win screen via entity win event
    player.getEvents().addListener("winScreenRequested", () -> winScreenRequested = true);
    // notify damage
    player
        .getEvents()
        .addListener(
            "damageTaken",
            (Entity attacker, Integer lost, Integer remaining) -> {
              AchievementContext ctx = new AchievementContext();
              ctx.playerDamaged = true;
              ServiceLocator.getAchievementService().update(ctx);
            });

    WorldConfig world = FileLoader.readClass(WorldConfig.class, "configs/rooms.json");
    if (world == null) {
      throw new IllegalStateException("Unable to load configs/rooms.json");
    }
    roomManager = new RoomManager(world, player, renderer.getCamera());
    if (loadedSave != null) {
      roomManager.initializeFromSavedRun(loadedSave, loadAtCheckpoint);
    }
    roomManager.create();

    if (loadedSave != null) {
      GameSaveMapper.restore(player, loadedSave);
      GameSaveMapper.restoreAchievements(loadedSave);
    }
    RoomCommand roomCommand = new RoomCommand(roomManager);
    terminal.addCommand("room", roomCommand);

    createUI();
  }

  @Override
  public void render(float delta) {
    physicsEngine.update();
    ServiceLocator.getEntityService().update();
    roomManager.update();
    renderer.render();
    runTimer.update();
  }

  @Override
  public void resize(int width, int height) {
    renderer.resize(width, height);
    logger.trace("Resized renderer: ({} x {})", width, height);
  }

  @Override
  public void pause() {
    logger.info("Game paused");
  }

  @Override
  public void resume() {
    logger.info("Game resumed");
  }

  @Override
  public void dispose() {
    logger.debug("Disposing main game screen");

    if (saveOnDispose && !runSaved && player != null && roomManager != null) {
      runSaved = true;
      try {
        GameSaveData save =
            GameSaveMapper.capture(
                player, roomManager.getCheckpointData(), roomManager.getResumePositionData());
        save.playTimeSeconds = runTimer.getTotalTime();
        save.dungeonTimesSeconds.putAll(runTimer.getDungeonTimes());
        FileLoader.save(save, saveSlot);
      } catch (RuntimeException exception) {
        logger.error("Failed to save game data for slot {}", saveSlot, exception);
      }
      Stage stage = ServiceLocator.getRenderService().getStage();
      try {
        stage.getRoot().setVisible(false);
        renderer.render();
        FileLoader.savePreview(saveSlot);
      } catch (RuntimeException exception) {
        logger.error("Failed to save preview for slot {}", saveSlot, exception);
      } finally {
        stage.getRoot().setVisible(true);
      }
    }

    renderer.dispose();
    unloadAssets();

    ServiceLocator.getEntityService().dispose();
    ServiceLocator.getRenderService().dispose();
    ServiceLocator.getResourceService().dispose();

    ServiceLocator.clear();
  }

  public void saveAndExit() {
    game.setScreen(ScreenType.MAIN_MENU);
  }

  public void deleteSaveAndExit() {
    saveOnDispose = false;
    runSaved = true;

    FileLoader.deleteSaveSlot(saveSlot);
    game.setScreen(ScreenType.MAIN_MENU);
  }

  private void loadAssets() {
    logger.debug("Loading assets");
    roomAssets.loadAll();
  }

  private void unloadAssets() {
    logger.debug("Unloading assets");
    roomAssets.dispose();
  }

  /**
   * Creates the main game's ui including components for rendering ui elements to the screen and
   * capturing and handling ui input.
   */
  private void createUI() {
    logger.debug("Creating ui");
    Stage stage = ServiceLocator.getRenderService().getStage();
    InputComponent inputComponent =
        ServiceLocator.getInputService().getInputFactory().createForTerminal();

    // Register on the shared terminal field: commands added elsewhere (e.g. "room" in the
    // constructor) must end up on the same Terminal instance that is attached to the UI below.
    terminal.addCommand("weapon", new WeaponCommand(player));
    terminal.addCommand("ability", new AbilityCommand(player));
    terminal.addCommand("effect", new StatusEffectCommand(player));
    terminal.addCommand("upgrade", new UpgradeCommand(player));
    terminal.addCommand("spell", new SpellCommand(player));
    terminal.addCommand("room", new RoomCommand(roomManager));
    // QA for the dialogue and cutscene systems, e.g. "dialogue demo" / "cutscene demovideo"
    terminal.addCommand("dialogue", new DialogueCommand(player));
    terminal.addCommand("cutscene", new CutsceneCommand(player));

    InventoryDisplay inventoryDisplay =
        new InventoryDisplay(player.getComponent(InventoryComponent.class));
    HotbarDisplay hotbarDisplay = new HotbarDisplay(player);
    ConsumableHotbarDisplay consumableHotbarDisplay = new ConsumableHotbarDisplay(player);
    InventoryActions inventoryActions = new InventoryActions(inventoryDisplay);
    player.getComponent(InventoryComponent.class).setDisplay(inventoryDisplay);
    TimerDisplay timerDisplay = new TimerDisplay();

    Entity ui = new Entity();
    ui.addComponent(new InputDecorator(stage, 10))
        .addComponent(new PerformanceDisplay())
        .addComponent(new MainGameActions(this.game))
        .addComponent(
            new MainGameExitDisplay(
                this::saveAndExit,
                this::deleteSaveAndExit,
                () -> {
                  player.getEvents().trigger("walkStop");
                  player.getEvents().trigger("resetMovementInput");
                  PhysicsComponent physics = player.getComponent(PhysicsComponent.class);
                  if (physics != null && physics.getBody() != null) {
                    physics.getBody().setLinearVelocity(0f, 0f);
                  }
                }))
        .addComponent(terminal)
        .addComponent(inputComponent)
        .addComponent(new TerminalDisplay())
        .addComponent(timerDisplay)
        .addComponent(new TimerDisplay.ToggleInput(timerDisplay))
        .addComponent(inventoryDisplay)
        .addComponent(hotbarDisplay)
        .addComponent(consumableHotbarDisplay)
        .addComponent(inventoryActions);
    ui.getComponent(InventoryDisplay.class).setEnabled(false);
    // The HUD keeps working while a dialogue or cutscene has the world frozen
    ui.setUpdatesWhilePaused(true);
    ServiceLocator.getEntityService().register(ui);

    // Dialogue and cutscene systems (events are sent on the player)
    ServiceLocator.getEntityService()
        .register(NarrativeFactory.createNarrative(player, terminal::isOpen));
  }

  /* Schedule the death screen to be shown */
  private void scheduleDeathScreen() {
    runTimer.stopRun();
    ServiceLocator.getEntityService().schedule(() -> game.setScreen(ScreenType.DEATH_SCREEN));
  }

  private AchievementService createAchievementService() {
    AchievementConfig[] configs =
        FileLoader.readClass(AchievementConfig[].class, "configs/achievements.json");
    if (configs == null) {
      throw new IllegalStateException("Unable to load configs/achievements.json");
    }
    AchievementService achievementService = new AchievementService();
    for (AchievementConfig c : configs) {
      achievementService.register(AchievementsFactory.build(c));
    }
    return achievementService;
  }
}

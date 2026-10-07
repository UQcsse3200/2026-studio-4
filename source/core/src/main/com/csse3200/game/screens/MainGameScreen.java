package com.csse3200.game.screens;

import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.csse3200.game.GdxGame;
import com.csse3200.game.GdxGame.ScreenType;
import com.csse3200.game.components.gamearea.PerformanceDisplay;
import com.csse3200.game.components.gamearea.TimerDisplay;
import com.csse3200.game.components.maingame.ConsumableHotbarDisplay;
import com.csse3200.game.components.maingame.HotbarDisplay;
import com.csse3200.game.components.maingame.InventoryActions;
import com.csse3200.game.components.maingame.InventoryDisplay;
import com.csse3200.game.components.maingame.MainGameActions;
import com.csse3200.game.components.maingame.MainGameExitDisplay;
import com.csse3200.game.components.maingame.ShopDisplay;
import com.csse3200.game.components.maingame.ShopEntry;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.components.rooms.RoomAssets;
import com.csse3200.game.components.rooms.RoomCommand;
import com.csse3200.game.components.rooms.RoomManager;
import com.csse3200.game.components.rooms.configs.WorldConfig;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.factories.PlayerFactory;
import com.csse3200.game.entities.factories.RenderFactory;
import com.csse3200.game.files.FileLoader;
import com.csse3200.game.input.InputComponent;
import com.csse3200.game.input.InputDecorator;
import com.csse3200.game.input.InputService;
import com.csse3200.game.physics.PhysicsEngine;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.rendering.Renderer;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.RunTimer;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.ui.terminal.Terminal;
import com.csse3200.game.ui.terminal.TerminalDisplay;
import com.csse3200.game.ui.terminal.commands.AbilityCommand;
import com.csse3200.game.ui.terminal.commands.BurnVialCommand;
import com.csse3200.game.ui.terminal.commands.ShopCommand;
import com.csse3200.game.ui.terminal.commands.SpellCommand;
import com.csse3200.game.ui.terminal.commands.StatusEffectCommand;
import com.csse3200.game.ui.terminal.commands.UpgradeCommand;
import com.csse3200.game.ui.terminal.commands.WeaponCommand;
import com.csse3200.game.items.ItemIds;
import java.util.List;
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
  private final RoomAssets roomAssets = new RoomAssets();
  private final RunTimer runTimer;

  public MainGameScreen(GdxGame game) {
    this.game = game;

    terminal = new Terminal();

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
    ServiceLocator.registerRunTimer(new RunTimer(new GameTime()));

    loadAssets();

    player = PlayerFactory.createPlayer();
    // trigger death screen via entity died event
    player.getEvents().addListener("entityDied", this::scheduleDeathScreen);

    WorldConfig world = FileLoader.readClass(WorldConfig.class, "configs/rooms.json");
    if (world == null) {
      throw new IllegalStateException("Unable to load configs/rooms.json");
    }

    roomManager = new RoomManager(world, player, renderer.getCamera());
    roomManager.create();

    runTimer = ServiceLocator.getRunTimer();
    runTimer.startRun();

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

    renderer.dispose();
    unloadAssets();

    ServiceLocator.getEntityService().dispose();
    ServiceLocator.getRenderService().dispose();
    ServiceLocator.getResourceService().dispose();

    ServiceLocator.clear();
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
    terminal.addCommand("burnvial", new BurnVialCommand(player));
    terminal.addCommand("effect", new StatusEffectCommand(player));
    terminal.addCommand("upgrade", new UpgradeCommand(player));
    terminal.addCommand("spell", new SpellCommand(player));
    terminal.addCommand("room", new RoomCommand(roomManager));

    InventoryDisplay inventoryDisplay =
        new InventoryDisplay(player.getComponent(InventoryComponent.class));
    HotbarDisplay hotbarDisplay = new HotbarDisplay(player);
    ConsumableHotbarDisplay consumableHotbarDisplay = new ConsumableHotbarDisplay(player);
    InventoryActions inventoryActions = new InventoryActions(inventoryDisplay);
    player.getComponent(InventoryComponent.class).setDisplay(inventoryDisplay);
    TimerDisplay timerDisplay = new TimerDisplay();

    // Placeholder catalogue for Sprint 3 #196 shop display (B3 UI slice, decoupled from the
    // real B1 catalogue and B2 purchase logic, which are being built separately). Swap this
    // list for the agreed catalogue once it lands.
    ShopDisplay shopDisplay =
        new ShopDisplay(
            player.getComponent(InventoryComponent.class),
            List.of(
                new ShopEntry(ItemIds.HEALTH_POTION, "Health Potion", 10),
                new ShopEntry(ItemIds.SHIELD, "Shield", 15),
                new ShopEntry(ItemIds.SPEED_POTION, "Speed Potion", 12)));

    Entity ui = new Entity();
    ui.addComponent(new InputDecorator(stage, 10))
        .addComponent(new PerformanceDisplay())
        .addComponent(new MainGameActions(this.game))
        .addComponent(new MainGameExitDisplay())
        .addComponent(terminal)
        .addComponent(inputComponent)
        .addComponent(new TerminalDisplay())
        .addComponent(timerDisplay)
        .addComponent(new TimerDisplay.ToggleInput(timerDisplay))
        .addComponent(inventoryDisplay)
        .addComponent(hotbarDisplay)
        .addComponent(consumableHotbarDisplay)
        .addComponent(inventoryActions)
        .addComponent(shopDisplay);
    ui.getComponent(InventoryDisplay.class).setEnabled(false);
    terminal.addCommand("shop", new ShopCommand(ui));
    ServiceLocator.getEntityService().register(ui);
  }

  /* Schedule the death screen to be shown */
  private void scheduleDeathScreen() {
    runTimer.stopRun();
    ServiceLocator.getEntityService().schedule(() -> game.setScreen(ScreenType.DEATH_SCREEN));
  }
}

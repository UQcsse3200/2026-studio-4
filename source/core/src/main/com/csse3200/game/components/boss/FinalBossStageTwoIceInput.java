package com.csse3200.game.components.boss;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.utils.Array;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.input.InputComponent;
import com.csse3200.game.input.InputService;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.ui.terminal.Terminal;
import java.util.Objects;
import java.util.function.BooleanSupplier;

/** Encounter-owned J override, after UI input and before the player's ordinary attack handler. */
public class FinalBossStageTwoIceInput extends InputComponent {
  private final BooleanSupplier activeEncounter;
  private final BooleanSupplier hasEnergy;
  private final BooleanSupplier physicalKeyDown;
  private InputService registeredService;
  private boolean held;
  private boolean interceptedPress;
  private boolean disposed;

  public FinalBossStageTwoIceInput(BooleanSupplier activeEncounter, BooleanSupplier hasEnergy) {
    this(activeEncounter, hasEnergy, () -> Gdx.input != null && Gdx.input.isKeyPressed(Keys.J));
  }

  /**
   * The physical key supplier lets a missed release clear the latch without synthesising presses.
   */
  public FinalBossStageTwoIceInput(
      BooleanSupplier activeEncounter, BooleanSupplier hasEnergy, BooleanSupplier physicalKeyDown) {
    super(6);
    this.activeEncounter = Objects.requireNonNull(activeEncounter);
    this.hasEnergy = Objects.requireNonNull(hasEnergy);
    this.physicalKeyDown = Objects.requireNonNull(physicalKeyDown);
  }

  /** Registers once; a call before the input service exists can be retried later. */
  @Override
  public void create() {
    if (disposed || registeredService != null) {
      return;
    }
    InputService service = ServiceLocator.getInputService();
    if (service != null) {
      service.register(this);
      registeredService = service;
    }
  }

  @Override
  public boolean keyDown(int keycode) {
    if (keycode != Keys.J) {
      return false;
    }
    if (!isActive()) {
      reset();
      return false;
    }
    // Remember a normal J press too: collecting energy while still holding it starts ice fire.
    held = true;
    boolean intercept = hasEnergy.getAsBoolean();
    interceptedPress |= intercept;
    return intercept;
  }

  @Override
  public boolean keyUp(int keycode) {
    if (keycode != Keys.J) {
      return false;
    }
    boolean intercept = !disposed && (interceptedPress || (isActive() && hasEnergy.getAsBoolean()));
    reset();
    return intercept;
  }

  /**
   * Whether an observed J press is still held. Energy and player action locks are checked by the
   * encounter before firing; energy alone must not clear a press made before collecting a pickup.
   */
  public boolean isHeld() {
    if (!isActive() || (held && !physicalKeyDown.getAsBoolean())) {
      reset();
    }
    return held;
  }

  /** Clears a press on phase exit, player death, or other encounter cleanup. */
  public void reset() {
    held = false;
    interceptedPress = false;
  }

  @Override
  public void setEnabled(boolean enabled) {
    super.setEnabled(enabled);
    if (!enabled) {
      reset();
    }
  }

  @Override
  public void dispose() {
    if (disposed) {
      return;
    }
    disposed = true;
    reset();
    if (registeredService != null) {
      registeredService.unregister(this);
      registeredService = null;
    }
  }

  private boolean isActive() {
    return !disposed && enabled && activeEncounter.getAsBoolean() && !isUiCapturingInput();
  }

  private boolean isUiCapturingInput() {
    RenderService rendering = ServiceLocator.getRenderService();
    Stage stage = rendering == null ? null : rendering.getStage();
    if (stage != null && stage.getKeyboardFocus() instanceof TextField) {
      return true;
    }

    EntityService entities = ServiceLocator.getEntityService();
    Array<Entity> registered = entities == null ? null : entities.getEntities();
    if (registered != null) {
      // EntityService.update() may already be iterating this Array with its reused iterator.
      // Index access keeps that outer iterator valid while polling input during an entity update.
      for (int i = 0; i < registered.size; i++) {
        Entity candidate = registered.get(i);
        Terminal terminal = candidate.getComponent(Terminal.class);
        if (terminal != null && terminal.isOpen()) {
          return true;
        }
      }
    }
    return false;
  }
}

package com.csse3200.game.components.maingame;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;
import com.csse3200.game.components.weapons.WeaponSelectionComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.items.WeaponItem;
import com.csse3200.game.items.WeaponItem.WeaponType;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.ui.UIComponent;
import java.util.List;
import java.util.Objects;

/**
 * Displays two groups of three circular slots at the bottom of the UI stage. The left three are for
 * player weapons and the right three are for player spells.
 */
public class HotbarDisplay extends UIComponent {
  // Dimensions are in UI stage units
  private static final float EDGE_INSET = 48f;
  private static final float SLOT_GAP = 6f;
  private static final float SIDE_INSET = 340f;
  private static final float SLOT_SIZE = 96f;
  private Table hotbarTable;
  private Texture hotbarTexture;
  private TextureRegionDrawable frameDrawable;
  private final Entity player;
  private final WeaponSelectionComponent selection;
  private final Image[] weaponFrames = new Image[3];
  private boolean disposed;

  /**
   * Binds the display to the player.
   *
   * @param player player whose equipment and item icons are displayed
   */
  public HotbarDisplay(Entity player) {
    this.player = Objects.requireNonNull(player);
    selection = Objects.requireNonNull(player.getComponent(WeaponSelectionComponent.class));
  }

  /**
   * Creates the actors, reads the initial selection, and subscribes once to later selection
   * changes.The player's weapon textures must be loaded before this method runs.
   */
  @Override
  public void create() {
    super.create();
    buildHotbar();
    updateWeaponSelection(selection.getSelectedWeapon());
    player.getEvents().addListener("weaponSelected", this::updateWeaponSelection);
  }

  /** Builds the root table and its two slot groups. */
  private void buildHotbar() {
    hotbarTexture = new Texture(Gdx.files.internal("images/ui/ui_big_pieces.png"));
    hotbarTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
    // The large circular frame in the sprite sheet's bottom-right minimap section.
    frameDrawable = new TextureRegionDrawable(new TextureRegion(hotbarTexture, 718, 288, 130, 130));

    hotbarTable =
        new Table() {
          @Override
          protected void sizeChanged() {
            super.sizeChanged();
            // Slot sizes depend on the viewport
            for (Actor child : getChildren()) {
              if (child instanceof Table group) {
                group.invalidate();
              }
            }
          }
        };
    hotbarTable.setName("inventory-hotbar");
    hotbarTable.setFillParent(true);
    // Keep the existing horizontal layout, anchored 48 pixels above the bottom edge.
    hotbarTable.bottom().padBottom(EDGE_INSET).padLeft(SIDE_INSET);
    hotbarTable.padRight(
        new Value() {
          @Override
          public float get(Actor context) {
            float groupsWidth = 2f * (3f * slotSize(context.getWidth()) + 2f * SLOT_GAP);
            // Prefer matching 340-unit side margins. Narrow windows reduce the right margin
            // to at least 96 units, reserving 24 units between the groups where space permits.
            return Math.clamp(context.getWidth() - SIDE_INSET - groupsWidth - 24f, 96f, SIDE_INSET);
          }
        });
    hotbarTable.setTouchable(Touchable.disabled);
    hotbarTable.add(createWeaponGroup());
    hotbarTable.add().expandX();
    hotbarTable.add(createSpellGroup());
    stage.addActor(hotbarTable);
  }

  /**
   * Applies the shared responsive sizing and spacing to already-created slot actors.
   *
   * @param slots slot contents in display order
   * @return a dynamically sized row of slots
   */
  private Table createHotbarGroup(List<Actor> slots) {
    Table group = new Table();
    Value slotSize =
        new Value() {
          @Override
          public float get(Actor context) {
            return slotSize(stage.getWidth());
          }
        };
    for (int i = 0; i < slots.size(); i++) {
      group.add(slots.get(i)).size(slotSize).padRight(i < slots.size() - 1 ? SLOT_GAP : 0f);
    }
    return group;
  }

  /** Builds the left-hand group in weapon-selection order: sword, knife, bow. */
  private Table createWeaponGroup() {
    return createHotbarGroup(
        List.of(createWeaponSlot(0), createWeaponSlot(1), createWeaponSlot(2)));
  }

  /** Reserves three empty right-hand frames for spells when they are added */
  private Table createSpellGroup() {
    return createHotbarGroup(
        List.of(createCircleFrame(), createCircleFrame(), createCircleFrame()));
  }

  /** Creates a separate frame actor using the shared sprite-sheet drawable. */
  private Image createCircleFrame() {
    Image frame = new Image(frameDrawable);
    frame.setScaling(Scaling.fit);
    return frame;
  }

  /**
   * Layers a frame, centred item icon, and bottom-aligned key label in a single slot.
   *
   * @param index zero-based position in the weapon-selection component's ordered items
   * @return the composed slot
   */
  private Stack createWeaponSlot(int index) {
    Image frame = createCircleFrame();
    weaponFrames[index] = frame;
    int key = index + 1;
    frame.setName("weapon-frame-" + key);
    WeaponItem item = selection.getWeapons().get(index);
    Stack slot = new Stack();
    slot.add(frame);
    // These textures belong to WeaponAssetsComponent
    Texture texture =
        ServiceLocator.getResourceService().getAsset(item.getTexture(), Texture.class);
    Image icon = new Image(texture);
    // Name for tests
    icon.setName("weapon-icon-" + key);
    icon.setScaling(Scaling.fit);
    Container<Image> iconContainer = new Container<>(icon);
    // A centred square at 55% of the diameter keeps the fitted icon clear of the ornate rim.
    iconContainer.size(Value.percentWidth(0.55f, slot));
    slot.add(iconContainer);

    // Copy the style so the white text colour does not alter other labels using the shared skin.
    Label.LabelStyle keyStyle = new Label.LabelStyle(skin.get(Label.LabelStyle.class));
    keyStyle.fontColor = Color.WHITE;
    Label keyLabel = new Label(Integer.toString(key), keyStyle);
    // Name for tests
    keyLabel.setName("weapon-key-" + key);
    keyLabel.setFontScale(1.05f);
    Table labelOverlay = new Table();
    labelOverlay.bottom().add(keyLabel).padBottom(2f);
    slot.add(labelOverlay);
    return slot;
  }

  /**
   * Calculates a common slot diameter, capped at 96 stage units.
   *
   * @param width available stage width
   * @return slot diameter between 1 and 96 stage units
   */
  private float slotSize(float width) {
    int groupCount = 2;
    int slotsPerGroup = 3;
    int totalSlots = groupCount * slotsPerGroup;
    int internalGapCount = groupCount * (slotsPerGroup - 1);
    float minimumRightMargin = 96f;
    float minimumGroupGap = 24f;
    float minimumSlotSize = 1f;
    float reservedWidth =
        SIDE_INSET + minimumRightMargin + minimumGroupGap + internalGapCount * SLOT_GAP;
    float availableWidthPerSlot = (width - reservedWidth) / totalSlots;

    // Keep a positive diameter on narrow windows and cap it at the intended full size.
    return Math.clamp(availableWidthPerSlot, minimumSlotSize, SLOT_SIZE);
  }

  /**
   * Highlights the selected weapon frame.
   *
   * @param selected current weapon type, or null to leave all weapon frames grey
   */
  private void updateWeaponSelection(WeaponType selected) {
    if (disposed) {
      return;
    }
    for (int i = 0; i < weaponFrames.length; i++) {
      weaponFrames[i].setColor(
          WeaponSelectionComponent.SLOT_WEAPONS.get(i) == selected ? Color.WHITE : Color.GRAY);
    }
  }

  /** Leaves rendering to the shared Scene2D stage rather than drawing the hotbar twice. */
  @Override
  public void draw(SpriteBatch batch) {
    // Scene2D draws the actors.
  }

  /** Returns the component render priority; sibling actor order is managed by Scene2D. */
  @Override
  public float getZIndex() {
    return 2f;
  }

  /** Removes the hotbar actor and releases only its owned frame texture. */
  @Override
  public void dispose() {
    disposed = true;
    hotbarTable.remove();
    hotbarTexture.dispose();
    super.dispose();
  }
}

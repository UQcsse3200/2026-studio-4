package com.csse3200.game.ui.minimap;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.csse3200.game.components.rooms.configs.RoomConfig;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.ui.UIComponent;
import com.csse3200.game.utils.shapes.Rectangle;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Minimap */
public class Minimap extends UIComponent {
  private static final Logger logger = LoggerFactory.getLogger(Minimap.class);

  private static final int WIDTH = 250;
  private static final int HEIGHT = 250;
  private static final int ROOM_W = 40;
  private static final int ROOM_H = 30;

  private static final Color ROOM_COLOR = Color.WHITE;
  private static final Color VISITED_COLOR = Color.GRAY;
  private static final Color BG_COLOR = new Color(0xa1a658ff);

  public static final String ROOM_CHANGE_EVENT = "RoomChanged";
  public static final String PLAYER_HEAD_PATH = "images/player_head.png";

  private String startingRoomId;
  private RoomConfig currentRoom;
  private Set<String> clearedRooms;

  private final ShapeRenderer shapeRenderer;
  private Table left;
  private Table center;
  private Table right;

  @Override
  public void create() {
    super.create();
    stage.addActor(buildRoot());
    entity.getEvents().addListener(ROOM_CHANGE_EVENT, this::onRoomChanged);
  }

  public Minimap(
      RoomConfig currentRoom,
      Set<String> clearedRooms,
      String startingRoomId,
      ShapeRenderer shapeRenderer) {
    logger.debug("Created with RoomConfig: {}, startingRoomId: {}", currentRoom.id, startingRoomId);
    this.currentRoom = currentRoom;
    this.clearedRooms = clearedRooms;
    this.startingRoomId = startingRoomId;
    this.shapeRenderer = shapeRenderer;
  }

  public Minimap(RoomConfig currentRoom, Set<String> clearedRooms) {
    this(currentRoom, clearedRooms, "selection", new ShapeRenderer());
  }

  /** pacakge private constructor for testing */
  Minimap(
      RoomConfig roomConfig,
      Set<String> clearedSet,
      Table left,
      Table right,
      Table center,
      ShapeRenderer shapeRenderer) {
    this.currentRoom = roomConfig;
    this.clearedRooms = clearedSet;
    this.left = left;
    this.right = right;
    this.center = center;
    this.shapeRenderer = shapeRenderer;
  }

  /**
   * builds and sets the {@link Minimap#exitContainer}.
   *
   * @return Returns the root table to be added to the stage.
   */
  private Table buildRoot() {
    // Root covers the screen size.
    Table root = new Table();
    root.setFillParent(true);
    root.top().right();
    root.padRight(20f);
    root.padTop(150f);

    // create and place the minimap into the root
    Table mapContainer = buildMapContainer();
    root.add(mapContainer).center().size(WIDTH, HEIGHT).fillX();

    left = new Table();
    center = buildCenter();
    right = new Table();

    // Add containers for actual room icons
    mapContainer.add(left).grow();
    mapContainer.add(center).grow();
    mapContainer.add(right).grow();

    rebuild();
    return root;
  }

  private Table buildCenter() {
    var table = new Table();
    Texture texture = ServiceLocator.getResourceService().getAsset(PLAYER_HEAD_PATH, Texture.class);
    Image playerHead = new Image(texture);

    table.add(playerHead).center();

    return table;
  }

  private void rebuild() {
    logger.debug(
        "rebuilding minimap at {}. cleared: {}",
        currentRoom.id,
        clearedRooms.contains(currentRoom.id));
    right.clearChildren();
    left.clearChildren();

    for (var exits : currentRoom.exits) {
      logger.trace("building exit: {}, destination: {}", exits.id, exits.destinationRoomId);
      RoomType type = determineRoomType(exits.destinationRoomId);

      logger.trace("Determined roomtype: {}", type);

      if (exits.side == null) {
        // Fallback to right if side is not set
        logger.warn("roomid {}: side field not set, falling back to right", exits.id);
        attachRoom(right, type);
        continue;
      }

      if (exits.side.equals("LEFT")) {
        attachRoom(left, type);
      } else if (exits.side.equals("RIGHT")) {
        attachRoom(right, type);
      } else {
        logger.error("roomid {}: skipping unknown side field", exits.id);
      }
    }
  }

  enum RoomType {
    DEFAULT,
    CLEARED,
    HOME
  }

  RoomType determineRoomType(String roomId) {
    if (roomId == null) return RoomType.DEFAULT;

    if (roomId.equals(startingRoomId)) return RoomType.HOME;

    if (clearedRooms.contains(roomId)) {
      return RoomType.CLEARED;
    }

    return RoomType.DEFAULT;
  }

  public void setCurrentRoom(RoomConfig newRoom) {
    this.currentRoom = newRoom;
    logger.debug("Switched to RoomConfig: {}", newRoom.id);
  }

  /**
   * Callback function for when player switches rooms
   *
   * <p>Sets {@link Minimap#currentRoom} then redraws exits
   *
   * @param newRoom The new room switched into. newRoom != null
   */
  public void onRoomChanged(RoomConfig newRoom) {
    setCurrentRoom(newRoom);
    rebuild();
  }

  /** Create the minimap container */
  private static Table buildMapContainer() {
    Table map = new Table();
    // This can be set to use a texture instead later on.
    setTableBackground(map, BG_COLOR);
    return map;
  }

  /**
   * Helper method to attach a room to the give table
   *
   * @param table The container to attach children
   * @param type The room type to add
   */
  void attachRoom(Table table, RoomType type) {
    Actor icon =
        switch (type) {
          case DEFAULT -> new Rectangle(ROOM_COLOR, true, shapeRenderer);
          case CLEARED -> new Rectangle(VISITED_COLOR, true, shapeRenderer);
          case HOME -> new Rectangle(Color.BROWN, true, shapeRenderer);
        };

    icon.setSize(ROOM_W, ROOM_H);

    table.add(icon).expand();
    table.row();
  }

  /**
   * Sets the background color for the given table using a pixmap converted to a texture
   *
   * @param table Table to draw background on. Cannot be null
   * @param color Background color {@link Color}. Cannot be null
   */
  private static void setTableBackground(Table table, Color color) {
    Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
    pixmap.setColor(color);
    pixmap.fill();
    TextureRegionDrawable textureRegion =
        new TextureRegionDrawable(new TextureRegion(new Texture(pixmap)));
    table.setBackground(textureRegion);
  }

  @Override
  protected void draw(SpriteBatch batch) {
    // Drawing is handled by the stage
  }

  @Override
  public void dispose() {
    super.dispose();
    shapeRenderer.dispose();
  }
}

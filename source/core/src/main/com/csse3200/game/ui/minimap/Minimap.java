package com.csse3200.game.ui.minimap;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.csse3200.game.components.rooms.configs.RoomConfig;
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
  private static final Color BG_COLOR = new Color(0, 0, 0, 0.4f);

  public static final String ROOM_CHANGE_EVENT = "RoomChanged";

  private RoomConfig currentRoom;
  private Set<String> clearedRooms;

  private Table left;
  private Table center;
  private Table right;

  @Override
  public void create() {
    super.create();
    stage.addActor(buildRoot());
    entity.getEvents().addListener(ROOM_CHANGE_EVENT, this::onRoomChanged);
  }

  public Minimap(RoomConfig currentRoom, Set<String> clearedRooms) {
    logger.debug("Created with RoomConfig: {}", currentRoom.id);
    this.currentRoom = currentRoom;
    this.clearedRooms = clearedRooms;
  }

  /** pacakge private constructor for testing */
  Minimap(RoomConfig roomConfig, Set<String> clearedSet, Table left, Table right, Table center) {
    this.currentRoom = roomConfig;
    this.clearedRooms = clearedSet;
    this.left = left;
    this.right = right;
    this.center = center;
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
    root.center().left();
    root.padLeft(20f);

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
    var center = new Table();
    center.add(createRoomIcon(true, true));
    return center;
  }

  private void rebuild() {
    logger.debug("rebuilding minimap at {}. cleared: {}", currentRoom.id, clearedRooms.contains(currentRoom.id));
    right.clearChildren();
    left.clearChildren();

    for (var exits : currentRoom.exits) {
      if (exits.side == null) {
        // Fallback to right if side is not set
        logger.debug("roomid {}: side field not set, falling back to right", exits.id);
        attachRoom(right, true, clearedRooms.contains(exits.destinationRoomId));
        continue;
      }

      if (exits.side.equals("LEFT")) {
        attachRoom(left, true, clearedRooms.contains(exits.destinationRoomId));
      } else if (exits.side.equals("RIGHT")) {
        attachRoom(right, true, clearedRooms.contains(exits.destinationRoomId));
      } else {
        logger.error("roomid {}: skipping unknown side field", exits.id);
      }
    }
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
    setTableBackground(map, BG_COLOR);
    return map;
  }

  /**
   * Helper method to attach x amount of exits to given table
   *
   * @param table The container to attach children
   * @param count Number of exits to attach
   */
  private static void attachRooms(Table table, int count) {
    logger.debug("attaching {} rooms", count);
    for (int i = 0; i < count; i++) {
      table.add(createRoomIcon()).expand();
      table.row();
    }
  }

  private void attachRoom(Table table, boolean fill, boolean visited) {
    table.add(createRoomIcon(fill, visited)).expand();
    table.row();
  }

  /**
   * @return Returns the room icon to be added to a table
   * @param fill Whether to fill the icon or not
   * @param visited Whether the room has been visited, will change color of icon.
   */
  private static Actor createRoomIcon(boolean fill, boolean visited) {
    Color color = visited ? VISITED_COLOR : ROOM_COLOR;
    var room = new Rectangle(color, fill);

    room.setSize(ROOM_W, ROOM_H);
    return room;
  }

  private static Actor createRoomIcon() {
    return createRoomIcon(true, false);
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
  protected void draw(SpriteBatch batch) {}
}

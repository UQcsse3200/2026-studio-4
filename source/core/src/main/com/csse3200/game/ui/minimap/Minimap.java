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
  private static final Color BG_COLOR = new Color(0, 0, 0, 0.4f);

  public static final String ROOM_CHANGE_EVENT = "RoomChanged";

  private RoomConfig currentRoom;
  private Table exitContainer;

  @Override
  public void create() {
    super.create();
    stage.addActor(buildRoot());
    entity.getEvents().addListener(ROOM_CHANGE_EVENT, this::onRoomChanged);
  }

  public Minimap(RoomConfig currentRoom) {
    logger.debug("Created with RoomConfig: {}", currentRoom.id);
    this.currentRoom = currentRoom;
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

    // Add containers for actual room icons
    mapContainer.add(buildLHS()).grow();
    mapContainer.add(buildCenter()).grow();
    mapContainer.add(buildRHS()).grow();

    return root;
  }

  private Table buildCenter() {
    var center = new Table();
    attachRooms(center, 1);
    return center;
  }

  private Table buildLHS() {
    var lhs = new Table();
    attachRooms(lhs, 0);
    return lhs;
  }

  private Table buildRHS() {
    var rhs = new Table();
    attachRooms(rhs, 4);
    return rhs;
  }


  /** Clears the children in {@link Minimap#exitContainer} then rebuilds new exit count */
  private void rebuildExits(int count) {
    exitContainer.clearChildren();
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
    rebuildExits(newRoom.exits.length);
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

  private static Actor createRoomIcon() {
    var room = new Rectangle(ROOM_COLOR);
    room.setSize(ROOM_W, ROOM_H);
    return room;
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

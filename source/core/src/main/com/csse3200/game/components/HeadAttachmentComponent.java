package com.csse3200.game.components;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.entities.Entity;

public class HeadAttachmentComponent extends Component {
  private final Entity mainBody;
  private final Vector2 offset;

  public HeadAttachmentComponent(Entity mainBody, Vector2 offset) {
    this.mainBody = mainBody;
    this.offset = offset;
  }

  @Override
  public void update() {
    if (mainBody != null && mainBody.getPosition() != null) {
      entity.setPosition(mainBody.getPosition().cpy().add(offset));
    }
  }
}

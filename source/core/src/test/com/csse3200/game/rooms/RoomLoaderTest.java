package com.csse3200.game.rooms;

import static org.mockito.Mockito.verify;

import com.csse3200.game.components.CameraComponent;
import com.csse3200.game.components.rooms.configs.WorldConfig;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@ExtendWith(GameExtension.class)
public class RoomLoaderTest {
  @Mock WorldConfig worldConfig;
  @Mock CameraComponent camera;
  @Mock RoomClearService roomClear;

  @BeforeEach
  void setup() {
    ServiceLocator.registerRoomClearService(roomClear);
  }

  @Test
  void shouldValidateWorldOnContructor() {
    var roomLoader = new RoomLoader(camera, worldConfig);

    verify(worldConfig).validate();
  }
}

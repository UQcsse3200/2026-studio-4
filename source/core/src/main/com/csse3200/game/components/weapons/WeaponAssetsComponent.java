package com.csse3200.game.components.weapons;

import com.csse3200.game.components.Component;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;

/**
 * Loads the sprites drawn by the wielder's weapons, and releases them when the wielder is disposed.
 *
 * <p>Attach this to whatever carries the weapon components, so the art lives exactly as long as the
 * thing holding it. Weapons resolve their texture lazily when an attack spawns a hitbox, so loading
 * only has to finish before the first attack rather than before the wielder is built.
 *
 * <p>Paths come from the weapons themselves rather than being repeated here. Each weapon owns its
 * own {@code TEXTURE} constant, so a renamed asset is a compile error instead of a crash on the
 * first swing.
 */
public class WeaponAssetsComponent extends Component {
  private static final String[] TEXTURES = {
    SwordWeaponComponent.TEXTURE, KnifeWeaponComponent.TEXTURE, BowWeaponComponent.TEXTURE
  };

  /** Loads the weapon sprites. Follows {@code RoomAssetsComponent} in loading on construction. */
  public WeaponAssetsComponent() {
    ResourceService resourceService = ServiceLocator.getResourceService();
    resourceService.loadTextures(TEXTURES);
    resourceService.loadAll();
  }

  @Override
  public void dispose() {
    ResourceService resourceService = ServiceLocator.getResourceService();
    if (resourceService != null) {
      resourceService.unloadAssets(TEXTURES);
    }
  }
}

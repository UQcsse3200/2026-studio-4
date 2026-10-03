package com.csse3200.game.files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.components.weapons.BowWeaponComponent;
import com.csse3200.game.components.weapons.KnifeWeaponComponent;
import com.csse3200.game.components.weapons.SwordWeaponComponent;
import com.csse3200.game.components.weapons.WeaponSelectionComponent;
import com.csse3200.game.components.weapons.WeaponUpgradeComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.items.ItemType;
import com.csse3200.game.items.WeaponItem.WeaponType;
import com.csse3200.game.items.charms.AttackSpeedCharm;
import com.csse3200.game.items.charms.Charm;
import com.csse3200.game.items.charms.SpeedCharm;
import com.csse3200.game.items.charms.StrengthCharm;
public class GameSaveData {
    public int version = 1;
    public Checkpoint checkpoint = new Checkpoint();
    public PlayerData playerData = new PlayerData();

    public static class Checkpoint{
        public String roomId;
        public String entryPointId;
        public int tileX;
        public int tileY;
    } 

    public static class PlayerData{
        public int gold;
        public Map<String, Integer> inventory = new HashMap<>();
        public List<String> charms = new ArrayList<>();
        public List<String> upgradedWeapons = new ArrayList<>();
        public String selectedWeapon = "SWORD";
    }
    
}

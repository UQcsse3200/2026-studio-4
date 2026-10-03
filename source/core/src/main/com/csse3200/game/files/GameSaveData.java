package com.csse3200.game.files;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameSaveData {
  public int version = 1;
  public float playTimeSeconds;
  public Checkpoint checkpoint = new Checkpoint();
  public ResumePosition resumePosition;
  public PlayerData playerData = new PlayerData();

  public static class Checkpoint {
    public String roomId;
    public String entryPointId;
    public int tileX;
    public int tileY;
  }

  public static class ResumePosition {
    public String roomId;
    public float x;
    public float y;
  }

  public static class PlayerData {
    public int gold;
    public Map<String, Integer> inventory = new HashMap<>();
    public List<String> charms = new ArrayList<>();
    public List<String> upgradedWeapons = new ArrayList<>();
    public String selectedWeapon = "SWORD";
  }
}

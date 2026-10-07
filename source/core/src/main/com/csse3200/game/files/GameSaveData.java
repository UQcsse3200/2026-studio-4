package com.csse3200.game.files;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GameSaveData {
  public int version = 1;
  public float playTimeSeconds;
  public Map<String, Float> dungeonTimesSeconds = new LinkedHashMap<>();
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

    /** Null in legacy saves; otherwise four ordered slots, with null for empty positions. */
    public List<String> consumableSlots;

    /** Null in legacy saves; otherwise one equipped flag per charm, including duplicates. */
    public List<Boolean> charmEquipped;

    public List<String> upgradedWeapons = new ArrayList<>();
    public String selectedWeapon = "SWORD";
  }
}

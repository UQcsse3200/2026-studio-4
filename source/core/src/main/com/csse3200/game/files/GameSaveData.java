package com.csse3200.game.files;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GameSaveData {
  public int version = 2;
  public float playTimeSeconds;
  public Map<String, Float> dungeonTimesSeconds = new LinkedHashMap<>();
  public ResumePosition resumePosition;
  public PlayerData playerData = new PlayerData();
  public String checkpointRoomId;
  public String checkpointEntryPointId;
  
  // Add these fields
  public List<String> unlockedAchievements = new ArrayList<>();
  public Map<String, Float> achievementProgress = new HashMap<>();

  public static class ResumePosition {
    public String roomId;
    public float x;
    public float y;
  }

  public static class PlayerData {
    public int health;
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

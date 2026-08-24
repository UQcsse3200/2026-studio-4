package com.csse3200.game.items;

/**
 * Represents a charm item that can be stored in the player's inventory.
 */
public class Charm {
    private final String name;

    public Charm(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
package com.catface.camkey.model;

import net.minecraft.world.entity.player.Player;

public record Keyframe(double x, double y, double z, float pitch, float yaw) {
    
    // Helper method to easily capture a player's exact position and rotation
    public static Keyframe capture(Player player) {
        return new Keyframe(
            player.getX(),
            player.getY(),
            player.getZ(),
            player.getXRot(),
            player.getYRot()
        );
    }
}
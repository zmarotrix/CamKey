package com.catface.camkey.playback;

import com.catface.camkey.CamKey;
import com.catface.camkey.model.CameraSequence;
import com.catface.camkey.model.Keyframe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.GameType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.List;

@EventBusSubscriber(modid = CamKey.MODID, value = Dist.CLIENT)
public class PlaybackEngine {
    
    private static boolean isPlaying = false;
    private static CameraSequence currentSequence = null;
    private static int totalTicks = 0;
    private static int currentTick = 0;
    
    // State tracking to restore the user's settings after playback
    private static boolean wasSmoothingEnabled = false; 
    private static GameType previousGameType = null; // NEW: Tracks the player's gamemode

    public static void play(CameraSequence sequence, float durationSeconds) {
        if (sequence.getFrames().size() < 2) return;
        
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        currentSequence = sequence;
        totalTicks = (int) (durationSeconds * 20.0f); 
        currentTick = 0;
        isPlaying = true;
        
        // 1. Save previous states
        wasSmoothingEnabled = CameraSmoother.isSmoothingEnabled;
        previousGameType = Minecraft.getInstance().gameMode.getPlayerMode();

        // 2. Force Spectator Mode to prevent collision jitter and hide the HUD
        if (previousGameType != GameType.SPECTATOR) {
            // Sends the command to the server without needing the '/'
            player.connection.sendCommand("gamemode spectator"); 
        }

        // 3. Disable smoothing momentarily so we can teleport
        CameraSmoother.isSmoothingEnabled = false;
        
        // 4. Teleport player instantly to the exact starting position of the sequence
        Keyframe start = sequence.getFrames().get(0);
        applyKeyframe(player, start.x(), start.y(), start.z(), start.pitch(), start.yaw());

        // 5. Wipe the smoother's memory of the pre-teleport camera angle
        CameraSmoother.reset();

        // 6. Apply the user's playback auto-smooth preference
        CameraSmoother.isSmoothingEnabled = CameraSmoother.autoSmoothPlayback;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (!isPlaying || currentSequence == null) return;
        
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            endPlayback();
            return;
        }

        float progress = (float) currentTick / totalTicks;
        
        if (progress >= 1.0f) {
            Keyframe last = currentSequence.getFrames().get(currentSequence.getFrames().size() - 1);
            applyKeyframe(player, last.x(), last.y(), last.z(), last.pitch(), last.yaw());
            endPlayback();
            return;
        }

        float t = progress * progress * (3 - 2 * progress);
        
        List<Keyframe> frames = currentSequence.getFrames();
        int frameCount = frames.size();
        
        float segmentProgress = t * (frameCount - 1);
        int startIndex = (int) Math.floor(segmentProgress);
        int endIndex = Math.min(startIndex + 1, frameCount - 1);
        
        float localT = segmentProgress - startIndex;
        Keyframe start = frames.get(startIndex);
        Keyframe end = frames.get(endIndex);

        double x = lerp(start.x(), end.x(), localT);
        double y = lerp(start.y(), end.y(), localT);
        double z = lerp(start.z(), end.z(), localT);
        float pitch = lerp(start.pitch(), end.pitch(), localT);
        float yaw = lerpRot(start.yaw(), end.yaw(), localT);

        applyKeyframe(player, x, y, z, pitch, yaw);
        currentTick++;
    }

    private static void endPlayback() {
        isPlaying = false;
        
        // Restore manual smoothing preference
        if (CameraSmoother.autoSmoothPlayback) {
            CameraSmoother.isSmoothingEnabled = wasSmoothingEnabled;
        }

        // NEW: Restore previous gamemode
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && previousGameType != null && previousGameType != GameType.SPECTATOR) {
            // Uses .name().toLowerCase() to safely convert CREATIVE -> "creative"
            player.connection.sendCommand("gamemode " + previousGameType.name().toLowerCase());
        }
        previousGameType = null;
    }

    private static void applyKeyframe(LocalPlayer player, double x, double y, double z, float pitch, float yaw) {
        player.setPos(x, y, z);
        player.setXRot(pitch);
        player.setYRot(yaw);
        player.setYHeadRot(yaw);
    }

    private static double lerp(double start, double end, float t) {
        return start + (end - start) * t;
    }

    private static float lerp(float start, float end, float t) {
        return start + (end - start) * t;
    }

    private static float lerpRot(float start, float end, float t) {
        float delta = end - start;
        while (delta < -180.0F) delta += 360.0F;
        while (delta >= 180.0F) delta -= 360.0F;
        return start + delta * t;
    }
}
package com.catface.camkey.playback;

import com.catface.camkey.CamKey;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * Operates on the per-frame Render Thread (not the 20 TPS game thread).
 * Applies an exponential decay smoothing effect to the camera for cinematic, OptiFine-style zoom movement.
 */
@EventBusSubscriber(modid = CamKey.MODID, value = Dist.CLIENT)
public class CameraSmoother {

    public static boolean isSmoothingEnabled = false;
    public static boolean autoSmoothPlayback = true; 
    
    // Configurable interpolation speed. 1.0 = instant, lower values = heavier smoothing.
    public static final float DEFAULT_SMOOTH_SPEED = 0.05f;
    public static float smoothSpeed = DEFAULT_SMOOTH_SPEED;
    
    private static boolean initialized = false;
    private static double currentYaw;
    private static double currentPitch;

    /**
     * Wipes the smoother's memory of previous angles. 
     * Crucial to call this when teleporting, otherwise the camera will violently drag across the world.
     */
    public static void reset() {
        initialized = false;
    }

    /**
     * Hooked into the viewport render event so we can manipulate the camera 
     * at the monitor's exact refresh rate (e.g., 144hz), bypassing the 20 TPS limit.
     */
    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (!isSmoothingEnabled) {
            initialized = false;
            return;
        }

        double targetYaw = event.getYaw();
        double targetPitch = event.getPitch();

        // Snap immediately on the first frame it is enabled
        if (!initialized) {
            currentYaw = targetYaw;
            currentPitch = targetPitch;
            initialized = true;
            return;
        }

        // Exponential decay algorithm for cinematic easing
        currentPitch += (targetPitch - currentPitch) * smoothSpeed;

        // Wrap delta yaw to prevent violent 360-degree spins when crossing the North pole boundary
        double deltaYaw = targetYaw - currentYaw;
        while (deltaYaw < -180.0) deltaYaw += 360.0;
        while (deltaYaw >= 180.0) deltaYaw -= 360.0;
        currentYaw += deltaYaw * smoothSpeed;

        // Override the actual rendering pipeline
        event.setYaw((float) currentYaw);
        event.setPitch((float) currentPitch);
    }
}
package com.catface.camkey.playback;

import com.catface.camkey.CamKey;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;

@EventBusSubscriber(modid = CamKey.MODID, value = Dist.CLIENT)
public class CameraSmoother {

    public static boolean isSmoothingEnabled = false;
    public static boolean autoSmoothPlayback = true; 
    
    public static final float DEFAULT_SMOOTH_SPEED = 0.05f;
    public static float smoothSpeed = DEFAULT_SMOOTH_SPEED;
    
    private static boolean initialized = false;
    private static double currentYaw;
    private static double currentPitch;

    public static void reset() {
        initialized = false;
    }

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (!isSmoothingEnabled) {
            initialized = false;
            return;
        }

        double targetYaw = event.getYaw();
        double targetPitch = event.getPitch();

        if (!initialized) {
            currentYaw = targetYaw;
            currentPitch = targetPitch;
            initialized = true;
            return;
        }

        currentPitch += (targetPitch - currentPitch) * smoothSpeed;

        double deltaYaw = targetYaw - currentYaw;
        while (deltaYaw < -180.0) deltaYaw += 360.0;
        while (deltaYaw >= 180.0) deltaYaw -= 360.0;
        currentYaw += deltaYaw * smoothSpeed;

        event.setYaw((float) currentYaw);
        event.setPitch((float) currentPitch);
    }
}
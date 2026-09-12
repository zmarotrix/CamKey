package com.catface.camkey.command;

import com.catface.camkey.CamKey;
import com.catface.camkey.model.CameraSequence;
import com.catface.camkey.model.Keyframe;
import com.catface.camkey.playback.PlaybackEngine;
import com.catface.camkey.playback.CameraSmoother;
import com.catface.camkey.storage.SequenceStorage;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

@EventBusSubscriber(modid = CamKey.MODID, value = Dist.CLIENT)
public class CamCommand {

    @SubscribeEvent
    public static void onClientCommands(RegisterClientCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("camkey")
            
            // ---------------------------------------------------------
            // COMMAND: /camkey smoothing
            // Purpose: Toggles cinematic camera smoothing on/off manually.
            // ---------------------------------------------------------
            .then(Commands.literal("smoothing")
                .executes(context -> {
                    CameraSmoother.isSmoothingEnabled = !CameraSmoother.isSmoothingEnabled;
                    boolean state = CameraSmoother.isSmoothingEnabled;
                    
                    context.getSource().sendSuccess(() -> Component.literal("Camera smoothing is now: " + (state ? "ON" : "OFF")), false);
                    return 1;
                })
            )
            
            // ---------------------------------------------------------
            // COMMAND: /camkey autosmooth
            // Purpose: Determines if smoothing automatically activates during playback.
            // ---------------------------------------------------------
            .then(Commands.literal("autosmooth")
                .executes(context -> {
                    CameraSmoother.autoSmoothPlayback = !CameraSmoother.autoSmoothPlayback;
                    boolean state = CameraSmoother.autoSmoothPlayback;
                    
                    context.getSource().sendSuccess(() -> Component.literal("Auto-smoothing during playback is now: " + (state ? "ON" : "OFF")), false);
                    return 1;
                })
            )
            
            // ---------------------------------------------------------
            // COMMAND: /camkey speed [default|<value>]
            // Purpose: Adjusts the interpolation speed of the camera smoother.
            // ---------------------------------------------------------
            .then(Commands.literal("speed")
                // Branch 1: No arguments (Prints current speed & default)
                .executes(context -> {
                    float current = CameraSmoother.smoothSpeed;
                    float def = CameraSmoother.DEFAULT_SMOOTH_SPEED;
                    context.getSource().sendSuccess(() -> Component.literal("Current smoothing speed: " + current + " (Default: " + def + ")"), false);
                    return 1;
                })
                
                // Branch 2: Reset to default keyword
                .then(Commands.literal("default")
                    .executes(context -> {
                        CameraSmoother.smoothSpeed = CameraSmoother.DEFAULT_SMOOTH_SPEED;
                        context.getSource().sendSuccess(() -> Component.literal("Camera smoothing speed reset to default (" + CameraSmoother.DEFAULT_SMOOTH_SPEED + ")"), false);
                        return 1;
                    })
                )
                
                // Branch 3: Set specific float value (0.01 to 1.0)
                .then(Commands.argument("value", FloatArgumentType.floatArg(0.01f, 1.0f))
                    .executes(context -> {
                        float newSpeed = FloatArgumentType.getFloat(context, "value");
                        CameraSmoother.smoothSpeed = newSpeed;
                        
                        context.getSource().sendSuccess(() -> Component.literal("Camera smoothing speed set to: " + newSpeed), false);
                        return 1;
                    })
                )
            )
            
            // ---------------------------------------------------------
            // COMMAND: /camkey add <sequence_name>
            // Purpose: Captures current pos/rot to a named sequence.
            // ---------------------------------------------------------
            .then(Commands.literal("add")
                .then(Commands.argument("sequence", StringArgumentType.word())
                    .executes(context -> {
                        String seqName = StringArgumentType.getString(context, "sequence");
                        LocalPlayer player = Minecraft.getInstance().player;
                        
                        if (player == null) return 0;

                        // Retrieve or create sequence, add frame, and save to disk immediately 
                        // to prevent data loss if the game crashes during a recording session.
                        CameraSequence sequence = SequenceStorage.getOrCreateSequence(seqName);
                        sequence.addKeyframe(Keyframe.capture(player));
                        SequenceStorage.saveSequence(seqName);
                        
                        context.getSource().sendSuccess(() -> Component.literal("Captured frame for '" + seqName + "' (Total frames: " + sequence.getFrames().size() + ")"), false);
                        return 1;
                    })
                )
            )

            // ---------------------------------------------------------
            // COMMAND: /camkey delete <sequence_name>
            // Purpose: Deletes a sequence from memory and disk.
            // ---------------------------------------------------------
            .then(Commands.literal("delete")
                .then(Commands.argument("sequence", StringArgumentType.word())
                    .executes(context -> {
                        String seqName = StringArgumentType.getString(context, "sequence");
                        
                        boolean success = SequenceStorage.deleteSequence(seqName);
                        if (success) {
                            context.getSource().sendSuccess(() -> Component.literal("Successfully deleted sequence '" + seqName + "'"), false);
                        } else {
                            // Production readiness: Graceful failure if they typo the name
                            context.getSource().sendFailure(Component.literal("Sequence '" + seqName + "' does not exist."));
                        }
                        
                        return 1;
                    })
                )
            )
            
            // ---------------------------------------------------------
            // COMMAND: /camkey play <sequence_name> <duration_seconds>
            // Purpose: Initiates cinematic playback of a sequence.
            // ---------------------------------------------------------
            .then(Commands.literal("play")
                .then(Commands.argument("sequence", StringArgumentType.word())
                    .then(Commands.argument("duration", FloatArgumentType.floatArg(0.1f))
                        .executes(context -> {
                            String seqName = StringArgumentType.getString(context, "sequence");
                            float duration = FloatArgumentType.getFloat(context, "duration");
                            
                            CameraSequence sequence = SequenceStorage.getSequence(seqName);
                            
                            // Production readiness: Fail gracefully if sequence doesn't exist
                            if (sequence == null) {
                                context.getSource().sendFailure(Component.literal("Sequence '" + seqName + "' does not exist!"));
                                return 0;
                            }
                            // Fail gracefully if sequence doesn't have enough points to interpolate
                            if (sequence.getFrames().size() < 2) {
                                context.getSource().sendFailure(Component.literal("Sequence '" + seqName + "' needs at least 2 keyframes to play!"));
                                return 0;
                            }

                            // Hand off to the playback engine
                            PlaybackEngine.play(sequence, duration);
                            context.getSource().sendSuccess(() -> Component.literal("Playing '" + seqName + "' over " + duration + "s"), false);
                            
                            return 1;
                        })
                    )
                )
            )
        );
    }
}
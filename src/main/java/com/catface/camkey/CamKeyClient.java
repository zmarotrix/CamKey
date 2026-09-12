package com.catface.camkey;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@Mod(value = CamKey.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = CamKey.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class CamKeyClient {
    
    public CamKeyClient(ModContainer container) {
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        CamKey.LOGGER.info("CamKey client setup complete.");
    }
}
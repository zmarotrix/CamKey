package com.catface.camkey;

import com.catface.camkey.storage.SequenceStorage;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(CamKey.MODID)
public class CamKey {
    public static final String MODID = "camkey";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CamKey(IEventBus modEventBus, ModContainer modContainer) {
        
        // Load any saved camera sequences from disk
        SequenceStorage.loadAllSequences();
        
        LOGGER.info("CamKey mod initialized. Ready to record!");
    }
}
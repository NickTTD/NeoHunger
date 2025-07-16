/*
 * NeoHunger.java
 * Main mod class for the NeoHunger mod for Minecraft 1.7.10.
 * Purpose:
 * - Entry point for the NeoHunger mod, which overhauls hunger, exhaustion, and natural regeneration mechanics to be
 * more modern and configurable.
 * - Registers proxies, event handlers, and sets up the mod lifecycle.
 * Author: (your name or mod name here)
 */
package com.nickttd.neohunger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

@Mod(modid = NeoHunger.MODID, version = Tags.VERSION, name = "NeoHunger", acceptedMinecraftVersions = "[1.7.10]")
public class NeoHunger {

    public static final String MODID = "neohunger";
    public static final Logger LOG = LogManager.getLogger(MODID);

    @SidedProxy(clientSide = "com.nickttd.neohunger.ClientProxy", serverSide = "com.nickttd.neohunger.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    // preInit "Run before anything else. Read your config, create blocks, items, etc, and register them with the
    // GameRegistry." (Remove if not needed)
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit(event);
    }

    @Mod.EventHandler
    // load "Do your mod setup. Build whatever data structures you care about. Register recipes." (Remove if not needed)
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
        // Register our custom hunger/regen handler
        HungerRegenHandler handler = new HungerRegenHandler();
        FMLCommonHandler.instance()
            .bus()
            .register(handler);
        HungerRegenHandler.setInstance(handler);
        // Register proxy for client-side events
        if (FMLCommonHandler.instance()
            .getSide()
            .isClient()) {
            FMLCommonHandler.instance()
                .bus()
                .register(proxy);
        }
    }

    @Mod.EventHandler
    // postInit "Handle interaction with other mods, complete your setup based on this." (Remove if not needed)
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit(event);
    }

    @Mod.EventHandler
    // register server commands in this event handler (Remove if not needed)
    public void serverStarting(FMLServerStartingEvent event) {
        proxy.serverStarting(event);
    }
}

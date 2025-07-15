/*
 * HungerRegenHandler.java
 *
 * Handles custom hunger and natural regeneration logic for NeoHunger.
 * Replaces 1.7.10 passive healing with the newer saturation-based healing and
 * "slow" healing when you have 9 or more shanks.
 *
 * Author: NickTTD
 */
package com.nickttd.neohunger;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.PlayerTickEvent;

public class HungerRegenHandler {

    // Timers for tracking healing per player
    private final Map<Integer, Integer> hungerHealTimer = new HashMap<>();
    private final Map<Integer, Integer> saturationHealTimer = new HashMap<>();

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent event) {
        // Only run at end of tick
        if (event.phase != PlayerTickEvent.Phase.END) return;
        EntityPlayer player = event.player;
        World world = player.worldObj;
        int id = player.getEntityId();
        float health = player.getHealth();
        float maxHealth = player.getMaxHealth();
        int foodLevel = player.getFoodStats().getFoodLevel();
        float saturation = player.getFoodStats().getSaturationLevel();

        // Skip on client side
        if (world.isRemote) return;

        // Only heal if natural regeneration is enabled
        if (!world.getGameRules().getGameRuleBooleanValue("naturalRegeneration")) return;

        // Cancel vanilla regen by resetting foodTimer via reflection
        try {
            Field foodTimerField = player.getFoodStats().getClass().getDeclaredField("foodTimer");
            foodTimerField.setAccessible(true);
            foodTimerField.setInt(player.getFoodStats(), 0);
        } catch (Exception e) {
            // Ignore errors
        }
        // Saturation-based healing: fast heal if saturation is available
        boolean didSaturationHeal = false;
        if (foodLevel >= 20 && saturation > 0 && health < maxHealth) {
            int timer = saturationHealTimer.getOrDefault(id, 0) + 1;
            if (timer >= 10) { // Heal every 0.5 seconds
                float healAmount = Math.min(1.0F, maxHealth - health); // Heal up to 1 health
                float saturationCost = 2.0F;
                player.heal(healAmount);
                player.getFoodStats().setFoodSaturationLevel(Math.max(0, saturation - saturationCost));
                timer = 0;
                didSaturationHeal = true;
            }
            saturationHealTimer.put(id, timer);
        } else {
            saturationHealTimer.put(id, 0);
        }

        // Hunger-based healing: slower heal when you don't have saturation
        // And have >=9 shanks 
        if (!didSaturationHeal && foodLevel >= 18 && health < maxHealth) {
            int timer = hungerHealTimer.getOrDefault(id, 0) + 1;
            if (timer >= 80) { // Heal every 4 seconds
                player.heal(1.0F);
                // Add exhaustion: 6 per heal (4 = 1 hunger, 2 = saturation)
                player.getFoodStats().addExhaustion(6.0F);
                timer = 0;
            }
            hungerHealTimer.put(id, timer);
        } else {
            hungerHealTimer.put(id, 0);
        }
    }

    // Singleton instance for client access
    private static HungerRegenHandler instance;

    public static void setInstance(HungerRegenHandler handler) {
        instance = handler;
    }

    public static HungerRegenHandler getInstance() {
        return instance;
    }
}

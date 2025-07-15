/*
 * HeartBlink.java
 *
 * Main mixin of NeoHunger.
 * The purpose of this mixin is just to make the health bar blink
 * when you restore health, just like in newer mc versions.
 * 
 * I'm not sure at all when this effect was added because it's not mentioned on
 * https://minecraft.wiki/w/
 * But it was probably on 1.9 15w40a
 * 
 * Author: NickTTD
 */
package com.nickttd.neohunger.mixin;

import net.minecraftforge.client.GuiIngameForge;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(GuiIngameForge.class)
public class HeartBlink {
    // Custom fields for tracking healing blink
    @Unique
    private int neoHunger$blinkEndTick$ = 0;
    @Unique
    private float neoHunger$lastHealth$ = -1;

    @ModifyVariable(
        method = "renderHealth",
        at = @At("STORE"),
        remap = false
    )
    private boolean modernBlinkLogic(boolean highlight) {
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        if (player == null) return false;

        float currentHealth = player.getHealth();
        // The following line may trigger the error 'Mixin class cannot be referenced directly'.
        // This is because I used a Mixin accessor interface from within a Mixin class.
        // However, this is the only way to access a superclass field (updateCounter)-
        // from a subclass mixin in 1.7.10, and it works correctly at runtime.
        // So I'll just leave this comment here for when it eventually decides to not work.
        int updateCounter = ((GuiIngameAccessor)this).getUpdateCounter();

        if (neoHunger$lastHealth$ < 0) {
            neoHunger$lastHealth$ = currentHealth;
        }

        if (currentHealth < neoHunger$lastHealth$) {
            // Took damage: 20 ticks
            neoHunger$blinkEndTick$ = updateCounter + 20;
        } else if (currentHealth > neoHunger$lastHealth$) {
            // Healed: 10 ticks
            neoHunger$blinkEndTick$ = updateCounter + 10;
        }

        neoHunger$lastHealth$ = currentHealth;

        int remaining = neoHunger$blinkEndTick$ - updateCounter;
        return remaining > 0 && (remaining / 3) % 2 == 1;
    }
}

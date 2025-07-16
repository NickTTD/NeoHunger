package com.nickttd.neohunger.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(targets = "tconstruct.armor.ArmorProxyClient")
public class HeartBlinkTinkers {

    static {
        System.out.println("[NeoHunger] HeartBlinkTinkers mixin loaded!");
    }

    @Unique
    private int neoHunger$blinkEndTick$ = 0;
    @Unique
    private float neoHunger$lastHealth$ = -1;

    @ModifyVariable(method = "renderHealthbar", at = @At("STORE"), remap = false)
    private boolean modernBlinkLogic(boolean highlight) {
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        if (player == null) return highlight;

        float currentHealth = player.getHealth();
        int updateCounter = ((ArmorProxyClientAccessor) this).getUpdateCounter();

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
        boolean healingBlink = remaining > 0 && (remaining / 3) % 2 == 1;

        // Return true if either damage highlight OR healing blink is active
        return highlight || healingBlink;
    }
}

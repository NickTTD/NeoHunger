package com.nickttd.neohunger.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "tconstruct.armor.ArmorProxyClient")
public interface ArmorProxyClientAccessor {

    @Accessor("updateCounter")
    int getUpdateCounter();
}

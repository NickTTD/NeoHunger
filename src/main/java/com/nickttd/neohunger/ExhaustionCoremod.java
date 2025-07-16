// Coremod plugin: registers the ASM transformer for exhaustion tweaks
// Required for bytecode patching in Forge 1.7.10
//
// Author: NickTTD

package com.nickttd.neohunger;

import java.util.Map;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;

@IFMLLoadingPlugin.Name("ExhaustionCoremod")
@IFMLLoadingPlugin.MCVersion("1.7.10")
public class ExhaustionCoremod implements IFMLLoadingPlugin {

    @Override
    public String[] getASMTransformerClass() {
        return new String[] { "com.nickttd.neohunger.ExhaustionTransformer" };
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {}

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}

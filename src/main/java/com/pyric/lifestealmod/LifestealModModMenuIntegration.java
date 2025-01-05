package com.pyric.lifestealmod;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public class LifestealModModMenuIntegration implements ModMenuApi {
    @Override

    /**
     * getModConfigScreenFactory() sets up the screen for Mod Menu using cloth config and the Mod Menu api.
     */
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return LifestealModConfigScreen::create;
    }
}

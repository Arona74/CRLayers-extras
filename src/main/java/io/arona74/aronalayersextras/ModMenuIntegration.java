package io.arona74.aronalayersextras;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.loader.api.FabricLoader;

public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        // Cloth Config is optional: without this guard, linking the config
        // screen code would throw NoClassDefFoundError and ModMenu would report
        // the whole mod as a broken ModMenuApi implementation.
        if (!isClothConfigLoaded()) {
            AronaLayersExtras.LOGGER.info(
                    "Cloth Config is not installed, so the in-game config screen is unavailable. "
                            + "Edit config/aronalayersextras.json instead.");
            return parent -> null;
        }
        return ClothConfigScreenFactory.create();
    }

    private static boolean isClothConfigLoaded() {
        FabricLoader loader = FabricLoader.getInstance();
        return loader.isModLoaded("cloth-config") || loader.isModLoaded("cloth-config2");
    }
}

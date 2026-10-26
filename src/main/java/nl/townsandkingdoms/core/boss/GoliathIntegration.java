package nl.townsandkingdoms.core.boss;

import java.nio.file.Files;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;

/** Dependency gate kept separate from GeckoLib entity class initialization. */
public final class GoliathIntegration {
    private GoliathIntegration() {}

    public static boolean dependenciesPresent() {
        ModList mods = ModList.get();
        return mods != null && mods.isLoaded("geckolib") && mods.isLoaded("twilightforest")
                && mods.isLoaded("deeperdarker") && mods.isLoaded("kubejs");
    }

    public static boolean packEnabled() {
        return dependenciesPresent() && Files.isRegularFile(FMLPaths.GAMEDIR.get()
                .resolve("kubejs/config/tnk3_goliath_enabled.json"));
    }

    public static void register(IEventBus bus) {
        if (dependenciesPresent()) SculkGoliathBootstrap.register(bus);
    }
}

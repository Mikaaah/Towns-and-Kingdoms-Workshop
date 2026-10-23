package nl.townsandkingdoms.core;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(TownsAndKingdomsCore.MOD_ID)
public final class TownsAndKingdomsCore {
    public static final String MOD_ID = "tnkcore";
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    public static final DeferredItem<SpawnerCoreItem> SPAWNER_CORE = ITEMS.register("spawner_core",
            () -> new SpawnerCoreItem(new Item.Properties().stacksTo(1)));
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("core", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.tnkcore"))
            .icon(() -> SPAWNER_CORE.toStack())
            .displayItems((parameters, output) -> output.accept(SPAWNER_CORE.get())).build());

    public TownsAndKingdomsCore(IEventBus bus) {
        ITEMS.register(bus);
        TABS.register(bus);
    }
}

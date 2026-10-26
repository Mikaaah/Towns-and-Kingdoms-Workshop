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
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MOD_ID);
    public static final net.neoforged.neoforge.registries.DeferredBlock<nl.townsandkingdoms.core.projector.ProjectorBlock> PROJECTOR = BLOCKS.register("quest_projector",
            () -> new nl.townsandkingdoms.core.projector.ProjectorBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(2F).noOcclusion().lightLevel(state -> 7).sound(net.minecraft.world.level.block.SoundType.NETHERITE_BLOCK)));
    public static final DeferredRegister<net.minecraft.world.level.block.entity.BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MOD_ID);
    public static final DeferredHolder<net.minecraft.world.level.block.entity.BlockEntityType<?>,net.minecraft.world.level.block.entity.BlockEntityType<nl.townsandkingdoms.core.projector.ProjectorBlockEntity>> PROJECTOR_ENTITY = BLOCK_ENTITIES.register("quest_projector",
            () -> net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(nl.townsandkingdoms.core.projector.ProjectorBlockEntity::new, PROJECTOR.get()).build(null));
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    public static final DeferredItem<SpawnerCoreItem> SPAWNER_CORE = ITEMS.register("spawner_core",
            () -> new SpawnerCoreItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<nl.townsandkingdoms.core.projector.ProjectorItem> PROJECTOR_ITEM = ITEMS.register("quest_projector", () -> new nl.townsandkingdoms.core.projector.ProjectorItem(PROJECTOR.get(), new Item.Properties()));
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("core", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.tnkcore"))
            .icon(() -> SPAWNER_CORE.toStack())
            .displayItems((parameters, output) -> { output.accept(SPAWNER_CORE.get()); output.accept(PROJECTOR_ITEM.get()); nl.townsandkingdoms.core.construction.ConstructionSet.creative(output); }).build());

    public TownsAndKingdomsCore(IEventBus bus) {
        nl.townsandkingdoms.core.construction.ConstructionSet.register(bus);
        BLOCKS.register(bus);
        BLOCK_ENTITIES.register(bus);
        ITEMS.register(bus);
        bus.addListener(nl.townsandkingdoms.core.projector.ProjectorNetwork::register);
        TABS.register(bus);
    }
}

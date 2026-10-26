package nl.townsandkingdoms.core.boss;


import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.townsandkingdoms.core.TownsAndKingdomsCore;

/**
 * Isolated TNK3-only feature. The remaining Workshop items and blocks do not
 * depend on GeckoLib, Twilight Forest, Deeper and Darker, or KubeJS.
 */
public final class SculkGoliathBootstrap {
    private SculkGoliathBootstrap() {}

    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, TownsAndKingdomsCore.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<SculkGoliathEntity>> SCULK_GOLIATH =
            ENTITY_TYPES.register("sculk_goliath", () -> EntityType.Builder
                    .of(SculkGoliathEntity::new, MobCategory.MONSTER)
                    .sized(3.2F, 4.5F)
                    .clientTrackingRange(16)
                    .build("tnkcore:sculk_goliath"));

    public static final DeferredHolder<EntityType<?>, EntityType<GoliathEffectEntity>> SCULK_EFFECT =
            ENTITY_TYPES.register("sculk_goliath_effect", () -> EntityType.Builder
                    .of(GoliathEffectEntity::new, MobCategory.MISC)
                    .sized(2.0F, 2.0F)
                    .clientTrackingRange(12)
                    .build("tnkcore:sculk_goliath_effect"));

    public static boolean dependenciesPresent() {
        return GoliathIntegration.dependenciesPresent();
    }

    /** The marker is distributed only in the TNK3 KubeJS directory. */
    public static boolean packEnabled() {
        return GoliathIntegration.packEnabled();
    }

    public static void register(IEventBus bus) {
        if (!dependenciesPresent()) return;
        ENTITY_TYPES.register(bus);
        bus.addListener(SculkGoliathBootstrap::attributes);
        NeoForge.EVENT_BUS.addListener(SculkGoliathEncounters::onPlayerTick);
    }

    private static void attributes(EntityAttributeCreationEvent event) {
        event.put(SCULK_GOLIATH.get(), SculkGoliathEntity.createAttributes().build());
        event.put(SCULK_EFFECT.get(), net.minecraft.world.entity.monster.Monster
                .createMonsterAttributes().add(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH, 1.0).build());
    }

    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        if (dependenciesPresent()) {
            event.registerEntityRenderer(SCULK_GOLIATH.get(), SculkGoliathRenderer::new);
            event.registerEntityRenderer(SCULK_EFFECT.get(), GoliathEffectRenderer::new);
        }
    }
}

package nl.townsandkingdoms.core.construction;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import nl.townsandkingdoms.core.TownsAndKingdomsCore;

public final class ConstructionSet {
    private static final DeferredRegister.Items ITEMS=TownsAndKingdomsCore.ITEMS;
    public static final DeferredItem<PencilItem> PENCIL=ITEMS.register("measuring_pencil",()->new PencilItem(props()));
    public static final DeferredItem<SatchelItem> SATCHEL=ITEMS.register("builders_satchel",()->new SatchelItem(props()));
    public static final DeferredItem<BrushItem> BRUSH=ITEMS.register("builders_brush",()->new BrushItem(props()));
    public static final DeferredItem<MobCageItem> CAGE=ITEMS.register("mob_cage",()->new MobCageItem(props()));
    public static final DeferredItem<MirrorItem> MIRROR=ITEMS.register("magic_mirror",()->new MirrorItem(props(),false));
    public static final DeferredItem<MirrorItem> AREA_MIRROR=ITEMS.register("chorus_mirror",()->new MirrorItem(props(),true));
    private static final DeferredRegister<MenuType<?>> MENUS=DeferredRegister.create(Registries.MENU,TownsAndKingdomsCore.MOD_ID);
    public static final DeferredHolder<MenuType<?>,MenuType<KitMenu>> KIT_MENU=MENUS.register("construction_kit",()->IMenuTypeExtension.create((id,inventory,buf)->new KitMenu(id,inventory,buf.readEnum(net.minecraft.world.InteractionHand.class))));
    private static Item.Properties props(){return new Item.Properties().stacksTo(1);}
    public static void register(IEventBus bus){MENUS.register(bus);}
    public static void creative(net.minecraft.world.item.CreativeModeTab.Output output){for(var item:new DeferredItem<?>[]{PENCIL,SATCHEL,BRUSH,CAGE,MIRROR,AREA_MIRROR})output.accept(item.get());}
}

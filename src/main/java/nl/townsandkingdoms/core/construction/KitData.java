package nl.townsandkingdoms.core.construction;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class KitData {
    private KitData() {}
    public static CompoundTag read(ItemStack stack){return stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();}
    public static void write(ItemStack stack,CompoundTag data){if(data.isEmpty())stack.remove(DataComponents.CUSTOM_DATA);else stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));}
    public static boolean flag(ItemStack stack,String key,boolean fallback){var t=read(stack);return t.contains(key)?t.getBoolean(key):fallback;}
}

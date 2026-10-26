package nl.townsandkingdoms.core.projector;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;

public final class ProjectorItem extends BlockItem {
    public ProjectorItem(net.minecraft.world.level.block.Block block, Properties properties) { super(block,properties); }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        super.appendHoverText(stack,context,lines,flag);
        lines.add(Component.translatable("tooltip.tnkcore.projector.use").withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("tooltip.tnkcore.projector.modes").withStyle(ChatFormatting.DARK_PURPLE));
        var tag=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
        if(tag.contains("projector")) lines.add(Component.translatable("tooltip.tnkcore.projector.configured").withStyle(ChatFormatting.GOLD));
    }
}

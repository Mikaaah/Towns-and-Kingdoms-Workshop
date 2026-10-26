package nl.townsandkingdoms.core.construction;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public final class PencilItem extends Item {
    public PencilItem(Properties p){super(p);}
    @Override public InteractionResult onItemUseFirst(ItemStack stack,UseOnContext c){return useOn(c);}
    @Override public InteractionResult useOn(UseOnContext c){
        if(c.getPlayer()==null)return InteractionResult.PASS;
        if(c.getLevel().isClientSide)return InteractionResult.SUCCESS;
        var stack=c.getItemInHand();var data=KitData.read(stack);String dim=c.getLevel().dimension().location().toString();
        if(c.getPlayer().isShiftKeyDown()){clear(stack);c.getPlayer().displayClientMessage(Component.literal("Measurement cleared."),true);return InteractionResult.CONSUME;}
        var a=NbtUtils.readBlockPos(data,"measure_a");
        if(a.isEmpty()||data.contains("measure_b")||!dim.equals(data.getString("measure_dimension"))){
            data.put("measure_a",NbtUtils.writeBlockPos(c.getClickedPos()));data.remove("measure_b");data.putString("measure_dimension",dim);
            KitData.write(stack,data);c.getPlayer().displayClientMessage(Component.literal("First point set. Select the second point."),true);
        }else{
            data.put("measure_b",NbtUtils.writeBlockPos(c.getClickedPos()));KitData.write(stack,data);
            c.getPlayer().displayClientMessage(measurement(a.get(),c.getClickedPos()),false);
        }return InteractionResult.CONSUME;
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        var stack=player.getItemInHand(hand);if(player.isShiftKeyDown()){if(!level.isClientSide)clear(stack);return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);}return InteractionResultHolder.pass(stack);
    }
    public static void clear(ItemStack stack){var data=KitData.read(stack);data.remove("measure_a");data.remove("measure_b");data.remove("measure_dimension");KitData.write(stack,data);}
    public static Component measurement(BlockPos a,BlockPos b){
        int x=Math.abs(a.getX()-b.getX()),y=Math.abs(a.getY()-b.getY()),z=Math.abs(a.getZ()-b.getZ());
        return Component.literal(String.format(java.util.Locale.ROOT,"Size: %d × %d × %d blocks | Distance: %.2f blocks",x+1,y+1,z+1,Math.sqrt(a.distSqr(b))));
    }
    @Override public void appendHoverText(ItemStack stack,TooltipContext c,List<Component> lines,TooltipFlag flag){
        lines.add(Component.literal("Right-click two blocks to measure. Sneak-right-click to clear.").withStyle(ChatFormatting.GRAY));
        var data=KitData.read(stack);var a=NbtUtils.readBlockPos(data,"measure_a");var b=NbtUtils.readBlockPos(data,"measure_b");
        if(a.isPresent()&&b.isPresent())lines.add(measurement(a.get(),b.get()).copy().withStyle(ChatFormatting.LIGHT_PURPLE));
    }
}

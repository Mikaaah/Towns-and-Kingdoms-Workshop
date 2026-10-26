package nl.townsandkingdoms.core.construction;

import java.util.*;
import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;

public final class SatchelItem extends Item {
    public static final int SLOTS=27;
    private record Held(int slot,ItemStack template){}
    private static final Map<ServerPlayer,Held> LAST_HELD=new WeakHashMap<>();
    public SatchelItem(Properties p){super(p);}
    public static boolean accepts(ItemStack stack){return stack.getItem() instanceof BlockItem&&!stack.has(DataComponents.CONTAINER)&&!stack.has(DataComponents.BLOCK_ENTITY_DATA);}
    public static NonNullList<ItemStack> contents(ItemStack bag){var items=NonNullList.withSize(SLOTS,ItemStack.EMPTY);bag.getOrDefault(DataComponents.CONTAINER,ItemContainerContents.EMPTY).copyInto(items);return items;}
    public static void save(ItemStack bag,List<ItemStack> items){bag.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(items));}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        if(player instanceof ServerPlayer sp)sp.openMenu(new SimpleMenuProvider((id,inv,p)->new KitMenu(id,inv,hand),getName(player.getItemInHand(hand))),buf->buf.writeEnum(hand));
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand),level.isClientSide);
    }
    public static int extract(Player player,ItemStack template,int wanted){
        int amount=0;
        for(int slot=0;slot<player.getInventory().getContainerSize()&&amount<wanted;slot++){
            ItemStack bag=player.getInventory().getItem(slot);
            if(!(bag.getItem() instanceof SatchelItem)||!KitData.flag(bag,"satchel_refill",true))continue;
            var items=contents(bag);boolean changed=false;
            for(var item:items)if(ItemStack.isSameItemSameComponents(item,template)&&!item.isEmpty()){
                int take=Math.min(item.getCount(),wanted-amount);item.shrink(take);amount+=take;changed|=take>0;if(amount==wanted)break;
            }
            if(changed)save(bag,items);
        }return amount;
    }
    public static void restock(ServerPlayer player){
        if(player.tickCount%4!=0)return;
        if(player.containerMenu!=player.inventoryMenu||!player.isAlive()||player.isSpectator()){LAST_HELD.remove(player);return;}
        var held=player.getMainHandItem();int selected=player.getInventory().selected;
        if(held.getItem() instanceof BlockItem){
            LAST_HELD.put(player,new Held(selected,held.copyWithCount(1)));
            int room=held.getMaxStackSize()-held.getCount();if(room>0)held.grow(extract(player,held,room));
        }else if(held.isEmpty()){
            var old=LAST_HELD.get(player);
            if(old!=null&&old.slot==selected){int count=extract(player,old.template,old.template.getMaxStackSize());if(count>0)player.setItemInHand(InteractionHand.MAIN_HAND,old.template.copyWithCount(count));}
            else LAST_HELD.remove(player);
        }else LAST_HELD.remove(player);
    }
    @Override public void appendHoverText(ItemStack stack,TooltipContext c,List<Component> lines,TooltipFlag flag){
        lines.add(Component.literal("27 slots for building blocks. Right-click to open.").withStyle(ChatFormatting.GRAY));
        lines.add(Component.literal("Held-block refill: "+(KitData.flag(stack,"satchel_refill",true)?"on":"off")).withStyle(ChatFormatting.LIGHT_PURPLE));
    }
}

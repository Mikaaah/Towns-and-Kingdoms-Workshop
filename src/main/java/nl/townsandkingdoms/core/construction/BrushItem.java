package nl.townsandkingdoms.core.construction;

import java.util.*;
import net.minecraft.ChatFormatting;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.*;
import net.minecraft.world.level.Level;

public final class BrushItem extends Item {
    public static final int SIZE=9;
    public static final String[] MODES={"Weighted random","Sequence","Coordinate pattern"};
    public BrushItem(Properties p){super(p);}
    public static NonNullList<ItemStack> palette(ItemStack brush,HolderLookup.Provider lookup){
        var items=NonNullList.withSize(SIZE,ItemStack.EMPTY);var list=KitData.read(brush).getList("brush_palette",Tag.TAG_COMPOUND);
        for(int i=0;i<Math.min(SIZE,list.size());i++){var stack=ItemStack.parseOptional(lookup,list.getCompound(i));if(stack.getItem() instanceof BlockItem)items.set(i,stack.copyWithCount(1));}return items;
    }
    public static int[] weights(ItemStack brush){var stored=KitData.read(brush).getIntArray("brush_weights");int[] out=new int[SIZE];Arrays.fill(out,1);for(int i=0;i<Math.min(SIZE,stored.length);i++)out[i]=Math.clamp(stored[i],0,64);return out;}
    public static void savePalette(ItemStack brush,List<ItemStack> items,int[] weights,HolderLookup.Provider lookup){
        var data=KitData.read(brush);var list=new ListTag();for(int i=0;i<SIZE;i++)list.add(items.get(i).isEmpty()?new CompoundTag():items.get(i).copyWithCount(1).save(lookup));
        data.put("brush_palette",list);data.putIntArray("brush_weights",weights);KitData.write(brush,data);
    }
    public static int choose(int[] weights,double sample){long total=0;for(int w:weights)total+=Math.max(0,w);if(total==0)return -1;long hit=Math.min(total-1,(long)(Math.clamp(sample,0D,Math.nextDown(1D))*total));for(int i=0;i<weights.length;i++){hit-=Math.max(0,weights[i]);if(hit<0)return i;}return -1;}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        if(player instanceof ServerPlayer sp)sp.openMenu(new SimpleMenuProvider((id,inv,p)->new KitMenu(id,inv,hand),getName(player.getItemInHand(hand))),buf->buf.writeEnum(hand));
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand),level.isClientSide);
    }
    @Override public InteractionResult onItemUseFirst(ItemStack brush,UseOnContext c){return useOn(c);}
    @Override public InteractionResult useOn(UseOnContext c){
        var p=c.getPlayer();if(p==null)return InteractionResult.FAIL;
        if(p.isShiftKeyDown())return use(c.getLevel(),p,c.getHand()).getResult();
        if(c.getLevel().isClientSide)return InteractionResult.SUCCESS;
        if(!p.getAbilities().mayBuild||p.isSpectator())return InteractionResult.FAIL;
        if(!c.getLevel().mayInteract(p,c.getClickedPos())||!p.mayUseItemAt(c.getClickedPos(),c.getClickedFace(),c.getItemInHand()))return InteractionResult.FAIL;
        var brush=c.getItemInHand();var items=palette(brush,c.getLevel().registryAccess());int[] weights=weights(brush);var sources=new Material[SIZE];
        boolean satchels=KitData.flag(brush,"brush_satchel",true);var data=KitData.read(brush);
        for(int i=0;i<SIZE;i++){if(items.get(i).isEmpty()||weights[i]==0)continue;sources[i]=find(p,items.get(i),satchels);if(sources[i]==null)weights[i]=0;}
        int alternatives=0;for(int w:weights)if(w>0)alternatives++;
        int last=data.contains("brush_last")?data.getInt("brush_last"):-1;
        if(alternatives>1&&KitData.flag(brush,"brush_avoid_repeat",false)&&last>=0&&last<SIZE&&!items.get(last).isEmpty()){
            boolean different=false;for(int i=0;i<SIZE;i++)if(weights[i]>0&&!ItemStack.isSameItemSameComponents(items.get(i),items.get(last)))different=true;
            if(different)for(int i=0;i<SIZE;i++)if(ItemStack.isSameItemSameComponents(items.get(i),items.get(last)))weights[i]=0;
        }
        int mode=Math.clamp(data.getInt("brush_mode"),0,2),cursor=Math.max(0,data.getInt("brush_cursor"));int selected=-1;
        if(mode==1){for(int i=0;i<SIZE;i++){int slot=(cursor+i)%SIZE;if(weights[slot]>0){selected=slot;break;}}}
        else {double random=mode==2?new java.util.Random(net.minecraft.util.Mth.getSeed(new BlockPlaceContext(c).getClickedPos())).nextDouble():c.getLevel().random.nextDouble();selected=choose(weights,random);}
        if(selected<0){p.displayClientMessage(Component.literal("No enabled palette material is available. Sneak-right-click to configure."),true);return InteractionResult.FAIL;}
        Material source=sources[selected];ItemStack block=source.template.copyWithCount(1);
        var hit=new net.minecraft.world.phys.BlockHitResult(c.getClickLocation(),c.getClickedFace(),c.getClickedPos(),c.isInside());
        // One ordinary NeoForge placement transaction around the actual BlockItem.
        // The brush bypasses its own item transaction via onItemUseFirst above.
        var result=block.useOn(new UseOnContext(c.getLevel(),p,c.getHand(),block,hit));
        if(result.consumesAction()){
            if(!p.getAbilities().instabuild&&block.isEmpty())source.consume();
            var now=KitData.read(brush);now.putInt("brush_last",selected);now.putInt("brush_cursor",(selected+1)%SIZE);KitData.write(brush,now);
        }return result;
    }
    private record Material(ItemStack template,java.util.function.BooleanSupplier consumeOne){void consume(){consumeOne.getAsBoolean();}}
    private static Material find(Player player,ItemStack template,boolean satchels){
        for(int i=0;i<player.getInventory().getContainerSize();i++){
            var stack=player.getInventory().getItem(i);if(!stack.isEmpty()&&ItemStack.isSameItemSameComponents(stack,template))return new Material(stack.copyWithCount(1),()->{stack.shrink(1);return true;});
        }
        if(satchels)for(int i=0;i<player.getInventory().getContainerSize();i++){
            var bag=player.getInventory().getItem(i);if(!(bag.getItem() instanceof SatchelItem))continue;var contents=SatchelItem.contents(bag);
            for(int j=0;j<contents.size();j++)if(!contents.get(j).isEmpty()&&ItemStack.isSameItemSameComponents(contents.get(j),template)){
                int index=j;return new Material(template.copyWithCount(1),()->{var current=SatchelItem.contents(bag);if(current.get(index).isEmpty()||!ItemStack.isSameItemSameComponents(current.get(index),template))return false;current.get(index).shrink(1);SatchelItem.save(bag,current);return true;});
            }
        }return null;
    }
    @Override public void appendHoverText(ItemStack stack,TooltipContext c,List<Component> lines,TooltipFlag flag){
        lines.add(Component.literal("Place a mix of real blocks. Sneak-right-click or use in air to configure.").withStyle(ChatFormatting.GRAY));
        lines.add(Component.literal("9 templates · weights 0–64 · random / sequence / coordinate pattern").withStyle(ChatFormatting.LIGHT_PURPLE));
    }
}

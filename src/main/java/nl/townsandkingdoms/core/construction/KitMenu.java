package nl.townsandkingdoms.core.construction;

import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;

public final class KitMenu extends AbstractContainerMenu {
    public final boolean brush;
    public final InteractionHand hand;
    public final ItemStack owner;
    private final Player player;
    private final int lockedHotbar;
    private final SimpleContainer storage;
    public final ContainerData values=new SimpleContainerData(13);
    private boolean loading=true;
    public KitMenu(int id,Inventory inventory,InteractionHand hand){
        super(ConstructionSet.KIT_MENU.get(),id);this.player=inventory.player;this.hand=hand;owner=player.getItemInHand(hand);brush=owner.getItem() instanceof BrushItem;
        lockedHotbar=hand==InteractionHand.MAIN_HAND?inventory.selected:-1;
        int size=brush?9:27;
        storage=new SimpleContainer(size){@Override public void setChanged(){super.setChanged();persist();}};
        var content=brush?BrushItem.palette(owner,player.level().registryAccess()):SatchelItem.contents(owner);
        for(int i=0;i<size;i++)storage.setItem(i,content.get(i));
        var weights=BrushItem.weights(owner);for(int i=0;i<9;i++)values.set(i,weights[i]);
        var data=KitData.read(owner);values.set(9,Math.clamp(data.getInt("brush_mode"),0,2));values.set(10,KitData.flag(owner,"brush_avoid_repeat",false)?1:0);
        values.set(11,KitData.flag(owner,"brush_satchel",true)?1:0);values.set(12,KitData.flag(owner,"satchel_refill",true)?1:0);
        for(int i=0;i<size;i++){
            int x=brush?8+(i%3)*54:8+(i%9)*18,y=brush?26+(i/3)*43:50+(i/9)*18;
            addSlot(new Slot(storage,i,x,y){@Override public boolean mayPlace(ItemStack stack){return !KitMenu.this.brush&&SatchelItem.accepts(stack);}@Override public boolean mayPickup(Player p){return !KitMenu.this.brush;}});
        }
        int invY=brush?166:126;
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addInventorySlot(inventory,col+row*9+9,8+col*18,invY+row*18);
        for(int col=0;col<9;col++)addInventorySlot(inventory,col,8+col*18,invY+58);
        addDataSlots(values);loading=false;
    }
    private void addInventorySlot(Inventory inv,int index,int x,int y){
        addSlot(new Slot(inv,index,x,y){@Override public boolean mayPickup(Player p){return index!=lockedHotbar;}@Override public boolean mayPlace(ItemStack s){return index!=lockedHotbar;}});
    }
    private void persist(){
        if(loading||player.level().isClientSide)return;
        if(brush){int[] weights=new int[9];for(int i=0;i<9;i++)weights[i]=values.get(i);BrushItem.savePalette(owner,storage.getItems(),weights,player.level().registryAccess());
            var d=KitData.read(owner);d.putInt("brush_mode",values.get(9));d.putBoolean("brush_avoid_repeat",values.get(10)!=0);d.putBoolean("brush_satchel",values.get(11)!=0);KitData.write(owner,d);
        }else{SatchelItem.save(owner,storage.getItems());var d=KitData.read(owner);d.putBoolean("satchel_refill",values.get(12)!=0);KitData.write(owner,d);}
        player.getInventory().setChanged();
    }
    @Override public boolean stillValid(Player p){return p.isAlive()&&p.getItemInHand(hand)==owner&&(owner.getItem() instanceof BrushItem||owner.getItem() instanceof SatchelItem);}
    @Override public void clicked(int index,int button,ClickType type,Player p){
        if(!stillValid(p))return;
        if(type==ClickType.SWAP&&(button==lockedHotbar||hand==InteractionHand.OFF_HAND&&button==40))return;
        if(index>=0&&index<slots.size()&&index==storage.getContainerSize()+27+lockedHotbar&&lockedHotbar>=0)return;
        if(brush&&index>=0&&index<9){
            if(type==ClickType.PICKUP){var carried=getCarried();if(carried.isEmpty())storage.setItem(index,ItemStack.EMPTY);else if(carried.getItem() instanceof BlockItem)storage.setItem(index,carried.copyWithCount(1));broadcastChanges();}
            return;
        }
        super.clicked(index,button,type,p);
    }
    @Override public ItemStack quickMoveStack(Player p,int index){
        if(!stillValid(p)||index<0||index>=slots.size())return ItemStack.EMPTY;
        var slot=slots.get(index);if(!slot.hasItem()||!slot.mayPickup(p))return ItemStack.EMPTY;
        ItemStack stack=slot.getItem(),old=stack.copy();int size=storage.getContainerSize();
        if(brush){if(index>=size&&stack.getItem() instanceof BlockItem){for(int i=0;i<9;i++)if(storage.getItem(i).isEmpty()){storage.setItem(i,stack.copyWithCount(1));break;}}return ItemStack.EMPTY;}
        if(index<size){if(!moveItemStackTo(stack,size,slots.size(),true))return ItemStack.EMPTY;}
        else if(!SatchelItem.accepts(stack)||!moveItemStackTo(stack,0,size,false))return ItemStack.EMPTY;
        if(stack.isEmpty())slot.set(ItemStack.EMPTY);else slot.setChanged();persist();return old;
    }
    @Override public boolean clickMenuButton(Player p,int id){
        if(!stillValid(p))return false;
        if(brush){
            if(id>=0&&id<9)values.set(id,Math.min(64,values.get(id)+1));
            else if(id>=16&&id<25)values.set(id-16,Math.max(0,values.get(id-16)-1));
            else if(id==32)values.set(9,(values.get(9)+1)%3);
            else if(id==33)values.set(10,1-values.get(10));
            else if(id==34)values.set(11,1-values.get(11));else return false;
        }else if(id==64)values.set(12,1-values.get(12));else return false;
        persist();broadcastChanges();return true;
    }
}

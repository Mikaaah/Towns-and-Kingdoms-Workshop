package nl.townsandkingdoms.core.construction.client;

import net.minecraft.client.gui.GuiGraphics;
import nl.townsandkingdoms.core.client.WorkshopStyle;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import nl.townsandkingdoms.core.construction.*;

public final class KitScreen extends AbstractContainerScreen<KitMenu> {
    public KitScreen(KitMenu menu,Inventory inv,Component title){super(menu,inv,title);imageWidth=menu.brush?330:176;imageHeight=menu.brush?250:214;inventoryLabelY=menu.brush?154:114;}
    private void send(int id){if(minecraft!=null&&minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,id);}
    private Button button(int x,int y,int w,String label,int id){return addRenderableWidget(WorkshopStyle.button(leftPos+x,topPos+y,w,18,label,b->send(id)));}
    @Override protected void init(){super.init();
        if(menu.brush){for(int i=0;i<9;i++){int x=8+(i%3)*54,y=46+(i/3)*43;button(x,y,16,"−",16+i);button(x+18,y,16,"+",i);}
            button(180,26,140,"Change selection mode",32);button(180,64,140,"Toggle avoid repeats",33);button(180,102,140,"Toggle satchel supply",34);
        }else button(8,28,160,"Toggle held-block refill",64);
    }
    @Override protected void renderBg(GuiGraphics g,float partial,int mouseX,int mouseY){
        WorkshopStyle.frame(g,leftPos,topPos,imageWidth,imageHeight);
        g.renderItem(menu.owner,leftPos+9,topPos+7);
        WorkshopStyle.inset(g,leftPos+5,topPos+inventoryLabelY-2,166,imageHeight-inventoryLabelY-4);
        if(menu.brush)WorkshopStyle.inset(g,leftPos+180,topPos+174,140,65);
        for(var slot:menu.slots)WorkshopStyle.slot(g,leftPos+slot.x,topPos+slot.y);

    }
    @Override protected void renderLabels(GuiGraphics g,int x,int y){
        g.drawString(font,title,30,12,WorkshopStyle.CREAM,false);g.drawString(font,playerInventoryTitle,8,inventoryLabelY,WorkshopStyle.MUTED,false);
        if(menu.brush){for(int i=0;i<9;i++)g.drawString(font,"×"+menu.values.get(i),28+(i%3)*54,31+(i/3)*43,WorkshopStyle.INK,false);
            g.drawString(font,"How blocks are selected",180,48,WorkshopStyle.INK,false);
            g.drawString(font,"Skip previous material",180,86,WorkshopStyle.INK,false);
            g.drawString(font,"Use carried satchels",180,124,WorkshopStyle.INK,false);
            g.drawString(font,"Click block onto a slot.",180,140,WorkshopStyle.MUTED,false);
            g.drawString(font,"Empty cursor clears.",180,153,WorkshopStyle.MUTED,false);
            g.drawString(font,"PALETTE NOTES",188,182,WorkshopStyle.CREAM,false);
            g.drawString(font,"Shift-click to add.",188,198,WorkshopStyle.MUTED,false);
            g.drawString(font,"Zero weight disables.",188,211,WorkshopStyle.MUTED,false);
            g.drawString(font,"Uses real materials.",188,224,WorkshopStyle.MUTED,false);
        }
    }
    @Override protected void containerTick(){
        super.containerTick();
        int i=menu.brush?18:0;
        var buttons=children().stream().filter(c->c instanceof Button).map(c->(Button)c).toList();
        if(menu.brush && buttons.size()>=21){
            buttons.get(i).setMessage(Component.literal(BrushItem.MODES[menu.values.get(9)]));
            buttons.get(i+1).setMessage(Component.literal("Avoid repeats: "+(menu.values.get(10)>0?"ON":"OFF")));
            buttons.get(i+2).setMessage(Component.literal("Satchel supply: "+(menu.values.get(11)>0?"ON":"OFF")));
        }else if(!menu.brush && !buttons.isEmpty())buttons.get(0).setMessage(Component.literal("Held-block refill: "+(menu.values.get(12)>0?"ON":"OFF")));
    }
    @Override public void render(GuiGraphics g,int x,int y,float tick){super.render(g,x,y,tick);renderTooltip(g,x,y);}
}


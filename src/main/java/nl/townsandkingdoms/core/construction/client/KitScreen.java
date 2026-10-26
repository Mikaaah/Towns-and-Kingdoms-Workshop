package nl.townsandkingdoms.core.construction.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import nl.townsandkingdoms.core.construction.*;

public final class KitScreen extends AbstractContainerScreen<KitMenu> {
    public KitScreen(KitMenu menu,Inventory inv,Component title){super(menu,inv,title);imageWidth=menu.brush?330:176;imageHeight=menu.brush?250:214;inventoryLabelY=menu.brush?154:114;}
    private void send(int id){if(minecraft!=null&&minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,id);}
    private void button(int x,int y,int w,String label,int id){addRenderableWidget(Button.builder(Component.literal(label),b->send(id)).bounds(leftPos+x,topPos+y,w,18).build());}
    @Override protected void init(){super.init();
        if(menu.brush){for(int i=0;i<9;i++){int x=8+(i%3)*54,y=46+(i/3)*43;button(x,y,16,"−",16+i);button(x+18,y,16,"+",i);}
            button(180,26,140,"Change selection mode",32);button(180,64,140,"Toggle avoid repeats",33);button(180,102,140,"Toggle satchel supply",34);
        }else button(8,23,160,"Toggle held-block refill",64);
    }
    @Override protected void renderBg(GuiGraphics g,float partial,int mouseX,int mouseY){
        g.fill(leftPos,topPos,leftPos+imageWidth,topPos+imageHeight,0xF01D1826);g.fill(leftPos,topPos,leftPos+imageWidth,topPos+2,0xFFAF7CEB);
        for(var slot:menu.slots){g.fill(leftPos+slot.x-1,topPos+slot.y-1,leftPos+slot.x+17,topPos+slot.y+17,0xFF74677F);g.fill(leftPos+slot.x,topPos+slot.y,leftPos+slot.x+16,topPos+slot.y+16,0xFF100D17);}
    }
    @Override protected void renderLabels(GuiGraphics g,int x,int y){
        g.drawString(font,title,8,6,0xFFE8D9FA,false);g.drawString(font,playerInventoryTitle,8,inventoryLabelY,0xFFBBAACB,false);
        if(menu.brush){for(int i=0;i<9;i++)g.drawString(font,"×"+menu.values.get(i),28+(i%3)*54,31+(i/3)*43,0xFFE1D3EE,false);
            g.drawString(font,BrushItem.MODES[menu.values.get(9)],180,48,0xFFD8B5FA,false);
            g.drawString(font,"Avoid repeats: "+(menu.values.get(10)>0?"on":"off"),180,86,0xFFD8B5FA,false);
            g.drawString(font,"Satchel supply: "+(menu.values.get(11)>0?"on":"off"),180,124,0xFFD8B5FA,false);
            g.drawString(font,"Click block onto a slot.",180,140,0xFFAE9DBB,false);
            g.drawString(font,"Empty cursor clears.",180,153,0xFFAE9DBB,false);
        }else g.drawString(font,"Refill: "+(menu.values.get(12)>0?"on":"off"),114,7,0xFFD8B5FA,false);
    }
    @Override public void render(GuiGraphics g,int x,int y,float tick){super.render(g,x,y,tick);renderTooltip(g,x,y);}
}

package nl.townsandkingdoms.core.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** Shared parchment, walnut and amethyst styling for Workshop menus. */
public final class WorkshopStyle {
    public static final int INK=0xFF392B27, MUTED=0xFF75604D, CREAM=0xFFFFE8BA;
    private WorkshopStyle() {}
    public static void inset(GuiGraphics g,int x,int y,int w,int h) {
        g.fill(x,y,x+w,y+h,0xFF4B352D);
        g.fill(x+1,y+1,x+w-1,y+h-1,0xFFA88A60);
        g.fill(x+2,y+2,x+w-2,y+h-2,0xFFE2C99A);
    }
    public static void frame(GuiGraphics g,int x,int y,int w,int h) {
        g.fill(x+4,y+5,x+w+4,y+h+5,0x70000000);
        g.fill(x,y,x+w,y+h,0xFF211B1A);
        g.fill(x+1,y+1,x+w-1,y+h-1,0xFFAE8858);
        g.fill(x+3,y+3,x+w-3,y+h-3,0xFF594032);
        g.fill(x+5,y+5,x+w-5,y+h-5,0xFFEAD4AB);
        g.fill(x+6,y+6,x+w-6,y+24,0xFF573D67);
        g.fill(x+6,y+24,x+w-6,y+25,0xFFBDA070);
        for(int dx:new int[]{2,w-5})for(int dy:new int[]{2,h-5})
            g.fill(x+dx,y+dy,x+dx+3,y+dy+3,0xFFE7C98A);
    }
    public static void slot(GuiGraphics g,int x,int y) {
        g.fill(x-1,y-1,x+17,y+17,0xFF72503A);
        g.fill(x,y,x+16,y+16,0xFFB29B75);
        g.fill(x+1,y+1,x+16,y+16,0xFFD3BC91);
        g.fill(x,y+16,x+17,y+17,0xFFFFE7BB);
    }
    public static Button button(int x,int y,int w,int h,String label,Button.OnPress action) {
        return new StyledButton(x,y,w,h,Component.literal(label),action);
    }
    public static void control(GuiGraphics g,int x,int y,int w,int h,boolean hover,boolean active) {
        g.fill(x,y,x+w,y+h,0xFF392834);
        g.fill(x+1,y+1,x+w-1,y+h-1,active?(hover?0xFFBE9CD3:0xFF9570AF):0xFF8A7D73);
        g.fill(x+2,y+2,x+w-2,y+h-2,active?(hover?0xFF79548E:0xFF5B3D70):0xFF685C59);
        g.fill(x+2,y+h-3,x+w-2,y+h-2,0xFF402D47);
    }
    private static final class StyledButton extends Button {
        StyledButton(int x,int y,int w,int h,Component label,OnPress action) {
            super(x,y,w,h,label,action,DEFAULT_NARRATION);
        }
        @Override public void renderWidget(GuiGraphics g,int x,int y,float tick) {
            control(g,getX(),getY(),getWidth(),getHeight(),isHoveredOrFocused(),active);
            renderScrollingString(g,Minecraft.getInstance().font,3,active?CREAM:0xFFB5AAA3);
        }
    }
}

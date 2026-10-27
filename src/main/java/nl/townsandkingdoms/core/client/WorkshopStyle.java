package nl.townsandkingdoms.core.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Texture-backed Workshop controls using the owner's Tavern HUD artwork. */
public final class WorkshopStyle {
    public static final int INK=0xFFE2D5B3, MUTED=0xFFB7AF97, CREAM=0xFFFFF0C9;
    private static final ResourceLocation TIMBER=texture("tavern_top"), SLOTS=texture("hotbar"), SELECTED=texture("hotbar_selection");
    private WorkshopStyle() {}
    private static ResourceLocation texture(String name){return ResourceLocation.fromNamespaceAndPath("tnkcore","textures/gui/tavern/"+name+".png");}

    /** Preserve the original pixel-art corners; extend only the edge/centre strips. */
    private static void nine(GuiGraphics g,ResourceLocation tex,int x,int y,int w,int h,int u,int v,int sw,int sh,int border,int tw,int th){
        int b=Math.min(border,Math.min(w,h)/2);
        int[] dx={x,x+b,x+w-b},dy={y,y+b,y+h-b};
        int[] dw={b,w-2*b,b},dh={b,h-2*b,b};
        int[] sx={u,u+border,u+sw-border},sy={v,v+border,v+sh-border};
        int[] cw={border,sw-2*border,border},ch={border,sh-2*border,border};
        for(int row=0;row<3;row++)for(int col=0;col<3;col++)if((row!=1||col!=1)&&dw[col]>0&&dh[row]>0)
            g.blit(tex,dx[col],dy[row],dw[col],dh[row],(float)sx[col],(float)sy[row],cw[col],ch[row],tw,th);
    }
    public static void frame(GuiGraphics g,int x,int y,int w,int h){
        g.fill(x+4,y+5,x+w+4,y+h+5,0x80000000);
        g.fill(x+3,y+3,x+w-3,y+h-3,0xFF464236);
        nine(g,TIMBER,x,y,w,h,0,0,42,43,7,122,60);
    }
    public static void inset(GuiGraphics g,int x,int y,int w,int h){
        g.fill(x+2,y+2,x+w-2,y+h-2,0xFF4B4638);
        nine(g,SLOTS,x,y,w,h,1,1,20,20,3,182,22);
    }
    public static void slot(GuiGraphics g,int x,int y){
        g.blit(SLOTS,x-1,y-1,18,18,2F,2F,18,18,182,22);
    }
    public static void control(GuiGraphics g,int x,int y,int w,int h,boolean hover,boolean active){
        inset(g,x,y,w,h);
        if(!active)g.fill(x+2,y+2,x+w-2,y+h-2,0x900D0B10);
        else if(hover)nine(g,SELECTED,x-2,y-2,w+4,h+4,0,0,24,23,4,24,23);
    }
    public static Button button(int x,int y,int w,int h,String label,Button.OnPress action){return new StyledButton(x,y,w,h,Component.literal(label),action);}
    private static final class StyledButton extends Button {
        StyledButton(int x,int y,int w,int h,Component label,OnPress action){super(x,y,w,h,label,action,DEFAULT_NARRATION);}
        @Override public void renderWidget(GuiGraphics g,int x,int y,float tick){
            control(g,getX(),getY(),getWidth(),getHeight(),isHoveredOrFocused()||getMessage().getString().startsWith("● "),active);
            renderScrollingString(g,Minecraft.getInstance().font,3,active?CREAM:0xFF8D8879);
        }
    }
}

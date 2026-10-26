package nl.townsandkingdoms.core.projector.client;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.townsandkingdoms.core.projector.*;

public final class ProjectorScreen extends Screen {
    private final BlockPos pos;
    private final int revision;
    private final boolean editable;
    private ProjectorSettings.Mode mode;
    private final List<Long> selected;
    private String text,url,query="",error="";
    private float displayWidth,elevation;
    private boolean billboard,background;
    private int page;
    private EditBox search,image;
    private MultiLineEditBox editor;
    private int left,top,w,h;
    public ProjectorScreen(ProjectorBlockEntity be) {
        super(Component.literal("Quest Projector"));pos=be.getBlockPos();revision=be.revision();
        editable=Minecraft.getInstance().player!=null&&be.canEdit(Minecraft.getInstance().player);
        var s=be.settings();mode=s.mode();selected=new ArrayList<>(s.quests());text=s.text();url=s.imageUrl();
        displayWidth=s.width();elevation=s.elevation();billboard=s.billboard();background=s.background();
    }
    private void capture(){if(editor!=null)text=editor.getValue();if(image!=null)url=image.getValue().trim();if(search!=null)query=search.getValue();}
    private void rebuild(){capture();rebuildWidgets();}
    private Button button(int x,int y,int width,String label,java.util.function.Consumer<Button> action) {
        return addRenderableWidget(Button.builder(Component.literal(label),action::accept).bounds(x,y,width,20).build());
    }
    @Override protected void init() {
        search=null;image=null;editor=null;
        w=Math.min(480,width-16);h=Math.min(340,height-16);left=(width-w)/2;top=(height-h)/2;
        int tab=(w-24)/3;
        for(var value:ProjectorSettings.Mode.values()) {
            var b=button(left+8+value.ordinal()*tab,top+27,tab-3,(value==mode?"● ":"")+switch(value){case QUESTS->"Quests";case IMAGE->"Image";case TEXT->"Formatted text";},b1->{mode=value;page=0;rebuild();});
            b.active=editable||value==mode;
        }
        int bodyTop=top+55,bodyBottom=top+h-93;
        if(mode==ProjectorSettings.Mode.QUESTS){
            search=addRenderableWidget(new EditBox(font,left+10,bodyTop,w-20,18,Component.literal("Search quests")));
            search.setMaxLength(120);search.setValue(query);search.setHint(Component.literal("Search by quest or chapter…"));
            search.setResponder(s->{query=s;page=0;refreshQuestButtons();});
            refreshQuestButtons();
        } else if(mode==ProjectorSettings.Mode.IMAGE){
            image=addRenderableWidget(new EditBox(font,left+10,bodyTop+14,w-20,20,Component.literal("Direct image URL")));
            image.setMaxLength(ProjectorSettings.MAX_URL);image.setValue(url);image.setEditable(editable);
            button(left+10,bodyTop+42,110,"Preview / reload",b->{capture();RemoteImages.invalidate(url);});
        } else {
            editor=addRenderableWidget(new MultiLineEditBox(font,left+10,bodyTop,w-20,Math.max(30,bodyBottom-bodyTop-27),Component.literal("Write your message…"),Component.literal("Formatted text")));
            editor.setCharacterLimit(ProjectorSettings.MAX_TEXT);editor.setValue(text);editor.active=editable;
            int x=left+10;for(String[] pair:new String[][]{{"Bold","&l"},{"Italic","&o"},{"Purple","&d"},{"Reset","&r"}}){String code=pair[1];button(x,bodyBottom-22,62,pair[0],b->{editor.setValue(editor.getValue()+code);editor.setFocused(true);}).active=editable;x+=65;}
        }
        int y=top+h-84;
        addRenderableWidget(new ValueSlider(left+10,y,(w-24)/2,displayWidth,1,8,"Width",v->displayWidth=v)).active=editable;
        addRenderableWidget(new ValueSlider(left+14+(w-24)/2,y,(w-24)/2,elevation,.5F,6,"Height",v->elevation=v)).active=editable;
        button(left+10,y+24,(w-24)/2,"Face player: "+(billboard?"on":"off"),b->{billboard=!billboard;b.setMessage(Component.literal("Face player: "+(billboard?"on":"off")));}).active=editable;
        button(left+14+(w-24)/2,y+24,(w-24)/2,"Backdrop: "+(background?"on":"off"),b->{background=!background;b.setMessage(Component.literal("Backdrop: "+(background?"on":"off")));}).active=editable;
        button(left+w-90,top+h-29,80,editable?"Save":"Close",b->{if(editable)save();else onClose();});
        if(editable)button(left+10,top+h-29,75,"Cancel",b->onClose());
    }
    private final List<AbstractWidget> questWidgets=new ArrayList<>();
    private void refreshQuestButtons(){
        questWidgets.forEach(this::removeWidget);questWidgets.clear();
        int count=Math.max(1,(h-188)/24);var list=QuestBridge.choices().stream().filter(q->editable||selected.contains(q.id())).filter(q->(q.title().getString()+" "+q.chapter()).toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT))).toList();
        page=Math.max(0,Math.min(page,Math.max(0,(list.size()-1)/count)));
        for(int i=0;i<count&&page*count+i<list.size();i++){
            var q=list.get(page*count+i);boolean chosen=selected.contains(q.id());
            String name=(chosen?"✓ ":"+ ")+q.title().getString()+"  ·  "+q.chapter();
            var b=button(left+10,top+79+i*24,w-48,font.plainSubstrByWidth(name,w-63),b1->{
                if(!editable){QuestBridge.open(q.id());return;}
                if(!selected.remove(q.id())&&selected.size()<5)selected.add(q.id());refreshQuestButtons();});
            b.setTooltip(Tooltip.create(Component.literal(q.title().getString()+"\n"+q.chapter())));
            b.active=!editable||chosen||selected.size()<5;questWidgets.add(b);
            questWidgets.add(button(left+w-34,top+79+i*24,24,"↗",open->QuestBridge.open(q.id())));
        }
        int y=top+h-111;
        questWidgets.add(button(left+10,y,24,"‹",b->{page--;refreshQuestButtons();}));
        questWidgets.add(button(left+38,y,24,"›",b->{page++;refreshQuestButtons();}));
        questWidgets.add(button(left+68,y,100,"Clear selection",b->{selected.clear();refreshQuestButtons();}));
        questWidgets.get(questWidgets.size()-1).active=editable;
    }
    private void save(){capture();try {
        var s=new ProjectorSettings(mode,selected,text,url,displayWidth,elevation,billboard,background);
        PacketDistributor.sendToServer(new ProjectorNetwork.Save(pos,revision,s));onClose();
    }catch(IllegalArgumentException e){error=e.getMessage();}}
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float tick){
        g.fill(left,top,left+w,top+h,0xF0181426);g.fill(left,top,left+w,top+2,0xFFAE77F5);
        g.drawString(font,title,left+10,top+10,0xFFE3CFFF);
        g.drawString(font,editable?"Configure your projection":"Read only · placer can edit",left+130,top+10,0xFFA89CB8);
        super.render(g,mouseX,mouseY,tick);
        if(mode==ProjectorSettings.Mode.QUESTS){
            String s=QuestBridge.status();g.drawString(font,s.isEmpty()?selected.size()+" / 5 quests selected":s,left+175,top+h-105,0xFFCEC4DD);
        } else if(mode==ProjectorSettings.Mode.IMAGE){
            g.drawString(font,"Direct PNG, JPG or GIF link (first frame)",left+10,top+57,0xFFCEC4DD);
            var entry=RemoteImages.get(url);
            if(entry.texture()!=null){int areaH=Math.max(15,h-213);int areaW=w-155;float scale=Math.min((float)areaW/entry.width(),(float)areaH/entry.height());int iw=Math.max(1,(int)(entry.width()*scale)),ih=Math.max(1,(int)(entry.height()*scale));
                g.blit(entry.texture(),left+140,top+99,0,0,iw,ih,iw,ih);
            } else g.drawString(font,font.plainSubstrByWidth(entry.message(),w-30),left+10,top+128,0xFFBEB0CE);
        } else {
            g.drawString(font,"&0–&f colors · &#RRGGBB · &l bold · &o italic · &n underline",left+10,top+h-116,0xFFBEB0CE);
        }
        if(!error.isEmpty())g.drawString(font,font.plainSubstrByWidth(error,w-120),left+90,top+h-22,0xFFFF8888);
    }
    @Override public void tick(){if(minecraft==null||minecraft.level==null||minecraft.player==null||!(minecraft.level.getBlockEntity(pos) instanceof ProjectorBlockEntity)||minecraft.player.distanceToSqr(pos.getCenter())>64)onClose();}
    @Override public void resize(Minecraft mc,int width,int height){capture();super.resize(mc,width,height);}
    @Override public boolean isPauseScreen(){return false;}
    private static final class ValueSlider extends AbstractSliderButton {
        final float min,max;final String label;final java.util.function.Consumer<Float> change;
        ValueSlider(int x,int y,int w,float initial,float min,float max,String label,java.util.function.Consumer<Float> change){super(x,y,w,20,Component.empty(),(initial-min)/(max-min));this.min=min;this.max=max;this.label=label;this.change=change;updateMessage();}
        @Override protected void updateMessage(){setMessage(Component.literal(label+": "+String.format(Locale.ROOT,"%.1f blocks",min+value*(max-min))));}
        @Override protected void applyValue(){change.accept(Math.round((min+(float)value*(max-min))*10)/10F);}
    }
}

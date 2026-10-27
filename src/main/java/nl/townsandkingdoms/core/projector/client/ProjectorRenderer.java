package nl.townsandkingdoms.core.projector.client;

import java.util.*;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.AABB;
import nl.townsandkingdoms.core.projector.*;

public final class ProjectorRenderer implements BlockEntityRenderer<ProjectorBlockEntity> {
    private static final ResourceLocation WHITE=ResourceLocation.fromNamespaceAndPath("tnkcore","textures/block/projector_white.png");
    private static final int LIGHT=15728880, W=400, ROW=18, PAGE_ROWS=26;
    private final Font font;
    private record Line(FormattedCharSequence text,ItemStack icon,int color,boolean header){}
    private record Cached(long at,ProjectorSettings settings,List<Line> lines){}
    private final Map<net.minecraft.core.BlockPos,Cached> cache=new LinkedHashMap<>();
    public ProjectorRenderer(BlockEntityRendererProvider.Context context){font=context.getFont();}
    @Override public int getViewDistance(){return 48;}
    @Override public AABB getRenderBoundingBox(ProjectorBlockEntity be){return new AABB(be.getBlockPos()).inflate(9).expandTowards(0,20,0);}
    private List<Line> lines(ProjectorBlockEntity be){
        long now=System.currentTimeMillis();var old=cache.get(be.getBlockPos());
        if(old!=null&&now-old.at<1000&&old.settings.equals(be.settings()))return old.lines;
        var s=be.settings();var colors=s.palette();
        int accent=ProjectorPalette.opaque(colors.accent()), textColor=ProjectorPalette.opaque(colors.text()),
                done=ProjectorPalette.opaque(colors.completed()), muted=ProjectorPalette.opaque(ProjectorPalette.muted(colors.text()));
        var result=new ArrayList<Line>();
        if(s.mode()==ProjectorSettings.Mode.TEXT){
            for(FormattedCharSequence text:font.split(FormattedText.parse(s.text()),W-32))result.add(new Line(text,ItemStack.EMPTY,textColor,false));
        }else{
            for(long id:s.quests()){
                var view=QuestBridge.view(id);
                if(view==null){result.add(new Line(Component.literal("Quest unavailable or hidden").getVisualOrderText(),ItemStack.EMPTY,muted,true));continue;}
                String mark=view.completed()?"✓ ":view.locked()?"◆ ":"";
                addWrapped(result,Component.literal(mark).append(view.title()),view.icon(),view.completed()?done:accent,true);
                if(view.tasks().isEmpty())addWrapped(result,Component.literal(view.locked()?"Locked · complete earlier quests":"No tasks"),ItemStack.EMPTY,muted,false);
                for(var task:view.tasks()){
                    String amount=task.progress();int available=W-46-font.width(amount)-12;
                    var wrapped=font.split(task.title(),Math.max(40,available));
                    for(int i=0;i<wrapped.size();i++){
                        // Keep the complete requirement; long task names wrap onto extra rows.
                        Component suffix=Component.literal(i==0?"    "+amount:"");
                        FormattedCharSequence sequence=FormattedCharSequence.composite(wrapped.get(i),suffix.getVisualOrderText());
                        result.add(new Line(sequence,i==0?task.icon():ItemStack.EMPTY,task.completed()?done:textColor,false));
                    }
                }
                result.add(new Line(Component.empty().getVisualOrderText(),ItemStack.EMPTY,0,false));
            }
            if(result.isEmpty())addWrapped(result,Component.literal(s.quests().isEmpty()?"Right-click to select up to five quests":QuestBridge.status()),ItemStack.EMPTY,accent,false);
        }
        if(s.mode()==ProjectorSettings.Mode.QUESTS) {
            var paged=new ArrayList<Line>(); Line heading=null;
            for(var line:result) {
                if(line.header && paged.size()%PAGE_ROWS==PAGE_ROWS-1)
                    paged.add(new Line(Component.empty().getVisualOrderText(),ItemStack.EMPTY,0,false));
                if(line.header) heading=line;
                else if(!paged.isEmpty()&&paged.size()%PAGE_ROWS==0&&heading!=null) paged.add(heading);
                paged.add(line);
            }
            result=paged;
        }
        if(cache.size()>128)cache.clear();var immutable=List.copyOf(result);cache.put(be.getBlockPos(),new Cached(now,s,immutable));return immutable;
    }
    private void addWrapped(List<Line> lines,Component text,ItemStack icon,int color,boolean header){var split=font.split(text,W-46);for(int i=0;i<split.size();i++)lines.add(new Line(split.get(i),i==0?icon:ItemStack.EMPTY,color,header));}
    @Override public void render(ProjectorBlockEntity be,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        var s=be.settings();var colors=s.palette();var mc=Minecraft.getInstance();
        if(mc.player==null||mc.player.distanceToSqr(be.getBlockPos().getCenter())>48*48)return;
        double ticks=(be.getLevel()==null?0:be.getLevel().getGameTime())+(double)partial;
        pose.pushPose();pose.translate(.5,s.elevation()+.6+ProjectionMotion.offset(ticks,s.bobbing()),.5);
        if(s.billboard())pose.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
        else pose.mulPose(Axis.YP.rotationDegrees(-be.getBlockState().getValue(ProjectorBlock.FACING).toYRot()));
        float scale=s.width()/W;pose.scale(scale,-scale,scale);
        if(s.mode()==ProjectorSettings.Mode.IMAGE){
            var image=RemoteImages.get(s.imageUrl());
            if(image.texture()!=null){float height=Math.min(600,W*(float)image.height()/image.width());float width=height*image.width()/image.height();
                if(s.background())quad(pose,buffers,WHITE,-width/2-6,-height-6,width+12,height+12,-.4F,(0xBB000000|colors.backdrop()));
                quad(pose,buffers,image.texture(),-width/2,-height,width,height,0,0xFFFFFFFF);
            }else{drawText(pose,buffers,Component.literal(image.message()).getVisualOrderText(),-W/2+12,-18,ProjectorPalette.opaque(colors.accent()));}
        }else{
            List<Line> lines=lines(be);int pages=Math.max(1,(lines.size()+PAGE_ROWS-1)/PAGE_ROWS);
            long time=be.getLevel()==null?0:be.getLevel().getGameTime();int page=(int)((time/160)%pages);
            int start=page*PAGE_ROWS,end=Math.min(lines.size(),start+PAGE_ROWS),height=Math.max(36,(end-start)*ROW+24+(pages>1?14:0));
            if(s.background())quad(pose,buffers,WHITE,-W/2,-height,W,height,-.4F,(0xCE000000|colors.backdrop()));
            quad(pose,buffers,WHITE,-W/2,-height,W,2,-.2F,(0xDD000000|colors.accent()));
            int y=-height+12;
            for(int i=start;i<end;i++,y+=ROW){var line=lines.get(i);
                if(!line.icon.isEmpty()){
                    pose.pushPose();pose.translate(-W/2+19,y+5,1);pose.scale(14,-14,14);
                    mc.getItemRenderer().renderStatic(line.icon,ItemDisplayContext.FIXED,LIGHT,OverlayTexture.NO_OVERLAY,pose,buffers,be.getLevel(),0);pose.popPose();
                }
                drawText(pose,buffers,line.text,-W/2+34,y,line.color);
            }
            if(pages>1)drawText(pose,buffers,Component.literal("Page "+(page+1)+" / "+pages+" · changes every 8 seconds").getVisualOrderText(),-W/2+12,-15,ProjectorPalette.opaque(ProjectorPalette.muted(colors.text())));
        }
        pose.popPose();
    }
    private void drawText(PoseStack pose,MultiBufferSource buffers,FormattedCharSequence text,float x,float y,int color){font.drawInBatch(text,x,y,color,false,pose.last().pose(),buffers,Font.DisplayMode.NORMAL,0,LIGHT);}
    private static void quad(PoseStack pose,MultiBufferSource buffers,ResourceLocation texture,float x,float y,float w,float h,float z,int color){
        // The emissive layer is COLOR_WRITE only. Later water/block-entity passes can
        // overwrite a hologram that never wrote its depth. Use the standard
        // translucent COLOR_DEPTH_WRITE layer; packed full-bright light is retained.
        var v=buffers.getBuffer(RenderType.entityTranslucent(texture));var p=pose.last();
        v.addVertex(p,x,y+h,z).setColor(color).setUv(0,1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LIGHT).setNormal(p,0,0,1);
        v.addVertex(p,x+w,y+h,z).setColor(color).setUv(1,1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LIGHT).setNormal(p,0,0,1);
        v.addVertex(p,x+w,y,z).setColor(color).setUv(1,0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LIGHT).setNormal(p,0,0,1);
        v.addVertex(p,x,y,z).setColor(color).setUv(0,0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LIGHT).setNormal(p,0,0,1);
    }
}

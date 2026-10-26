package nl.townsandkingdoms.core.projector.client;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import javax.net.ssl.*;
import javax.imageio.ImageIO;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import nl.townsandkingdoms.core.projector.ProjectorSettings;

/** Client-only, bounded image cache. Downloads never run on a game/render/server thread. */
public final class RemoteImages {
    private static final int LIMIT=8*1024*1024;
    private static final ExecutorService WORKERS=new ThreadPoolExecutor(2,2,30,TimeUnit.SECONDS,new ArrayBlockingQueue<>(32),r->{Thread t=new Thread(r,"Workshop image download");t.setDaemon(true);return t;},new ThreadPoolExecutor.AbortPolicy());
    private static final LinkedHashMap<String,Entry> CACHE=new LinkedHashMap<>(32,.75F,true);
    private static long generation;
    public record Entry(ResourceLocation texture,int width,int height,String message) {}
    public static synchronized Entry get(String url) {
        if(url.isBlank())return new Entry(null,0,0,"Paste a direct image link, then preview.");
        Entry cached=CACHE.get(url);if(cached!=null)return cached;
        if(!ProjectorSettings.validUrl(url)||url.length()>ProjectorSettings.MAX_URL)return new Entry(null,0,0,"Use a direct HTTP/HTTPS image URL.");
        while(CACHE.size()>=32){String key=CACHE.keySet().iterator().next();release(CACHE.remove(key));}
        Entry loading=new Entry(null,0,0,"Loading image…");CACHE.put(url,loading);long stamp=generation;
        try {WORKERS.submit(()->load(url,loading,stamp));}catch(RejectedExecutionException e){CACHE.put(url,new Entry(null,0,0,"Download queue full. Preview again."));}
        return loading;
    }
    public static synchronized void invalidate(String url){release(CACHE.remove(url));}
    public static synchronized void clear(){generation++;CACHE.values().forEach(RemoteImages::release);CACHE.clear();}
    private static void release(Entry e){if(e!=null&&e.texture!=null)Minecraft.getInstance().getTextureManager().release(e.texture);}
    private static void load(String url,Entry pending,long stamp){
        NativeImage image=null;
        try {
            byte[] bytes=nl.townsandkingdoms.core.projector.PublicImageDownload.download(url);
            // Inspect dimensions before decoding/decompressing pixel data.
            try(var in=ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))){
                var readers=ImageIO.getImageReaders(in);if(!readers.hasNext())throw new IOException("Unsupported image. Use PNG, JPG or GIF.");
                var reader=readers.next();try{
                    reader.setInput(in,true,true);int w=reader.getWidth(0),h=reader.getHeight(0);
                    if(w<1||h<1||w>4096||h>4096||(long)w*h>8_388_608)throw new IOException("Image exceeds 4096 px / 8 megapixels.");
                    var decoded=reader.read(0);image=new NativeImage(w,h,false);
                    for(int y=0;y<h;y++)for(int x=0;x<w;x++){int c=decoded.getRGB(x,y);image.setPixelRGBA(x,y,(c&0xFF00FF00)|((c&0xFF)<<16)|((c>>>16)&0xFF));}
                }finally{reader.dispose();}
            }
            NativeImage ready=image;image=null;
            Minecraft.getInstance().execute(()->{
                synchronized(RemoteImages.class){
                    if(generation!=stamp||CACHE.get(url)!=pending){ready.close();return;}
                    var texture=Minecraft.getInstance().getTextureManager().register("workshop_projector",new DynamicTexture(ready));
                    CACHE.put(url,new Entry(texture,ready.getWidth(),ready.getHeight(),""));
                }
            });
        }catch(Exception e){String message=e instanceof IOException&&e.getMessage()!=null?e.getMessage():"Image unavailable. Check the link.";
            Minecraft.getInstance().execute(()->{synchronized(RemoteImages.class){if(generation==stamp&&CACHE.get(url)==pending)CACHE.put(url,new Entry(null,0,0,message));}});
        }finally{if(image!=null)image.close();}
    }
}

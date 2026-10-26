package nl.townsandkingdoms.core.projector.client;

import java.lang.reflect.*;
import java.util.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

/** Optional 1.21.1 FTB Quests bridge; no FTB classes loaded on a server or without the mod. */
public final class QuestBridge {
    private static final Logger LOG=LogUtils.getLogger();
    private static boolean logged;
    private static String status="FTB Quests is not installed.";
    private static long refreshed=-1;
    private static List<Choice> choices=List.of();
    public record Choice(long id, Component title, String chapter, ItemStack icon) {}
    public record TaskLine(Component title, ItemStack icon, String progress, boolean completed) {}
    public record View(long id, Component title, ItemStack icon, List<TaskLine> tasks, boolean completed, boolean locked) {}
    private static Object file() throws ReflectiveOperationException {
        if(!ModList.get().isLoaded("ftbquests")) return null;
        Class<?> c=Class.forName("dev.ftb.mods.ftbquests.client.ClientQuestFile");
        if(!Boolean.TRUE.equals(c.getMethod("exists").invoke(null))) {status="Waiting for quest data…";return null;}
        Object file=c.getMethod("getInstance").invoke(null);
        if(file.getClass().getField("selfTeamData").get(file)==null) {status="Waiting for team data…";return null;}
        status="";return file;
    }
    private static Object call(Object target,String name,Object...args) throws ReflectiveOperationException {
        for(Method m:target.getClass().getMethods()) {
            if(!m.getName().equals(name)||m.getParameterCount()!=args.length) continue;
            boolean fits=true; Class<?>[] types=m.getParameterTypes();
            for(int i=0;i<args.length;i++) if(args[i]!=null && !types[i].isInstance(args[i])
                    && !(types[i]==long.class&&args[i] instanceof Long) && !(types[i]==boolean.class&&args[i] instanceof Boolean)) {fits=false;break;}
            if(fits) return m.invoke(target,args);
        }
        throw new NoSuchMethodException(target.getClass().getName()+"."+name);
    }
    private static Object team(Object file) throws ReflectiveOperationException {return file.getClass().getField("selfTeamData").get(file);}
    private static ItemStack icon(Object obj) {
        try {
            if(obj.getClass().getSimpleName().equals("ItemTask")) return ((ItemStack)call(obj,"getItemStack")).copy();
            Object icon=call(obj,"getIcon"); Object item=call(icon,"getIngredient");
            if(item instanceof ItemStack stack) return stack.copy();
            try {return ((ItemStack)call(icon,"getStack")).copy();} catch(ReflectiveOperationException ignored){}
        } catch(ReflectiveOperationException | RuntimeException ignored){}
        return ItemStack.EMPTY;
    }
    private static boolean visible(Object quest,Object data) throws ReflectiveOperationException {
        Object chapter=call(quest,"getChapter");
        return Boolean.TRUE.equals(call(chapter,"isVisible",data)) && Boolean.TRUE.equals(call(quest,"isVisible",data));
    }
    public static String status() {try{file();}catch(ReflectiveOperationException|LinkageError e){failure(e);}return status;}
    private static void failure(Throwable e) {status="FTB Quests integration unavailable. Check the log.";if(!logged){LOG.warn("Workshop projector could not read FTB Quests 1.21.1 data",e);logged=true;}}
    public static List<Choice> choices() {
        long now=System.currentTimeMillis(); if(now-refreshed<1000) return choices;refreshed=now;
        var result=new ArrayList<Choice>();
        try {
            Object file=file(); if(file==null) return choices=List.of();Object data=team(file);
            for(Object chapter:(Collection<?>)call(file,"getAllChapters")) {
                if(!Boolean.TRUE.equals(call(chapter,"isVisible",data))) continue;
                for(Object quest:(Collection<?>)call(chapter,"getQuests")) if(visible(quest,data))
                    result.add(new Choice((long)call(quest,"getId"),(Component)call(quest,"getTitle"),((Component)call(chapter,"getTitle")).getString(),icon(quest)));
            }
        } catch(ReflectiveOperationException|LinkageError e){failure(e);}
        result.sort(Comparator.comparing(Choice::chapter).thenComparing(q->q.title.getString()));
        return choices=List.copyOf(result);
    }
    public static View view(long id) {
        try {
            Object f=file();if(f==null)return null; Object data=team(f),q=call(f,"getQuest",id);
            if(q==null||!visible(q,data)) return null;
            boolean completed=Boolean.TRUE.equals(call(data,"isCompleted",q));
            boolean locked=!Boolean.TRUE.equals(call(data,"canStartTasks",q));
            var tasks=new ArrayList<TaskLine>();
            if(!locked||!Boolean.TRUE.equals(call(q,"hideDetailsUntilStartable"))) {
                for(Object t:(Collection<?>)call(q,"getTasks")) {
                    if(!Boolean.TRUE.equals(call(t,"isVisible",data)))continue;
                    long progress=(long)call(data,"getProgress",t);
                    String amount=call(t,"formatProgress",data,progress)+" / "+call(t,"formatMaxProgress");
                    tasks.add(new TaskLine((Component)call(t,"getTitle"),icon(t),amount,Boolean.TRUE.equals(call(data,"isCompleted",t))));
                }
            }
            return new View(id,(Component)call(q,"getTitle"),icon(q),List.copyOf(tasks),completed,locked);
        } catch(ReflectiveOperationException|LinkageError e){failure(e);return null;}
    }
    public static void open(long id) {
        try {
            Object file=file();if(file==null)return;Object q=call(file,"getQuest",id);if(q==null||!visible(q,team(file)))return;
            for(Method m:file.getClass().getMethods()) if(m.getName().equals("openGui") && m.getParameterCount()==2) {m.invoke(null,q,true);return;}
        } catch(ReflectiveOperationException|LinkageError e){failure(e);}
    }
    public static void clear(){choices=List.of();refreshed=-1;logged=false;}
}

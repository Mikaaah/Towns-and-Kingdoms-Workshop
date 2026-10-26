package nl.townsandkingdoms.core.test;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;
import nl.townsandkingdoms.core.*;
import nl.townsandkingdoms.core.projector.*;
import nl.townsandkingdoms.core.projector.client.FormattedText;

@GameTestHolder("tnkcore")
@PrefixGameTestTemplate(false)
public class ProjectorTests {
    static final BlockPos POS=new BlockPos(2,2,2);
    static ProjectorBlockEntity setup(GameTestHelper h){h.setBlock(POS,TownsAndKingdomsCore.PROJECTOR.get());return (ProjectorBlockEntity)h.getBlockEntity(POS);}
    static ProjectorSettings sample(){return new ProjectorSettings(ProjectorSettings.Mode.IMAGE,List.of(1L,2L,3L,4L,5L),"&d**Town Square**\nGather &l32 iron&r.","https://example.org/poster.png",4,2.5F,true,false);}
    @GameTest(template="empty") public static void projectorPersistsSettingsAndOwner(GameTestHelper h){
        var be=setup(h);UUID owner=UUID.randomUUID();be.setOwner(owner);be.update(sample());
        var tag=be.saveWithFullMetadata(h.getLevel().registryAccess());
        h.setBlock(POS,Blocks.AIR);h.setBlock(POS,TownsAndKingdomsCore.PROJECTOR.get());
        var restored=(ProjectorBlockEntity)h.getBlockEntity(POS);restored.loadWithComponents(tag,h.getLevel().registryAccess());
        h.assertTrue(restored.settings().equals(sample())&&owner.equals(restored.owner()),"Projector lost settings or owner on reload");
        h.assertTrue(restored.revision()==1,"Revision lost");h.succeed();
    }
    @GameTest(template="empty") public static void projectorDropsKeepContent(GameTestHelper h){
        var be=setup(h);be.setOwner(UUID.randomUUID());be.update(sample());
        var drops=Block.getDrops(be.getBlockState(),h.getLevel(),h.absolutePos(POS),be);
        h.assertTrue(drops.size()==1&&drops.get(0).is(TownsAndKingdomsCore.PROJECTOR_ITEM.get()),"Wrong projector drop");
        var data=drops.get(0).getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
        h.assertTrue(ProjectorSettings.read(data.getCompound("projector")).equals(sample()),"Drop lost content");
        h.assertTrue(!data.contains("owner"),"Drop must not inherit another player's ownership");h.succeed();
    }
    @GameTest(template="empty") public static void projectorOwnerAndOperatorPermissions(GameTestHelper h){
        var be=setup(h);var owner=h.makeMockServerPlayerInLevel();var other=h.makeMockServerPlayerInLevel();
        be.setOwner(owner.getUUID());h.assertTrue(be.canEdit(owner),"Placer cannot edit");
        h.assertTrue(!be.canEdit(other),"Other survival player can edit");h.succeed();
    }
    @GameTest(template="empty") public static void projectorRejectsInvalidContent(GameTestHelper h){
        int rejected=0;
        try{new ProjectorSettings(ProjectorSettings.Mode.QUESTS,List.of(1L,2L,3L,4L,5L,6L),"","",3,1,false,true);}catch(IllegalArgumentException e){rejected++;}
        try{new ProjectorSettings(ProjectorSettings.Mode.TEXT,List.of(),"x".repeat(4097),"",3,1,false,true);}catch(IllegalArgumentException e){rejected++;}
        try{new ProjectorSettings(ProjectorSettings.Mode.IMAGE,List.of(),"","file:///etc/passwd",3,1,false,true);}catch(IllegalArgumentException e){rejected++;}
        try{new ProjectorSettings(ProjectorSettings.Mode.TEXT,List.of(),"","",Float.NaN,1,false,true);}catch(IllegalArgumentException e){rejected++;}
        try{new ProjectorSettings(ProjectorSettings.Mode.IMAGE,List.of(),"","https://user:pass@example.com/image.png",3,1,false,true);}catch(IllegalArgumentException e){rejected++;}
        h.assertTrue(rejected==5,"Invalid settings accepted");h.succeed();
    }
    @GameTest(template="empty") public static void projectorFormattingIsStyledPlainText(GameTestHelper h){
        var text=FormattedText.parse("&d**Workshop**&r\\n&oHello &nworld");
        h.assertTrue(text.getString().equals("Workshop\nHello world"),"Formatting codes appeared as literal text");
        var styled=new ArrayList<net.minecraft.network.chat.Style>();text.visit((style,s)->{if(!s.isBlank())styled.add(style);return Optional.empty();},net.minecraft.network.chat.Style.EMPTY);
        h.assertTrue(styled.get(0).isBold()&&styled.get(0).getColor().getValue()==0xFF55FF,"Bold/color missing");
        h.assertTrue(styled.stream().anyMatch(s->s.isItalic())&&styled.stream().anyMatch(s->s.isUnderlined()),"Text styles missing");
        h.assertTrue(styled.stream().allMatch(s->s.getClickEvent()==null),"Text contains executable events");h.succeed();
    }
    @GameTest(template="empty") public static void projectorCopiesToNewPlacer(GameTestHelper h){
        var be=setup(h);be.setOwner(UUID.randomUUID());be.update(sample());var owner=h.makeMockServerPlayerInLevel();
        var stack=Block.getDrops(be.getBlockState(),h.getLevel(),h.absolutePos(POS),be).get(0);
        TownsAndKingdomsCore.PROJECTOR.get().setPlacedBy(h.getLevel(),h.absolutePos(POS),be.getBlockState(),owner,stack);
        h.assertTrue(be.owner().equals(owner.getUUID())&&be.settings().equals(sample()),"Placed copy did not restore data and assign new owner");h.succeed();
    }

    @GameTest(template="empty") public static void projectorBlocksPrivateImageTargets(GameTestHelper h) throws Exception {
        for(String ip:List.of("127.0.0.1","10.1.2.3","172.16.1.1","192.168.1.1","169.254.169.254","100.64.1.1","::1","fc00::1","fe80::1"))
            h.assertTrue(!PublicImageDownload.isPublic(java.net.InetAddress.getByName(ip)),"Private image target accepted: "+ip);
        h.assertTrue(PublicImageDownload.isPublic(java.net.InetAddress.getByName("1.1.1.1")),"Public IPv4 rejected");
        h.assertTrue(PublicImageDownload.isPublic(java.net.InetAddress.getByName("2606:4700:4700::1111")),"Public IPv6 rejected");
        boolean rejected=false;try{PublicImageDownload.download("http://127.0.0.1/image.png");}catch(java.io.IOException e){rejected=true;}
        h.assertTrue(rejected,"Downloader attempted a private target");h.succeed();
    }

    @GameTest(template="empty") public static void projectorSaveValidatesSenderAndRevision(GameTestHelper h) {
        var be=setup(h);var owner=h.makeMockServerPlayerInLevel();var other=h.makeMockServerPlayerInLevel();
        owner.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);other.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        be.setOwner(owner.getUUID());owner.setPos(h.absolutePos(POS).getCenter());other.setPos(h.absolutePos(POS).getCenter());
        var packet=new ProjectorNetwork.Save(be.getBlockPos(),0,sample());
        ProjectorNetwork.apply(packet,other);h.assertTrue(be.revision()==0,"Non-owner save accepted");
        owner.setPos(h.absolutePos(POS).getCenter().add(20,0,0));ProjectorNetwork.apply(packet,owner);
        h.assertTrue(be.revision()==0,"Remote save accepted");
        owner.setPos(h.absolutePos(POS).getCenter());ProjectorNetwork.apply(packet,owner);
        h.assertTrue(be.revision()==1&&be.settings().equals(sample()),"Valid save failed");
        ProjectorNetwork.apply(new ProjectorNetwork.Save(be.getBlockPos(),0,ProjectorSettings.DEFAULT),owner);
        h.assertTrue(be.revision()==1&&be.settings().equals(sample()),"Stale save overwrote new data");h.succeed();
    }
    @GameTest(template="empty") public static void projectorSaveHonorsClaimDenial(GameTestHelper h) {
        var be=setup(h);var owner=h.makeMockServerPlayerInLevel();owner.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        owner.setPos(h.absolutePos(POS).getCenter());be.setOwner(owner.getUUID());
        java.util.function.Consumer<net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock> listener=event->{
            if(event.getPos().equals(be.getBlockPos()))event.setUseBlock(net.neoforged.neoforge.common.util.TriState.FALSE);
        };
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(listener);
        try{ProjectorNetwork.apply(new ProjectorNetwork.Save(be.getBlockPos(),0,sample()),owner);}
        finally{net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(listener);}
        h.assertTrue(be.revision()==0,"Claim-denied settings were applied");h.succeed();
    }
}

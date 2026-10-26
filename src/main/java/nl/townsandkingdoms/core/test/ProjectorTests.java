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
    static ProjectorSettings sample(){return new ProjectorSettings(ProjectorSettings.Mode.IMAGE,List.of(1L,2L,3L,4L,5L),"&d**Town Square**\nGather &l32 iron&r.","https://example.org/poster.png",4,2.5F,true,false,new ProjectorPalette(0x00CC88,0x1199EE,0xFFFFFF,0x55FF00,0x102030),false);}
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

    @GameTest(template="empty") public static void projectorHexColorsAcceptFullRgbRange(GameTestHelper h) {
        h.assertTrue(ProjectorPalette.parseHex("#000000")==0,"Black rejected");
        h.assertTrue(ProjectorPalette.parseHex("ffffff")==0xFFFFFF,"White rejected");
        h.assertTrue(ProjectorPalette.parseHex(" #aB01fF ")==0xAB01FF,"Mixed-case hex rejected");
        h.assertTrue(ProjectorPalette.hex(0x001234).equals("#001234"),"Leading zeroes lost");
        int rejected=0;
        for(String invalid:List.of("#FFF","#12345678","#GG1234","#-00001","0x123456",""))
            try{ProjectorPalette.parseHex(invalid);}catch(IllegalArgumentException e){rejected++;}
        try{new ProjectorPalette(-1,0,0,0,0);}catch(IllegalArgumentException e){rejected++;}
        try{new ProjectorPalette(0x1000000,0,0,0,0);}catch(IllegalArgumentException e){rejected++;}
        h.assertTrue(rejected==8,"Invalid color accepted");h.succeed();
    }
    @GameTest(template="empty") public static void projectorMigratesLegacySettings(GameTestHelper h) {
        var old=sample().save();old.remove("palette");old.remove("bobbing");
        var migrated=ProjectorSettings.read(old);
        h.assertTrue(migrated.quests().equals(sample().quests())&&migrated.text().equals(sample().text())
                &&migrated.imageUrl().equals(sample().imageUrl())&&migrated.width()==sample().width(),"Legacy content lost");
        h.assertTrue(migrated.palette().equals(ProjectorPalette.DEFAULT)&&migrated.bobbing(),"Legacy defaults missing");
        var partial=new net.minecraft.nbt.CompoundTag();partial.putInt("body",0);partial.putInt("accent",-1);
        var colors=ProjectorPalette.read(partial);
        h.assertTrue(colors.body()==0&&colors.accent()==ProjectorPalette.DEFAULT.accent(),"Partial palette lost black or retained invalid color");h.succeed();
    }
    @GameTest(template="empty") public static void projectorPacketPreservesColorsAndMotion(GameTestHelper h) {
        var buf=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),h.getLevel().registryAccess());
        try {
            var expected=new ProjectorNetwork.Save(h.absolutePos(POS),12,sample());
            ProjectorNetwork.Save.CODEC.encode(buf,expected);
            var decoded=ProjectorNetwork.Save.CODEC.decode(buf);
            h.assertTrue(decoded.equals(expected),"Network changed color/motion settings");
            h.assertTrue(buf.readableBytes()==0,"Packet left undecoded fields");
        }finally{buf.release();}h.succeed();
    }
    @GameTest(template="empty") public static void projectorBobbingIsSubtleAndOptional(GameTestHelper h) {
        double period=ProjectionMotion.PERIOD_TICKS,amp=ProjectionMotion.AMPLITUDE;
        h.assertTrue(Math.abs(ProjectionMotion.offset(period/4,true)-amp)<1e-9,"Upper bob limit incorrect");
        h.assertTrue(Math.abs(ProjectionMotion.offset(3*period/4,true)+amp)<1e-9,"Lower bob limit incorrect");
        h.assertTrue(Math.abs(ProjectionMotion.offset(0,true)-ProjectionMotion.offset(period,true))<1e-9,"Motion does not loop");
        h.assertTrue(ProjectionMotion.offset(period/4,false)==0,"Disabled motion still bobs");
        h.assertTrue(amp<=0.04,"Motion is not subtle");h.succeed();
    }
}

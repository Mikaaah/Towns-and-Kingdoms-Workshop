package nl.townsandkingdoms.core.test;

import java.util.*;
import java.util.function.Consumer;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.*;
import nl.townsandkingdoms.core.construction.*;
import nl.townsandkingdoms.core.construction.BrushItem;

@GameTestHolder("tnkcore") @PrefixGameTestTemplate(false)
public final class ConstructionTests {
    private static final BlockPos BASE=new BlockPos(4,1,4),DEST=new BlockPos(20,2,20);
    static ServerPlayer player(GameTestHelper h,ItemStack stack){var p=h.makeMockServerPlayerInLevel();p.setGameMode(GameType.SURVIVAL);p.setPos(h.absolutePos(new BlockPos(2,2,2)).getCenter());p.setItemInHand(InteractionHand.MAIN_HAND,stack);return p;}
    static UseOnContext context(GameTestHelper h,ServerPlayer p,BlockPos pos){var absolute=h.absolutePos(pos);return new UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(absolute.getCenter(),Direction.UP,absolute,false));}
    static void floor(GameTestHelper h,int x0,int z0,int x1,int z1){for(int x=x0;x<=x1;x++)for(int z=z0;z<=z1;z++)h.setBlock(new BlockPos(x,1,z),Blocks.STONE);}
    static void configure(GameTestHelper h,ItemStack brush){var palette=NonNullList.withSize(9,ItemStack.EMPTY);palette.set(0,new ItemStack(Items.COBBLESTONE));BrushItem.savePalette(brush,palette,new int[]{1,0,0,0,0,0,0,0,0},h.getLevel().registryAccess());}
    static InteractionResult brushUse(GameTestHelper h,ServerPlayer p){return p.gameMode.useItemOn(p,h.getLevel(),p.getMainHandItem(),InteractionHand.MAIN_HAND,new BlockHitResult(h.absolutePos(BASE).getCenter(),Direction.UP,h.absolutePos(BASE),false));}
    static Cow cow(GameTestHelper h,Vec3 pos){var mob=EntityType.COW.create(h.getLevel());mob.moveTo(pos.x,pos.y,pos.z,0,0);mob.setNoAi(true);h.getLevel().addFreshEntity(mob);return mob;}
    @GameTest(template="construction_empty") public static void pencilRecordsAndClearsDimensions(GameTestHelper h){
        var stack=ConstructionSet.PENCIL.toStack();var p=player(h,stack);
        ConstructionSet.PENCIL.get().useOn(context(h,p,new BlockPos(1,2,1)));ConstructionSet.PENCIL.get().useOn(context(h,p,new BlockPos(4,6,1)));
        var d=KitData.read(stack);var a=NbtUtils.readBlockPos(d,"measure_a").orElseThrow();var b=NbtUtils.readBlockPos(d,"measure_b").orElseThrow();
        h.assertTrue(PencilItem.measurement(a,b).getString().contains("4 × 5 × 1")&&PencilItem.measurement(a,b).getString().contains("5.00"),"Wrong measurement");
        PencilItem.clear(stack);h.assertTrue(!KitData.read(stack).contains("measure_a"),"Measurement did not clear");h.succeed();
    }
    @GameTest(template="construction_empty") public static void satchelOnlyAcceptsBuildingBlocks(GameTestHelper h){
        h.assertTrue(SatchelItem.accepts(new ItemStack(Items.STONE)),"Building blocks rejected");h.assertTrue(!SatchelItem.accepts(ConstructionSet.SATCHEL.toStack()),"Satchel nesting allowed");
        var filled=new ItemStack(Items.SHULKER_BOX);filled.set(DataComponents.CONTAINER,net.minecraft.world.item.component.ItemContainerContents.fromItems(List.of(new ItemStack(Items.DIAMOND))));
        h.assertTrue(!SatchelItem.accepts(filled),"Nested inventory allowed");h.succeed();
    }
    @GameTest(template="construction_empty") public static void satchelSupplyPersistsAndRespectsToggle(GameTestHelper h){
        var bag=ConstructionSet.SATCHEL.toStack();var contents=NonNullList.withSize(27,ItemStack.EMPTY);contents.set(0,new ItemStack(Items.STONE,12));SatchelItem.save(bag,contents);
        var p=player(h,new ItemStack(Items.STONE));p.getInventory().setItem(2,bag);
        h.assertTrue(SatchelItem.extract(p,new ItemStack(Items.STONE),7)==7&&SatchelItem.contents(bag).get(0).getCount()==5,"Wrong satchel extraction");
        var d=KitData.read(bag);d.putBoolean("satchel_refill",false);KitData.write(bag,d);
        h.assertTrue(SatchelItem.extract(p,new ItemStack(Items.STONE),7)==0&&SatchelItem.contents(bag).get(0).getCount()==5,"Disabled refill consumed blocks");h.succeed();
    }
    @GameTest(template="construction_empty") public static void brushTemplatesDoNotCreateOrConsumeBlocks(GameTestHelper h){
        var brush=ConstructionSet.BRUSH.toStack();var p=player(h,brush);var menu=new KitMenu(1,p.getInventory(),InteractionHand.MAIN_HAND);
        menu.setCarried(new ItemStack(Items.STONE,32));menu.clicked(0,0,ClickType.PICKUP,p);
        h.assertTrue(menu.getCarried().getCount()==32&&BrushItem.palette(brush,h.getLevel().registryAccess()).get(0).getCount()==1,"Template changed carried inventory");
        menu.clicked(0,0,ClickType.QUICK_MOVE,p);h.assertTrue(menu.getCarried().getCount()==32,"Template shift click yielded free blocks");
        menu.setCarried(ItemStack.EMPTY);menu.clicked(0,0,ClickType.PICKUP,p);h.assertTrue(BrushItem.palette(brush,h.getLevel().registryAccess()).get(0).isEmpty(),"Ghost template did not clear");h.succeed();
    }
    @GameTest(template="construction_empty") public static void brushPlacementConsumesOneRealBlock(GameTestHelper h){
        h.setBlock(BASE,Blocks.STONE);var brush=ConstructionSet.BRUSH.toStack();configure(h,brush);var p=player(h,brush);p.getInventory().setItem(1,new ItemStack(Items.COBBLESTONE,3));
        h.assertTrue(brushUse(h,p).consumesAction(),"Brush placement failed");h.assertBlockPresent(Blocks.COBBLESTONE,BASE.above());
        h.assertTrue(p.getInventory().getItem(1).getCount()==2&&p.getMainHandItem()==brush,"Brush lost itself or consumed wrong number of blocks");h.succeed();
    }
    @GameTest(template="construction_empty") public static void brushCannotPlaceWithoutMaterials(GameTestHelper h){
        h.setBlock(BASE,Blocks.STONE);var brush=ConstructionSet.BRUSH.toStack();configure(h,brush);var p=player(h,brush);brushUse(h,p);
        h.assertBlockPresent(Blocks.AIR,BASE.above());h.assertTrue(p.getMainHandItem()==brush,"Brush was replaced");h.succeed();
    }
    @GameTest(template="construction_empty") public static void brushCanceledPlacementKeepsMaterial(GameTestHelper h){
        h.setBlock(BASE,Blocks.STONE);var brush=ConstructionSet.BRUSH.toStack();configure(h,brush);var p=player(h,brush);p.getInventory().setItem(1,new ItemStack(Items.COBBLESTONE,3));
        Consumer<BlockEvent.EntityPlaceEvent> listener=e->{if(e.getPos().equals(h.absolutePos(BASE.above())))e.setCanceled(true);};NeoForge.EVENT_BUS.addListener(listener);
        try{brushUse(h,p);}finally{NeoForge.EVENT_BUS.unregister(listener);}
        h.assertBlockPresent(Blocks.AIR,BASE.above());h.assertTrue(p.getInventory().getItem(1).getCount()==3,"Denied brush placement consumed materials");h.succeed();
    }
    @GameTest(template="construction_empty") public static void brushUsesSatchelBlocks(GameTestHelper h){
        h.setBlock(BASE,Blocks.STONE);var brush=ConstructionSet.BRUSH.toStack();configure(h,brush);var p=player(h,brush);var bag=ConstructionSet.SATCHEL.toStack();var contents=NonNullList.withSize(27,ItemStack.EMPTY);contents.set(3,new ItemStack(Items.COBBLESTONE,4));SatchelItem.save(bag,contents);p.getInventory().setItem(1,bag);
        brushUse(h,p);h.assertBlockPresent(Blocks.COBBLESTONE,BASE.above());h.assertTrue(SatchelItem.contents(bag).get(3).getCount()==3,"Brush did not consume satchel material");h.succeed();
    }
    @GameTest(template="construction_empty") public static void brushWeightedChoiceHonorsDisabledSlots(GameTestHelper h){
        int[] weights={0,1,3,0};h.assertTrue(BrushItem.choose(weights,0)==1&&BrushItem.choose(weights,.24)==1&&BrushItem.choose(weights,.25)==2&&BrushItem.choose(weights,.99)==2,"Incorrect weighting");h.assertTrue(BrushItem.choose(new int[9],.5)==-1,"Zero weights should not select an item");h.succeed();
    }
    @GameTest(template="construction_empty",timeoutTicks=100) public static void mobCagePreservesNamedMobAndEmptiesOnRelease(GameTestHelper h){
        floor(h,1,1,8,8);var cage=ConstructionSet.CAGE.toStack();var p=player(h,cage);var mob=cow(h,h.absolutePos(new BlockPos(5,2,5)).getBottomCenter());var uuid=mob.getUUID();mob.setCustomName(Component.literal("Workshop Cow"));mob.setHealth(6);mob.getPersistentData().putString("test_marker","preserved");
        h.assertTrue(ConstructionSet.CAGE.get().interactLivingEntity(cage,p,mob,InteractionHand.MAIN_HAND).consumesAction(),"Capture failed");h.assertTrue(MobCageItem.filled(cage)&&mob.isRemoved(),"Capture did not remove mob");
        h.runAfterDelay(2,()->{var result=ConstructionSet.CAGE.get().useOn(context(h,p,BASE));h.assertTrue(result.consumesAction()&&!MobCageItem.filled(cage)&&cage.getCount()==1,"Release lost payload or cage");var released=h.getLevel().getEntity(uuid);h.assertTrue(released instanceof Cow&&released.getName().getString().equals("Workshop Cow")&&((Cow)released).getHealth()==6&&released.getPersistentData().getString("test_marker").equals("preserved"),"Mob data changed");h.succeed();});
    }
    @GameTest(template="construction_empty") public static void mobCageDeniedCaptureKeepsMob(GameTestHelper h){
        var cage=ConstructionSet.CAGE.toStack();var p=player(h,cage);var mob=cow(h,h.absolutePos(new BlockPos(5,2,5)).getBottomCenter());
        Consumer<PlayerInteractEvent.EntityInteract> listener=e->{if(e.getTarget()==mob)e.setCanceled(true);};NeoForge.EVENT_BUS.addListener(listener);
        try{ConstructionSet.CAGE.get().interactLivingEntity(cage,p,mob,InteractionHand.MAIN_HAND);}finally{NeoForge.EVENT_BUS.unregister(listener);}
        h.assertTrue(!mob.isRemoved()&&!MobCageItem.filled(cage),"Denied capture duplicated or removed mob");h.succeed();
    }
    @GameTest(template="construction_empty",timeoutTicks=100) public static void mobCageBlockedReleaseKeepsPayload(GameTestHelper h){
        floor(h,1,1,8,8);var cage=ConstructionSet.CAGE.toStack();var p=player(h,cage);var mob=cow(h,h.absolutePos(new BlockPos(6,2,6)).getBottomCenter());ConstructionSet.CAGE.get().interactLivingEntity(cage,p,mob,InteractionHand.MAIN_HAND);
        h.setBlock(BASE.above(),Blocks.STONE);h.setBlock(BASE.above(2),Blocks.STONE);
        h.runAfterDelay(2,()->{ConstructionSet.CAGE.get().useOn(context(h,p,BASE));h.assertTrue(MobCageItem.filled(cage),"Blocked release lost the mob");h.succeed();});
    }
    @GameTest(template="construction_empty") public static void mobCageRejectsBosses(GameTestHelper h){
        var cage=ConstructionSet.CAGE.toStack();var p=player(h,cage);var boss=EntityType.WITHER.create(h.getLevel());boss.setPos(p.position());
        ConstructionSet.CAGE.get().interactLivingEntity(cage,p,boss,InteractionHand.MAIN_HAND);h.assertTrue(!MobCageItem.filled(cage),"Wither captured");h.succeed();
    }
    static void bindAtDestination(GameTestHelper h,ItemStack mirror,ServerPlayer p){floor(h,14,14,27,27);p.setPos(h.absolutePos(DEST).getBottomCenter());MirrorItem.bind(mirror,p);p.setPos(h.absolutePos(new BlockPos(3,2,3)).getBottomCenter());}
    @GameTest(template="construction_empty") public static void normalMirrorOnlyMovesPlayer(GameTestHelper h){
        floor(h,1,1,8,8);var mirror=ConstructionSet.MIRROR.toStack();var p=player(h,mirror);bindAtDestination(h,mirror,p);var mob=cow(h,p.position().add(2,0,0));var before=mob.position();
        h.assertTrue(MirrorItem.teleport(p,mirror,false),"Mirror failed");h.assertTrue(p.distanceToSqr(h.absolutePos(DEST).getBottomCenter())<2&&mob.position().equals(before),"Normal mirror moved the wrong entities");h.succeed();
    }
    @GameTest(template="construction_empty") public static void areaMirrorMovesMobsAndItemsWithinFiveBlocks(GameTestHelper h){
        floor(h,1,1,11,11);var mirror=ConstructionSet.AREA_MIRROR.toStack();var p=player(h,mirror);bindAtDestination(h,mirror,p);var origin=p.position();var mob=cow(h,origin.add(2,0,0));var outside=cow(h,origin.add(6,0,0));var outsideBefore=outside.position();
        var drop=new ItemEntity(h.getLevel(),origin.x,origin.y,origin.z+2,new ItemStack(Items.DIAMOND,3));h.getLevel().addFreshEntity(drop);
        h.assertTrue(MirrorItem.teleport(p,mirror,true),"Area mirror failed");h.assertTrue(mob.distanceToSqr(h.absolutePos(DEST).getBottomCenter())<30&&drop.distanceToSqr(h.absolutePos(DEST).getBottomCenter())<30,"Mobs/items were not brought along");h.assertTrue(outside.position().equals(outsideBefore),"Entity outside five-block radius moved");h.succeed();
    }
    @GameTest(template="construction_empty") public static void mirrorBlockedDestinationMovesNobody(GameTestHelper h){
        floor(h,1,1,8,8);var mirror=ConstructionSet.AREA_MIRROR.toStack();var p=player(h,mirror);bindAtDestination(h,mirror,p);
        for(int x=12;x<=28;x++)for(int z=12;z<=28;z++)for(int y=2;y<=8;y++)h.setBlock(new BlockPos(x,y,z),Blocks.STONE);
        var before=p.position();var mob=cow(h,before.add(1,0,0));var mobBefore=mob.position();h.assertTrue(!MirrorItem.teleport(p,mirror,true)&&p.position().equals(before)&&mob.position().equals(mobBefore),"Blocked waypoint partially teleported group");h.succeed();
    }
    @GameTest(template="construction_empty") public static void mirrorRequiresFortyTicksAndHasIndependentWaypoints(GameTestHelper h){
        var a=ConstructionSet.MIRROR.toStack();var b=ConstructionSet.MIRROR.toStack();var p=player(h,a);MirrorItem.bind(a,p);
        h.assertTrue(ConstructionSet.MIRROR.get().getUseDuration(a,p)==40,"Mirror wind-up is not two seconds");h.assertTrue(MirrorItem.waypoint(a).isPresent()&&MirrorItem.waypoint(b).isEmpty(),"Waypoints leaked between mirrors");
        var before=p.position();ConstructionSet.MIRROR.get().use(h.getLevel(),p,InteractionHand.MAIN_HAND);h.assertTrue(p.isUsingItem()&&p.position().equals(before),"Mirror teleported before wind-up");p.stopUsingItem();h.succeed();
    }

    @GameTest(template="construction_empty") public static void satchelMenuMovesRealBlocksAndLocksItself(GameTestHelper h){
        var bag=ConstructionSet.SATCHEL.toStack();var p=player(h,bag);p.getInventory().setItem(1,new ItemStack(Items.STONE,20));var menu=new KitMenu(1,p.getInventory(),InteractionHand.MAIN_HAND);
        menu.quickMoveStack(p,27+27+1);h.assertTrue(p.getInventory().getItem(1).isEmpty()&&SatchelItem.contents(bag).get(0).getCount()==20,"Satchel quick transfer lost or duplicated blocks");
        menu.clicked(0,p.getInventory().selected,ClickType.SWAP,p);menu.clicked(27+27+p.getInventory().selected,0,ClickType.PICKUP,p);
        h.assertTrue(menu.getCarried().isEmpty()&&p.getMainHandItem()==bag&&SatchelItem.contents(bag).get(0).getCount()==20,"Open satchel was moved into itself");h.succeed();
    }
    @GameTest(template="construction_empty") public static void satchelRefillsPartialsAndLastEmptyStack(GameTestHelper h){
        var p=player(h,new ItemStack(Items.STONE,62));var bag=ConstructionSet.SATCHEL.toStack();var content=NonNullList.withSize(27,ItemStack.EMPTY);content.set(0,new ItemStack(Items.STONE,23));SatchelItem.save(bag,content);p.getInventory().setItem(2,bag);p.tickCount=0;
        SatchelItem.restock(p);h.assertTrue(p.getMainHandItem().getCount()==64&&SatchelItem.contents(bag).get(0).getCount()==21,"Partial stack refill was incorrect");
        p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);SatchelItem.restock(p);h.assertTrue(p.getMainHandItem().is(Items.STONE)&&p.getMainHandItem().getCount()==21&&SatchelItem.contents(bag).get(0).isEmpty(),"Last-block refill was incorrect");h.succeed();
    }
    @GameTest(template="construction_empty",timeoutTicks=100) public static void mobCageCanceledJoinKeepsPayload(GameTestHelper h){
        floor(h,1,1,8,8);var cage=ConstructionSet.CAGE.toStack();var p=player(h,cage);var mob=cow(h,h.absolutePos(new BlockPos(6,2,6)).getBottomCenter());var uuid=mob.getUUID();ConstructionSet.CAGE.get().interactLivingEntity(cage,p,mob,InteractionHand.MAIN_HAND);
        h.runAfterDelay(2,()->{Consumer<net.neoforged.neoforge.event.entity.EntityJoinLevelEvent> listener=e->{if(e.getEntity().getUUID().equals(uuid))e.setCanceled(true);};NeoForge.EVENT_BUS.addListener(listener);
            try{ConstructionSet.CAGE.get().useOn(context(h,p,BASE));}finally{NeoForge.EVENT_BUS.unregister(listener);}
            h.assertTrue(MobCageItem.filled(cage)&&h.getLevel().getEntity(uuid)==null,"Canceled release lost payload or spawned a duplicate");h.succeed();});
    }
    @GameTest(template="construction_empty") public static void areaMirrorKeepsRidingGroupsTogether(GameTestHelper h){
        floor(h,1,1,10,10);var mirror=ConstructionSet.AREA_MIRROR.toStack();var p=player(h,mirror);bindAtDestination(h,mirror,p);
        var boat=EntityType.BOAT.create(h.getLevel());boat.moveTo(p.getX()+2,p.getY(),p.getZ()+2,0,0);h.getLevel().addFreshEntity(boat);var passenger=cow(h,boat.position());passenger.startRiding(boat,true);
        h.assertTrue(MirrorItem.teleport(p,mirror,true),"Area mirror failed");h.assertTrue(passenger.getVehicle()==boat&&boat.distanceToSqr(h.absolutePos(DEST).getBottomCenter())<40,"Riding group separated or failed to move");h.succeed();
    }
    @GameTest(template="construction_empty",timeoutTicks=300) public static void mirrorCrossDimensionPreservesMobIdentity(GameTestHelper h){
        floor(h,1,1,8,8);var mirror=ConstructionSet.AREA_MIRROR.toStack();var p=player(h,mirror);var mob=cow(h,p.position().add(2,0,0));mob.setHealth(5);mob.setCustomName(Component.literal("Traveller"));var uuid=mob.getUUID();
        var target=p.server.getLevel(net.minecraft.world.level.Level.NETHER);h.assertTrue(target!=null,"Nether unavailable");
        for(int x=96;x<=106;x++)for(int z=96;z<=106;z++){target.setBlock(new BlockPos(x,129,z),Blocks.STONE.defaultBlockState(),3);for(int y=130;y<=134;y++)target.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),3);}
        var d=KitData.read(mirror);var point=new CompoundTag();point.putString("dimension","minecraft:the_nether");point.putDouble("x",100.5);point.putDouble("y",130);point.putDouble("z",100.5);d.put("mirror_waypoint",point);KitData.write(mirror,d);
        var destinationEntity=new java.util.concurrent.atomic.AtomicReference<Entity>();
        Consumer<net.neoforged.neoforge.event.entity.EntityJoinLevelEvent> listener=event->{if(event.getLevel()==target&&event.getEntity().getUUID().equals(uuid))destinationEntity.set(event.getEntity());};
        NeoForge.EVENT_BUS.addListener(listener);
        try{h.assertTrue(MirrorItem.teleport(p,mirror,true)&&p.serverLevel()==target,"Player did not change dimension");}
        finally{NeoForge.EVENT_BUS.unregister(listener);}
        // UUID lookups expose only tracked sections; a fresh destination can still be loading.
        // Check the actual entity registered by the destination, including its added-to-level state.
        h.runAfterDelay(2,()->{var arrived=destinationEntity.get();
            h.assertTrue(arrived instanceof Cow&&arrived.isAddedToLevel()&&!arrived.isRemoved()&&arrived.level()==target
                    &&arrived.getUUID().equals(uuid)&&arrived.getName().getString().equals("Traveller")&&((Cow)arrived).getHealth()==5&&mob.isRemoved(),
                "Cross-dimension mob identity/data: arrived="+arrived+" health="+(arrived instanceof Cow c?c.getHealth():-1)+" sourceRemoved="+mob.isRemoved());h.succeed();});
    }

    @GameTest(template="construction_empty",timeoutTicks=120) public static void mirrorHeldUseActuallyWaitsThenTeleports(GameTestHelper h){
        floor(h,1,1,8,8);var mirror=ConstructionSet.MIRROR.toStack();var p=player(h,mirror);bindAtDestination(h,mirror,p);var origin=p.position();
        ConstructionSet.MIRROR.get().use(h.getLevel(),p,InteractionHand.MAIN_HAND);
        // Embedded mock connections are not in the normal network ticking loop.
        for(int tick=0;tick<39;tick++)p.doTick();
        h.assertTrue(p.distanceToSqr(origin)<4&&p.getUseItemRemainingTicks()==1,"Mirror traveled before forty living ticks");
        p.doTick();h.assertTrue(p.distanceToSqr(h.absolutePos(DEST).getBottomCenter())<4,"Held mirror did not teleport on tick forty");h.succeed();
    }
    @GameTest(template="construction_empty") public static void areaMirrorIncludesNearbyPlayers(GameTestHelper h){
        floor(h,1,1,10,10);var mirror=ConstructionSet.AREA_MIRROR.toStack();var p=player(h,mirror);bindAtDestination(h,mirror,p);var friend=player(h,ItemStack.EMPTY);friend.setPos(p.position().add(2,0,0));
        h.assertTrue(MirrorItem.teleport(p,mirror,true),"Area mirror failed");h.assertTrue(friend.distanceToSqr(h.absolutePos(DEST).getBottomCenter())<30,"Nearby player was not teleported");h.succeed();
    }

    @GameTest(template="construction_empty") public static void brushAvoidsRepeatingTheSameMaterialAcrossDuplicateSlots(GameTestHelper h){
        h.setBlock(BASE,Blocks.STONE);h.setBlock(BASE.east(),Blocks.STONE);var brush=ConstructionSet.BRUSH.toStack();var palette=NonNullList.withSize(9,ItemStack.EMPTY);
        palette.set(0,new ItemStack(Items.COBBLESTONE));palette.set(1,new ItemStack(Items.COBBLESTONE));palette.set(2,new ItemStack(Items.DIRT));
        BrushItem.savePalette(brush,palette,new int[]{1,1,1,0,0,0,0,0,0},h.getLevel().registryAccess());var data=KitData.read(brush);data.putInt("brush_mode",1);data.putBoolean("brush_avoid_repeat",true);KitData.write(brush,data);
        var p=player(h,brush);p.getInventory().setItem(1,new ItemStack(Items.COBBLESTONE,3));p.getInventory().setItem(2,new ItemStack(Items.DIRT,3));brushUse(h,p);
        p.gameMode.useItemOn(p,h.getLevel(),brush,InteractionHand.MAIN_HAND,new BlockHitResult(h.absolutePos(BASE.east()).getCenter(),Direction.UP,h.absolutePos(BASE.east()),false));
        h.assertBlockPresent(Blocks.COBBLESTONE,BASE.above());h.assertBlockPresent(Blocks.DIRT,BASE.east().above());h.succeed();
    }
}

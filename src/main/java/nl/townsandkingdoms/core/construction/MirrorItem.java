package nl.townsandkingdoms.core.construction;

import java.util.*;
import net.minecraft.ChatFormatting;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.*;

public final class MirrorItem extends Item {
    public static final int WINDUP=40,RADIUS=5,COOLDOWN=100;
    public final boolean area;
    public MirrorItem(Properties p,boolean area){super(p);this.area=area;}
    public record Waypoint(ResourceKey<Level> dimension,Vec3 pos,float yaw,float pitch){}
    public static Optional<Waypoint> waypoint(ItemStack stack){
        var d=KitData.read(stack).getCompound("mirror_waypoint");var id=ResourceLocation.tryParse(d.getString("dimension"));
        if(id==null||d.getString("dimension").isBlank()||!d.contains("x",Tag.TAG_ANY_NUMERIC)
                ||!d.contains("y",Tag.TAG_ANY_NUMERIC)||!d.contains("z",Tag.TAG_ANY_NUMERIC))return Optional.empty();
        double x=d.getDouble("x"),y=d.getDouble("y"),z=d.getDouble("z");
        if(!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(z)||Math.abs(x)>30_000_000||Math.abs(z)>30_000_000)return Optional.empty();
        float yaw=d.getFloat("yaw"),pitch=d.getFloat("pitch");if(!Float.isFinite(yaw)||!Float.isFinite(pitch))return Optional.empty();
        return Optional.of(new Waypoint(ResourceKey.create(Registries.DIMENSION,id),new Vec3(x,y,z),yaw,Math.clamp(pitch,-90F,90F)));
    }
    public static void bind(ItemStack stack,ServerPlayer player){
        var d=KitData.read(stack);var point=new CompoundTag();point.putString("dimension",player.level().dimension().location().toString());
        point.putDouble("x",player.getX());point.putDouble("y",player.getY());point.putDouble("z",player.getZ());point.putFloat("yaw",player.getYRot());point.putFloat("pitch",player.getXRot());d.put("mirror_waypoint",point);KitData.write(stack,d);
        player.displayClientMessage(Component.literal("Mirror bound to "+player.blockPosition().toShortString()+" · "+player.level().dimension().location()),false);
    }
    @Override public InteractionResult onItemUseFirst(ItemStack stack,UseOnContext c){return c.getPlayer()==null?InteractionResult.PASS:use(c.getLevel(),c.getPlayer(),c.getHand()).getResult();}
    @Override public InteractionResult useOn(UseOnContext c){return c.getPlayer()==null?InteractionResult.PASS:use(c.getLevel(),c.getPlayer(),c.getHand()).getResult();}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        var stack=player.getItemInHand(hand);
        if(player.isShiftKeyDown()){
            if(player instanceof ServerPlayer sp)bind(stack,sp);
            return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
        }
        if(waypoint(stack).isEmpty()){if(!level.isClientSide)player.displayClientMessage(Component.literal("Sneak-right-click to bind a waypoint first."),true);return InteractionResultHolder.fail(stack);}
        if(player.getCooldowns().isOnCooldown(this))return InteractionResultHolder.fail(stack);
        player.startUsingItem(hand);return InteractionResultHolder.consume(stack);
    }
    @Override public int getUseDuration(ItemStack stack,LivingEntity entity){return WINDUP;}
    @Override public UseAnim getUseAnimation(ItemStack stack){return UseAnim.NONE;}
    @Override public void onUseTick(Level level,LivingEntity user,ItemStack stack,int remaining){
        if(level instanceof ServerLevel server&&remaining%5==0){
            server.sendParticles(area?ParticleTypes.PORTAL:ParticleTypes.END_ROD,user.getX(),user.getY()+1,user.getZ(),area?12:4,.5,.6,.5,.02);
            if(user instanceof Player p)p.displayClientMessage(Component.literal(String.format(java.util.Locale.ROOT,"Teleporting… %.1fs",remaining/20F)),true);
        }
    }
    @Override public ItemStack finishUsingItem(ItemStack stack,Level level,LivingEntity user){
        if(user instanceof ServerPlayer player){if(teleport(player,stack,area))player.getCooldowns().addCooldown(this,COOLDOWN);}
        return stack;
    }
    private record Plan(Entity root,Vec3 position,AABB bounds){}
    public static boolean teleport(ServerPlayer player,ItemStack stack,boolean area){
        var waypoint=waypoint(stack);if(waypoint.isEmpty())return false;
        var point=waypoint.get();var target=player.server.getLevel(point.dimension);if(target==null)return fail(player,"The bound dimension is unavailable.");
        if(!player.isAlive()||player.isSpectator())return false;
        if(!area&&(player.isPassenger()||player.isVehicle()))return fail(player,"Dismount before using the single-target mirror.");
        var source=player.serverLevel();var origin=player.position();
        var roots=new LinkedHashMap<UUID,Entity>();roots.put(player.getRootVehicle().getUUID(),player.getRootVehicle());
        if(area)for(var e:source.getEntities(player,player.getBoundingBox().inflate(RADIUS),e->e.isAlive()&&!e.isSpectator()&&e.distanceToSqr(origin)<=RADIUS*RADIUS)){
            Entity root=e.getRootVehicle();roots.putIfAbsent(root.getUUID(),root);
        }
        if(roots.size()>256)return fail(player,"Too many entities are in the mirror's radius.");
        var all=new HashSet<UUID>();for(var root:roots.values())root.getSelfAndPassengers().forEach(e->all.add(e.getUUID()));
        if(all.size()>256)return fail(player,"Too many passengers are in the mirror's radius.");
        for(var root:roots.values())for(var e:root.getSelfAndPassengers().toList())if(source!=target&&!e.canChangeDimensions(source,target))return fail(player,"One of the entities cannot travel to that dimension.");
        target.getChunk(BlockPos.containing(point.pos));var plans=new ArrayList<Plan>();
        // Plan every group before moving any: a blocked destination leaves the entire group at home.
        for(var root:roots.values()){
            Vec3 preferred=point.pos.add(area?root.getX()-origin.x:0,0,area?root.getZ()-origin.z:0);
            AABB group=root.getBoundingBox();for(var passenger:root.getIndirectPassengers())group=group.minmax(passenger.getBoundingBox());
            var safe=findSafe(target,preferred,root.position(),group,plans.stream().map(Plan::bounds).toList(),all);
            if(safe.isEmpty())return fail(player,"There is not enough safe space at the waypoint.");
            var destination=safe.get();plans.add(new Plan(root,destination,group.move(destination.subtract(root.position()))));
        }
        // Move the caster's root last, so feedback is delivered in the correct world.
        plans.sort(Comparator.comparing(p->p.root.getSelfAndPassengers().anyMatch(e->e==player)));
        source.playSound(null,player.blockPosition(),SoundEvents.ENDERMAN_TELEPORT,SoundSource.PLAYERS,.8F,1.0F);
        int moved=0;
        for(var plan:plans){
            var root=plan.root;var result=root.changeDimension(new DimensionTransition(target,plan.position,Vec3.ZERO,root==player?point.yaw:root.getYRot(),root==player?point.pitch:root.getXRot(),DimensionTransition.PLACE_PORTAL_TICKET));
            if(result!=null){result.fallDistance=0;result.setDeltaMovement(Vec3.ZERO);moved++;}
        }
        target.playSound(null,BlockPos.containing(point.pos),SoundEvents.ENDERMAN_TELEPORT,SoundSource.PLAYERS,.8F,1.25F);
        if(moved<plans.size())player.displayClientMessage(Component.literal("Some travel was prevented by another mod."),true);
        return moved>0;
    }
    public static Optional<Vec3> findSafe(ServerLevel level,Vec3 preferred,Vec3 oldOrigin,AABB oldBounds,List<AABB> reserved,Set<UUID> moving){
        int[] vertical={0,1,-1,2,-2,3,-3};
        for(int radius=0;radius<=4;radius++)for(int dx=-radius;dx<=radius;dx++)for(int dz=-radius;dz<=radius;dz++){
            if(Math.max(Math.abs(dx),Math.abs(dz))!=radius)continue;
            for(int dy:vertical){
                int x=net.minecraft.util.Mth.floor(preferred.x)+dx,z=net.minecraft.util.Mth.floor(preferred.z)+dz,y=net.minecraft.util.Mth.floor(preferred.y)+dy;
                var floor=new BlockPos(x,y-1,z);if(level.isOutsideBuildHeight(floor)||!level.getWorldBorder().isWithinBounds(floor))continue;
                var support=level.getBlockState(floor);var shape=support.getCollisionShape(level,floor);if(shape.isEmpty()||hazard(support))continue;
                Vec3 pos=new Vec3(x+.5,floor.getY()+shape.max(Direction.Axis.Y),z+.5);AABB box=oldBounds.move(pos.subtract(oldOrigin));
                if(box.maxY>level.getMaxBuildHeight()||box.minY<level.getMinBuildHeight()||!level.getWorldBorder().isWithinBounds(box)||level.getBlockCollisions(null,box).iterator().hasNext())continue;
                boolean unsafe=false;
                for(var bp:BlockPos.betweenClosed(BlockPos.containing(box.minX,box.minY,box.minZ),BlockPos.containing(box.maxX-1e-5,box.maxY-1e-5,box.maxZ-1e-5)))if(hazard(level.getBlockState(bp))){unsafe=true;break;}
                if(unsafe||reserved.stream().anyMatch(box::intersects))continue;
                if(!level.getEntities((Entity)null,box,e->!moving.contains(e.getUUID())&&e.isAlive()&&!e.isSpectator()&&e.isPickable()).isEmpty())continue;
                return Optional.of(pos);
            }
        }return Optional.empty();
    }
    private static boolean hazard(net.minecraft.world.level.block.state.BlockState s){return s.is(Blocks.LAVA)||s.is(Blocks.FIRE)||s.is(Blocks.SOUL_FIRE)||s.is(Blocks.CACTUS)||s.is(Blocks.MAGMA_BLOCK)||s.is(Blocks.CAMPFIRE)||s.is(Blocks.SOUL_CAMPFIRE)||s.is(Blocks.POWDER_SNOW)||s.is(Blocks.SWEET_BERRY_BUSH)||s.is(Blocks.WITHER_ROSE);}
    private static boolean fail(Player player,String text){player.displayClientMessage(Component.literal(text),true);return false;}
    @Override public void appendHoverText(ItemStack stack,TooltipContext c,List<Component> lines,TooltipFlag flag){
        lines.add(Component.literal("Sneak-right-click to bind. Hold right-click for 2 seconds to travel.").withStyle(ChatFormatting.GRAY));
        if(area)lines.add(Component.literal("Brings entities within 5 blocks, including items and riding groups.").withStyle(ChatFormatting.LIGHT_PURPLE));
        waypoint(stack).ifPresent(p->lines.add(Component.literal("Bound: "+BlockPos.containing(p.pos).toShortString()+" · "+p.dimension.location()).withStyle(ChatFormatting.AQUA)));
    }
}

package nl.townsandkingdoms.core.construction;

import java.util.*;
import java.io.*;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.tags.TagKey;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.*;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public final class MobCageItem extends Item {
    private static final String PAYLOAD="caged_mob";
    public static final TagKey<EntityType<?>> UNCAGEABLE=TagKey.create(Registries.ENTITY_TYPE,ResourceLocation.fromNamespaceAndPath("tnkcore","uncageable"));
    private static final TagKey<EntityType<?>> BOSSES=TagKey.create(Registries.ENTITY_TYPE,ResourceLocation.fromNamespaceAndPath("c","bosses"));
    private static final TagKey<EntityType<?>> NEO_BOSSES=TagKey.create(Registries.ENTITY_TYPE,ResourceLocation.fromNamespaceAndPath("neoforge","bosses"));
    public MobCageItem(Properties p){super(p);}
    public static boolean filled(ItemStack stack){return KitData.read(stack).contains(PAYLOAD,Tag.TAG_COMPOUND);}
    @Override public InteractionResult interactLivingEntity(ItemStack stack,Player p,LivingEntity target,InteractionHand hand){
        if(filled(stack)||!(target instanceof Mob mob))return InteractionResult.PASS;
        if(p.level().isClientSide)return InteractionResult.SUCCESS;
        if(!(p instanceof ServerPlayer player)||!player.getAbilities().mayBuild||player.isSpectator())return InteractionResult.FAIL;
        if(!mob.isAlive()||mob.isRemoved()||mob.getType().is(UNCAGEABLE)||mob.getType().is(BOSSES)||mob.getType().is(NEO_BOSSES))return fail(p,"That mob cannot be caged.");
        if(mob.isPassenger()||mob.isVehicle()||mob.isLeashed())return fail(p,"Dismount or unleash the mob first.");
        if(!p.level().mayInteract(p,mob.blockPosition())||NeoForge.EVENT_BUS.post(new PlayerInteractEvent.EntityInteract(p,hand,mob)).isCanceled())return InteractionResult.FAIL;
        CompoundTag tag=new CompoundTag();if(!mob.saveAsPassenger(tag))return fail(p,"This mob cannot be saved.");
        try{var bytes=new ByteArrayOutputStream();NbtIo.write(tag,new DataOutputStream(bytes));if(bytes.size()>65536)return fail(p,"This mob has too much stored data for a cage.");}catch(IOException e){return InteractionResult.FAIL;}
        var data=KitData.read(stack);data.put(PAYLOAD,tag);KitData.write(stack,data);mob.discard();
        p.level().playSound(null,p.blockPosition(),SoundEvents.FIRECHARGE_USE,SoundSource.PLAYERS,.7F,1.2F);return InteractionResult.CONSUME;
    }
    @Override public InteractionResult onItemUseFirst(ItemStack stack,UseOnContext c){return filled(stack)?useOn(c):InteractionResult.PASS;}
    @Override public InteractionResult useOn(UseOnContext c){
        if(!filled(c.getItemInHand())||c.getPlayer()==null)return InteractionResult.PASS;
        var p=c.getPlayer();if(!p.getAbilities().mayBuild||p.isSpectator())return InteractionResult.FAIL;
        var pos=new BlockPlaceContext(c).getClickedPos();if(!c.getLevel().mayInteract(p,pos)||!p.mayUseItemAt(pos,c.getClickedFace(),c.getItemInHand()))return InteractionResult.FAIL;
        if(c.getLevel().isClientSide)return InteractionResult.SUCCESS;
        var level=(ServerLevel)c.getLevel();var event=new PlayerInteractEvent.RightClickBlock(p,c.getHand(),c.getClickedPos(),new net.minecraft.world.phys.BlockHitResult(c.getClickLocation(),c.getClickedFace(),c.getClickedPos(),c.isInside()));
        if(NeoForge.EVENT_BUS.post(event).isCanceled()||event.getUseBlock()==net.neoforged.neoforge.common.util.TriState.FALSE)return InteractionResult.FAIL;
        var data=KitData.read(c.getItemInHand());var tag=data.getCompound(PAYLOAD).copy();
        if(tag.hasUUID("UUID"))for(var world:level.getServer().getAllLevels()){var existing=world.getEntity(tag.getUUID("UUID"));if(existing!=null&&!existing.isRemoved())return fail(p,"That mob already exists in the world.");}
        Entity entity=EntityType.loadEntityRecursive(tag,level,e->e);
        if(!(entity instanceof Mob mob))return fail(p,"The captured mob type is unavailable.");
        mob.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,p.getYRot(),0);
        if(level.isOutsideBuildHeight(pos)||!level.getWorldBorder().isWithinBounds(mob.getBoundingBox())||!level.noCollision(mob,mob.getBoundingBox()))return fail(p,"Not enough space to release this mob.");
        if(!level.addFreshEntity(mob))return fail(p,"The mob could not be released here.");
        data.remove(PAYLOAD);KitData.write(c.getItemInHand(),data);level.playSound(null,pos,SoundEvents.FIRE_EXTINGUISH,SoundSource.PLAYERS,.7F,1.1F);return InteractionResult.CONSUME;
    }
    private static InteractionResult fail(Player p,String text){p.displayClientMessage(Component.literal(text),true);return InteractionResult.FAIL;}
    @Override public void appendHoverText(ItemStack stack,TooltipContext c,List<Component> lines,TooltipFlag flag){
        if(filled(stack)){var tag=KitData.read(stack).getCompound(PAYLOAD);var id=ResourceLocation.tryParse(tag.getString("id"));var type=id==null?Optional.<EntityType<?>>empty():BuiltInRegistries.ENTITY_TYPE.getOptional(id);
            lines.add(Component.literal("Contains: ").append(type.<Component>map(EntityType::getDescription).orElseGet(()->Component.literal(tag.getString("id")))).withStyle(ChatFormatting.GOLD));
            lines.add(Component.literal("Right-click a block to release. Cage is reusable.").withStyle(ChatFormatting.GRAY));
        }else lines.add(Component.literal("Right-click a mob to capture it. Bosses are excluded.").withStyle(ChatFormatting.GRAY));
    }
}

package nl.townsandkingdoms.core.boss;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/** Visual-only native ModelEngine replacement; all damage stays server-side. */
public final class GoliathEffectEntity extends Monster implements GeoEntity {
    private static final EntityDataAccessor<Integer> EFFECT =
            SynchedEntityData.defineId(GoliathEffectEntity.class, EntityDataSerializers.INT);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int lifetime = 80;

    public GoliathEffectEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
        setNoAi(true);
        setInvulnerable(true);
    }

    public void setGroundBreak(boolean groundBreak) {
        entityData.set(EFFECT, groundBreak ? 1 : 0);
    }

    public boolean isGroundBreak() { return entityData.get(EFFECT) == 1; }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(EFFECT, 0);
    }

    @Override
    protected void registerGoals() { }

    @Override
    public boolean isPickable() { return false; }

    @Override
    public boolean hurt(DamageSource source, float amount) { return false; }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && --lifetime <= 0) discard();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "effect", 0, state -> {
            if (isGroundBreak()) state.setAnimation(RawAnimation.begin().thenLoop(
                    "animation.tnkcore.sculk_goliath_groundbreak.idle"));
            else state.setAnimation(RawAnimation.begin().thenPlay(
                    "animation.tnkcore.sculk_goliath_dig.spawn"));
            return PlayState.CONTINUE;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("EffectVariant", entityData.get(EFFECT));
        tag.putInt("RemainingTicks", lifetime);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(EFFECT, tag.getInt("EffectVariant"));
        lifetime = tag.getInt("RemainingTicks");
    }
}

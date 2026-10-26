package nl.townsandkingdoms.core.boss;

import java.util.List;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Native Sculk Goliath entity. No MythicMobs or ModelEngine runtime is used.
 *
 * Attack timings are intentionally isolated here so the full original skill
 * port can be added without touching Workshop's existing features.
 */
public final class SculkGoliathEntity extends Monster implements GeoEntity {
    private static final EntityDataAccessor<Integer> PHASE =
            SynchedEntityData.defineId(SculkGoliathEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> BUSY =
            SynchedEntityData.defineId(SculkGoliathEntity.class, EntityDataSerializers.BOOLEAN);

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private final ServerBossEvent bossBar = new ServerBossEvent(
            Component.literal("SCULK GOLIATH"), BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.PROGRESS);

    private enum Attack {
        NONE, SWIPE1, SWIPE2, SWIPE3, STOMP, SONIC, SCREAM, CHARGE, PULL, LEAP, SOUL_BURST, ANTIMACE
    }

    private Attack activeAttack = Attack.NONE;
    private int attackAge;
    private int nextAttack = 60;
    private int transformationTicks;
    private int entranceTicks = 125;
    private boolean entranceStarted;
    private boolean validatedPack;
    private long castleKey;
    private int deathAnimationTicks;

    public SculkGoliathEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        this.setPersistenceRequired();
        this.xpReward = 500;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 850.0)
                .add(Attributes.MOVEMENT_SPEED, 0.20)
                .add(Attributes.ATTACK_DAMAGE, 18.0)
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.ARMOR, 12.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(PHASE, 1);
        builder.define(BUSY, false);
    }

    public boolean isPhaseTwo() {
        return this.entityData.get(PHASE) == 2;
    }

    public void setCastleKey(long key) {
        this.castleKey = key;
    }

    public long getCastleKey() {
        return this.castleKey;
    }

    private String animBase() {
        return isPhaseTwo() ? "animation.tnkcore.sculk_goliath_destroyed." :
                "animation.tnkcore.sculk_goliath.";
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 5, state -> {
            if (this.entityData.get(BUSY)) return PlayState.STOP;
            state.setAnimation(RawAnimation.begin().thenLoop(animBase() +
                    (state.isMoving() ? "walk" : "idle")));
            return PlayState.CONTINUE;
        }));
        AnimationController<SculkGoliathEntity> attacks =
                new AnimationController<>(this, "attack", 0, state -> PlayState.STOP);
        String[] animations = {"attack1", "attack2", "attack3", "antimace", "stompcombo",
                "sonicsmash", "scream", "run", "pullsmashes", "leapsmash", "soulscream",
                "startphase2", "death", "spawn", "dig", "pickupsmash", "groundsmash",
                "halfexplode", "breakscream", "sniff"};
        for (String name : animations) {
            attacks.triggerableAnim("p1_" + name,
                    RawAnimation.begin().thenPlay("animation.tnkcore.sculk_goliath." + name));
            if (!name.equals("pullsmashes") && !name.equals("leapsmash") && !name.equals("spawn")) {
                attacks.triggerableAnim("p2_" + name,
                        RawAnimation.begin().thenPlay("animation.tnkcore.sculk_goliath_destroyed." + name));
            }
        }
        controllers.add(attacks);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }

    private void animate(String name) {
        String phase = isPhaseTwo() ? "p2_" : "p1_";
        this.triggerAnim("attack", phase + name);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(6, new RandomStrollGoal(this, 0.7));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 48.0F));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel server)) return;

        // Vanilla continues ticking a mob while its death animation is active.
        // Never let a defeated boss acquire targets or execute attacks as a corpse.
        if (isDeadOrDying()) {
            activeAttack = Attack.NONE;
            this.entityData.set(BUSY, true);
            getNavigation().stop();
            return;
        }

        if (!validatedPack) {
            validatedPack = true;
            if (!SculkGoliathBootstrap.packEnabled()) {
                discard();
                return;
            }
        }

        // The original pack has a 6.25-second emergence animation. Play it
        // once at the beginning of the encounter, before any attack can start.
        if (entranceTicks > 0) {
            if (!entranceStarted) {
                entranceStarted = true;
                this.entityData.set(BUSY, true);
                this.triggerAnim("attack", "p1_spawn");
                this.playSound(SoundEvents.WARDEN_EMERGE, 3.0F, 0.6F);
            }
            getNavigation().stop();
            entranceTicks--;
            if (entranceTicks == 0) this.entityData.set(BUSY, false);
            return;
        }

        if (this.tickCount % 5 == 0) {
            float total = isPhaseTwo() ? 400.0F : 850.0F;
            bossBar.setName(Component.literal(isPhaseTwo() ? "SCULK GOLIATH - PHASE 2" : "SCULK GOLIATH"));
            bossBar.setProgress(Math.clamp(getHealth() / total, 0.0F, 1.0F));
        }

        if (transformationTicks > 0) {
            transformationTicks--;
            if (transformationTicks == 0) this.entityData.set(BUSY, false);
            getNavigation().stop();
            if (transformationTicks % 8 == 0)
                server.sendParticles(ParticleTypes.SCULK_SOUL, getX(), getY() + 2.5, getZ(), 14, 1.2, 1.4, 1.2, 0.03);
            return;
        }

        LivingEntity target = getTarget();
        if (target == null || !target.isAlive() || distanceToSqr(target) > 48 * 48) {
            activeAttack = Attack.NONE;
            this.entityData.set(BUSY, false);
            return;
        }
        if (activeAttack != Attack.NONE) {
            attackAge++;
            processAttack(server, target);
            return;
        }
        if (--nextAttack <= 0) selectAttack(target);
        if (this.tickCount % 180 == 0) this.playSound(SoundEvents.WARDEN_AMBIENT, 2.0F, 0.7F);
    }

    private void selectAttack(LivingEntity target) {
        double distance = this.distanceTo(target);
        if (distance <= 6) {
            Attack[] attacks = {Attack.SWIPE1, Attack.SWIPE2, Attack.SWIPE3, Attack.STOMP, Attack.PULL};
            startAttack(attacks[this.random.nextInt(attacks.length)]);
        } else if (distance > 16 && this.random.nextBoolean()) {
            startAttack(Attack.CHARGE);
        } else {
            Attack[] attacks = {Attack.SONIC, Attack.SCREAM, Attack.LEAP, Attack.SOUL_BURST};
            startAttack(attacks[this.random.nextInt(attacks.length)]);
        }
    }

    private void startAttack(Attack attack) {
        activeAttack = attack;
        this.entityData.set(BUSY, true);
        attackAge = 0;
        getNavigation().stop();
        String animation = switch (attack) {
            case SWIPE1 -> "attack1";
            case SWIPE2 -> "attack2";
            case SWIPE3 -> "attack3";
            case STOMP -> "stompcombo";
            case SONIC -> "sonicsmash";
            case SCREAM -> "scream";
            case CHARGE -> "run";
            case PULL -> "pickupsmash";
            case LEAP -> isPhaseTwo() ? "groundsmash" : "leapsmash";
            case SOUL_BURST -> "soulscream";
            case ANTIMACE -> "antimace";
            default -> "idle";
        };
        animate(animation);
        if (attack == Attack.SCREAM || attack == Attack.SONIC)
            this.playSound(SoundEvents.WARDEN_ANGRY, 2.0F, 0.7F);
    }

    private void processAttack(ServerLevel server, LivingEntity target) {
        switch (activeAttack) {
            case SWIPE1, SWIPE2, SWIPE3 -> {
                if (attackAge == 25) {
                    this.playSound(SoundEvents.WARDEN_ATTACK_IMPACT, 2.0F, 0.7F);
                    hitPlayers(5.0, 18.0F, 1.6);
                }
                if (attackAge >= 55) finishAttack();
            }
            case STOMP -> {
                if (attackAge == 25 || attackAge == 50 || attackAge == 75) {
                    this.playSound(SoundEvents.WARDEN_ATTACK_IMPACT, 2.0F, 0.6F);
                    server.sendParticles(ParticleTypes.SCULK_SOUL, getX(), getY() + 0.2, getZ(),
                            35, 3.0, 0.15, 3.0, 0.13);
                    hitPlayers(7.0, 14.0F, 1.4);
                }
                if (attackAge >= 100) finishAttack();
            }
            case SONIC -> {
                if (attackAge == 15) this.playSound(SoundEvents.WARDEN_SONIC_CHARGE, 2.0F, 0.8F);
                if (attackAge == 38) {
                    this.playSound(SoundEvents.WARDEN_SONIC_BOOM, 3.0F, 0.7F);
                    fireSonicRay(server, target, isPhaseTwo() ? 30.0F : 24.0F);
                }
                if (attackAge >= 65) finishAttack();
            }
            case SCREAM -> {
                if (attackAge == 15) {
                    this.playSound(SoundEvents.WARDEN_ROAR, 3.0F, 0.55F);
                    hitPlayers(18.0, 8.0F, 2.5);
                }
                if (attackAge >= 65) finishAttack();
            }
            case CHARGE -> {
                if (attackAge < 50) {
                    Vec3 delta = target.position().subtract(this.position()).multiply(1, 0, 1).normalize();
                    this.setDeltaMovement(delta.scale(0.65));
                    if (this.distanceTo(target) < 4.0) {
                        hitPlayers(4.5, 24.0F, 2.0);
                        finishAttack();
                    }
                } else finishAttack();
            }
            case PULL -> {
                if (attackAge == 15) {
                    for (Player player : nearbyPlayers(14)) {
                        Vec3 delta = position().subtract(player.position()).normalize();
                        player.push(delta.x * 1.0, 0.3, delta.z * 1.0);
                    }
                }
                if (attackAge == 40) hitPlayers(6.5, 22.0F, 2.0);
                if (attackAge >= 65) finishAttack();
            }
            case LEAP -> {
                if (attackAge == 10) {
                    Vec3 delta = target.position().subtract(position()).multiply(1, 0, 1).normalize();
                    setDeltaMovement(delta.scale(1.15).add(0, 0.8, 0));
                }
                if (attackAge == 40) hitPlayers(7.5, 28.0F, 1.8);
                if (attackAge >= 80) finishAttack();
            }
            case SOUL_BURST -> {
                if (attackAge >= 35 && attackAge <= 75 && attackAge % 8 == 0) {
                    server.sendParticles(ParticleTypes.SCULK_SOUL, target.getX(), target.getY() + 0.7,
                            target.getZ(), 30, 1.2, 0.5, 1.2, 0.03);
                    if (target.distanceTo(this) <= 28 && this.hasLineOfSight(target))
                        target.hurt(this.damageSources().mobAttack(this), 9.0F);
                }
                if (attackAge >= 120) finishAttack();
            }
            case ANTIMACE -> {
                if (attackAge == 30) {
                    hitPlayers(5.0, 32.0F, 2.2);
                    this.playSound(SoundEvents.WARDEN_ATTACK_IMPACT, 3.0F, 0.6F);
                }
                if (attackAge >= 75) finishAttack();
            }
            default -> finishAttack();
        }
    }

    private List<Player> nearbyPlayers(double radius) {
        return level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(radius),
                p -> p.isAlive() && !p.isCreative() && !p.isSpectator() && distanceToSqr(p) <= radius * radius);
    }

    private void hitPlayers(double radius, float damage, double knockback) {
        for (Player player : nearbyPlayers(radius)) {
            if (player.hurt(this.damageSources().mobAttack(this), damage)) {
                Vec3 delta = player.position().subtract(this.position()).multiply(1, 0, 1).normalize();
                player.push(delta.x * knockback, 0.55, delta.z * knockback);
                player.hurtMarked = true;
            }
        }
    }

    private void fireSonicRay(ServerLevel server, LivingEntity target, float damage) {
        Vec3 from = this.getEyePosition();
        Vec3 to = target.getEyePosition();
        Vec3 step = to.subtract(from);
        int count = Math.max(1, (int) (step.length() * 3));
        for (int i = 1; i <= count; i++) {
            Vec3 point = from.add(step.scale((double) i / count));
            server.sendParticles(ParticleTypes.SONIC_BOOM, point.x, point.y, point.z, 1, 0, 0, 0, 0);
        }
        if (distanceTo(target) <= 30 && hasLineOfSight(target)) {
            target.hurt(this.damageSources().sonicBoom(this), damage);
            target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 70, 0));
        }
    }

    private void finishAttack() {
        activeAttack = Attack.NONE;
        this.entityData.set(BUSY, false);
        attackAge = 0;
        nextAttack = isPhaseTwo() ? 30 : 45;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() instanceof Player player && !isPhaseTwo()
                && player.getMainHandItem().is(Items.MACE) && activeAttack == Attack.NONE
                && transformationTicks == 0 && distanceToSqr(player) < 64) {
            startAttack(Attack.ANTIMACE);
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public void die(DamageSource source) {
        if (!isPhaseTwo() && !level().isClientSide) {
            this.entityData.set(PHASE, 2);
            this.setHealth(400.0F);
            this.activeAttack = Attack.NONE;
            this.entranceTicks = 0;
            this.entityData.set(BUSY, true);
            this.transformationTicks = 140;
            this.nextAttack = 80;
            this.animate("startphase2");
            this.playSound(SoundEvents.WARDEN_ROAR, 3.0F, 0.45F);
            return;
        }
        SculkGoliathEncounters.defeated(this);
        this.animate("death");
        super.die(source);
    }

    @Override
    protected void tickDeath() {
        // Keep the corpse rendered long enough for the original four-second
        // ModelEngine death animation instead of vanishing after 20 ticks.
        if (++deathAnimationTicks < 60) return;
        super.tickDeath();
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossBar.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossBar.removePlayer(player);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("GoliathPhase", isPhaseTwo() ? 2 : 1);
        tag.putInt("GoliathTransformationTicks", transformationTicks);
        tag.putInt("GoliathEntranceTicks", entranceTicks);
        tag.putLong("GoliathCastleKey", castleKey);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(PHASE, tag.getInt("GoliathPhase") == 2 ? 2 : 1);
        this.transformationTicks = tag.getInt("GoliathTransformationTicks");
        this.entranceTicks = tag.getInt("GoliathEntranceTicks");
        this.entranceStarted = false;
        this.castleKey = tag.getLong("GoliathCastleKey");
        this.activeAttack = Attack.NONE;
        this.entityData.set(BUSY, transformationTicks > 0 || entranceTicks > 0);
        this.nextAttack = 60;
    }
}

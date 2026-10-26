package nl.townsandkingdoms.core.boss;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Server-safe replacement for ModelEngine helper effects, missiles and target
 * selectors. No fake silverfish mobs, persistent blocks or griefing entities.
 */
public final class GoliathSkillVisuals {
    private static final int MAX_PROJECTILES = 80;
    private static final int MAX_EFFECTS = 100;
    private static final int MAX_PARTICLES_PER_TICK = 1500;

    private static final class Projectile {
        Vec3 position;
        final Vec3 velocity;
        final LivingEntity target;
        final String onTick;
        final String onHit;
        final String onHitBlock;
        final int interval;
        final int maximumAge;
        int age;

        Projectile(Vec3 position, Vec3 velocity, LivingEntity target, String onTick,
                   String onHit, String onHitBlock, int interval, int maximumAge) {
            this.position = position;
            this.velocity = velocity;
            this.target = target;
            this.onTick = onTick;
            this.onHit = onHit;
            this.onHitBlock = onHitBlock;
            this.interval = Math.max(1, interval);
            this.maximumAge = maximumAge;
        }
    }

    private static final class Helper {
        final Vec3 position;
        final String type;
        int age;
        Helper(Vec3 position, String type) {
            this.position = position;
            this.type = type;
        }
    }

    private final SculkGoliathEntity owner;
    private final GoliathSkillRuntime runtime;
    private final List<Projectile> projectiles = new ArrayList<>();
    private final List<Projectile> newlyLaunched = new ArrayList<>();
    private final List<Helper> helpers = new ArrayList<>();
    private final List<Helper> newHelpers = new ArrayList<>();
    private int particleBudget = MAX_PARTICLES_PER_TICK;

    GoliathSkillVisuals(SculkGoliathEntity owner, GoliathSkillRuntime runtime) {
        this.owner = owner;
        this.runtime = runtime;
    }

    public void clear() {
        projectiles.clear();
        newlyLaunched.clear();
        helpers.clear();
        newHelpers.clear();
    }

    public Vec3 modelPart(String part) {
        Vec3 heading = owner.getLookAngle().multiply(1, 0, 1).normalize();
        Vec3 sideways = new Vec3(-heading.z, 0, heading.x);
        String name = part.toLowerCase(Locale.ROOT);
        if (name.contains("heart") || name.contains("chest"))
            return owner.position().add(0, 3.1, 0);
        if (name.contains("leftarm"))
            return owner.position().add(sideways.scale(-1.4)).add(0, 2.8, 0);
        if (name.contains("rightarm"))
            return owner.position().add(sideways.scale(1.4)).add(0, 2.8, 0);
        if (name.contains("sonic"))
            return owner.position().add(heading.scale(1.8)).add(0, 3.0, 0);
        if (name.contains("groundsmash_targeter")) {
            int index = 0;
            for (char ch : name.toCharArray()) if (Character.isDigit(ch)) index = ch - '0';
            return owner.position().add(heading.scale(2.0 + index * 0.7))
                    .add(sideways.scale((index % 3 - 1) * 1.5)).add(0, 0.3, 0);
        }
        return owner.position().add(heading.scale(1.5)).add(0, 1.7, 0);
    }

    public void tick(ServerLevel level) {
        particleBudget = MAX_PARTICLES_PER_TICK;
        Iterator<Projectile> missile = projectiles.iterator();
        while (missile.hasNext()) {
            Projectile projectile = missile.next();
            projectile.age++;
            projectile.position = projectile.position.add(projectile.velocity);
            if (projectile.age % projectile.interval == 0) {
                emit(level, ParticleTypes.SCULK_SOUL, projectile.position, 4, 0.2, 0.015);
                callback(projectile.onTick, projectile.target, projectile.position);
            }
            if (projectile.target != null && projectile.target.isAlive() &&
                    projectile.target.getBoundingBox().inflate(0.7).contains(projectile.position)) {
                callback(projectile.onHit, projectile.target, projectile.position);
                missile.remove();
                continue;
            }
            BlockPos collisionPos = BlockPos.containing(projectile.position);
            if (!level.hasChunkAt(collisionPos)) {
                // Never load distant chunks just to update a visual projectile.
                missile.remove();
                continue;
            }
            if (level.getBlockState(collisionPos).isSolid()) {
                callback(projectile.onHitBlock, projectile.target, projectile.position);
                missile.remove();
                continue;
            }
            if (projectile.age >= projectile.maximumAge) missile.remove();
        }
        if (!newlyLaunched.isEmpty()) {
            for (Projectile p : newlyLaunched)
                if (projectiles.size() < MAX_PROJECTILES) projectiles.add(p);
            newlyLaunched.clear();
        }

        Iterator<Helper> fx = helpers.iterator();
        while (fx.hasNext()) {
            Helper helper = fx.next();
            helper.age++;
            if (helper.type.contains("GROUNDBREAK") || helper.type.contains("DIG")) {
                if (helper.age <= 5) {
                    emit(level, new BlockParticleOption(ParticleTypes.BLOCK,
                            Blocks.SCULK.defaultBlockState()), helper.position, 12, 0.8, 0.15);
                    damageRadius(helper.position, 2.3, 12f);
                }
                if (helper.age % 16 == 0)
                    emit(level, ParticleTypes.SCULK_SOUL, helper.position, 16, 1.0, 0.08);
            } else if (helper.type.contains("SOULSHOOTER")) {
                if (helper.age % 3 == 0)
                    emit(level, ParticleTypes.SCULK_SOUL, helper.position, 8, 0.5, 0.02);
                if (helper.age == 20 && owner.getTarget() != null) {
                    Vec3 direction = owner.getTarget().getEyePosition().subtract(helper.position).normalize();
                    newlyLaunched.add(new Projectile(helper.position, direction.scale(0.8),
                            owner.getTarget(), null, "SG_S_SHOOT_OH", "SG_S_SHOOT_OHB", 2, 80));
                }
            } else if (helper.type.contains("FALLINGROCK")) {
                if (helper.age == 20) {
                    emit(level, new BlockParticleOption(ParticleTypes.BLOCK,
                            Blocks.SCULK.defaultBlockState()), helper.position, 30, 1.4, 0.2);
                    damageRadius(helper.position, 3.0, 14f);
                }
            }
            if (helper.age >= 80) fx.remove();
        }
        if (!newHelpers.isEmpty()) {
            for (Helper helper : newHelpers)
                if (helpers.size() < MAX_EFFECTS) helpers.add(helper);
            newHelpers.clear();
        }
    }

    private void callback(String name, LivingEntity target, Vec3 pos) {
        if (name == null || name.isBlank()) return;
        runtime.cast(name, target, pos, 1);
    }

    private void damageRadius(Vec3 origin, double radius, float damage) {
        for (Player player : owner.level().getEntitiesOfClass(Player.class,
                new net.minecraft.world.phys.AABB(origin, origin).inflate(radius),
                p -> p.isAlive() && !p.isCreative() && !p.isSpectator())) {
            if (player.position().distanceTo(origin) <= radius)
                player.hurt(owner.damageSources().mobAttack(owner), damage);
        }
    }

    public void launch(ServerLevel level, GoliathSkillLibrary.Step step,
                       Vec3 origin, LivingEntity target) {
        if (projectiles.size() + newlyLaunched.size() >= MAX_PROJECTILES) return;
        LivingEntity aim = target != owner && target != null ? target : owner.getTarget();
        Vec3 direction = aim != null ? aim.getEyePosition().subtract(origin).normalize()
                : owner.getLookAngle().normalize();
        double speed = Math.clamp(step.decimal("v", 16.0) / 20.0, 0.15, 3.0);
        double maxDistance = Math.clamp(step.decimal("mr", 40), 4, 128);
        Vec3 start = origin.add(0, step.decimal("verticaloffset", 0) / 4.0, 0);
        String onHit = step.arg("oh", "");
        String onHitBlock = step.arg("ohb", "");
        String onTick = step.arg("ot", "");
        int lifetime = (int) Math.min(160, Math.ceil(maxDistance / speed));
        newlyLaunched.add(new Projectile(start, direction.scale(speed), aim,
                onTick, onHit, onHitBlock, step.integer("interval", 1), lifetime));
        emit(level, ParticleTypes.SCULK_SOUL, start, 14, 0.4, 0.03);
    }

    public void summon(ServerLevel level, GoliathSkillLibrary.Step step, Vec3 pos) {
        String type = step.arg("t", step.arg("type", ""));
        if (type.isBlank() || type.contains("PHASE2")) return;
        int count = Math.min(16, Math.max(1, step.integer("a", 1)));
        double radius = Math.min(24, step.decimal("noise", 0));
        for (int i = 0; i < count && helpers.size() + newHelpers.size() < MAX_EFFECTS; i++) {
            Vec3 location = pos;
            if (radius > 0) {
                location = location.add((owner.getRandom().nextDouble() - 0.5) * radius,
                        0, (owner.getRandom().nextDouble() - 0.5) * radius);
            }
            newHelpers.add(new Helper(location, type));
            // Render the two supplied ModelEngine helper models as native,
            // visual-only GeckoLib entities. Combat stays in the bounded
            // effect scheduler above, and each visual despawns after 80 ticks.
            if (type.contains("GROUNDBREAK") || type.contains("DIG")) {
                var effect = SculkGoliathBootstrap.SCULK_EFFECT.get().create(level);
                if (effect != null) {
                    effect.setGroundBreak(type.contains("GROUNDBREAK"));
                    effect.moveTo(location.x, location.y, location.z, owner.getYRot(), 0);
                    level.addFreshEntity(effect);
                }
            }
        }
        emit(level, ParticleTypes.SCULK_SOUL, pos, 12, 1.0, 0.04);
    }

    public void blockFracture(ServerLevel level, Vec3 pos, GoliathSkillLibrary.Step step) {
        // Explicitly non-destructive: original Mythic breakblock attempts are
        // converted to impact particles, never world.setBlock or destroyBlock.
        emit(level, new BlockParticleOption(ParticleTypes.BLOCK,
                Blocks.DEEPSLATE_BRICKS.defaultBlockState()), pos, 12, 1.2, 0.12);
    }

    public void glow(ServerLevel level, String part, int brightness) {
        if (brightness <= 0 || owner.tickCount % 3 != 0) return;
        emit(level, ParticleTypes.SCULK_SOUL, modelPart(part), Math.min(8, brightness / 3 + 1),
                0.3, 0.005);
    }

    public void impactPulse(ServerLevel level, Vec3 pos, double magnitude) {
        if (owner.tickCount % 3 == 0)
            emit(level, ParticleTypes.SCULK_SOUL, pos, (int) Math.min(4, Math.max(1, magnitude * 3)),
                    0.15, 0.01);
    }

    public void particles(ServerLevel level, String shape,
                          GoliathSkillLibrary.Step step, Vec3 center) {
        int count = Math.max(1, Math.min(128, step.integer("a", 8)));
        ParticleOptions particle = particle(step.arg("p", "sculk_soul"));
        double spread = Math.max(0, Math.min(12, step.decimal("spread",
                step.decimal("hs", 0.3))));
        double speed = Math.max(0, Math.min(1, step.decimal("s", step.decimal("speed", 0.03))));
        double radius = Math.min(16, step.decimal("r", 2));
        Vec3 origin = center.add(0, step.decimal("y", 0), 0);
        if (shape.equals("particle")) {
            emit(level, particle, origin, count, spread, speed);
            return;
        }
        int points = Math.max(4, Math.min(36, step.integer("points", 16)));
        int perPoint = Math.max(1, count / points);
        for (int i = 0; i < points; i++) {
            double angle = i * 2 * Math.PI / points;
            double y = shape.equals("particlesphere") ?
                    Math.sin(i * Math.PI / points) * radius : 0;
            Vec3 point = origin.add(radius * Math.cos(angle), y, radius * Math.sin(angle));
            emit(level, particle, point, perPoint, 0.1, speed);
        }
    }

    private ParticleOptions particle(String name) {
        String value = name.toLowerCase(Locale.ROOT);
        return switch (value) {
            case "sculk_soul" -> ParticleTypes.SCULK_SOUL;
            case "soulflame", "soulfire" -> ParticleTypes.SOUL_FIRE_FLAME;
            case "poof" -> ParticleTypes.POOF;
            case "lava" -> ParticleTypes.LAVA;
            case "squid_ink" -> ParticleTypes.SQUID_INK;
            case "red_dust", "effect" -> ParticleTypes.SCULK_SOUL;
            case "block_crack" -> new BlockParticleOption(ParticleTypes.BLOCK,
                    Blocks.SCULK.defaultBlockState());
            default -> {
                ResourceLocation id = ResourceLocation.tryParse(value.contains(":") ? value : "minecraft:" + value);
                var optional = id == null ? java.util.Optional.<net.minecraft.core.particles.ParticleType<?>>empty()
                        : BuiltInRegistries.PARTICLE_TYPE.getOptional(id);
                yield optional.isPresent() && optional.get() instanceof net.minecraft.core.particles.SimpleParticleType simple
                        ? simple : ParticleTypes.SCULK_SOUL;
            }
        };
    }

    private void emit(ServerLevel level, ParticleOptions type, Vec3 pos,
                      int requested, double spread, double speed) {
        int count = Math.min(particleBudget, Math.max(0, requested));
        if (count == 0) return;
        particleBudget -= count;
        level.sendParticles(type, pos.x, pos.y, pos.z, count,
                spread, spread, spread, speed);
    }
}

package nl.townsandkingdoms.core.boss;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.PriorityQueue;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

/**
 * Native, bounded execution of every HypactC Sculk Goliath skill definition.
 * All names, per-skill cooldowns, conditions, repeats and original delays are
 * read from converted data. Nothing in this class invokes Bukkit/MythicMobs.
 */
public final class GoliathSkillRuntime {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int MAX_TASKS = 6000;
    private static final int MAX_TASKS_PER_TICK = 320;
    private static final int MAX_RECURSION = 12;
    private static final int MAX_PLAYERS_PER_ACTION = 32;

    private record Context(LivingEntity trigger, Vec3 origin) {}
    private record Task(long due, long sequence, GoliathSkillLibrary.Step step,
                        Context context, int depth) {}
    public record Target(LivingEntity actor, Vec3 pos) {}

    private final SculkGoliathEntity owner;
    private final PriorityQueue<Task> tasks = new PriorityQueue<>(
            Comparator.comparingLong(Task::due).thenComparingLong(Task::sequence));
    private final Map<String, Long> cooldowns = new HashMap<>();
    private final GoliathSkillVisuals visuals;
    private GoliathSkillLibrary library;
    private boolean attemptedLoad;
    private boolean initialized;
    private boolean inDeathSequence;
    private boolean damageCancelled;
    private long nextSequence;
    private long gcdUntil;
    private float lockedYaw;
    private boolean rotationLocked;

    public GoliathSkillRuntime(SculkGoliathEntity owner) {
        this.owner = owner;
        this.visuals = new GoliathSkillVisuals(owner, this);
    }

    public boolean available(ServerLevel level) {
        if (!attemptedLoad) {
            attemptedLoad = true;
            library = GoliathSkillLibrary.load(level);
            if (library != null) LOGGER.info("TNK3 Sculk Goliath: loaded {}/115 native skill definitions",
                    library.definitionCount());
        }
        return library != null;
    }

    public void tick(ServerLevel level) {
        if (!available(level)) return;
        if (rotationLocked) owner.setYRot(lockedYaw);
        if (!initialized) {
            initialized = true;
            castEvent("onspawnorload");
            if (!owner.skillsSpawnPlayed()) {
                owner.markSkillsSpawnPlayed();
                castEvent("onspawn");
            }
            // Set the spawn/global cooldown before evaluating any onTimer:1
            // triggers, otherwise multiple boss attacks start on tick one.
            runDueTasks(level, MAX_TASKS_PER_TICK);
        }
        if (!inDeathSequence) {
            for (GoliathSkillLibrary.Trigger trigger : library.triggers(owner.isPhaseTwo())) {
                if (!"ontimer".equals(trigger.event()) || trigger.period() < 1
                        || owner.tickCount % trigger.period() != 0) continue;
                castTrigger(trigger, null);
            }
        }
        runDueTasks(level, MAX_TASKS_PER_TICK);
        visuals.tick(level);
    }

    public void reloaded(ServerLevel level) {
        if (!available(level)) return;
        castEvent("onspawnorload");
    }

    public void newPhase(ServerLevel level) {
        if (!available(level)) return;
        tasks.clear();
        cooldowns.clear();
        gcdUntil = level.getGameTime();
        rotationLocked = false;
        visuals.clear();
        inDeathSequence = false;
        initialized = true;
        castEvent("onspawnorload");
        castEvent("onspawn");
        runDueTasks(level, MAX_TASKS_PER_TICK);
    }

    public void died(ServerLevel level) {
        if (!available(level)) return;
        tasks.clear();
        visuals.clear();
        inDeathSequence = true;
        castEvent("ondeath");
        runDueTasks(level, MAX_TASKS_PER_TICK);
    }

    public boolean isBusy(ServerLevel level) {
        return inDeathSequence || level.getGameTime() < gcdUntil;
    }

    /** Return true if one of the original onDamaged skills cancelled the hit. */
    public boolean damaged(ServerLevel level, DamageSource source) {
        if (!available(level)) return false;
        damageCancelled = false;
        LivingEntity trigger = source.getEntity() instanceof LivingEntity living ? living : null;
        for (GoliathSkillLibrary.Trigger event : library.triggers(owner.isPhaseTwo())) {
            if (!"ondamaged".equals(event.event())) continue;
            castTrigger(event, trigger);
        }
        runDueTasks(level, MAX_TASKS_PER_TICK);
        return damageCancelled;
    }

    private void castEvent(String event) {
        for (GoliathSkillLibrary.Trigger trigger : library.triggers(owner.isPhaseTwo()))
            if (event.equals(trigger.event())) castTrigger(trigger, null);
    }

    private void castTrigger(GoliathSkillLibrary.Trigger trigger, LivingEntity source) {
        float phaseMaxHealth = owner.isPhaseTwo() ? 400.0f : 850.0f;
        if (trigger.healthBelow() <= 100 && owner.getHealth() >=
                phaseMaxHealth * trigger.healthBelow() / 100.0f) return;
        if (trigger.requiresMace() && !(source instanceof Player player
                && player.getMainHandItem().is(Items.MACE))) return;
        if (trigger.skills().isEmpty()) return;
        Context context = new Context(source != null ? source : owner.getTarget(), owner.position());
        if (trigger.random()) {
            String chosen = trigger.skills().get(owner.getRandom().nextInt(trigger.skills().size()));
            cast(chosen, context, 0);
        } else {
            for (String name : trigger.skills()) cast(name, context, 0);
        }
    }

    boolean cast(String name, LivingEntity target, Vec3 origin, int depth) {
        return cast(name, new Context(target, origin), depth);
    }

    private boolean cast(String name, Context context, int depth) {
        if (library == null || name == null || depth > MAX_RECURSION) return false;
        GoliathSkillLibrary.Definition definition = library.definition(name);
        if (definition == null) return false;
        ServerLevel level = (ServerLevel) owner.level();
        long time = level.getGameTime();
        if (cooldowns.getOrDefault(name, 0L) > time || !checkConditions(definition.conditions(), time))
            return false;
        cooldowns.put(name, time + definition.cooldownTicks());
        long relativeDelay = 0;
        for (GoliathSkillLibrary.Step step : definition.steps()) {
            if (step.operation().equals("delay")) {
                relativeDelay += step.delayTicks();
                continue;
            }
            for (int repeated = 0; repeated <= step.repeats(); repeated++) {
                long due = time + relativeDelay + step.delayTicks() +
                        (long) repeated * step.repeatInterval();
                enqueue(new Task(due, nextSequence++, step, context, depth + 1));
            }
        }
        return true;
    }

    private boolean checkConditions(List<String> conditions, long tick) {
        for (String condition : conditions) {
            String text = condition.trim();
            String lower = text.toLowerCase(Locale.ROOT);
            boolean expected = !lower.endsWith(" false");
            int brace = lower.indexOf('{');
            String key = (brace >= 0 ? lower.substring(0, brace) : lower.split(" ")[0]);
            GoliathSkillLibrary.Step parsed = GoliathSkillLibrary.parseStep(text);
            double distance = parsed.decimal("d", 0);
            boolean actual;
            switch (key) {
                case "offgcd" -> actual = tick >= gcdUntil;
                case "skilloncooldown" -> actual = cooldowns.getOrDefault(
                        parsed.arg("s", parsed.arg("skill", "")), 0L) > tick;
                case "playerwithin" -> actual = !nearbyPlayers(distance).isEmpty();
                case "playernotwithin" -> actual = nearbyPlayers(distance).isEmpty();
                case "ismoving" -> actual = owner.getDeltaMovement().horizontalDistanceSqr() > 0.002;
                case "blocktypeinradius" -> actual = hasMatchingTerrain(
                        parsed.decimal("r", 2.0), parsed.arg("types", ""));
                default -> {
                    // Don't accidentally enable unknown damaging skills.
                    LOGGER.debug("Unknown Goliath condition: {}", text);
                    return false;
                }
            }
            if (actual != expected) return false;
        }
        return true;
    }

    private boolean hasMatchingTerrain(double radius, String names) {
        if (names.isBlank()) return false;
        List<String> blocks = List.of(names.toUpperCase(Locale.ROOT).split(","));
        BlockPos center = owner.blockPosition();
        int r = Math.min(4, (int) Math.ceil(radius));
        for (int x = -r; x <= r; x++) for (int z = -r; z <= r; z++) {
            BlockPos p = center.offset(x, 0, z);
            String id = BuiltInRegistries.BLOCK.getKey(owner.level().getBlockState(p).getBlock())
                    .getPath().toUpperCase(Locale.ROOT);
            if (blocks.contains(id)) return true;
        }
        return false;
    }

    private void enqueue(Task task) {
        if (tasks.size() < MAX_TASKS) tasks.add(task);
    }

    private void runDueTasks(ServerLevel level, int cap) {
        int executions = 0;
        while (!tasks.isEmpty() && tasks.peek().due() <= level.getGameTime() && executions++ < cap) {
            Task task = tasks.poll();
            execute(level, task.step(), task.context(), task.depth());
        }
    }

    private List<Player> nearbyPlayers(double radius) {
        if (radius <= 0) return List.of();
        List<Player> found = owner.level().getEntitiesOfClass(Player.class,
                owner.getBoundingBox().inflate(radius), player -> player.isAlive() &&
                !player.isCreative() && !player.isSpectator() && owner.distanceTo(player) <= radius);
        return found.size() > MAX_PLAYERS_PER_ACTION ? found.subList(0, MAX_PLAYERS_PER_ACTION) : found;
    }

    private List<Target> select(GoliathSkillLibrary.Step step, Context context) {
        List<Target> targets = new ArrayList<>();
        String selector = step.target();
        switch (selector) {
            case "pir" -> {
                double radius = decimal(step.targetArgs().get("r"),
                        decimal(step.targetArgs().get("radius"), 8.0));
                for (Player player : nearbyPlayers(Math.min(96.0, radius)))
                    targets.add(new Target(player, player.position()));
            }
            case "target" -> {
                LivingEntity target = owner.getTarget();
                if (target != null) targets.add(new Target(target, target.position()));
            }
            case "trigger" -> {
                if (context.trigger() != null)
                    targets.add(new Target(context.trigger(), context.trigger().position()));
            }
            case "parent" -> targets.add(new Target(owner, owner.position()));
            case "origin" -> targets.add(new Target(null, context.origin()));
            case "modelpart" -> targets.add(new Target(null,
                    visuals.modelPart(step.targetArgs().getOrDefault("p", ""))));
            case "forward" -> {
                double forward = decimal(step.targetArgs().get("f"), 0);
                double vertical = decimal(step.targetArgs().get("yoffset"), 0);
                Vec3 direction = owner.getLookAngle().multiply(1, 0, 1).normalize();
                targets.add(new Target(null, owner.position().add(direction.scale(forward)).add(0, vertical, 0)));
            }
            case "blocksinradius" -> targets.add(new Target(null, owner.position()));
            default -> targets.add(new Target(owner, owner.position()));
        }
        return targets;
    }

    private void execute(ServerLevel level, GoliathSkillLibrary.Step step, Context context, int depth) {
        List<Target> targets = select(step, context);
        String kind = step.operation();
        if (kind.equals("setgcd")) {
            gcdUntil = Math.max(gcdUntil, level.getGameTime() +
                    step.integer("ticks", step.integer("t", 60)));
            return;
        }
        if (kind.equals("cancelevent")) {
            damageCancelled = true;
            return;
        }
        if (kind.equals("lockrotation")) {
            rotationLocked = step.bool("l", false);
            lockedYaw = owner.getYRot();
            return;
        }
        if (kind.equals("state")) {
            owner.playSkillAnimation(step.arg("s", "idle").toLowerCase(Locale.ROOT));
            return;
        }
        if (kind.equals("setspeed")) {
            var movement = owner.getAttribute(Attributes.MOVEMENT_SPEED);
            if (movement != null)
                movement.setBaseValue(0.2 * Math.clamp(step.decimal("s", 1), 0.0, 4.0));
            return;
        }
        if (kind.equals("setai")) {
            owner.setNoAi(!step.bool("ai", true));
            return;
        }
        if (kind.equals("brightness")) {
            visuals.glow(level, step.arg("p", "heartlight"), step.integer("b", 0));
            return;
        }
        if (kind.equals("skill")) {
            String name = step.arg("s", step.arg("skill", ""));
            if (targets.isEmpty()) cast(name, context, depth);
            for (Target target : targets)
                cast(name, new Context(target.actor() != null ? target.actor() : context.trigger(),
                        target.pos()), depth);
            return;
        }
        if (kind.equals("dismountallmodel")) {
            owner.ejectPassengers();
            return;
        }
        for (Target target : targets) {
            Vec3 pos = target.pos() != null ? target.pos() : owner.position();
            LivingEntity actor = target.actor();
            switch (kind) {
                case "sound" -> {
                    String raw = step.arg("s", step.arg("sound", ""));
                    ResourceLocation id = ResourceLocation.tryParse(raw.contains(":") ? raw : "minecraft:" + raw);
                    if (id == null) break;
                    BuiltInRegistries.SOUND_EVENT.getOptional(id).ifPresent(sound ->
                            level.playSound(null, pos.x, pos.y, pos.z, sound, SoundSource.HOSTILE,
                                    (float) Math.clamp(step.decimal("v", step.decimal("volume", 1)), 0, 5),
                                    (float) Math.clamp(step.decimal("p", step.decimal("pitch", 1)), 0.2, 2)));
                }
                case "particle", "particlering", "particlesphere" -> visuals.particles(level, kind, step, pos);
                case "recoil" -> visuals.impactPulse(level, pos, step.decimal("r", step.decimal("recoil", 0.5)));
                case "damage" -> {
                    if (actor != null && actor != owner && actor.isAlive()) {
                        if (step.bool("is", false)) actor.invulnerableTime = 0;
                        actor.hurt(owner.damageSources().mobAttack(owner),
                                (float) Math.clamp(step.decimal("a", 1), 0.0, 100.0));
                    }
                }
                case "throw", "pull" -> {
                    if (actor != null && actor != owner) {
                        boolean pull = kind.equals("pull") || step.bool("to", false);
                        Vec3 vector = (pull ? owner.position().subtract(actor.position()) :
                                actor.position().subtract(owner.position())).multiply(1, 0, 1).normalize();
                        double velocity = Math.min(3.0, step.decimal("v", 1) * 0.15);
                        double vertical = Math.min(1.25, step.decimal("vy", 0) * 0.2);
                        actor.push(vector.x * velocity, vertical, vector.z * velocity);
                        actor.hurtMarked = true;
                    }
                }
                case "potion" -> {
                    if (actor == null) break;
                    String effectName = step.arg("effect", step.arg("type", "")).toUpperCase(Locale.ROOT);
                    var effect = switch (effectName) {
                        case "DARKNESS", "BLINDNESS" -> MobEffects.DARKNESS;
                        case "SLOWNESS", "SLOW" -> MobEffects.MOVEMENT_SLOWDOWN;
                        case "WEAKNESS" -> MobEffects.WEAKNESS;
                        case "LEVITATION" -> MobEffects.LEVITATION;
                        default -> null;
                    };
                    if (effect != null) actor.addEffect(new MobEffectInstance(effect,
                            step.integer("duration", step.integer("d", 60)),
                            Math.max(0, step.integer("level", 1) - 1),
                            false, step.bool("ambientparticles", false), step.bool("hasicon", false)));
                }
                case "heal" -> {
                    if (actor != null) actor.heal((float) step.decimal("a", 1));
                }
                case "setnodamageticks" -> {
                    if (actor != null) actor.invulnerableTime = step.integer("ticks", 20);
                }
                case "mountmodel" -> {
                    if (actor instanceof Player player && actor != owner) player.startRiding(owner, true);
                }
                case "summon" -> visuals.summon(level, step, pos);
                case "projectile", "missile" -> visuals.launch(level, step, pos, actor);
                case "breakblock" -> visuals.blockFracture(level, pos, step);
                case "remove" -> {
                    // Helper despawns are handled by bounded transient native effects.
                    // The boss itself is NEVER discarded by a nested helper script.
                }
                default -> LOGGER.debug("Unsupported Sculk Goliath mechanic '{}'", kind);
            }
        }
    }

    private static double decimal(String value, double fallback) {
        if (value == null) return fallback;
        try { return Double.parseDouble(value); }
        catch (NumberFormatException e) { return fallback; }
    }
}

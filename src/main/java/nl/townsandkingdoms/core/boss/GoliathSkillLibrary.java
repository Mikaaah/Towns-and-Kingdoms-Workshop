package nl.townsandkingdoms.core.boss;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;

/** Exact MythicMobs definition table, with plugin syntax parsed into safe data. */
public final class GoliathSkillLibrary {
    private static final Logger LOGGER = LogUtils.getLogger();
    public record Definition(int cooldownTicks, List<String> conditions, List<Step> steps) {}
    public record Trigger(String event, int period, List<String> skills, boolean random,
                          int healthBelow, boolean requiresMace) {}
    public record Step(String operation, Map<String, String> args, String target,
                       Map<String, String> targetArgs, int delayTicks, int repeats, int repeatInterval) {
        public String arg(String key, String fallback) {
            return args.getOrDefault(key, fallback);
        }

        public int integer(String key, int fallback) {
            try { return (int) Double.parseDouble(arg(key, "" + fallback)); }
            catch (NumberFormatException e) { return fallback; }
        }

        public double decimal(String key, double fallback) {
            try { return Double.parseDouble(arg(key, "" + fallback)); }
            catch (NumberFormatException e) { return fallback; }
        }

        public boolean bool(String key, boolean fallback) {
            return Boolean.parseBoolean(arg(key, "" + fallback));
        }
    }

    private final Map<String, Definition> definitions;
    private final List<Trigger> phaseOne;
    private final List<Trigger> phaseTwo;

    private GoliathSkillLibrary(Map<String, Definition> definitions,
                                List<Trigger> phaseOne, List<Trigger> phaseTwo) {
        this.definitions = Map.copyOf(definitions);
        this.phaseOne = List.copyOf(phaseOne);
        this.phaseTwo = List.copyOf(phaseTwo);
    }

    public Definition definition(String id) { return definitions.get(id); }
    public List<Trigger> triggers(boolean secondPhase) { return secondPhase ? phaseTwo : phaseOne; }
    public int definitionCount() { return definitions.size(); }

    public static GoliathSkillLibrary load(ServerLevel level) {
        ResourceLocation location = ResourceLocation.fromNamespaceAndPath(
                "tnkcore", "sculk_goliath/skills.json");
        var resource = level.getServer().getResourceManager().getResource(location);
        if (resource.isEmpty()) return null;
        try (var input = resource.get().open();
             var reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            Map<String, Definition> definitions = new LinkedHashMap<>();
            for (var entry : root.getAsJsonObject("definitions").entrySet()) {
                JsonObject raw = entry.getValue().getAsJsonObject();
                List<String> conditions = new ArrayList<>();
                for (JsonElement condition : raw.getAsJsonArray("conditions"))
                    conditions.add(condition.getAsString());
                List<Step> actions = new ArrayList<>();
                for (JsonElement step : raw.getAsJsonArray("steps"))
                    actions.add(parseStep(step.getAsString()));
                int cooldown = (int) Math.ceil(raw.get("cooldown").getAsDouble() * 20.0);
                definitions.put(entry.getKey(), new Definition(cooldown,
                        List.copyOf(conditions), List.copyOf(actions)));
            }
            if (definitions.size() != 115) throw new IllegalArgumentException(
                    "Sculk Goliath expected 115 skills, got " + definitions.size());
            JsonObject triggers = root.getAsJsonObject("triggers");
            return new GoliathSkillLibrary(definitions,
                    parseTriggers(triggers.getAsJsonArray("phase1")),
                    parseTriggers(triggers.getAsJsonArray("phase2")));
        } catch (Exception error) {
            LOGGER.error(
                    "Unable to load full Sculk Goliath skills; using prototype combat", error);
            return null;
        }
    }

    private static List<Trigger> parseTriggers(JsonArray array) {
        List<Trigger> triggers = new ArrayList<>();
        for (JsonElement element : array) {
            JsonObject object = element.getAsJsonObject();
            List<String> names = new ArrayList<>();
            for (JsonElement name : object.getAsJsonArray("skills")) names.add(name.getAsString());
            triggers.add(new Trigger(object.get("event").getAsString(),
                    object.get("period").getAsInt(), List.copyOf(names),
                    "random".equals(object.get("type").getAsString()),
                    object.get("health_below").getAsInt(),
                    object.get("requires_mace").getAsBoolean()));
        }
        return triggers;
    }

    /** Parse one MythicMobs action, preserving all its parameters and target selection. */
    public static Step parseStep(String source) {
        String text = source.trim();
        if (text.toLowerCase(Locale.ROOT).startsWith("delay ")) {
            int duration;
            try { duration = Integer.parseInt(text.substring(6).trim()); }
            catch (NumberFormatException e) { duration = 0; }
            return new Step("delay", Map.of(), "self", Map.of(),
                    Math.max(0, duration), 0, 1);
        }
        int brace = text.indexOf('{');
        int space = text.indexOf(' ');
        int opEnd = brace >= 0 ? brace : (space >= 0 ? space : text.length());
        String name = text.substring(0, opEnd).toLowerCase(Locale.ROOT);
        Map<String, String> args = brace >= 0 ? params(text, brace) : Map.of();
        int targetAt = text.indexOf('@');
        String target = "self";
        Map<String, String> targetArgs = Map.of();
        if (targetAt >= 0) {
            int end = targetAt + 1;
            while (end < text.length() && Character.isLetter(text.charAt(end))) end++;
            target = text.substring(targetAt + 1, end).toLowerCase(Locale.ROOT);
            if (end < text.length() && text.charAt(end) == '{')
                targetArgs = params(text, end);
        }
        int delay = parseInt(args.get("delay"), 0);
        int repeats = Math.min(150, Math.max(0, parseInt(args.get("repeat"), 0)));
        int period = Math.max(1, parseInt(args.getOrDefault("repeati",
                args.get("repeatinterval")), 1));
        return new Step(name, Map.copyOf(args), target, Map.copyOf(targetArgs),
                Math.max(0, delay), repeats, period);
    }

    private static Map<String, String> params(String text, int openingBrace) {
        int closing = text.indexOf('}', openingBrace + 1);
        if (closing < 0) return Map.of();
        Map<String, String> map = new HashMap<>();
        for (String section : text.substring(openingBrace + 1, closing).split(";")) {
            int equals = section.indexOf('=');
            if (equals < 0) continue;
            map.put(section.substring(0, equals).trim().toLowerCase(Locale.ROOT),
                    section.substring(equals + 1).trim());
        }
        return map;
    }

    private static int parseInt(String value, int fallback) {
        if (value == null) return fallback;
        try { return (int) Double.parseDouble(value); }
        catch (NumberFormatException e) { return fallback; }
    }
}

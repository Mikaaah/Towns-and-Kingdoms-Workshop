package nl.townsandkingdoms.core.projector;

import java.net.URI;
import java.util.*;
import net.minecraft.nbt.CompoundTag;

/** Small, bounded, immutable payload shared by the block, editor and network. */
public record ProjectorSettings(Mode mode, List<Long> quests, String text, String imageUrl,
                                float width, float elevation, boolean billboard, boolean background, ProjectorPalette palette, boolean bobbing) {
    public enum Mode { QUESTS, IMAGE, TEXT }
    public static final int MAX_QUESTS = 5, MAX_TEXT = 4096, MAX_URL = 2048;
    public static final ProjectorSettings DEFAULT = new ProjectorSettings(Mode.QUESTS, List.of(), "&d&lTowns & Kingdoms\n&fYour next adventure starts here.", "", 3, 1.25F, false, true);
    /** Preserve source compatibility with existing projector integrations. */
    public ProjectorSettings(Mode mode, List<Long> quests, String text, String imageUrl,
                             float width, float elevation, boolean billboard, boolean background) {
        this(mode, quests, text, imageUrl, width, elevation, billboard, background, ProjectorPalette.DEFAULT, true);
    }
    public ProjectorSettings {
        Objects.requireNonNull(mode);
        Objects.requireNonNull(palette);
        quests = List.copyOf(new LinkedHashSet<>(quests));
        if (quests.size() > MAX_QUESTS || quests.contains(0L)) throw new IllegalArgumentException("Select up to five quests.");
        if (text.length() > MAX_TEXT || imageUrl.length() > MAX_URL) throw new IllegalArgumentException("Content is too long.");
        if (!Float.isFinite(width) || !Float.isFinite(elevation) || width < 1 || width > 8 || elevation < .5F || elevation > 6)
            throw new IllegalArgumentException("Projection size is out of range.");
        if (!imageUrl.isEmpty() && !validUrl(imageUrl)) throw new IllegalArgumentException("Use a direct HTTP or HTTPS image link.");
    }
    public static boolean validUrl(String value) {
        try {
            URI u = URI.create(value);
            return ("https".equalsIgnoreCase(u.getScheme()) || "http".equalsIgnoreCase(u.getScheme()))
                    && u.getHost() != null && u.getRawUserInfo() == null && u.getFragment() == null
                    && (u.getPort() == -1 || u.getPort() == 80 || u.getPort() == 443);
        } catch (IllegalArgumentException e) { return false; }
    }
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("mode", mode.name()); tag.putLongArray("quests", quests);
        tag.putString("text", text); tag.putString("image", imageUrl);
        tag.putFloat("width", width); tag.putFloat("elevation", elevation);
        tag.putBoolean("billboard", billboard); tag.putBoolean("background", background);
        tag.put("palette", palette.save()); tag.putBoolean("bobbing", bobbing);
        return tag;
    }
    public static ProjectorSettings read(CompoundTag tag) {
        try {
            return new ProjectorSettings(Mode.valueOf(tag.getString("mode")),
                    Arrays.stream(tag.getLongArray("quests")).boxed().toList(), tag.getString("text"), tag.getString("image"),
                    tag.getFloat("width"), tag.getFloat("elevation"), tag.getBoolean("billboard"), tag.getBoolean("background"),
                    ProjectorPalette.read(tag.getCompound("palette")), !tag.contains("bobbing") || tag.getBoolean("bobbing"));
        } catch (IllegalArgumentException | NullPointerException e) { return DEFAULT; }
    }
}

package nl.townsandkingdoms.core.projector;

import java.util.Locale;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

/** RGB only: opacity belongs to the renderer, not to a user-entered hex color. */
public record ProjectorPalette(int body, int accent, int text, int completed, int backdrop) {
    public static final ProjectorPalette DEFAULT = new ProjectorPalette(0x9D5BFF, 0xBB88FF, 0xE8E0F0, 0x91E7A9, 0x170F29);
    public ProjectorPalette {
        for (int color : new int[]{body, accent, text, completed, backdrop})
            if (color < 0 || color > 0xFFFFFF) throw new IllegalArgumentException("Use a six-digit hex color, such as #9D5BFF.");
    }
    public static int parseHex(String value) {
        String hex = value.strip();
        if (hex.startsWith("#")) hex = hex.substring(1);
        if (!hex.matches("[0-9a-fA-F]{6}")) throw new IllegalArgumentException("Use a six-digit hex color, such as #9D5BFF.");
        return Integer.parseInt(hex, 16);
    }
    public static String hex(int rgb) { return String.format(Locale.ROOT, "#%06X", rgb); }
    public static int opaque(int rgb) { return 0xFF000000 | rgb; }
    public static int muted(int rgb) {
        return ((rgb >> 16 & 255) * 3 / 4 << 16) | ((rgb >> 8 & 255) * 3 / 4 << 8) | (rgb & 255) * 3 / 4;
    }
    public CompoundTag save() {
        var tag = new CompoundTag();
        tag.putInt("body", body); tag.putInt("accent", accent); tag.putInt("text", text);
        tag.putInt("completed", completed); tag.putInt("backdrop", backdrop);
        return tag;
    }
    public static ProjectorPalette read(CompoundTag tag) {
        return new ProjectorPalette(readColor(tag,"body",DEFAULT.body), readColor(tag,"accent",DEFAULT.accent),
                readColor(tag,"text",DEFAULT.text), readColor(tag,"completed",DEFAULT.completed), readColor(tag,"backdrop",DEFAULT.backdrop));
    }
    private static int readColor(CompoundTag tag, String key, int fallback) {
        if (!tag.contains(key, Tag.TAG_ANY_NUMERIC)) return fallback;
        int value = tag.getInt(key);
        return value >= 0 && value <= 0xFFFFFF ? value : fallback;
    }
}

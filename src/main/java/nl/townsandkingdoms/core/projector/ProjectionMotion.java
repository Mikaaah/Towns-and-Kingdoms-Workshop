package nl.townsandkingdoms.core.projector;

/** One transform for the entire hologram; never modifies the saved position. */
public final class ProjectionMotion {
    public static final double AMPLITUDE = 0.035;
    public static final double PERIOD_TICKS = 140;
    private ProjectionMotion() {}
    public static double offset(double ticks, boolean enabled) {
        return enabled ? Math.sin(ticks * (Math.PI * 2 / PERIOD_TICKS)) * AMPLITUDE : 0;
    }
}

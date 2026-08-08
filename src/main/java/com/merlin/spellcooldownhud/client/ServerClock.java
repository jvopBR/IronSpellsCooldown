package com.merlin.spellcooldownhud.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;

/**
 * Server clock seen from the client: authoritative game time and estimated TPS.
 *
 * <p>No mixin needed: the server sends {@code ClientboundSetTimePacket} every 20 ticks and the
 * client hard-sets the value, so over a few seconds game time advances at the server's real tick
 * rate. Game time is global and monotonic (1 per server tick) -- the same pace Iron's Spells
 * decrements cooldowns, which is what lets us anchor a cooldown's end to it.
 */
public final class ServerClock {

    /** 5 seconds of samples at 20 client ticks per second. */
    private static final int SAMPLE_CAPACITY = 100;

    /**
     * Minimum window before the measurement is trustworthy. Server corrections arrive only every
     * 20 ticks, so shorter windows just measure the client's local 20/s count and read 20 TPS.
     */
    private static final long MIN_WINDOW_NANOS = 2_000_000_000L;

    /** Backward jump (new world) or absurd forward jump (frozen client): restart. */
    private static final long JUMP_RESET_TICKS = 200L;

    private static final float NOMINAL_TPS = 20.0f;

    private static final long[] sampleGameTime = new long[SAMPLE_CAPACITY];
    private static final long[] sampleNanos = new long[SAMPLE_CAPACITY];

    private static int head;
    private static int size;
    private static long lastGameTime = Long.MIN_VALUE;

    private static float smoothedTps = NOMINAL_TPS;
    private static boolean reliable;

    private ServerClock() {
    }

    /** Current server game time, or 0 outside a world. */
    public static long gameTime() {
        ClientLevel level = Minecraft.getInstance().level;
        return level == null ? 0L : level.getGameTime();
    }

    /** Estimated server TPS, 0..20. Reads {@value #NOMINAL_TPS} until measured. */
    public static float tps() {
        return smoothedTps;
    }

    /** True once there has been enough window for the measurement to mean something. */
    public static boolean isReliable() {
        return reliable;
    }

    public static void reset() {
        head = 0;
        size = 0;
        lastGameTime = Long.MIN_VALUE;
        smoothedTps = NOMINAL_TPS;
        reliable = false;
    }

    /** Called once per client tick. */
    public static void tick() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            reset();
            return;
        }

        long gameTime = level.getGameTime();
        if (lastGameTime != Long.MIN_VALUE
                && (gameTime < lastGameTime || gameTime - lastGameTime > JUMP_RESET_TICKS)) {
            reset();
        }
        lastGameTime = gameTime;

        sampleGameTime[head] = gameTime;
        sampleNanos[head] = System.nanoTime();
        head = (head + 1) % SAMPLE_CAPACITY;
        if (size < SAMPLE_CAPACITY) {
            size++;
        }

        recomputeTps();
    }

    private static void recomputeTps() {
        if (size < 2) {
            return;
        }
        int oldest = Math.floorMod(head - size, SAMPLE_CAPACITY);
        int newest = Math.floorMod(head - 1, SAMPLE_CAPACITY);

        long elapsedNanos = sampleNanos[newest] - sampleNanos[oldest];
        if (elapsedNanos < MIN_WINDOW_NANOS) {
            return;
        }

        long elapsedTicks = sampleGameTime[newest] - sampleGameTime[oldest];
        float measured = (float) (elapsedTicks * 1_000_000_000.0 / elapsedNanos);
        measured = Mth.clamp(measured, 0.0f, NOMINAL_TPS);

        // Exponential average: the signal is jagged between server corrections, and a number
        // jumping on screen is worse than one that lags slightly behind.
        smoothedTps = reliable ? smoothedTps * 0.9f + measured * 0.1f : measured;
        reliable = true;
    }
}

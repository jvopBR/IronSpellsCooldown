package com.merlin.spellcooldownhud.data;

import com.merlin.spellcooldownhud.client.ServerClock;
import com.merlin.spellcooldownhud.config.ContentMode;
import com.merlin.spellcooldownhud.config.HudConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Corrects the client's cooldown against the server clock.
 *
 * <h2>The problem</h2>
 * Iron's Spells syncs a cooldown once, when it starts ({@code SyncCooldownPacket}), then the client
 * decrements it on its own in {@code ClientPlayerEvents.onPlayerTick} -- 20 times a second, fixed.
 * There is no periodic resync: {@code PlayerCooldowns.syncToPlayer} only runs on login and respawn.
 *
 * <p>Since the client always runs at 20 ticks/s but the server runs at its real TPS, on a lagging
 * server the client's count runs faster than the server's. The number hits zero while the spell is
 * still on cooldown -- and the HUD starts lying exactly when it matters most.
 *
 * <h2>The fix</h2>
 * The first time we see a cooldown, we record the <em>game time</em> at which it ends:
 * {@code end = gameTime + ticksRemaining}. From then on the remaining time is always
 * {@code end - gameTime}.
 *
 * <p>This is exact, not an estimate, because game time advances by 1 per server tick -- the same
 * pace as the cooldown decrement -- and the server corrects it on the client every 20 ticks (see
 * {@link ServerClock}). The measured TPS doesn't enter this calculation; it only converts the
 * remaining ticks into seconds for display.
 *
 * <h2>Authoritative clearing (e.g. /clearCooldowns)</h2>
 * The Iron's Spells {@code /clearCooldowns} command clears cooldowns on the server and syncs to the
 * client, so the spell vanishes from the map even with "natural" time still left. That's
 * indistinguishable from "the client counted too fast" by the map alone -- but not by the last
 * CLIENT-SIDE counter: the client only removes an entry by expiry when its own counter hits zero.
 * If the spell vanished with a lot of client-side time still left, it was a clear/resync, and the
 * entry must NOT be resurrected (see {@link #collectDroppedEarly}).
 */
public final class ServerSyncedSource implements CooldownSource {

    /**
     * Slack before treating a value as a recast. The client counts faster, so its remaining should
     * never exceed the prediction; when it does with slack, the spell was cast again.
     */
    private static final int RECAST_TOLERANCE_TICKS = 5;

    /**
     * How much the client-side counter may have left and still count as natural expiry. The client
     * decrements to ~0 before removing an entry, so the last value seen is always low; if the spell
     * vanished with much more than this left, it was an authoritative clear (/clearCooldowns) or a
     * server resync, not expiry -- and the spell is actually ready.
     */
    private static final int NATURAL_EXPIRY_TOLERANCE_TICKS = 3;

    private final CooldownSource delegate;
    private final Map<String, Anchor> anchors = new HashMap<>();

    public ServerSyncedSource(CooldownSource delegate) {
        this.delegate = delegate;
    }

    private static final class Anchor {
        private long endGameTime;
        /** Last seen version of the entry, to redraw it if the client drops it too early. */
        private CooldownEntry lastSeen;
    }

    @Override
    public List<CooldownEntry> collect(ContentMode mode) {
        List<CooldownEntry> raw = delegate.collect(mode);

        long gameTime = ServerClock.gameTime();
        if (!HudConfig.USE_SERVER_TIME.get() || gameTime <= 0L) {
            anchors.clear();
            return raw;
        }

        Set<String> seen = new HashSet<>(raw.size());
        List<CooldownEntry> out = new ArrayList<>(raw.size());

        for (CooldownEntry entry : raw) {
            seen.add(entry.spellId());
            out.add(reconcile(entry, gameTime));
        }

        collectDroppedEarly(mode, gameTime, seen, out);
        return out;
    }

    /** Anchors the entry (or re-anchors, if it was recast) and returns it with the corrected remaining. */
    private CooldownEntry reconcile(CooldownEntry entry, long gameTime) {
        Anchor anchor = anchors.get(entry.spellId());

        if (entry.remainingTicks() > 0) {
            long predicted = anchor == null ? Long.MIN_VALUE : anchor.endGameTime - gameTime;
            if (anchor == null || entry.remainingTicks() > predicted + RECAST_TOLERANCE_TICKS) {
                anchor = new Anchor();
                anchor.endGameTime = gameTime + entry.remainingTicks();
                anchors.put(entry.spellId(), anchor);
            }
        }

        if (anchor == null) {
            return entry;
        }

        anchor.lastSeen = entry;
        long remaining = anchor.endGameTime - gameTime;
        if (remaining <= 0L) {
            anchors.remove(entry.spellId());
            return entry.withRemaining(0);
        }
        return entry.withRemaining((int) remaining);
    }

    /**
     * Spells the client already dropped from the list by counting too fast, but that the server
     * still has on cooldown. Without this the HUD would show the spell as ready too early, which is
     * exactly the bug this decorator exists to fix.
     */
    private void collectDroppedEarly(ContentMode mode, long gameTime,
                                     Set<String> seen, List<CooldownEntry> out) {
        Iterator<Map.Entry<String, Anchor>> iterator = anchors.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<String, Anchor> mapping = iterator.next();
            if (seen.contains(mapping.getKey())) {
                continue;
            }

            Anchor anchor = mapping.getValue();
            long remaining = anchor.endGameTime - gameTime;
            if (remaining <= 0L || anchor.lastSeen == null) {
                iterator.remove();
                continue;
            }

            // If the entry vanished with the client-side counter still high, it was /clearCooldowns
            // or a server resync -- the spell is genuinely ready. Only natural expiry (client-side
            // counter near zero) deserves to be resurrected by server time.
            if (anchor.lastSeen.remainingTicks() > NATURAL_EXPIRY_TOLERANCE_TICKS) {
                iterator.remove();
                continue;
            }

            // Only in ONLY_ON_COOLDOWN: in ALL_EQUIPPED, absence means the spell was unequipped, and
            // bringing back a spell the player removed from the spellbook would be wrong.
            if (mode == ContentMode.ONLY_ON_COOLDOWN) {
                out.add(anchor.lastSeen.withRemaining((int) remaining));
            }
        }
    }
}

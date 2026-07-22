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
 * Corrige o cooldown do cliente pelo relogio do servidor.
 *
 * <h2>O problema</h2>
 * O Iron's Spells sincroniza o cooldown uma unica vez, quando ele comeca
 * ({@code SyncCooldownPacket}), e depois o cliente decrementa sozinho em
 * {@code ClientPlayerEvents.onPlayerTick} -- 20 vezes por segundo, fixo. Nao existe resync
 * periodico: {@code PlayerCooldowns.syncToPlayer} so e chamado em login e respawn.
 *
 * <p>Como o cliente sempre roda a 20 ticks/s mas o servidor roda a TPS reais, num servidor
 * lagado a contagem do cliente corre mais rapido que a do servidor. O numero na tela chega a
 * zero enquanto a magia ainda esta em cooldown de verdade -- e a HUD passa a mentir justamente
 * quando mais importa.
 *
 * <h2>A correcao</h2>
 * Ao ver um cooldown pela primeira vez, gravamos em que <em>tempo de jogo</em> ele termina:
 * {@code fim = gameTime + ticksRestantes}. Dali em diante o restante e sempre
 * {@code fim - gameTime}.
 *
 * <p>Isso e exato, e nao uma estimativa, porque o tempo de jogo avanca 1 por tick de servidor --
 * o mesmo compasso do decremento do cooldown -- e o servidor o corrige no cliente a cada 20
 * ticks (ver {@link ServerClock}). O TPS medido nao entra nesta conta; ele so serve para
 * converter os ticks restantes em segundos na hora de exibir.
 */
public final class ServerSyncedSource implements CooldownSource {

    /**
     * Folga antes de tratar um valor como recast. O cliente conta mais rapido, entao o restante
     * dele nunca deveria passar do previsto; quando passa com folga, a magia foi lancada de novo.
     */
    private static final int RECAST_TOLERANCE_TICKS = 5;

    private final CooldownSource delegate;
    private final Map<String, Anchor> anchors = new HashMap<>();

    public ServerSyncedSource(CooldownSource delegate) {
        this.delegate = delegate;
    }

    private static final class Anchor {
        private long endGameTime;
        /** Ultima versao vista da entrada, para redesenha-la se o cliente derrubar cedo demais. */
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

    /** Ancora a entrada (ou reancora, se foi relancada) e devolve com o restante corrigido. */
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
     * Magias que o cliente ja tirou da lista por ter contado rapido demais, mas que o servidor
     * ainda tem em cooldown. Sem isto a HUD mostraria a magia como pronta antes da hora, que e
     * exatamente o erro que este decorator existe para consertar.
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

            // So em ONLY_ON_COOLDOWN: em ALL_EQUIPPED, ausencia significa magia desequipada, e
            // trazer de volta uma magia que o player tirou do spellbook seria errado.
            if (mode == ContentMode.ONLY_ON_COOLDOWN) {
                out.add(anchor.lastSeen.withRemaining((int) remaining));
            }
        }
    }
}

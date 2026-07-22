package com.merlin.spellcooldownhud.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;

/**
 * Relogio do servidor visto do cliente: tempo de jogo autoritativo e TPS estimado.
 *
 * <p>Por que isto funciona sem mixin nenhum: o servidor envia {@code ClientboundSetTimePacket} a
 * cada 20 ticks de servidor e o cliente <em>crava</em> o valor recebido em
 * {@code ClientLevel.setGameTime}. Entre um pacote e outro o cliente avanca o contador sozinho a
 * 20/s, mas as correcoes periodicas puxam tudo de volta -- entao, medido numa janela de alguns
 * segundos, o tempo de jogo avanca exatamente na taxa real de ticks do servidor.
 *
 * <p>O tempo de jogo e global (dimensoes que nao a principal usam {@code DerivedLevelData}) e
 * monotonico, avancando 1 por tick de servidor. E o mesmo compasso em que o Iron's Spells
 * decrementa os cooldowns, o que e justamente o que permite ancorar o fim de um cooldown nele.
 */
public final class ServerClock {

    /** 5 segundos de amostras a 20 ticks de cliente por segundo. */
    private static final int SAMPLE_CAPACITY = 100;

    /**
     * Janela minima antes de confiar na medicao. A correcao do servidor so chega a cada 20 ticks,
     * entao janelas curtas medem a contagem local do cliente e dariam 20 TPS sempre.
     */
    private static final long MIN_WINDOW_NANOS = 2_000_000_000L;

    /** Salto para tras (mundo novo) ou para frente absurdo (cliente congelado): recomeca. */
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

    /** Tempo de jogo atual do servidor, ou 0 fora de um mundo. */
    public static long gameTime() {
        ClientLevel level = Minecraft.getInstance().level;
        return level == null ? 0L : level.getGameTime();
    }

    /** TPS estimado do servidor, entre 0 e 20. Vale {@value #NOMINAL_TPS} ate haver medicao. */
    public static float tps() {
        return smoothedTps;
    }

    /** True quando ja houve janela suficiente para a medicao valer alguma coisa. */
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

    /** Chamado uma vez por tick de cliente. */
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

        // Media exponencial: o sinal e serrilhado entre uma correcao do servidor e a proxima, e
        // um numero pulando na tela seria pior do que um numero levemente atrasado.
        smoothedTps = reliable ? smoothedTps * 0.9f + measured * 0.1f : measured;
        reliable = true;
    }
}

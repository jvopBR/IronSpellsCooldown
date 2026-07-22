package com.merlin.spellcooldownhud.data;

import com.merlin.spellcooldownhud.config.ContentMode;

import java.util.List;

/**
 * De onde a HUD tira as magias a mostrar.
 *
 * <p>Existem duas implementacoes: {@link IronSpellsSource}, que le o jogo de verdade, e
 * {@link DemoSource}, que produz cooldowns falsos para o preview do editor. Isolar isso atras de
 * uma interface e o que permite ajustar a HUD sem estar em combate -- e, no futuro, suportar
 * outro mod de magia sem mexer em renderer nenhum.
 */
public interface CooldownSource {

    /** Entradas do frame atual, sem ordenacao nem corte -- disso cuida o tracker. */
    List<CooldownEntry> collect(ContentMode mode);
}

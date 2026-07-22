package com.merlin.spellcooldownhud.config;

/**
 * Ponto da tela a partir do qual a HUD e posicionada.
 *
 * <p>Guardamos ancora + offset em vez de coordenadas absolutas para que a HUD continue no lugar
 * certo quando a resolucao da janela ou a escala de GUI muda: uma HUD ancorada em
 * {@link #BOTTOM_RIGHT} fica colada no canto inferior direito em qualquer tela, enquanto uma
 * coordenada absoluta escaparia da tela ao diminuir a janela.
 */
public enum Anchor {
    TOP_LEFT(0.0f, 0.0f),
    TOP_CENTER(0.5f, 0.0f),
    TOP_RIGHT(1.0f, 0.0f),
    MIDDLE_LEFT(0.0f, 0.5f),
    CENTER(0.5f, 0.5f),
    MIDDLE_RIGHT(1.0f, 0.5f),
    BOTTOM_LEFT(0.0f, 1.0f),
    BOTTOM_CENTER(0.5f, 1.0f),
    BOTTOM_RIGHT(1.0f, 1.0f);

    private final float xFraction;
    private final float yFraction;

    Anchor(float xFraction, float yFraction) {
        this.xFraction = xFraction;
        this.yFraction = yFraction;
    }

    public int anchorX(int screenWidth) {
        return Math.round(screenWidth * xFraction);
    }

    public int anchorY(int screenHeight) {
        return Math.round(screenHeight * yFraction);
    }

    /**
     * Deslocamento a aplicar no bloco para que a ancora se comporte como se espera: ancorado a
     * direita o bloco cresce para a esquerda, e centralizado ele fica centralizado de fato.
     */
    public int alignX(int blockWidth) {
        return -Math.round(blockWidth * xFraction);
    }

    public int alignY(int blockHeight) {
        return -Math.round(blockHeight * yFraction);
    }

    /**
     * Ancora correspondente a regiao da tela onde o ponto (x, y) caiu, dividindo a tela em nove
     * tercos. Usada pelo editor ao arrastar.
     *
     * <p>Regiao, e nao ancora euclidianamente mais proxima: uma HUD 60px acima da hotbar tem o
     * centro mais perto do MEIO da tela do que da borda inferior, entao a versao por distancia
     * re-ancorava para o centro ao menor arrasto -- e ai a HUD saia do lugar em outra resolucao.
     * Por tercos, "esta no terco de baixo" resulta em ancora de baixo, que e o esperado.
     */
    public static Anchor nearest(int x, int y, int screenWidth, int screenHeight) {
        int column = third(x, screenWidth);
        int row = third(y, screenHeight);
        // A ordem das constantes e linha a linha, da esquerda para a direita.
        return values()[row * 3 + column];
    }

    /** 0, 1 ou 2 conforme a posicao caia no primeiro, segundo ou terceiro terco. */
    private static int third(int position, int size) {
        if (size <= 0) {
            return 1;
        }
        return Math.min(2, Math.max(0, position * 3 / size));
    }
}

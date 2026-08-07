Spell Cooldown HUD
==================

[English](README.md) · **Português (BR)**

Mod **client-side** para Minecraft 1.21.1 que mostra o cooldown das magias do
[Iron's Spells 'n Spellbooks](https://www.curseforge.com/minecraft/mc-mods/irons-spells-n-spellbooks)
numa HUD totalmente configurável — estilo, posição, cores, conteúdo e animações.

Dois pontos que o diferenciam da HUD padrão:

- **Mostra o cooldown real mesmo com o servidor lagado.** O Iron's Spells sincroniza o cooldown uma
  única vez e o cliente conta sozinho a 20/s, então num servidor com TPS baixo o número chega a zero
  antes da magia ficar pronta. Este mod corrige isso pelo relógio do servidor
  ([detalhes](#cooldown-real-em-servidor-lagado)).
- **Funciona com magias de addons** (`somakespells`, `gametechbcs_spellbooks`, ...) sem nenhum
  ajuste, ícones inclusive: tudo é resolvido pelo `SpellRegistry` do Iron's Spells.

Como não registra nenhum payload de rede, **dá para usar em servidores que não têm este mod
instalado** — o NeoForge nem o inclui na negociação de mods.

Idiomas
-------

Toda a interface é traduzida e segue o idioma do jogo automaticamente (o Minecraft carrega o
`.json` correspondente; nada a configurar):

- English (`en_us`)
- Português do Brasil (`pt_br`)
- Español — Espanha (`es_es`) e México (`es_mx`)
- Русский (`ru_ru`)

O Minecraft **não** faz fallback entre variantes regionais: quem joga numa variante de espanhol sem
arquivo próprio (ex. `es_ar`) cai no inglês, não em `es_es`. Para adicionar um idioma, copie
`en_us.json`, traduza os valores e salve como `<código>.json` — as chaves não mudam. A tradução em
russo ainda precisa de revisão de um falante nativo; correções são bem-vindas.

Requisitos
----------

| | |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.0 ou superior (compilado contra 21.1.241) |
| Iron's Spells 'n Spellbooks | 1.21.1-3.16.0 ou superior |
| Lado | Somente cliente — **não** instale no servidor |

Uso
---

- Tecla **K** abre a tela de mover a HUD: totalmente transparente, arraste com o mouse para onde
  quiser (remapeável em Opções > Controles).
- De lá, **Configurações** abre o editor completo. Também acessível por
  **Mods > Spell Cooldown HUD > Config**.
- Setas ajustam 1px, Shift+setas 10px. A HUD não sai da tela.

Configuração
------------

Tudo é editável no jogo, com preview ao vivo. O arquivo fica em
`config/spellcooldownhud-client.toml`.

**`[content]` — o que aparece**
`contentMode` (`ONLY_ON_COOLDOWN` \| `ALL_EQUIPPED`) · `sortMode` (`TIME_REMAINING_ASC` \|
`TIME_REMAINING_DESC` \| `SLOT_ORDER` \| `NAME`) · `maxEntries` · `hideWhenGuiHidden` ·
`useServerTime`

**`[layout]` — onde fica**
`anchor` (9 pontos) · `offsetX`/`offsetY` · `growDirection` (`RIGHT` \| `LEFT` \| `DOWN` \| `UP`) ·
`iconSize` · `spacing` · `maxPerLine`

**`[style]` — como cada entrada é desenhada**
`style` (`RADIAL` \| `BAR` \| `TEXT_LIST`) · `showIcon` · `showSpellName` · `showTimer` ·
`timerFormat` (`SECONDS` \| `TENTHS` \| `MM_SS`) · `showSpellLevel` · `drawBorder`

**`[colors]` — hex `#RRGGBB` ou `#AARRGGBB`**
`useSchoolColor` (tira a cor da escola da magia) · `backgroundColor` · `sweepColor` ·
`borderColor` · `textColor` · `readyFlashColor`

**`[effects]`**
`opacity` · `scale` · `fadeInTicks` · `fadeOutTicks` · `flashWhenReady` · `dimWhenOnCooldown`

> Com `showSpellLevel` ligado, ícones abaixo de ~18px não têm altura para o tempo e o nível sem
> encostarem (fonte de 9px, dois textos). O nível tem fundo escuro justamente para continuar
> legível nesse caso.

Cooldown real em servidor lagado
--------------------------------

O Iron's Spells sincroniza o cooldown **uma única vez**, quando ele começa (`SyncCooldownPacket`);
depois o cliente decrementa sozinho em `ClientPlayerEvents.onPlayerTick`. Não há resync periódico —
`PlayerCooldowns.syncToPlayer` só roda em login e respawn.

O cliente sempre roda a 20 ticks/s, mas o servidor roda a TPS reais. Num servidor a 10 TPS a
contagem do cliente corre o dobro da velocidade da do servidor: **o número chega a zero enquanto a
magia ainda está em cooldown de verdade**, e a HUD passa a mentir justamente quando mais importa.

A correção (`data/ServerSyncedSource`) grava, ao ver o cooldown pela primeira vez, em que *tempo de
jogo* ele termina — e daí em diante o restante é sempre `fim - gameTime`. Quando o cliente derruba a
entrada cedo demais, a HUD a mantém em tela até o servidor realmente terminar.

Isso é exato, não uma estimativa, porque o tempo de jogo avança 1 por tick de servidor (o mesmo
compasso do decremento do cooldown) e o servidor o corrige no cliente a cada 20 ticks via
`ClientboundSetTimePacket`. Nenhum mixin é necessário: `ClientLevel.getGameTime()` já é o relógio
autoritativo do servidor.

O TPS medido (`client/ServerClock`, derivado da mesma fonte) **não** entra nessa conta — serve só
para converter os ticks restantes em segundos na hora de exibir. O editor mostra o TPS medido no
rodapé quando ele cai abaixo de 19,5.

Desligável em `useServerTime`.

### Limitação conhecida: `/tick freeze`

`/tick rate` é tratado corretamente: o cliente é avisado da nova taxa, então tanto o tempo de jogo
quanto o decremento local desaceleram juntos e a correção vira um no-op. (Por isso `/tick rate`
**não serve para testar** a correção: ele é uma desaceleração sincronizada, e o bug só aparece
quando o servidor atrasa sem o cliente saber.)

`/tick freeze` não: ele congela o tempo de jogo mas **players continuam tickando**, então o cooldown
avança no servidor enquanto o nosso âncora fica parado — a HUD passa a mostrar mais tempo do que o
real. É um comando de debug, não uma condição de servidor em produção, e o erro é para o lado
conservador (tempo a mais, nunca a menos), então foi aceito em vez de tratado.

Arquitetura
-----------

A regra que sustenta o resto: **`data/IronSpellsSource` é a única classe que importa
`io.redspace.*`**. Ela traduz a API do Iron's Spells em `CooldownEntry`, um record que só usa tipos
do Minecraft.

Isso dá três coisas de uma vez: o editor roda um preview ao vivo com dados falsos
(`data/DemoSource`) sem estar em combate ou sequer num mundo; uma mudança de API do Iron's Spells
quebra a compilação num arquivo só; e dá para suportar outro mod de magia depois adicionando um
`CooldownSource`, sem tocar em renderer nenhum.

```
SpellCooldownHud            entry point, @Mod(dist = CLIENT)

config/                     ModConfigSpec + enums
    HudConfig, CachedColor, Anchor, GrowDirection,
    HudStyle, ContentMode, SortMode, TimerFormat

data/                       dados, sem tipos do Minecraft além do necessário
    CooldownEntry           record puro (id, ícone, nome, ticks, cor da escola)
    CooldownSource          interface
    IronSpellsSource        ÚNICA classe que fala com o Iron's Spells
    ServerSyncedSource      decorator: corrige o cooldown pelo relógio do servidor
    DemoSource              cooldowns falsos para o preview do editor

client/
    ClientEvents            tick do jogo: relógio, tracker, keybind
    ClientModEvents         registro da camada de GUI e das teclas
    ServerClock             tempo de jogo + TPS estimado, sem mixin
    CooldownTracker         estado de animação (fade, brilho ao ficar pronta)
    HudLayout               âncora + offset + direção -> coordenadas
    HudLayer                LayeredDraw.Layer, acima da hotbar
    Keybinds
    render/                 RenderSupport + RadialRenderer, BarRenderer, TextListRenderer

screen/
    HudMoveScreen           overlay transparente, arrasto livre
    HudEditorScreen         editor completo em abas, com preview
    HudDragController       arrasto/preview compartilhados pelas duas telas
    HudPreviewScreen        marca as telas que desenham o próprio preview
```

### Notas de implementação

Coisas descobertas na marra, registradas para não voltarem:

- **`Screen.render` chama `renderBackground` antes dos widgets**, e o padrão aplica o blur do
  vanilla mais o fundo de menu. Como as telas deste mod desenham o preview antes de chamar
  `super.render()`, isso caía *por cima* do preview. Ambas sobrescrevem `renderBackground` como
  no-op e desenham o próprio fundo na ordem que querem.
- **`GuiGraphics.blit` de 9 argumentos não escala** — usa os mesmos números para origem e destino.
  Para desenhar um ícone 16×16 em outro tamanho é preciso o overload de 11 argumentos.
- **O `GuiLayerManager` achata os grupos de camadas** e embrulha cada camada *vanilla* no teste de
  `hideGui`. Camadas de mod inseridas entre elas **não** herdam esse teste, então o F1 precisa ser
  verificado à mão (`HudLayer.shouldRender`).
- **A varredura radial projeta os vértices na borda do quadrado**, não de um círculo, para cobrir o
  ícone até os cantos sem vazar pelas laterais.
- **`neo_version` ≠ `neo_version_range`**: o primeiro é a versão de compilação, o segundo o mínimo
  exigido em runtime. Declarar a de compilação como mínimo faz o mod ser recusado em instâncias um
  pouco mais antigas da mesma linha 21.1.x.

Compilando
----------

Requer **JDK 21**. Para apenas compilar, é só clonar e rodar `./gradlew build` — a API do Iron's
Spells é resolvida do Modrinth (`maven.modrinth:irons-spells-n-spellbooks`), sem nenhum passo
manual.

```
./gradlew build             # compila e empacota em build/libs/
./gradlew runClient         # dev client com Iron's Spells carregado
./gradlew deployToInstance  # copia o jar para o mods/ de uma instância do CurseForge
```

Para apontar o `deployToInstance` para outra instância, defina `instance_mods_dir` no seu
`~/.gradle/gradle.properties` em vez de editar o `build.gradle`.

### `libs/` — só para o `runClient`

O dev client roda o Iron's Spells de verdade, e para isso precisa dos jars dele e das 5
dependências. Sem eles o `build` continua funcionando normalmente; só o `runClient` sobe sem magia
nenhuma.

`libs/*.jar` está no `.gitignore` — não redistribuímos mods de terceiros, e é por isso que a
compilação usa o Modrinth: um clone limpo (CI incluído) não teria nada local contra o que compilar.
Para popular, copie estes arquivos de uma instância do CurseForge (as versões precisam bater com as
de `gradle.properties`):

```
irons_spellbooks-1.21.1-3.16.2.jar
irons_lib-1.21.1-2.1.0.jar
geckolib-neoforge-1.21.1-4.8.3.jar
player-animation-lib-forge-2.0.4+1.21.1.jar
curios-neoforge-9.5.1+1.21.1.jar
```

E `libs/irons_spellbooks_at.cfg`: uma cópia **sanitizada** do access transformer que vem dentro do
jar do Iron's Spells, registrada no `build.gradle` via `neoForge.accessTransformers`. O ModDevGradle
aplica ATs no artefato do Minecraft em tempo de build, e o AT de um mod que está só no runtime
classpath não é aplicado sozinho — sem isso o Iron's Spells quebra com `IllegalAccessError` no boot.
Duas linhas do original foram removidas por usarem nomes SRG legados (`f_59605_`, `f_129744_`), que
não existem com mappings Mojang; regenere e refaça essa remoção ao atualizar o Iron's Spells. O AT
só é registrado quando `libs/` está completo, então um build limpo não quebra.

Contribuindo
------------

Issues e pull requests são bem-vindos. Para traduções, copie `en_us.json`, traduza os valores (as
chaves não mudam) e abra um PR — veja [Idiomas](#idiomas).

Licença
-------

[MIT](LICENSE). Os mappings da Mojang usados na compilação têm licença própria:
https://github.com/NeoForged/NeoForm/blob/main/Mojang.md

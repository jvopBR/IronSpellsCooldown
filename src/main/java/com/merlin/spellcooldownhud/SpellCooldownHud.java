package com.merlin.spellcooldownhud;

import com.merlin.spellcooldownhud.client.ClientModEvents;
import com.merlin.spellcooldownhud.config.HudConfig;
import com.merlin.spellcooldownhud.screen.HudEditorScreen;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
//? if <1.21 {
/*import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
*///?} else {
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
//?}
import org.slf4j.Logger;

/**
 * Cooldown HUD for Iron's Spells 'n Spellbooks.
 *
 * <p>Fully client-side: it reads the state Iron's Spells already syncs to the client
 * ({@code ClientMagicData}) and draws it. It registers no network payload, which keeps it out of
 * NeoForge's mod negotiation -- so you can use it on a server that doesn't have it installed, which
 * is exactly the use case.
 */
//? if <1.21 {
/*@Mod(SpellCooldownHud.MODID)
*///?} else {
@Mod(value = SpellCooldownHud.MODID, dist = Dist.CLIENT)
//?}
public final class SpellCooldownHud {

    public static final String MODID = "spellcooldownhud";

    public static final Logger LOGGER = LogUtils.getLogger();

    //? if <1.21 {
    /*public SpellCooldownHud() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, HudConfig.SPEC);

        modEventBus.addListener(ClientModEvents::onRegisterGuiLayers);
        modEventBus.addListener(ClientModEvents::onRegisterKeyMappings);

        // Mods > Spell Cooldown HUD > Config opens the editor with preview, not the generic config
        // screen: tuning position and colors from a list of values would be blind.
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((mc, parent) -> new HudEditorScreen(parent)));
    }
    *///?} else {
    public SpellCooldownHud(IEventBus modEventBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, HudConfig.SPEC);

        modEventBus.addListener(ClientModEvents::onRegisterGuiLayers);
        modEventBus.addListener(ClientModEvents::onRegisterKeyMappings);

        // Mods > Spell Cooldown HUD > Config opens the editor with preview, not NeoForge's generic
        // config screen: tuning position and colors from a list of values would be blind.
        container.registerExtensionPoint(IConfigScreenFactory.class,
                (modContainer, parent) -> new HudEditorScreen(parent));
    }
    //?}

    public static ResourceLocation id(String path) {
        //? if >=1.21 {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
        //?} else {
        /*return new ResourceLocation(MODID, path);
        *///?}
    }
}

package com.client2end.addon;

import com.client2end.addon.commands.CommandExample;
import com.client2end.addon.hud.HudExample;
import com.client2end.addon.modules.ChestCrash;
import com.client2end.addon.modules.ModuleExample;
import com.client2end.addon.modules.ModuleTestC2S;
import com.client2end.addon.modules.Spammer;
import com.client2end.addon.modules.TCPFlood;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public class Client2End extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();
    public static final Category CATEGORY = new Category("Client2End");
    public static final HudGroup HUD_GROUP = new HudGroup("Client2End");

    @Override
    public void onInitialize() {
        LOG.info("Initializing Meteor Client2End");

        Modules.get().add(new ModuleExample());
        Modules.get().add(new ModuleTestC2S());
        Modules.get().add(new ChestCrash());
        Modules.get().add(new Spammer());
        Modules.get().add(new TCPFlood());

        Commands.add(new CommandExample());

        Hud.get().register(HudExample.INFO);
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
    }

    @Override
    public String getPackage() {
        return "com.client2end.addon";
    }

    @Override
    public GithubRepo getRepo() {
        return new GithubRepo("MeteorDevelopment", "meteor-addon-template");
    }
}

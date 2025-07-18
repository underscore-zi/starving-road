package com.example.us.ri0.starving;

import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.slf4j.Logger;
import us.ri0.starving.modules.Pather;

public class AddonTemplate extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();
    public static final Category CATEGORY = new Category("Starving Road Coalition");

    @Override
    public void onInitialize() {
        Modules.get().add(new Pather());
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
    }

    @Override
    public String getPackage() {
        return "us.ri0.starving";
    }

    @Override
    public GithubRepo getRepo() {
        return new GithubRepo("underscore-zi", "starving-road");
    }
}

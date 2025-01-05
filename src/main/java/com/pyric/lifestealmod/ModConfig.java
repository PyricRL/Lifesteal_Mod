package com.pyric.lifestealmod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import com.terraformersmc.modmenu.util.mod.Mod;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

public class ModConfig{
    private static final Path CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("hardcover.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static ModConfig config;

    public static final int defaultMaxHeartCap = 20;
    public static final int defaultMinHeartCap = 7;
    public static final int defaultHeartIncrease = 1;
    public static final int defaultHeartDecrease = 1;
    public static final boolean defaultHeartRegen = true;
    public static final int defaultHeartRegenAmount = 10;
    public static final int defaultHeartRegenTime = 30;
    public static final boolean defaultHeartWithdraw = true;

    public int maxHeartCap;
    public int minHeartCap;
    public int heartIncrease;
    public int heartDecrease;
    public boolean heartRegen;
    public int heartRegenAmount;
    public int heartRegenTime;
    public boolean heartWithdraw;

    /**
     * ModConfig() sets the values of the ModConfig to the default values.
     */
    public ModConfig() {
        this.maxHeartCap = defaultMaxHeartCap;
        this.minHeartCap = defaultMinHeartCap;
        this.heartIncrease = defaultHeartIncrease;
        this.heartDecrease = defaultHeartDecrease;
        this.heartRegen = defaultHeartRegen;
        this.heartRegenAmount = defaultHeartRegenAmount;
        this.heartRegenTime = defaultHeartRegenTime;
        this.heartWithdraw = defaultHeartWithdraw;
    }

    /**
     * instance() creates an instance of the ModConfig if it is not there, and returns the instance.
     */
    public static ModConfig instance() {
        return config == null ? config = load() : config;
    }

    /**
     *  load() loads the config settings from a json file using GSON.
     */
    private static ModConfig load() {
        if (Files.exists(CONFIG_FILE)) {
            try (var reader = Files.newBufferedReader(CONFIG_FILE)) {
                return GSON.fromJson(reader, ModConfig.class);
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        }
        return new ModConfig();
    }

    /**
     * save() saves the config settings to a json file using GSON.
     */
    public void save() {
        try (var writer = Files.newBufferedWriter(CONFIG_FILE)) {
            GSON.toJson(config, writer);
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }
}
package com.pyric.lifestealmod;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class LifestealModConfigScreen {

    /**
     * create() creates the screen for the Mod Menu, adds a title to it, and adds all the configuration settings to it.
     */
    public static Screen create(Screen parent) {

        // create the config screen with a title "Lifesteal Options"
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.translatable("Lifesteal Options"));

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        // creates tabs for each section of the config screen
        ConfigCategory general = builder.getOrCreateCategory(Text.translatable("General Options"));
        ConfigCategory extra = builder.getOrCreateCategory(Text.translatable("Extra Options"));

        // adds an option for "Maximum Health Capacity"
        general.addEntry(entryBuilder
                .startIntField(Text.translatable("Maximum Health Capacity"), ModConfig.instance().maxHeartCap)
                .setDefaultValue(ModConfig.defaultMaxHeartCap)
                .setTooltip(Text.translatable("Sets the maximum hearts a player can have"))
                .setSaveConsumer(newValue -> ModConfig.instance().maxHeartCap = newValue)
                .build());

        // adds an option for "Minimum Health Capacity"
        general.addEntry(entryBuilder
                .startIntField(Text.translatable("Minimum Health Capacity"), ModConfig.instance().minHeartCap)
                .setDefaultValue(ModConfig.defaultMinHeartCap)
                .setTooltip(Text.translatable("Sets the minimum hearts a player can have"))
                .setSaveConsumer(newValue -> ModConfig.instance().minHeartCap = newValue)
                .build());

        // adds an option for "Heart Increase Amount"
        general.addEntry(entryBuilder
                .startIntField(Text.translatable("Heart Increase Amount"), ModConfig.instance().heartIncrease)
                .setDefaultValue(ModConfig.defaultHeartIncrease)
                .setTooltip(Text.translatable("Sets how many hearts to increase the player by each kill"))
                .setSaveConsumer(newValue -> ModConfig.instance().heartIncrease = newValue)
                .build());

        // adds an option for "Heart Decrease Amount"
        general.addEntry(entryBuilder
                .startIntField(Text.translatable("Heart Decrease Amount"), ModConfig.instance().heartDecrease)
                .setDefaultValue(ModConfig.defaultHeartDecrease)
                .setTooltip(Text.translatable("Sets how many hearts to increase the player by each kill"))
                .setSaveConsumer(newValue -> ModConfig.instance().heartDecrease = newValue)
                .build());

        // adds an option for "Allow Heart Withdraw"
        extra.addEntry(entryBuilder
                .startBooleanToggle(Text.translatable("Allow Heart Withdraw"), ModConfig.instance().heartWithdraw)
                .setDefaultValue(ModConfig.defaultHeartWithdraw)
                .setTooltip(Text.translatable("Sets whether heart crafting is allowed"))
                .setSaveConsumer(newValue -> ModConfig.instance().heartWithdraw = newValue)
                .build());

        // adds an option for "Allow Heart Regeneration"
        extra.addEntry(entryBuilder
                .startBooleanToggle(Text.translatable("Allow Heart Regeneration"), ModConfig.instance().heartRegen)
                .setDefaultValue(ModConfig.defaultHeartRegen)
                .setTooltip(Text.translatable("Sets whether heart regeneration is allowed"))
                .setSaveConsumer(newValue -> ModConfig.instance().heartRegen = newValue)
                .build());

        // adds an option for "Heart Regeneration Amount"
        extra.addEntry(entryBuilder
                .startIntField(Text.translatable("Heart Regeneration Amount"), ModConfig.instance().heartRegenAmount)
                .setDefaultValue(ModConfig.defaultHeartRegenAmount)
                .setTooltip(Text.translatable("Sets how many hearts a player can regenerate to"))
                .setSaveConsumer(newValue -> ModConfig.instance().heartRegenAmount = newValue)
                .build());

        // adds an option for "Heart Regeneration Amount"
        extra.addEntry(entryBuilder
                .startIntField(Text.translatable("Heart Regeneration Time"), ModConfig.instance().heartRegenTime)
                .setDefaultValue(ModConfig.defaultHeartRegenTime)
                .setTooltip(Text.translatable("Sets how long it takes for a player to regen a single heart in minutes"))
                .setSaveConsumer(newValue -> ModConfig.instance().heartRegenTime = newValue)
                .build());

        // sets the Mod Config to call save() when the screen is exited
        builder.setSavingRunnable(() -> {
            ModConfig.instance().save();
        });

        return builder.build();
    }
}
